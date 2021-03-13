(ns formatting-stack.alias-rewriter.impl.parsing
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.string :as string]
   [clojure.tools.namespace.parse :as parse]
   [clojure.walk :as walk]
   [formatting-stack.alias-rewriter.kws :as kws]
   [formatting-stack.util]
   [nedap.speced.def :as speced]
   [nedap.utils.spec.predicates :refer [present-string?]]))

(speced/defn add-to-atom! [^sequential? libspec, aliases-atom]
  (let [v (first libspec)
        k (->> libspec
               (drop-while (complement #{:as}))
               (second))]
    (when k
      (swap! aliases-atom assoc k v))))

(defn process-prefix-list! [[prefix & libs] aliases-atom]
  (->> libs
       (map (fn [l]
              (let [[lib & opts] (cond-> l
                                   (not (sequential? l)) vector)]
                (into (->> [prefix lib] (string/join ".") symbol vector)
                      opts))))
       (mapv (fn [x]
               (add-to-atom! x aliases-atom)
               x))))

(speced/defn ns-decl->ns-aliases ^::kws/ns-aliases [ns-decl]
  (let [result (atom {})]
    (->> ns-decl
         (walk/postwalk (fn [x]
                          (cond
                            (and (sequential? x)
                                 (seq x)
                                 (not-any? keyword? x)
                                 (some (some-fn vector?) x))
                            (do
                              (process-prefix-list! x result)
                              ;; prevent processing of leave nodes:
                              nil)

                            (and (sequential? x)
                                 (some #{:as} x))
                            (do
                              (add-to-atom! x result)
                              x)

                            true
                            x))))
    @result))

(speced/defn ^{::speced/spec (spec/cat ::kws/unqualified-symbol? ::kws/unqualified-symbol?
                                       ::kws/ns-aliases          ::kws/ns-aliases
                                       ::kws/requires            ::kws/requires)}
  parse-filename [^present-string? filename]
  (let [decl (-> filename formatting-stack.util/read-ns-decl)
        the-ns-name (parse/name-from-ns-decl decl)
        aliases (if (find-ns the-ns-name)
                  ;; Use runtime insights:
                  (-> the-ns-name the-ns ns-aliases)
                  ;; Fall back to parsing:
                  (ns-decl->ns-aliases decl))]
    [the-ns-name
     (into {}
           (map (fn [[k v]]
                  [k
                   (-> v str symbol)]))
           aliases)
     (parse/deps-from-ns-decl decl)]))
