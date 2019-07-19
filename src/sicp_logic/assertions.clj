(ns sicp-logic.assertions
  (:require [sicp-logic.db :refer [fetch-assertions]]
            [sicp-logic.match :refer [unify-match]]))

(defn check-an-assertion [assertion query frame]
  (let [match-result (unify-match query assertion frame)]
    (if (= match-result :failed)
      []
      [match-result])))

(defn find-assertions [db query frame]
  (mapcat
   (fn [assertion]
     (check-an-assertion assertion query frame))
   (fetch-assertions db query frame)))
