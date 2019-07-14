(ns sicp-logic.assertions
  (:require [sicp-logic.db :as db]
            [sicp-logic.match :refer [pattern-match]]))

(defn fetch-assertions [db query frame]
  (db/fetch-assertions db query frame))

(defn check-an-assertion [assertion query frame]
  (let [match-result (pattern-match query assertion frame)]
    (if (= match-result :failed)
      []
      [match-result])))

(defn find-assertions [db query frame]
  (mapcat
   (fn [assertion]
     (check-an-assertion assertion query frame))
   (fetch-assertions db query frame)))
