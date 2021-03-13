(ns unit.formatting-stack.alias-rewriter.impl.deriving
  (:require
   [clojure.test :refer [are deftest is testing]]
   [formatting-stack.alias-rewriter.impl.deriving :as sut]))

(deftest permutations
  (are [input expected] (testing input
                          (is (= expected
                                 (sut/permutations input)))
                          true)
    'foo             '(foo)
    'foo.bar         '(bar foo.bar)
    'foo.bar.baz     '(baz bar.baz foo.bar.baz)
    'foo.core        '(foo foo.core)
    'foo.bar.core    '(bar foo.bar bar.core foo.bar.core)
    'base64-clj.core '(base64 base64-clj base64.core base64-clj.core)))

(deftest derivations
  (are [desc the-ns-name current-ns-aliases current-requires project-aliases expected]
       (testing [desc the-ns-name current-ns-aliases current-requires project-aliases]
         (is (= expected
                (sut/derivations 'sample the-ns-name current-ns-aliases current-requires project-aliases)))
         true)
    #_the-ns-name #_current-ns-aliases #_current-requires #_project-aliases   #_expected

    "Returns x if there are no other possible options"
    'foo          {}                   #{}                {}                  '[foo]

    "`.core` takes less priority"
    'foo.core     {}                   #{}                {}                  '[foo foo.core]

    "Identically named aliases that refer to different namespaces result in an exclusion of the alias"
    'foo.core     {}                   #{}                '{foo #{bar}}       '[foo.core]

    "Prioritizes existing project aliases, even if they are not derived"
    'foo.core     {}                   #{}                '{quux #{foo.core}} '[quux foo foo.core]

    "Removes aliases already used in the current ns (part 1)"
    'foo.core     '{foo bar}           #{}                {}                  '[foo.core]

    "Removes aliases already used in the current ns (part 2)"
    'foo.core     '{foo bar}           #{}                '{quux #{foo.core}} '[quux foo.core]

    "Removes aliases already used in the current ns (part 3)"
    'foo.core     '{quux bar}          #{}                '{quux #{foo.core}} '[foo foo.core]

    "Removes aliases already used in the current ns (part 4)"
    'foo.core     '{foo.core bar}      #{}                '{quux #{foo.core}} '[quux foo]

    "Doesn't remove aliases already used in the current ns if they are properly derived from the current namespace"
    'foo.core     '{foo foo.core}      #{}                {}                  '[foo foo.core]

    "Removes aliases already used in the current ns if they are not properly derived from the current namespace"
    'foo.core     '{foo bar.core}      #{}                {}                  '[foo.core]

    "Never suggests an alias that is exactly equivalent to the current ns name"
    'sample.core  {}                   #{}                {}                  '[sample.core]

    "Never suggests an alias that is exactly equivalent to a different namespace that is required but not aliased"
    'foo.schemas  {}                   #{'schemas}        {}                  '[foo.schemas]))

(deftest correct-ns-aliases-for
  (are [desc current-ns-aliases acceptable-aliases-whitelist expected]
       (testing desc
         (is (= expected
                (sut/correct-ns-aliases-for 'sample
                                            current-ns-aliases
                                            #{}
                                            (atom {})
                                            acceptable-aliases-whitelist)))
         true)
    #_current-ns-aliases  #_acceptable-aliases-whitelist #_expected

    "It returns a better possible alias when adequate"
    '{s schema.core}      {}                             '{s schema}

    "If a given current alias is included in `acceptable-aliases-whitelist`, it won't be changed, even if it can be improved"
    '{s schema.core}      '{s [schema.core]}             {}

    "Never suggests an alias that is exactly equivalent to the current ns name"
    '{sample.core sample} {}                             {}))
