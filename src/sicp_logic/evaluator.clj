(ns sicp-logic.evaluator
  (:require [sicp-logic.assertions :refer [find-assertions]]
            [sicp-logic.binding :refer [instantiate]]
            [sicp-logic.db :refer [fetch-rules]]
            [sicp-logic.match :refer [unify-match]]))

(declare qeval)

(defn conjoin [db conjuncts input-frames]
  (if (empty? conjuncts)
    input-frames
    (conjoin db
             (rest conjuncts)
             (qeval db (first conjuncts) input-frames))))

(defn disjoin [db disjuncts input-frames]
  (if (empty? disjuncts)
    nil
    (concat (qeval db (first disjuncts) input-frames)
            (disjoin db (rest disjuncts) input-frames))))

(defn negate [db operands input-frames]
  (mapcat
   (fn [frame]
     (if (empty? (qeval db (first operands) [frame]))
       [frame]
       []))
   input-frames))

(defn execute [exp]
  (let [predicate (first exp)
        args (rest exp)]
    (apply (eval predicate) args)))

(defn lisp-value [call input-frames]
  "Evaluates `call` with any logic variables in it instantiated for each
   input frame. If the call returns a falsy value, filter that frame out."
  (mapcat
   (fn [frame]
     (if (execute
          (instantiate
           call
           frame
           (fn [v f]
             (throw
              (IllegalArgumentException. (str "Unknown pattern variable -- LISP-VALUE: " v))))))
       [frame]
       []))
   input-frames))

;; Rule functions need to be in the same namespace as qeval to avoid
;; a circular dependency, even though it would be more elegant to
;; put them in a separate namespace.

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

(defn apply-rules [db query frame]
  (mapcat
   (fn [rule]
     (apply-a-rule rule query frame))
   (fetch-rules db query frame)))

(defn simple-query [db q input-frames]
  "Processes a simple query, producing a sequence of frames with
   bindings for the variables in `q`."
  (mapcat
   (fn [frame]
     (concat
      (find-assertions db q frame)
      (apply-rules db q frame)))
   input-frames))

(defn qeval [db q input-frames]
  "Evaluates the query `q` in the context of the `input-frames` using
   assertions and rules from the `db`."
  (let [q-type (first q)]
    (cond
      (= q-type 'and) (conjoin db (rest q) input-frames)
      (= q-type 'or) (disjoin db (rest q) input-frames)
      (= q-type 'not) (negate db (rest q) input-frames)
      (= q-type 'lisp-value) (lisp-value (rest q) input-frames)
      :else (simple-query db q input-frames))))
