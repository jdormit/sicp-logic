# SICP Logic Programming
> A Clojure implementation of the logic programming language described in *Structure and Interpretation of Computer Programs*

## What is this?
This is a Clojure implementation of the logic programming language described in chapter 4 of the classic computer science textbook [*Structure and Interpration of Computer Programs*](https://mitpress.mit.edu/sites/default/files/sicp/index.html). The language allows users to store **facts** - assertions about the world - in a database. Additionally, users can define **rules**, which express logical relationships between facts. Users can then execute queries against the database. Queries contain so-called **logic variables**, which are placeholders that can be filled in by the database with values such that the entire query statement is logically true. The result of a query is a list of all values for the logic variables that result in the query being true.

Although this implementation supports multiple types of storage for the database, currently only an in-memory database is implemented.

## Installation and usage
To play around with this you'll need to clone this repository and install the [Clojure command line tools](https://www.clojure.org/guides/getting_started). The project doesn't have any external dependencies, so you can just jump right into a REPL:

``` shell
$ git clone https://github.com/jdormit/sicp-logic
$ cd sicp-logic
$ clj
```

Once you're in the REPL, you can `require` the necessary namespaces:

``` clojure
user> (require '[sicp-logic.core :as logic]
               '[sicp-logic.db.memory :as memdb])
nil
```

Define a new in-memory database:

``` clojure
user> (def db (memdb/new-db))
#'user/db
```

Load some facts into it:

``` clojure
user> (let [facts '[[address [Bitdiddle Ben] [Slumerville [Ridge Road] 10]]
                    [job [Bitdiddle Ben] [computer wizard]]
                    [salary [Bitdiddle Ben] 60000]
                    [address [Hacker Alyssa P] [Cambridge [Mass Ave] 78]]
                    [job [Hacker Alyssa P] [computer programmer]]
                    [salary [Hacker Alyssa P] 40000]
                    [supervisor [Hacker Alyssa P] [Bitdiddle Ben]]
					[address [Fect Cy D] [Cambridge [Ames Street] 3]]
					[job [Fect Cy D] [computer programmer]]
					[salary [Fect Cy D] 35000]
					[supervisor [Fect Cy D] [Bitdiddle Ben]]]]
		(doseq [fact facts]
          (logic/assert! db fact)))
nil
```

Now you can run some simple queries over the facts in the database. For example, you can ask who all the computer programmers are:

``` clojure
user> (logic/query db [job ?who [computer programmer]])
((job (Hacker Alyssa P) (computer programmer))
 (job (Fect Cy D) (computer programmer)))
```

Or how much money Cy D Fect is paid:

``` clojure
user> (logic/query db [salary [Fect Cy D] ?amount])
((salary (Fect Cy D) 35000))
```

You can even ask the database for everything it knows about Ben Bitdiddle:

``` clojure
user> (logic/query db [?attribute [Bitdiddle Ben] ?value])
((address (Bitdiddle Ben) (Slumerville (Ridge Road) 10))
 (job (Bitdiddle Ben) (computer wizard))
 (salary (Bitdiddle Ben) 60000))
```

Compound queries can be constructed with the operators `and`, `or`, `not`, and `lisp-value`. `And` and `or` work as you'd expect; `not` and `lisp-value` work as filters on the query results. `Not` works by filtering out queries that meet the `not` condition. `Lisp-value` works by filtering out queries for which the Clojure expression contained in the `lisp-value` returns `true`.

``` clojure
user> (logic/query db (and [supervisor ?who [Bitdiddle Ben]]
                           (not [address ?who [Cambridge [Ames Street] 3]])))
((and
  (supervisor (Hacker Alyssa P) (Bitdiddle Ben))
  (not (address (Hacker Alyssa P) (Cambridge (Ames Street) 3)))))
user> (logic/query db (and [supervisor ?who [Bitdiddle Ben]]
                           [salary ?who ?amount]
                           (lisp-value > ?amount 37000)))
((and
  (supervisor (Hacker Alyssa P) (Bitdiddle Ben))
  (salary (Hacker Alyssa P) 40000)
  (lisp-value > 40000 37000)))
user> (logic/query db (or [address ?who [Cambridge [Ames Street] 3]]
                          [address ?who [Slumerville [Ridge Road] 10]]))
((or
  (address (Fect Cy D) (Cambridge (Ames Street) 3))
  (address (Fect Cy D) (Slumerville (Ridge Road) 10)))
 (or
  (address (Bitdiddle Ben) (Cambridge (Ames Street) 3))
  (address (Bitdiddle Ben) (Slumerville (Ridge Road) 10))))
```

The query language also support "rest-variables" in the same style as Clojure's function definitions or sequence destructuring via the `&` symbol. A query of the form `[?var1 & ?rest]` would match any assertion of length 2 or greater, matching the first item in the assertion to `?var1` and binding `?rest` to a list consisting of the remaining items in the assertion. The usefulness of this is best illustrated with an example:

``` clojure
user> (logic/query db [address ?who [Cambridge & ?rest]])
((address (Hacker Alyssa P) (Cambridge & ((Mass Ave) 78)))
 (address (Fect Cy D) (Cambridge & ((Ames Street) 3))))
```

Things start getting really interesting when you define some rules. A rule consists of a **`condition`** and optionally a **`body`**, and declares that the `condition` is true if-and-only-if the `body` is true. A rule without a body is true for any fact that matches the condition.

For example, let's define a rule that tells the logic engine how to determine if two entities are logically the same:

``` clojure
user> (logic/defrule! db [same ?x ?x])
user> ;; Ben is the same as himself
user> (logic/query db [same [Ben Bitdiddle] [Ben Bitdiddle]])
((same (Ben Bitdiddle) (Ben Bitdiddle)))
user> ;; But not the same as Cy D Fect
user> (logic/query db [same [Ben Bitdiddle] [Fect Cy D]])
()
```

The `same` rule only has a conclusion, meaning it is true for any query that matches it. Okay, this doesn't seem useful on its own, but it does let us define an actually-useful rule describing whether two people live near each other. Two people live near each other if they have the same town in their address but are not the same person:

``` clojure
user> (logic/defrule! db [lives-near ?a ?b]
        (and [address ?a [?town & ?rest-1]]
             [address ?b [?town & ?rest-2]]
             (not [same ?a ?b])))
user> (logic/query db [lives-near [Hacker Alyssa P] ?who])
((lives-near (Hacker Alyssa P) (Fect Cy D)))
```

## How it works
At a very high level, the logic engine sees the world as a sequence of **frames**. A frame is simply a container that contains mappings from variable names to values (where the values can also contain variables). The engine starts by fetching every assertion and rule that might satisfy the query. 

To determine which assertions actually match, the engine performs **unification** on the query and the assertion. Unification is the process in which the variables in the query get bound to the corresponding parts of the assertion, and results in a new frame. For example, unifying the query `[job ?who [computer ?what]]` with the assertion `[job [Bitdiddle Ben] [computer programmer]]` results in the variable bindings `{who [Bitdiddle Ben], what programmer}`. Unification of an assertion can fail if the assertion cannot match the query (for example, unifying `[job ?who [computer programmer]]` with `[salary [Bitdiddle Ben] 60000]` would fail).

To determine which rules satisfy the query, the engine starts by unifying each rule conclusion with the query. Since rule conclusions can contain variables as well, some query variables can remain unbound after unifying; to bind these remaining variables, the logic engine then evaluates each rule body in the context of the respective frame.

The result of performing unification on all the relevant assertions and rules is a sequence of frames from the successes; the engine then iterates through each frame and returns a version of the query with its variables filled in with the bindings from that frame.

`And` queries operate by taking the stream of frames from the first clause of the query and evaluating the next clause in the query in the context of each of the frames from the first query, and so on. `Or` queries are similar, except that they simply evaluate all of the clauses simultaneously and then concatenate the results into a single sequence of frames at the end. `Not` queries work by taking the sequence of frames passed to them, evaluating their clauses in the context of each of those frames, and returning only those frames for which their evaluation returns no bindings. `Lisp-value` queries work the same way, except they call into Clojure directly to evaluate the Lisp predicate, returning only those frames for which the predicate returns `true`.

If this piqued your interest and you want to peek under the hood, the source code for this project has comments in some key parts of the algorithm. To really dive in, I highly recommend reading [Chapter 4.4 of SICP](https://mitpress.mit.edu/sites/default/files/sicp/full-text/book/book-Z-H-29.html#%_sec_4.4). Well, honestly you should just read the whole book - it totally changed the way I think about programming and it remains relevant and useful even though it was first published in the 80s.
