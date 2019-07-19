(ns sicp-logic.core
  (:require [sicp-logic.binding :refer [instantiate]]
            [sicp-logic.db :as db]
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
  (let [processed-q (query-syntax-process q)]
    `(map (fn [frame#]
            (instantiate (quote ~processed-q)
                         frame#
                         (fn [v# f#] (contract-question-mark v#))))
          (qeval ~db (quote ~processed-q) [{}]))))

(defn assert! [db assertion]
  "Adds a new assertion to the database."
  (db/add-assertion db assertion))

(defn add-rule! [db rule]
  "Adds a new rule to the database."
  (db/add-rule db (query-syntax-process rule)))

(defmacro defrule [db conclusion body]
  "Convenience macro to add a new rule to the database.

   Usage:
       (defrule [grandparent ?x ?y]
         (and [parent ?x ?z]
              [parent ?z ?y]))"
  `(add-rule! ~db (quote [conclusion body])))
