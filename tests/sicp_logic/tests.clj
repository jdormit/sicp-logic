(ns sicp-logic.tests
  (:require [clojure.test :as test :refer [deftest is]]
            [sicp-logic.core :as logic]
            [sicp-logic.db.memory :as memdb]))

(defn setup-sicp-dataset! [db]
  "Loads the example data set from SICP into `db`."
  (let [facts '[(address (Bitdiddle Ben) (Slumerville (Ridge Road) 10))
                (job (Bitdiddle Ben) (computer wizard))
                (salary (Bitdiddle Ben) 60000)
                (address (Hacker Alyssa P) (Cambridge (Mass Ave) 78))
                (job (Hacker Alyssa P) (computer programmer))
                (salary (Hacker Alyssa P) 40000)
                (supervisor (Hacker Alyssa P) (Bitdiddle Ben))
                (address (Fect Cy D) (Cambridge (Ames Street) 3))
                (job (Fect Cy D) (computer programmer))
                (salary (Fect Cy D) 35000)
                (supervisor (Fect Cy D) (Bitdiddle Ben))
                (address (Tweakit Lem E) (Boston (Bay State Road) 22))
                (job (Tweakit Lem E) (computer technician))
                (salary (Tweakit Lem E) 25000)
                (supervisor (Tweakit Lem E) (Bitdiddle Ben))
                (address (Reasoner Louis) (Slumerville (Pine Tree Road) 80))
                (job (Reasoner Louis) (computer programmer trainee))
                (salary (Reasoner Louis) 30000)
                (supervisor (Reasoner Louis) (Hacker Alyssa P))
                (supervisor (Bitdiddle Ben) (Warbucks Oliver))
                (address (Warbucks Oliver) (Swellesley (Top Heap Road)))
                (job (Warbucks Oliver) (administration big wheel))
                (salary (Warbucks Oliver) 150000)
                (address (Scrooge Eben) (Weston (Shady Lane) 10))
                (job (Scrooge Eben) (accounting chief accountant))
                (salary (Scrooge Eben) 75000)
                (supervisor (Scrooge Eben) (Warbucks Oliver))
                (address (Cratchet Robert) (Allston (N Harvard Street) 16))
                (job (Cratchet Robert) (accounting scrivener))
                (salary (Cratchet Robert) 18000)
                (supervisor (Cratchet Robert) (Scrooge Eben))
                (address (Aull DeWitt) (Slumerville (Onion Square) 5))
                (job (Aull DeWitt) (administration secretary))
                (salary (Aull DeWitt) 25000)
                (supervisor (Aull DeWitt) (Warbucks Oliver))
                (can-do-job (computer wizard) (computer programmer))
                (can-do-job (computer wizard) (computer technician))
                (can-do-job (computer programmer) (computer programmer trainee))
                (can-do-job (administration secretary) (administration big wheel))]
        rules '[((same ?x ?x))
                ((lives-near ?person-1 ?person-2)
                 (and (address ?person-1 (?town & ?rest-1))
                      (address ?person-2 (?town & ?rest-2))
                      (not (same ?person-1 ?person-2))))
                ((wheel ?person)
                 (and (supervisor ?middle-manager ?person)
                      (supervisor ?x ?middle-manager)))
                ((outranked-by ?staff-person ?boss)
                 (or (supervisor ?staff-person ?boss)
                     (and (supervisor ?staff-person ?middle-manager)
                          (outranked-by ?middle-manager ?boss))))]]
    (doseq [fact facts]
      (logic/assert! db fact))
    (doseq [rule rules]
      (logic/add-rule! db rule))))

(deftest basic-pattern-matching
  (let [db (memdb/new-db)]
    (setup-sicp-dataset! db)
    (is (= (logic/query db [job ?x [computer programmer]])
           '[(job (Hacker Alyssa P) (computer programmer))
             (job (Fect Cy D) (computer programmer))]))
    (is (= (logic/query db [job ?x [computer ?type]])
           '[(job (Bitdiddle Ben) (computer wizard))
             (job (Hacker Alyssa P) (computer programmer))
             (job (Fect Cy D) (computer programmer))
             (job (Tweakit Lem E) (computer technician))]))
    (is (= (logic/query db [?key [Bitdiddle Ben] ?value])
           '[(address (Bitdiddle Ben) (Slumerville (Ridge Road) 10))
             (job (Bitdiddle Ben) (computer wizard))
             (salary (Bitdiddle Ben) 60000)
             (supervisor (Bitdiddle Ben) (Warbucks Oliver))]))))
             

(deftest compound-queries
  (let [db (memdb/new-db)]
    (setup-sicp-dataset! db)
    (is (= (logic/query db (and [job ?person [computer programmer]]
                                [address ?person ?where]))
           '[(and (job (Hacker Alyssa P) (computer programmer))
                  (address (Hacker Alyssa P) (Cambridge (Mass Ave) 78)))
             (and (job (Fect Cy D) (computer programmer))
                  (address (Fect Cy D) (Cambridge (Ames Street) 3)))]))
    (is (= (logic/query db (or [supervisor ?x [Bitdiddle Ben]]
                               [supervisor ?x [Hacker Alyssa P]]))
           '[(or (supervisor (Hacker Alyssa P) (Bitdiddle Ben))
                 (supervisor (Hacker Alyssa P) (Hacker Alyssa P)))
             (or (supervisor (Fect Cy D) (Bitdiddle Ben))
                 (supervisor (Fect Cy D) (Hacker Alyssa P)))
             (or (supervisor (Tweakit Lem E) (Bitdiddle Ben))
                 (supervisor (Tweakit Lem E) (Hacker Alyssa P)))
             (or (supervisor (Reasoner Louis) (Bitdiddle Ben))
                 (supervisor (Reasoner Louis) (Hacker Alyssa P)))]))
    (is (= (logic/query db (and [supervisor ?x [Bitdiddle Ben]]
                                (not [job ?x [computer programmer]])))
           '[(and (supervisor (Tweakit Lem E) (Bitdiddle Ben))
                  (not (job (Tweakit Lem E) (computer programmer))))]))
    (is (= (logic/query db (and [salary ?person ?amount]
                                (lisp-value > ?amount 30000)))
           '[(and (salary (Bitdiddle Ben) 60000)
                  (lisp-value > 60000 30000))
             (and (salary (Hacker Alyssa P) 40000)
                  (lisp-value > 40000 30000))
             (and (salary (Fect Cy D) 35000)
                  (lisp-value > 35000 30000))
             (and (salary (Warbucks Oliver) 150000)
                  (lisp-value > 150000 30000))
             (and (salary (Scrooge Eben) 75000)
                  (lisp-value > 75000 30000))]))))

(deftest rules
  (let [db (memdb/new-db)]
    (setup-sicp-dataset! db)
    (is (= (logic/query db [lives-near ?x [Bitdiddle Ben]])
           '[(lives-near (Reasoner Louis) (Bitdiddle Ben))
             (lives-near (Aull DeWitt) (Bitdiddle Ben))]))
    (is (= (logic/query db (and [job ?x [computer programmer]]
                                [lives-near ?x [Hacker Alyssa P]]))
           '[(and (job (Fect Cy D) (computer programmer))
                  (lives-near (Fect Cy D) (Hacker Alyssa P)))]))))

(defn run-tests []
  (test/run-tests 'sicp-logic.tests))
