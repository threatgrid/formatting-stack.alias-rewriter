(ns integration.formatting-stack.alias-rewriter.api
  (:require
   [clojure.java.io :as io :refer [as-file]]
   [clojure.test :refer [are deftest is join-fixtures testing use-fixtures]]
   [formatting-stack.alias-rewriter.api :as sut]))

(use-fixtures :each (join-fixtures [(fn [t]
                                      (remove-ns 'example-one.input)
                                      (remove-ns 'example-two.input)
                                      (t))]))

(defn full-filename-of [filename]
  (-> filename io/resource (doto assert) as-file str))

(def input-filename
  (full-filename-of "example_one/input.clj"))

(def output-filename
  (full-filename-of "example_one/output.clj"))

(assert (not= input-filename output-filename))

(def input-2-filename
  (full-filename-of "example_two/input.clj"))

(def output-2-filename
  (full-filename-of "example_two/output.clj"))

(deftest works
  (are [desc input output] (testing desc
                             (is (= (slurp output)
                                    (sut/rewrite-aliases input
                                                         (atom {})
                                                         {})))
                             true)
    "Reformats contents to a well-known output"
    input-filename
    output-filename

    "Doesn't alter well-formed inputs"
    output-filename
    output-filename

    ;; NOTE: the following is not perfect, but it's better than:
    ;; a) introducing duplicate aliases
    ;; b) blowing up
    "Doesn't change aliases that conflict with aliases that existed beforehand"
    input-2-filename
    output-2-filename))
