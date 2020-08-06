(ns unit.formatting-stack.alias-rewriter.impl.parsing
  (:require
   [clojure.test :refer [are deftest is testing]]
   [formatting-stack.alias-rewriter.impl.parsing :as sut]))

(deftest ns-decl->ns-aliases
  (are [input expected] (testing input
                          (is (= expected
                                 (sut/ns-decl->ns-aliases input)))
                          true)
    '(ns foo)                                                 {}
    '(ns foo (:require [bar :as baz]))                        '{baz bar}
    '(ns foo (:require [bar :as baz], [quux :as quuz]))       '{baz bar, quuz quux}
    '(ns foo (:require [bar :as baz]) (:import java.io.File)) '{baz bar}
    '(ns foo (:require [flanders will-not-be-aliased]))       {}
    '(ns foo (:require
              [flanders
               [schema :as fs]
               will-not-be-aliased
               [spec :as f-spec]]))                           '{fs     flanders.schema
                                                                f-spec flanders.spec}))

(deftest process-prefix-list!
  (are [input expected-flattening expected-aliases] (testing input
                                                      (let [a (atom {})]
                                                        (is (= expected-flattening
                                                               (sut/process-prefix-list! input a)))
                                                        (is (= expected-aliases
                                                               @a)))
                                                      true)
    []                                    ()                                            {}
    '[clojure core set]                   '([clojure.core] [clojure.set])               {}
    '[clojure core [set]]                 '([clojure.core] [clojure.set])               {}
    '[clojure [core] set]                 '([clojure.core] [clojure.set])               {}
    '[clojure [core] [set]]               '([clojure.core] [clojure.set])               {}
    '[clojure core [set :as foo]]         '([clojure.core] [clojure.set :as foo])       '{foo clojure.set}
    '[clojure [core :as c] [set :as foo]] '([clojure.core :as c] [clojure.set :as foo]) '{c clojure.core foo clojure.set}))
