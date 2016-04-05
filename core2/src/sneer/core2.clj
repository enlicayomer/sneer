(ns sneer.core2
  (require
    [sneer.invite :as invite]
    [sneer.util.core :refer [handle prepend assoc-some]]
    [sneer.streem :refer :all]))

#_(defn- message-sim [n]
  {:id     (+ 10000 n)
   :text   (str "Hi There! " n)
   :date   (str "Today " n)
   :is-own (zero? (mod n 3))})

#_(defn- convo-sim [n]
  {:id       (+ 1000 n)
   :nickname (str "Neide " n)
   :preview  (str "Hi There! " n)
   :date     (str "Today " n)
   :unread   (get unreads (mod n 3))})

#_(defn- convos-view-sim [count]
  {:view :convos
   :convo-list (convo-sims count)})

#_(defn- convo-view-sim [count]
  {:view :convo
   :id 1042
   :tab :chat
   :message-list (message-sims count)})

(defmethod handle :own-name-set [state event]
  (let [own-name (event :own-name)]
    (assoc-in state [:profile :own-name] own-name)))



(defn summary-append [state summary]
  (let [id (summary :contact-id)
        summary (assoc summary :last-event-id id)]
    (update-in state [:convos :id->summary] assoc id summary)))

(defn puk2 [state]
  (get-in state [:key-pair :puk]))

(defn- own-name [state]
  (get-in state [:profile :own-name]))

(defn- invite [state random-long]
  (invite/encode {:puk   (puk2 state)
                  :name  (own-name state)
                  :nonce random-long}))

(defmethod handle :contact-new [state event]
  (summary-append state
    {:contact-id (event :id)
     :nick       (event :nick)
     :invite     (invite state (event :random-bytes))}))

(defmethod handle :contact-delete [state event]
  (update-in state [:convos :id->summary] dissoc (:contact-id event)))

(defmethod handle :contact-rename [state event]
  (let [id (:contact-id event)
        new-nick (:new-nick event)]
    (assoc-in state [:convos :id->summary id :nick] new-nick)))

(defn- invite->contact-id [state invite]
  (->> state
    :convos
    :id->summary
    vals
    (some #(-> % :invite (= invite)))
    :contact-id))

(defmethod handle :contact-invite-accept [state event]
  (let [invite (invite/decode (event :invite))]
    (-> state
      (summary-append {:contact-id (event :id)
                       :nick       (invite :name)
                       :puk        (invite :puk)})
      ))

  ; THE FOLLOWING MUST HAPPEN IN THE SENDER:
  #_(let [invite (:invite event)
        contact-id (invite->contact-id state invite)]
    (update-in state [:convos :id->summary contact-id] dissoc :invite)))

(defmethod handle :msg-send [state event]
  (let [contact-id (event :contact-id)]
    (-> state
      (assoc-in [:convos :id->summary contact-id :preview]       (event :text))
      (assoc-in [:convos :id->summary contact-id :last-event-id] (event :id)))))

(defmethod handle :keys-init [state event]
  (assoc state :key-pair (select-keys event [:prik :puk])))

(defn- convo-list [model]
  (->> model :convos :id->summary vals (sort-by :last-event-id) reverse vec))

(defn- chat [streems contact-id]
  (catch-up! streems conj [] contact-id))

(defn- convo [streems model contact-id]
  (-> model
    (get-in [:convos :id->summary contact-id])
    (select-keys [:contact-id :nick :invite])
    (assoc :chat (chat streems contact-id))))

(defn- view [streems model [activity contact-id]]
  (cond-> {:convo-list (convo-list model)
           :profile (:profile model)}
    (= activity :convo)
    (assoc :convo (convo streems model contact-id))))

(defn- update-ui! [sneer model]
  ((sneer :ui-fn) (view (sneer :streems) model @(sneer :view-path))))

(defn- streem-id [event]
  (case (event :type)
    :msg-send (event :contact-id)
    nil))

(defn- update-network! [sneer model]

  )

(defn- catch-up-model! [streems]
  (catch-up! streems handle))

(defn- model! [sneer]
  (catch-up-model! (sneer :streems)))

(defn- random-bytes [sneer array-size]
  ((-> sneer :crypto-fns :generate-random-bytes) array-size))

(defn- deterministic!
  "Adds information such as timestamp and random bytes when necessary."
  [sneer event]
  (if (-> event :type (= :contact-new))
    (assoc event :random-bytes (random-bytes sneer 8))
    event))

(defn handle! [sneer event]
  (let [event (deterministic! sneer event)
        streems (sneer :streems)]
    (if (= (event :type) :view)
      (reset! (sneer :view-path) (event :path))
      (append! streems event (streem-id event)))
    (let [model (catch-up-model! streems)]
      (update-network! sneer model)
      (update-ui! sneer model))))

(defn- keys-init-if-necessary [sneer model]
  (when-not (:key-pair model)
    (let [key-pair ((get-in sneer [:crypto-fns :generate-key-pair]))]
      (handle! sneer {:type :keys-init
                      :puk  (key-pair "puk")
                      :prik (key-pair "prik")}))))

(defn puk [sneer]
  (-> sneer model! puk2))

(defn sneer [streems ui-fn server> crypto-fns]
  (let [sneer {:streems    streems
               :ui-fn      ui-fn
               :server>    server>
               :crypto-fns crypto-fns
               :view-path  (atom nil)}
        model (model! sneer)]
    (keys-init-if-necessary sneer model)
    (update-ui! sneer model)
    sneer))
