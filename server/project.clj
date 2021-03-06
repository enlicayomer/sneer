(defproject sneer.server "0.1.0-SNAPSHOT"
  :description "The Sneer Temporary Server"
  :dependencies [[me.sneer/sneer-java-api "0.1.5"]
                 [me.sneer/core "0.1.5"]
                 [http-kit "2.1.18"]
                 [org.clojure/core.match "0.2.2"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/clojure "1.7.0"]
                 [compojure "1.4.0"]
                 [ring/ring-core "1.4.0"]]
  :main ^:skip-aot sneer.server.main
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[midje "1.7.0"]
                                  [org.xerial/sqlite-jdbc "3.8.11.1"]]
                   :plugins [[lein-midje "3.1.3"]]}})
