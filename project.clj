(defproject net.thegeez/snippetlist "0.0.1"
  :dependencies [[org.clojure/clojure "1.7.0"]

                 [com.stuartsierra/component "0.2.1"]

                 [net.thegeez/w3a "0.0.8"]

                 [org.webjars/bootstrap "3.3.4"]
                 [org.webjars/jquery "1.11.1"]

                 [buddy/buddy-hashers "0.4.2"]

                 [clj-time "0.11.0"]]

  :resource-paths ["config", "resources"]

  :min-lein-version "2.0.0"

  :source-paths ["src/clj"]
  :profiles {:dev {:source-paths ["dev"]
                   :main user
                   :dependencies [[ns-tracker "0.2.2"]
                                  [org.clojure/tools.namespace "0.2.3"]
                                  [org.clojure/java.classpath "0.2.0"]
                                  [org.apache.derby/derby "10.8.1.2"]
                                  [kerodon "0.7.0"]
                                  [peridot "0.3.1" :exclusions [clj-time]]]}
             :uberjar {:main net.thegeez.snippetlist.main
                       :aot [net.thegeez.snippetlist.main]
                       :uberjar-name "snippetlist-prod-standalone.jar"}})
