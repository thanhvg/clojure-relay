(defproject relay "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/data.xml "0.0.8"]
                 [org.clojure/data.zip "1.1.0"]
                 [ring/ring-core "1.12.1"]
                 [org.clojure/data.codec "0.1.1"]
                 [ring/ring-jetty-adapter "1.12.1"]]
  :main ^:skip-aot relay.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
