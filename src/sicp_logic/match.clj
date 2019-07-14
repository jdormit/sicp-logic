(ns sicp-logic.match
  (:require [sicp-logic.binding :refer [binding-in-frame extend var?]]))

(declare pattern-match)

(defn extend-if-consistent [var data frame]
  "Extends `frame` by binding `var` to `data` as long as this is
   consistent with the bindings already in `frame`."
  (let [binding-value (binding-in-frame var frame)]
    (if binding-value
      (pattern-match binding-value data frame)  ;; recursive call to bind any variables in the binding-value
      (extend var data frame))))

(defn pattern-match [pattern data frame]
  "Matches `pattern` against `data`, returning either a new frame
   with the pattern variables bound or the keyword :failed if matching
   fails"
  (cond
    ;; If the frame has already failed, fail
    (= frame :failed) :failed
    ;; If the pattern already equals the data,
    ;; the frame already has the correct bindings
    (= pattern data) frame
    ;; If the pattern is a variable, try to extend the frame by binding that
    ;; variable to the data
    (var? pattern) (extend-if-consistent pattern data frame)
    ;; If the pattern and data are both lists, recurse into the list
    (and (sequential? pattern) (sequential? data))
    (pattern-match
     (rest pattern)
     (rest data)
     (pattern-match (first pattern)
                    (first data)
                    frame))
    ;; Otherwise we can't match this pattern
    :else :failed))
