(defproject data-fetcher "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [clj-http "3.12.3"]
                 [cheshire "5.10.0"]
                 [com.taoensso/timbre "5.1.2"]]
  :repl-options {:init-ns data-fetcher.core})
(defproject data-fetcher "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [;; clojure libraries
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/test.check "1.1.0"]
                 [org.clojure/spec.alpha "0.2.187"]
                 [org.clojure/core.async "1.3.618"]
                 [org.clojure/tools.cli "1.0.206"]

                 ;; external libraries
                 [tick "0.5.0-RC5"]
                 [aero "1.1.6"]
                 [cheshire "5.10.0"]
                 [com.taoensso/nippy "3.1.1"]
                 [com.stuartsierra/component "1.0.0"]

                 ;; logging
                 [com.taoensso/timbre "5.1.2"]
                 [org.graylog2/gelfclient "1.5.1"]

                 ;; postgres
                 [com.mchange/c3p0 "0.9.5.5"]
                 [org.postgresql/postgresql "42.2.23"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [nilenso/honeysql-postgres "0.4.112"]
                 [ragtime "0.8.1"]

                 ;; http
                 [clj-http "3.12.3"]

                 ;; hermes libraries
                 [hermes.lib/component "1.0.0"]

                 ;;pedestal
                 [io.pedestal/pedestal.service       "0.5.10"]
                 [io.pedestal/pedestal.service-tools "0.5.10"]
                 [io.pedestal/pedestal.route         "0.5.10"]
                 [io.pedestal/pedestal.jetty         "0.5.10"]
                 ;;
                 [org.clojure/test.check "1.1.0"]
                 [org.clojure/spec.alpha "0.2.187"]]

  ;; :repl-options {:init-ns hermes.van-buren.sle-router.core}

  :min-lein-version "2.0.0"

  :jvm-opts       ["-XX:+UseG1GC"]
  :javac-options  ["-target" "11" "-source" "11"]

  :global-vars {*warn-on-reflection* false
                *assert*             true}

  :main data-fetcher.core

  :profiles {:dev     {:global-vars  {*warn-on-reflection* true
                                      *assert*             true}
                       :plugins      [[jonase/eastwood "0.3.10"]
                                      [lein-marginalia "0.9.1"]]
                       :dependencies []}
             :uberjar {:aot :all}}

  :repositories [["releases"  {:url           "https://nexus.stellaramc.ir/repository/maven-releases/"
                               :username      :env/nexus_username
                               :password      :env/nexus_password
                               :sign-releases false}]
                 ["snapshots" {:url           "https://nexus.stellaramc.ir/repository/maven-snapshots/"
                               :username      :env/nexus_username
                               :password      :env/nexus_password
                               :sign-releases false}]]
  )
