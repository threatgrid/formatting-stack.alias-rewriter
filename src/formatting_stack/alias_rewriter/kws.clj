(ns formatting-stack.alias-rewriter.kws
  (:require
   [clojure.spec.alpha :as spec]
   [nedap.speced.def :as speced]
   [nedap.utils.spec.predicates :refer [present-string?]]
   [rewrite-clj.node.protocols :refer [Node]])
  (:import
   (clojure.lang Atom)
   (java.io File)))

(spec/def ::node (partial speced/satisfies? Node))

(spec/def ::unqualified-symbol? (spec/and symbol? (complement qualified-symbol?)))

(spec/def ::requires (spec/coll-of ::unqualified-symbol? :kind set?))

(spec/def ::ns-aliases (spec/map-of ::unqualified-symbol? ::unqualified-symbol?))

(spec/def ::correct-aliases ::ns-aliases)

(spec/def ::existing-filename (spec/and present-string?
                                        (speced/fn [^String s]
                                          (-> s File. .exists))))

(spec/def ::global-project-aliases (spec/map-of ::unqualified-symbol?
                                                (spec/coll-of ::unqualified-symbol? :kind set?)))

(spec/def ::state (spec/and (partial instance? Atom)
                            (fn [x]
                              (spec/valid? ::global-project-aliases @x))))
