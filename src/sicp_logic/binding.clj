(ns sicp-logic.binding)

(defn var? [exp]
  (and (sequential? exp) (= (first exp) '?)))

(defn binding-in-frame [var frame]
  "Returns the value the `var` is bound to in `frame`, or nil."
  (frame (second var)))

(defn extend [var data frame]
  "Binds `var` to `data` in `frame`"
  (assoc frame (second var) data))

(defn instantiate [q frame unbound-var-handler]
  "Instantiates the query `q` with the variables bound in `frame`."
  (letfn [(copy [exp]
               (cond (var? exp) (let [binding-value (binding-in-frame exp frame)]
                                  (if binding-value
                                    (copy binding-value)
                                    (unbound-var-handler exp frame)))
                     (and (sequential? exp) (not (empty? exp))) (cons (copy (first exp)) (copy (rest exp)))
                     :else exp))]
    (copy q)))
