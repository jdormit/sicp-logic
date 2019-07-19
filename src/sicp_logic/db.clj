(ns sicp-logic.db)

(defprotocol FactDB
  "The FactDB protocol specifies methods to store and retrieve
   assertions (facts) and rules."
  (fetch-assertions [db query frame]
    "Fetches assertions that may match the given query and frame.")
  (add-assertion [db assertion]
    "Stores an assertion (a fact) in the database.")
  (fetch-rules [db query frame]
    "Fetches rules whose conditions may unify with the given query and frame")
  (add-rule [db rule]
    "Adds a new rule to the database"))
