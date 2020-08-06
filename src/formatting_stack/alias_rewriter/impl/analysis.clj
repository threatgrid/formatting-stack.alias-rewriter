(ns formatting-stack.alias-rewriter.impl.analysis
  (:require
   [clojure.tools.namespace.find :as find]
   [clojure.tools.namespace.parse :as parse]
   [clojure.tools.namespace.repl :refer [refresh-dirs]]
   [formatting-stack.alias-rewriter.kws :as kws]
   [formatting-stack.project-parsing :as project-parsing]
   [formatting-stack.util :refer [read-ns-decl]]
   [nedap.speced.def :as speced]
   [nedap.utils.collections.eager :refer [partitioning-pmap]])
  (:import
   (java.io File)))

(defn project-namespaces
  "Returns all the namespaces contained in the current project's source paths."
  []
  (->> (project-parsing/find-files (or (seq refresh-dirs)
                                       (project-parsing/classpath-directories))
                                   find/clj)
       (partitioning-pmap (speced/fn [^File file]
                            (let [decl (-> file str read-ns-decl)
                                  n (some-> decl parse/name-from-ns-decl)]
                              [n])))
       (apply concat)
       (distinct)
       (filter identity)
       (keep find-ns)))

(speced/defn ^::kws/global-project-aliases
  global-project-aliases [& {:keys [namespaces]
                             :or   {namespaces (project-namespaces)}}]
  (->> namespaces
       (mapcat ns-aliases)
       (map (fn [[k v]]
              [k #{(-> v str symbol)}]))
       (group-by first)
       (map (fn [[k v]]
              [k (->> v
                      (map second)
                      (reduce into))]))
       (into {})))
