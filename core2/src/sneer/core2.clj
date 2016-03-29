(ns sneer.core2
  (require [sneer.util :refer [handle prepend]]
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

(defmethod handle :contact-new [state event]
  (let [nick (event :nick)
        id (event :id)
        contact {:contact-id id
                 :nick       nick}]
    (-> state
      (update-in [:contacts :id->contact] assoc id contact))))

(defmethod handle :contact-delete [state event]
  (update-in state [:contacts :id->contact] dissoc (:contact-id event)))

(defmethod handle :contact-rename [state event]
  (let [id (:contact-id event)
        new-nick (:new-nick event)]
    (assoc-in state [:contacts :id->contact id :nick] new-nick)))

(defn convo-list [model]
  (->> model :contacts :id->contact vals (sort-by :contact-id) reverse vec))

(defn- view [model [activity convo-id]]
  (cond-> {:convo-list (convo-list model)}
    (= activity :convo)
    (assoc :convo {:nick (get-in model [:contacts :id->contact convo-id :nick])
                   :chat []})))

(defn- update-ui [sneer]
  (let [model (catch-up! (sneer :streems) handle)]
    ((sneer :ui-fn) (view model @(sneer :view-path)))))

(defn handle! [sneer event]
  (if (= (event :type) :view)
    (reset! (sneer :view-path) (event :path))
    (append! (sneer :streems) event))
  (update-ui sneer))

(defn sneer [ui-fn streems]
  (doto
    {:ui-fn     ui-fn
     :streems   streems
     :view-path (atom nil)}
    (update-ui)))