(ns formatting-stack.alias-rewriter.impl.deriving
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.string :as string]
   [formatting-stack.alias-rewriter.kws :as kws]
   [formatting-stack.linters.ns-aliases :as linters.ns-aliases]
   [nedap.speced.def :as speced]))

(speced/defn size-comparator [^string? a, ^string? b]
  (let [a-length (-> a .length)
        b-length (-> b .length)]
    (cond
      (< a-length b-length) -1
      (> a-length b-length) 1
      true                  0)))

(speced/defn alias-comparator [^string? a, ^string? b]
  (let [a-core? (-> a (.contains "core"))
        b-core? (-> b (.contains "core"))]
    (cond
      (and a-core? (not b-core?)) 2
      (and a-core? b-core?)       (size-comparator a b)
      b-core?                     -1
      true                        (size-comparator a b))))

(speced/defn permutations
  "Returned in priority order."
  [^::kws/unqualified-symbol? the-ns-name]
  (let [split (-> the-ns-name str (string/split #"\."))
        n (-> split count inc)
        longer-variations (->> (range 1 n)
                               (mapv (fn [i]
                                       (->> split (take-last i) (string/join "."))))
                               (sort alias-comparator))
        shorter-variations (->> longer-variations
                                (mapcat (fn [the-ns-name]
                                          (->> [#".api$"
                                                #".core$"
                                                "-clj"
                                                "clj-"
                                                "-cljs"
                                                "cljs-"
                                                "-clojure"
                                                "clojure-"]
                                               (map (fn [s]
                                                      (-> the-ns-name (string/replace s "")))))))
                                (sort alias-comparator)
                                (vec))]
    (->> (into shorter-variations longer-variations)
         (remove #{"core"})
         (distinct)
         (map symbol))))

(speced/defn ^{::speced/spec (spec/coll-of ::kws/unqualified-symbol?)}
  derivations [^::kws/unqualified-symbol? current-ns-name
               ^::kws/unqualified-symbol? the-ns-name
               ^::kws/ns-aliases current-ns-aliases
               ^::kws/global-project-aliases global-project-aliases]
  (let [candidates (->> global-project-aliases
                        (filter (speced/fn [[k, ^set? v]]
                                  (v the-ns-name))))
        used-and-unambiguous (when (and (-> candidates count #{1})
                                        (-> candidates first second count #{1}))
                               (ffirst candidates))
        already-in-current-ns (->> current-ns-aliases keys set)]
    (->> (into (->> [used-and-unambiguous]
                    (filterv some?))
               (->> the-ns-name
                    permutations
                    (remove (speced/fn [^::kws/unqualified-symbol? s]
                              (->> global-project-aliases
                                   (some (fn [[alias mapped-namespaces]]
                                           (and (= s alias)
                                                (->> mapped-namespaces
                                                     (some (complement #{the-ns-name})))))))))))
         (remove already-in-current-ns)
         (remove #{current-ns-name})
         (vec))))

(defn set-conj [coll x]
  (if coll
    (conj (set coll) x)
    (hash-set x)))

(speced/defn ^::speced/nilable ^::kws/unqualified-symbol?
  retrying-derivations
  "Returns the first derived value that wasn't already taken, locally or globally.

Updates `state` with the found value."
  [^::kws/unqualified-symbol? current-ns-name
   ^::kws/unqualified-symbol? the-ns-name
   ^::kws/ns-aliases current-ns-aliases
   ^::kws/state state]
  (let [success? (atom false)]
    (-> (for [d (derivations current-ns-name the-ns-name current-ns-aliases @state)
              :while (not @success?)
              :let [v @state
                    contained? (contains? (get v d) the-ns-name)
                    _ (cond-> success?
                        contained?       (reset! true)
                        (not contained?) (reset! (compare-and-set! state
                                                                   v
                                                                   (update v d set-conj the-ns-name))))]]
          d)

        first)))

(speced/defn ^::kws/ns-aliases correct-ns-aliases-for [the-ns-name
                                                       ^::kws/ns-aliases current-ns-aliases
                                                       ^::kws/state state
                                                       acceptable-aliases-whitelist]
  (let [acceptable (merge-with into
                               linters.ns-aliases/default-acceptable-aliases-whitelist
                               acceptable-aliases-whitelist)]
    (into {}
          (comp (remove (fn [[k v]]
                          (linters.ns-aliases/acceptable-require-clause? acceptable [v :as k])))
                (keep (fn [[k v]]
                        (when-let [result (retrying-derivations the-ns-name v current-ns-aliases state)]
                          [k result]))))
          current-ns-aliases)))
