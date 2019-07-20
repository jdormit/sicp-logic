(ns sicp-logic.db.memory
  (:require [sicp-logic.binding :refer [instantiate var?]]
            [sicp-logic.db :refer [FactDB]]
            [sicp-logic.evaluator :refer [conclusion]]))

(defn get-indexed-assertions [db query]
  (get @(:assertion-index db) (first query)))

(defn get-all-assertions [db]
  @(:assertion-store db))

(defn use-assertion-index? [query]
  (not (var? (first query))))

(defn index-assertion! [db assertion]
  (swap!
   (:assertion-index db)
   (fn [index]
     (let [index-value (or (get index (first assertion)) [])]
       (assoc index (first assertion) (conj index-value assertion))))))

(defn store-assertion! [db assertion]
  (swap!
   (:assertion-store db)
   (fn [store]
     (conj store assertion))))

(defn get-indexed-rules [db query]
  (concat
   (get @(:rule-index db) (first query))
   (get @(:rule-index db) '?)))

(defn index-rule! [db rule]
  (swap!
   (:rule-index db)
   (fn [index]
     (let [index-key (if (var? (first (conclusion rule)))
                       '?
                       (first (conclusion rule)))
           index-value (or (get index index-key) [])]
       (assoc index index-key (conj index-value rule))))))

(defrecord InMemoryDB [assertion-index rule-index assertion-store rule-store]
  FactDB
  (fetch-assertions [db query frame]
    (let [instantiated (instantiate query frame (fn [v f] v))]
      (if (use-asserttion-index? query)
        (get-indexed-assertions db query)
        (get-all-assertions db))))
  (add-assertion [db assertion]
    (index-assertion! db assertion))
  (fetch-rules [db query frame]
    (get-indexed-rules db query))
  (add-rule [db rule]
    (index-rule! db rule)))

(defn new-db []
  (->InMemoryDB (atom {}) (atom {}) (atom []) (atom [])))
