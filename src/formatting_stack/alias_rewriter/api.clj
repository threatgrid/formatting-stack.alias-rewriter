(ns formatting-stack.alias-rewriter.api
  "This library's general-purpose API - it may be invoked directly, instead of via the `formatting-stack` framework."
  (:require
   [formatting-stack.alias-rewriter.impl.deriving :as deriving]
   [formatting-stack.alias-rewriter.impl.parsing :as parsing]
   [formatting-stack.alias-rewriter.impl.rewriting :as rewriting]
   [formatting-stack.alias-rewriter.kws :as kws]
   [nedap.speced.def :as speced]
   [nedap.utils.spec.predicates :refer [present-string?]]
   [rewrite-clj.zip :as zip]))

(speced/defn ^present-string? rewrite-aliases
  "Returns an improved formatting of `filename`, by renaming aliases to follow these guidelines:

   https://stuartsierra.com/2015/05/10/clojure-namespace-aliases

  `filename` will not be written to."
  [^::kws/existing-filename filename
   ^::kws/state state
   acceptable-aliases-whitelist]
  (let [[the-ns-name current-ns-aliases requires] (parsing/parse-filename filename)
        correct-ns-aliases (deriving/correct-ns-aliases-for the-ns-name
                                                            current-ns-aliases
                                                            requires
                                                            state
                                                            acceptable-aliases-whitelist)
        formatter (rewriting/node-formatter correct-ns-aliases)]
    (loop [current-node (zip/of-file filename)]
      (let [formatted-node (zip/postwalk current-node formatter)]
        (if-let [next-node (zip/right formatted-node)]
          (recur next-node)
          (zip/root-string formatted-node))))))
