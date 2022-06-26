(defproject isin-fetcher "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [stellar.lib/schema "0.4.8-SNAPSHOT"]
                 [hermes.lib/component "1.0.3"]
                 [aero "1.1.6"]
                 ;; [http-kit "2.4.0"]
                 [org.clojure/tools.cli "1.0.206"]

                 [incanter "1.5.5"]
                 [enlive "1.1.5"]
                 [clj-fuzzy "0.4.1"]
                 [clj-http "3.12.3"]
                 [org.clojure/core.async "1.3.618"]
                 [cheshire "5.10.1"]
                 [com.novemberain/monger "3.5.0"]
                 [com.taoensso/timbre "5.1.2"]
                  ;; buddy
                 [clojure.java-time "0.3.3"]
                 [buddy/buddy-hashers "1.4.0"]
                 [buddy/buddy-auth "2.2.0"]

                 ;; postgres
                 [com.mchange/c3p0 "0.9.5.5"]
                 [org.postgresql/postgresql "42.3.3"]
                 [org.clojure/java.jdbc "0.7.12"]
                 [nilenso/honeysql-postgres "0.4.112"]
                 [ragtime "0.8.1"]]

  :main     isin-fetcher.core
  :repl-options {:init-ns isin-fetcher.core}
  :profiles {:dev     {:plugins      [[jonase/eastwood "0.4.0"]]}}
  :repositories [["releases"  {:url           "https://nexus.stellaramc.ir/repository/maven-releases/"
                               :username      "pourya"
                               :password      "AliAmidianAmoodian@123"
                               :sign-releases false}]
                 ["snapshots" {:url           "https://nexus.stellaramc.ir/repository/maven-snapshots/"
                               :username      "pourya"
                               :password      "AliAmidianAmoodian@123"
                               :sign-releases false}]])
