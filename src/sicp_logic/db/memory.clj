(ns sicp-logic.db.memory
  (:require [sicp-logic.binding :refer [instantiate var?]]
            [sicp-logic.db :refer [FactDB]]))

(defn use-index? [query]
  (not (var? (first query))))

(defn get-indexed-assertions [db query]
  (get (deref (:index db)) (first query)))

(defn get-all-assertions [db]
  (deref (:store db)))

(defn indexable? [assertion]
  (not (var? (first assertion))))

(defn index-assertion! [db assertion]
  (swap!
   (:index db)
   (fn [index]
     (let [index-value (or (get index (first assertion)) [])]
       (assoc index (first assertion) (conj index-value assertion))))))


(defn store! [db assertion]
  (swap! (:store db) (fn [assertions] (conj assertions assertion))))

(defrecord InMemoryDB [index store]
  FactDB
  (fetch-assertions [db query frame]
    (let [instantiated (instantiate query frame (fn [v f] v))]
      (if (use-index? query)
       (get-indexed-assertions db query)
       (get-all-assertions db))))
  (add-assertion [db assertion]
    (when (indexable? assertion)
      (index-assertion! db assertion))
    (store! db assertion)))

(defn new-db []
  (->InMemoryDB (atom {}) (atom [])))
