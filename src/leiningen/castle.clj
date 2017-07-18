(ns leiningen.castle
  (:require [clojure.set :refer [intersection]]
            [clojure.tools.namespace.parse :as p]
            [clojure.tools.namespace.find :as f]
            [clojure.string :as string]))

(defn path-parents
  "Given a path with '.' as a separator return a set of all parents absolute paths.
   Includes itself, assumes path is a symbol.
  "
  [path]
  (let [parts (-> path str (string/split #"\."))
        perms (for [i (-> parts count range)]
                (->> parts
                     (take (inc i))
                     (string/join ".")))]
    (->> perms
         (map symbol)
         set)))

(defn import-valid
  "If the current namespace is importing another namespace we need to make sure that
   other namespace isn't walled off. If it is we can check to see if we are on the list
   of allowed namespaces.

   Returns a sequence of violations.
  "
  [rules current-ns imported-ns]
  (let [possible-walls (path-parents imported-ns)
        possible-alloweds (path-parents current-ns)
        walls (intersection possible-walls (-> rules keys set))]

    (->> walls

          ;; remove any walls where the current ns is inside the wall.
         (remove (fn [wall]
                   (wall possible-alloweds)))

         ;; rule application
         (mapcat (fn [wall]
                   (let [{:keys [gates allowed]} (rules wall)

                         any-valid-gates (some
                                          (fn [gate]
                                            ;; gates must be exact.
                                            (= gate imported-ns))
                                          gates)

                         allowed-set (set allowed)
                         current-ns-allowed (not-empty (intersection allowed-set possible-alloweds))]

                     (remove nil?
                             [(when-not any-valid-gates {:violating-ns current-ns
                                                         :invalid-import imported-ns
                                                         :reason ::no-valid-gate})

                              (when-not current-ns-allowed {:violating-ns current-ns
                                                            :invalid-import imported-ns
                                                            :reason ::not-allowed})])))))))

(def art "
                 |ZZzzz
                 |
                 |
    |ZZzzz      /^\\            |ZZzzz
    |          |~~~|           |
    |        |^^^^^^^|        / \\
   /^\\       |[]+    |       |~~~|
|^^^^^^^|    |    +[]|       |   |
|    +[]|/\\/\\/\\/\\^/\\/\\/\\/\\/|^^^^^^^|
|+[]+   |~~~~~~~~~~~~~~~~~~|    +[]|
|       |  []   /^\\   []   |+[]+   |
|   +[]+|  []  || ||  []   |   +[]+|
|[]+    |      || ||       |[]+    |
|_______|------------------|_______|
                                🐕     🌲
🐕         🐕      🐕                 🌲
   🐕                                🌲
")


(def badart "
   🐶  ^
  /👕 \\|
   👖  |
Check your imports!
")

(defn print-failures
  "Pretty print a seq of failures."
  [failures]

  (let [collected-failures (reduce
                            (fn [acc failure]
                              (update acc (:violating-ns failure) conj failure))
                            {}
                            failures)]

    (doseq [failing-ns (-> collected-failures keys sort)]
      (let [failures
            (collected-failures failing-ns)]
        (println)
        (println failing-ns)
        (doseq [{:keys [invalid-import reason]} failures]
          (case reason
            ::not-allowed   (println (format "✋  %s not allowed entry" invalid-import))
            ::no-valid-gate (println (format "🏰  %s walled off" invalid-import)))))))

  (println))

(defn find-and-parse-ns [path]
  (->> (clojure.java.io/file path)

       f/find-ns-decls-in-dir

       (map (fn [ns]
              [(p/name-from-ns-decl ns)
               (p/deps-from-ns-decl ns)]))

       (sort-by first)))

(defn castle
  "I don't do a lot."
  [project & args]

  (let [src-paths (concat (:source-paths project) (:test-paths project))
        rules (:castle project)
        namespaces (mapcat find-and-parse-ns src-paths)

        failures (mapcat (fn [[current-ns imports]]
                           (mapcat (partial import-valid rules current-ns) imports))
                         namespaces)]
    (print-failures failures)

    (if (empty? failures)
      (do
        (println art)
        (System/exit 0))
      (do
        (println badart)
        (System/exit 1)))))

