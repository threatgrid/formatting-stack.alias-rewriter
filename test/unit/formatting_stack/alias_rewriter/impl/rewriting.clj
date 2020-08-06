(ns unit.formatting-stack.alias-rewriter.impl.rewriting
  (:require
   [clojure.string :as string]
   [clojure.test :refer [are deftest is testing]]
   [formatting-stack.alias-rewriter.impl.rewriting :as sut]
   [rewrite-clj.zip :as zip]))

(deftest maybe-replace-string
  (let [correct '{bar foo}]
    (are [input expected] (testing input
                            (let [node (-> input pr-str zip/of-string)]
                              (is (= expected
                                     (-> node
                                         (sut/maybe-replace-string correct)
                                         (zip/sexpr)))))
                            true)
      ""                 ""
      "a"                "a"
      "::bar/x"          "::foo/x"
      "::bar/x\n::bar/y" "::foo/x\n::foo/y")

    (are [input expected] (testing input
                            (let [node (-> input pr-str zip/of-string)
                                  actual (-> node
                                             (sut/maybe-replace-string correct)
                                             (zip/string))]
                              (is (= expected
                                     actual))

                              (is (not (string/includes? actual "\\"))
                                  "\n is encoded/preserved as \n, not as \\n"))
                            true)
      ""                 "\"\""
      "a"                "\"a\""
      "::bar/x"          "\"::foo/x\""
      "::bar/x\n::bar/y" "\"::foo/x\n::foo/y\"")))
