(ns sicp-logic.evaluator
  (:require [sicp-logic.binding :refer [instantiate]]
            [sicp-logic.assertions :refer [find-assertions]]
            [sicp-logic.rules :refer [apply-rules]]))

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
