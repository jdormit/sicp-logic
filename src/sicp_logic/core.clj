(ns sicp-logic.core
  (:require [sicp-logic.binding :refer [instantiate]]
            [sicp-logic.db :refer [add-assertion]]
            [sicp-logic.evaluator :refer [qeval]]))

(defn contract-question-mark [v]
  (symbol
   (str "?"
        (second v))))

(defn map-over-symbols [proc exp]
  (cond
    (and (sequential? exp) (not (empty? exp)))
    (cons (map-over-symbols proc (first exp))
          (map-over-symbols proc (rest exp)))
    (symbol? exp) (proc exp)
    :else exp))

(defn expand-question-mark [sym]
  (let [chars (str sym)]
    (if (= "?" (subs chars 0 1))
      ['? (symbol (subs chars 1))]
      sym)))

(defn query-syntax-process [q]
  (map-over-symbols #'expand-question-mark q))

(defmacro query [db q]
  "Queries the database for assertions that match the query."
  (letfn [(process-frame [q frame]
            (instantiate q frame (fn [v f] (contract-question-mark v))))]
    `(map ~process-frame
          (qeval ~db (query-syntax-process (quote ~q)) [{}]))))

(defn assert! [db assertion]
  "Adds a new assertion to the database."
  (add-assertion db assertion))

(defmacro defrule []
  "Adds a new rule to the database.")
