(ns sicp-logic.rules
  (:require [sicp-logic.binding :refer [var?]]
            [sicp-logic.db :as db]
            [sicp-logic.evaluator :refer [qeval]]
            [sicp-logic.match :refer [unify-match]]))

(defn rename-variables-in [rule]
  "Gives all the variables in the rule globally unique names
   to prevent name collisions during unification."
  (let [bindings (atom {})
        rename-var (fn [var]
                     (let [var-name (second var)
                           binding (get @bindings var-name)]
                       (if binding
                         binding
                         (let [new-binding (gensym var-name)]
                           (swap! bindings (fn [m] (assoc m var-name new-binding)))
                           new-binding))))
        rename-vars (fn rename-vars [exp]
                      (cond
                        (var? exp) (rename-var exp)
                        (and (sequential? exp) (not (empty? exp)))
                        (cons (rename-vars (first exp))
                              (rename-vars (rest exp)))
                        :else exp))]
    (rename-vars rule)))


(defn conclusion [rule]
  "Selects the rule's conclusion")

(defn rule-body [rule]
  "Selects the rule's body")

(defn apply-a-rule [rule query frame]
  "Applies the `rule` to the `query` in the
   `frame` by unifying the query with the rule to
   produce a new frame then evaluating the body
   of the rule in that new frame."
  (let [clean-rule (rename-variables-in rule)
        unify-result (unify-match query
                                  (conclusion clean-rule)
                                  frame)]
    (if (= unify-result :failed)
      []
      (qeval (rule-body clean-rule)
             [unify-result]))))

(defn fetch-rules [db query frame]
  (db/fetch-rules db query frame))

(defn apply-rules [db query frame]
  (mapcat
   (fn [rule]
     (apply-a-rule rule query frame))
   (fetch-rules db query frame)))
