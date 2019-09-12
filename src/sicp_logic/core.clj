(ns sicp-logic.core
  (:require [sicp-logic.binding :refer [instantiate var?]]
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

(defn sanitize-frame [q frame]
  "Fully resolves all variables in q and returns a map
   of the variable names to their bindings"
  (letfn [(vars [acc node]
            (cond
              (var? node) (conj acc [(second node) node])
              (and (sequential? node) (not (empty? node)))
              (concat
               (vars acc (first node))
               (vars acc (rest node)))))]
    (let [qvars (vars [] q)]
      (into {} (map vec (instantiate qvars frame (fn [v f] v)))))))

(defn query-results [db q]
  "Queries the database for assertions that match the query."
  (let [processed-q (query-syntax-process q)]
    (map (fn [frame]
           (sanitize-frame processed-q frame))
         (qeval db processed-q [{}]))))

(defn instantiate-query [q frames]
  "Fills in the query with variables from frames"
  (let [processed-q (query-syntax-process q)]
    (map (fn [frame]
           (instantiate processed-q
                        frame
                        (fn [v f] (contract-question-mark v))))
         frames)))

(defn query* [db q]
  (instantiate-query q
    (query-results db q)))

(defmacro query [db q]
  "Convenience macro to query the database for assertions
   that match the query."
  `(query* ~db (quote ~q)))

(defn assert! [db assertion]
  "Adds a new assertion to the database."
  (db/add-assertion db assertion))

(defn add-rule! [db rule]
  "Adds a new rule to the database."
  (db/add-rule db (query-syntax-process rule)))

(defmacro defrule!
  "Convenience macro to add a new rule to the database.

   Usage example:
       (defrule [grandparent ?x ?y]
         (and [parent ?x ?z]
              [parent ?z ?y]))"
  ([db conclusion]
   `(add-rule! ~db (quote [~conclusion])))
  ([db conclusion body]
   `(add-rule! ~db (quote [~conclusion ~body]))))
