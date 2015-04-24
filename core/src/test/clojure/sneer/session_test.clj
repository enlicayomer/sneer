(ns sneer.session-test
  (:require [midje.sweet :refer :all]
            [sneer.tuple.jdbc-database :refer [create-sqlite-db]]
            [sneer.tuple-base-provider :refer :all]
            [sneer.rx :refer [subscribe-on-io]]
            [sneer.test-util :refer [<!!? observable->chan]]
            [sneer.admin :refer [new-sneer-admin]]
            [sneer.keys :refer [->puk]]
            [sneer.tuple.persistent-tuple-base :as tuple-base])
  (:import (sneer.crypto.impl KeysImpl)))

(defn- ->chan [obs]
  (observable->chan (subscribe-on-io obs)))

(facts "About Sessions"
  (with-open [db (create-sqlite-db)
              base (tuple-base/create db)
              neide-admin (new-sneer-admin (.createPrivateKey (KeysImpl.)) base)
              maico-admin (new-sneer-admin (.createPrivateKey (KeysImpl.)) base)]
    (let [neide-puk (.. neide-admin privateKey publicKey)
          maico-puk (.. maico-admin privateKey publicKey)
          neide-sneer (.sneer neide-admin)
          maico-sneer (.sneer maico-admin)
          neide (.produceContact maico-sneer "neide" (.produceParty maico-sneer neide-puk) nil)
          maico (.produceContact neide-sneer "maico" (.produceParty neide-sneer maico-puk) nil)
          n->m (.. neide-sneer conversations (withContact maico))
          m->n (.. maico-sneer conversations (withContact neide))]

      (fact "Communication is working"
            (.sendMessage n->m "Hello")
            (let [messages (->chan (.messages m->n))]
              (<!!? messages 500)                               ; Skip history replay :(
              (.size (<!!? messages 500)) => 1))

      (fact "Custom field can be used in filter"
            (let [space (.tupleSpace neide-sneer)
                  _ (.. space publisher (type "ourtype") (field "banana" 42) pub)
                  _ (.. space publisher (type "ourtype") (field "banana" 43) pub)
                  filter (->chan (.. space filter (field "banana" 43) tuples))
                  tuple (<!!? filter)]
              (get tuple "type") => "ourtype"
              (get tuple "banana") => 43))

      (fact "Neide sees her own messages in the session"
            (let [session (<!!? (->chan (.startSession n->m)))
                  messages (->chan (.messages session))]
              (.send session "some payload")
              (.payload (<!!? messages)) => "some payload"
              )))))
