(ns formatting-stack.alias-rewriter.formatter
  "A Component meant to be run via the `formatting-stack` framework."
  (:require
   [formatting-stack.alias-rewriter.api :as alias-rewriter.api]
   [formatting-stack.alias-rewriter.impl.analysis :as analysis]
   [formatting-stack.protocols.formatter :as formatter]
   [formatting-stack.util :refer [process-in-parallel!]]
   [nedap.utils.modular.api :refer [implement]]))

(defn format! [{::keys [acceptable-aliases-whitelist]} files]
  (let [state (atom (analysis/global-project-aliases))]
    (->> files
         (process-in-parallel! (fn [filename]
                                 (let [formatting (alias-rewriter.api/rewrite-aliases filename
                                                                                      state
                                                                                      acceptable-aliases-whitelist)]
                                   (when-not (= formatting
                                                (slurp filename))
                                     (spit filename formatting)))))))
  nil)

(defn new [& {:keys [acceptable-aliases-whitelist]}]
  (implement {:id ::id
              ::acceptable-aliases-whitelist acceptable-aliases-whitelist}
    formatter/--format! format!))
