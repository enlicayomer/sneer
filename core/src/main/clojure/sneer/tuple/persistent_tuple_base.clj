(ns sneer.tuple.persistent-tuple-base
  (:import [sneer.crypto.impl KeysImpl])
  (:require [sneer.core :as core]
            [sneer.async :refer [dropping-chan go-while-let dropping-tap]]
            [clojure.core.async :refer [go-loop <! >! >!! mult tap chan close! go]]
            [sneer.rx :refer [filter-by seq->observable]]
            [sneer.serialization :as serialization]
            [rx.lang.clojure.core :as rx]
            [rx.lang.clojure.interop :as rx-interop]))

(defprotocol TupleBase
  "A backing store for tuples (represented as maps)."

  (store-tuple ^Void [this tuple]
    "Stores the tuple represented as map.")

  (query-tuples
    [this criteria tuples-out]
    [this criteria tuples-out lease]
    "Filters tuples by the criteria represented as a map of
     field/value. When a lease channel is passed the result
     channel will keep receiving new tuples until the lease
     emits a value.")
  
  (restarted ^TupleBase [this]))

(defprotocol Database
  (db-create-table [this table columns])
  (db-create-index [this table index-name column-names])
  (db-insert [this table row])
  (db-query [this sql-and-params]))

(extend-protocol Database
  sneer.admin.Database
  (db-create-table [this table columns]
    (.createTable this (name table) columns))
  (db-create-index [this table index-name column-names]
    (.createIndex this (name table) (name index-name) (mapv name column-names)))
  (db-insert [this table row]
    (.insert this (name table) row))
  (db-query [this sql-and-params]
    (.query this (first sql-and-params) (subvec sql-and-params 1))))

(defn- create-tuple-table [db]
  (db-create-table
    db :tuple
    [[:id :integer "PRIMARY KEY AUTOINCREMENT"]
     [:type :varchar "NOT NULL"]
     [:payload :blob]
     [:timestamp :timestamp "NOT NULL" "DEFAULT CURRENT_TIMESTAMP"]
     [:author :blob "NOT NULL"]
     [:audience :blob]
     ;[:device :blob "NOT NULL"]
     ;[:sequence :integer "NOT NULL"]
     ;[:signature :blob "NOT NULL"]
     [:custom :blob]]))

(defn- create-tuple-indices [db]
  (db-create-index db :tuple "idx_tuple_type" [:type]))

(defn- create-prik-table [db]
  (db-create-table
    db :keys
    [[:prik :blob]]))

(def builtin-field? #{"type" "payload" "author" "audience"})

(def puk-serializer 
  (let [keys-impl (KeysImpl.)]
    {:serialize #(.toBytes ^sneer.PublicKey %)
     :deserialize #(.createPublicKey keys-impl ^bytes %)}))

(def core-serializer {:serialize serialization/serialize
                      :deserialize serialization/deserialize})

(def serializers {"author" puk-serializer
                  "audience" puk-serializer
                  "payload" core-serializer
                  "custom" core-serializer})

(defn apply-serializer [op row field serializer]
  (let [v (get row field)]
    (if (nil? v)
      row
      (assoc row field ((op serializer) v)))))

(def serialize-entry (partial apply-serializer :serialize))

(def deserialize-entry (partial apply-serializer :deserialize))

(defn apply-serializers [f row]
  (reduce-kv f row serializers))

(defn serialize-entries [row]
  (apply-serializers serialize-entry row))

(defn deserialize-entries [row]
  (apply-serializers deserialize-entry row))

(defn ->custom-field-map [tuple]
  (reduce-kv
    (fn [map k v]
      (if (or (builtin-field? k) (nil? v))
        map
        (assoc map k v)))
    nil
    tuple))

(defn query-all [db]
  (db-query db ["SELECT * FROM tuple"]))

(defn dump-tuples [db]
  (->> (query-all db) (map println) doall))

(defn query-for [criteria]
  (if-let [type (criteria "type")]
    ["SELECT * FROM tuple WHERE type = ?" type]
    ["SELECT * FROM tuple"]))

(defn query-tuples-from-db [db criteria]
  (let [rs (db-query db (query-for criteria))
        field-names (mapv name (first rs))]
    (->>
      (next rs)
      (map #(deserialize-entries (zipmap field-names %)))
      (map #(merge (get % "custom") (dissoc % "custom"))))))

(defn insert-tuple [db tuple]
  (let [custom (->custom-field-map tuple)
        row (select-keys tuple builtin-field?)]
    (db-insert db :tuple (serialize-entries (assoc row "custom" custom)))))

(defmacro rx-defer [& body]
  `(rx.Observable/defer
     (rx-interop/fn [] ~@body)))

(defn idempotently [creation-fn]
  (try
    (creation-fn)
    (catch Exception e
      (when-not (-> e .getMessage (.contains "already exists"))
        (throw e)))))

(defn setup [db]
  (idempotently #(create-tuple-table db))
  (idempotently #(create-prik-table db))
  (idempotently #(create-tuple-indices db)))

(defn create [db]
  (let [new-tuples (dropping-chan)
        new-tuples-mult (mult new-tuples)]

    (setup db)

    (reify TupleBase

      (store-tuple [this tuple]
        (insert-tuple db tuple)
        ;(dump-tuples db)
        (>!! new-tuples tuple))

      (query-tuples [this criteria tuples-out lease]
        (let [new-tuples (dropping-tap new-tuples-mult)]
          (go (<! lease) (close! new-tuples))
          (go-loop []
            (doseq [tuple (query-tuples-from-db db criteria)]
              (>! tuples-out tuple))
            ;(>! tuples-out :wait-marker)
            (when (<! new-tuples)
              (recur)))))

      (restarted [this]
        (create db)))))

