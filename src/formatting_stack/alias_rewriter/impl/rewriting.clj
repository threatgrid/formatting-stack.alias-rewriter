(ns formatting-stack.alias-rewriter.impl.rewriting
  (:require
   [clojure.string :as string]
   [formatting-stack.alias-rewriter.kws :as kws]
   [formatting-stack.linters.ns-aliases :as linters.ns-aliases]
   [nedap.speced.def :as speced]
   [rewrite-clj.node :as node]
   [rewrite-clj.node.protocols :refer [Node]]
   [rewrite-clj.node.string :as node.string]
   [rewrite-clj.zip :as zip]
   [rewrite-clj.zip.base]))

(speced/defn maybe-replace-string [^::kws/node ns-node
                                   ^::kws/correct-aliases correct-ns-aliases]
  (let [e (zip/sexpr ns-node)
        new-val (->> correct-ns-aliases
                     (reduce (fn [acc [old-alias correct-alias]]
                               (reduce (fn [s x]
                                         (string/replace s
                                                         (-> x (str old-alias "/") (re-pattern))
                                                         (-> x (str correct-alias "/"))))
                                       acc
                                       ["#'"
                                        "`"
                                        "::"]))
                             e))]
    (cond-> ns-node
      (not= e new-val)
      (zip/replace (node.string/string-node new-val)))))

(speced/defn maybe-replace-ns-clause [^::kws/node ns-node
                                      ^::kws/correct-aliases correct-ns-aliases]
  (let [e (zip/sexpr ns-node)
        [the-ns-name alias] (linters.ns-aliases/name-and-alias e)]
    (if-not (correct-ns-aliases alias)
      ns-node
      (let [new-val (-> (->> e
                             (reduce (fn [[x {:keys [found-as? alias]
                                              :as   result}]
                                          member]
                                       (let [v (cond
                                                 alias          result
                                                 found-as?      {:found-as? true
                                                                 :alias     member}
                                                 (= :as member) {:found-as? true
                                                                 :alias     nil})]
                                         [(conj x (if-not (and found-as?
                                                               member
                                                               (-> v :alias #{member}))
                                                    member
                                                    (-> v :alias correct-ns-aliases)))
                                          v]))
                                     [[]
                                      {:found-as? false
                                       :alias     nil}])
                             (first))

                        (with-meta {}))]
        (cond-> ns-node
          (not= e new-val)
          (zip/replace new-val))))))

(defn ensure-auto [x]
  (let [s (some-> x str)]
    (if-not (and s
                 (string/starts-with? s ":")
                 (not (string/starts-with? s "::")))
      x
      (str ":" s))))

(speced/defn maybe-replace-ident [^::kws/node node
                                  ^{::speced/spec #{keyword symbol}} converter
                                  ^::kws/correct-aliases correct-ns-aliases]
  (let [e (zip/sexpr node)
        n (-> e namespace symbol)
        corrected-ns (some-> correct-ns-aliases (get n) str)
        kw-converter? (#{keyword} converter)
        auto-kw? (and kw-converter?
                      (->> node (into {}) :namespaced?))
        fix? (and corrected-ns
                  (if kw-converter?
                    auto-kw? ;; non-auto kws should never be renamed - it doesn't make sense
                    true))]

    (cond-> node
      fix?
      (zip/replace (let [v (->> e name (converter corrected-ns))]
                     (cond-> v
                       true     node/coerce
                       auto-kw? (assoc :namespaced?  true
                                       :string-value (ensure-auto v))))))))

(defn node-formatter [correct-ns-aliases]
  (fn format-node [node]
    (let [e (try
              (zip/sexpr node)
              (catch UnsupportedOperationException _))]
      (cond
        ;; XXX also process comments
        (and (vector? e) ;; XXX should be `sequential?` after ensuring maybe-replace-ns-clause is inside a ns
             (->> e (some #{:as}))) (maybe-replace-ns-clause node correct-ns-aliases)
        (qualified-keyword? e)      (maybe-replace-ident node keyword correct-ns-aliases)
        (qualified-symbol? e)       (maybe-replace-ident node symbol correct-ns-aliases)
        (string? e)                 (maybe-replace-string node correct-ns-aliases)
        true                        node))))
