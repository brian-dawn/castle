(defproject lein-castle "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/tools.namespace "0.3.0-alpha4"]]
  :plugins [[lein-cljfmt "0.5.6"]]

  :castle {clojure.set {:gates [com.myproj.io.db.public]
                        :allowed [com.myproj.io.routes]}}
  
  :eval-in-leiningen true)
