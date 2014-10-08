(ns sneer.conversation
  (:require
   [rx.lang.clojure.core :as rx]
   [sneer.rx :refer [atom->observable]]
   [sneer.party :refer [party-puk]]
   [sneer.commons :refer [now]])
  (:import
   [sneer PublicKey Party Conversation Message]
   [sneer.rx ObservedSubject]
   [sneer.tuples Tuple TupleSpace]
   [java.text SimpleDateFormat]))

(def simple-date-format (SimpleDateFormat. "HH:mm"))

(defn format-date [time] (.format simple-date-format time))

(defn tuple->message [own-puk ^Tuple tuple]
  (let [created (.timestampCreated tuple)
        type (.type tuple)
        label (.get tuple "label")
        content (if (= type "message") (.payload tuple) (if label label type))
        own? (= own-puk (.author tuple))]
    
    (reify Message
      (isOwn [this] own?)
      (content [this] content)
      (timestampCreated [this] created)
      (timestampReceived [this] 0)
      (timeCreated [this] (format-date created))
      (tuple [this] tuple))))

(defn values-to-compare [msg] [(.timestampCreated msg) (.content msg)])
(def message-comparator (fn [m1 m2] (compare (values-to-compare m1) (values-to-compare m2))))

(def unread-message-counter (atom 0))

(defn reify-conversation [^TupleSpace tuple-space ^rx.Observable conversation-menu-items ^PublicKey own-puk ^Party party]
  (let [^PublicKey party-puk (party-puk party)
        messages (atom (sorted-set-by message-comparator))
        observable-messages (rx/map vec (atom->observable messages))
        message-filter (.. tuple-space filter (field "conversation?" true))]
    
    (rx/subscribe
      (rx/subscribe-on
        (rx.schedulers.Schedulers/io)
        (rx/merge
          (.. message-filter (author own-puk) (audience party-puk) tuples)
          (.. message-filter (author party-puk) (audience own-puk) tuples)))
      (fn [tuple]
        (swap! messages conj (tuple->message own-puk tuple))        
        (swap! unread-message-counter inc)
        (println "unread: " @unread-message-counter)))
    
    (reify
      Conversation      
      (party [this] party)
      
      (messages [this]
        observable-messages)
      
      (sendMessage [this content]
        (..
          tuple-space
          publisher
          (field "conversation?" true)
          (audience party-puk)
          (type "message")
          (pub content)))
      
      (mostRecentMessageContent [this]
        (.observed (ObservedSubject/create "hello")))
      
      (mostRecentMessageTimestamp [this]
        (.observed (ObservedSubject/create (now))))
    
      (menu [this] conversation-menu-items)
    
      (unreadMessageCount [this]
        (atom->observable unread-message-counter))
    
      (unreadMessageCountReset [this]
        (swap! unread-message-counter (fn [_] 0))
        ;(reset! unread-message-counter 0)
        (println "reset: " @unread-message-counter)))))

(defn produce-conversation [tuple-space conversation-menu-items own-puk party]
  (reify-conversation tuple-space (.asObservable conversation-menu-items) own-puk party))
