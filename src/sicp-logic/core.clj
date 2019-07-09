(ns sicp-logic.core)

(declare qeval)

(defn var? [exp])

(defn binding-in-frame [var frame]
  "Returns the value the `var` is bound to in `frame`, or nil."
  (frame (second var)))


(defn instantiate [q frame unbound-var-handler]
  "Instantiates the query `q` with the variables bound in `frame`."
  (letfn [(copy (fn [exp])
               (cond (var? exp) (let [binding-value (binding-in-frame exp frame)]
                                  (if binding-value
                                    (copy binding-value)
                                    (unbound-var-handler exp frame)))
                     (seq? exp) (cons (copy (first exp)) (copy (rest exp)))
                     :else exp))]
    (copy q)))

(defn conjoin [conjuncts input-frames]
  (if (empty? conjuncts)
    input-frames
    (conjoin (rest conjuncts)
             (qeval (first conjuncts) input-frames))))

(defn disjoin [disjuncts input-frames]
  (if (empty? disjuncts)
    nil
    (concat (qeval (first disjuncts) input-frames)
            (disjoin (rest disjuncts) input-frames))))


(defn negate [operands input-frames]
  (filter
   (fn [frame]
     (empty? (qeval operands [frame])))
   input-frames))

(defn execute [exp]
  (let [predicate (first exp)
        args (rest exp)]
    (apply (eval predicate) args)))

(defn lisp-value [call input-frames]
  (mapcat
   (fn [frame]
     (if (execute
          (instantiate
           call
           frame
           (fn [v f]
             (throw (java.lang.IllegalArgumentException. (str "Unknown pattern variable -- LISP-VALUE: " v))))))
       [frame]
       []))
   input-frames))

(defn find-assertions [query frame])

(defn apply-rules [query frame])

(defn simple-query [q input-frames]
  "Processes a simple query, producing a sequence of frames with bindings for the variables in `q`."
  (mapcat
   (fn [frame]
     (concat
      (find-assertions q frame)
      (apply-rules q frame)))
   input-frames))

(defn qeval [q input-frames]
  "Evaluates the query `q` in the context of the `input-frames`."
  (let [q-type (first q)]
    (cond
      (= q-type 'and) (conjoin (rest q) input-frames)
      (= q-type 'or) (disjoin (rest q) input-frames)
      (= q-type 'not) (negate (rest q) input-frames)
      (= q-type 'lisp-value (list-value (rest q) input-frames))
      :else (simple-query q input-frames))))

(defn contract-question-mark [v])

(defmacro query [q]
  "Queries the database for assertions that match the query."
  `(map (fn [frame]
          (instantiate (quote ~q) frame (fn [v f] (contract-question-mark v))))
        ;; TODO expand variable names into [? var]
    (qeval (quote ~q) [{}])))

(defmacro assert! []
  "Adds a new assertion to the database.")

(defmacro defrule []
  "Adds a new rule to the database.")
