(ns administrator.db.conn-pool
  "This namespace is providing multiple facilities to work with postgresDB."
  (:import (com.mchange.v2.c3p0 ComboPooledDataSource))
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.jdbc :as jdbc]

   [ragtime.jdbc]
   [ragtime.repl]
   [ragtime.core]

   [honeysql.core :as sql]
   [honeysql.helpers          :refer [insert-into columns values join left-join
                                      select where from order-by limit]]
   [honeysql-postgres.helpers :refer [upsert on-conflict do-update-set returning]]
   [honeysql-postgres.format]

   [com.stuartsierra.component :as component]

   [taoensso.timbre :as timbre]))

;;------------------------------------------------------------------;;
;; database configuration
;;------------------------------------------------------------------;;

(defn- load-ragtime-config
  [config migrations-path]
  {:datastore (ragtime.jdbc/sql-database config)
   :migrations (ragtime.jdbc/load-resources migrations-path)})

(defn migrate
  "Load migrations from `migration-path` and migrates the migrations
  corresponding to migration keys specified as `to-migrate-keys` arg
  on the db specified with `db-config`. Finally it reports back the list of
  migration keys currently that are currently applied on the db.
  NOTE :: the migration key for a file named as '002-initial.up.sql' is
  `002-initial.up`"
  [db-config migrations-path & to-migrate-keys]
  {:pre [(not (empty? to-migrate-keys))]}
  (let [migratable-db    (ragtime.jdbc/sql-database db-config)
        migrations       (ragtime.jdbc/load-resources migrations-path)
        migrations-index (ragtime.core/into-index migrations)]
    (doseq [mig-key to-migrate-keys]
      (ragtime.core/migrate migratable-db (get migrations-index mig-key)))))

(defn rollback
  "Load migrations from `migration-path` and rollbacks the migrations
  corresponding to the keys specified as `to-rollback-keys` arg
  on the db specified with `db-config`.Finally it reports back the list of
  migration keys currently that are currently applied on the db.
  NOTE :: the key for a migration file named as '002-initial.up.sql' is
  `002-initial.up`"
  [db-config migrations-path & to-rollback-keys]
  (let [migratable-db    (ragtime.jdbc/sql-database db-config)
        migrations       (ragtime.jdbc/load-resources migrations-path)
        migrations-index (ragtime.core/into-index migrations)]
    (doseq [mig-key to-rollback-keys]
      (ragtime.core/rollback migratable-db (get migrations-index mig-key)))
    (map :id (ragtime.core/applied-migrations migratable-db migrations-index))))

;;------------------------------------------------------------------;;
;; connection pool
;;------------------------------------------------------------------;;

(defmacro ^:private resolve-new
  [class]
  (when-let [resolved (resolve class)]
    `(new ~resolved)))

(defn- as-properties
  [m]
  (let [p (java.util.Properties.)]
    (doseq [[k v] m]
      (.setProperty p (name k) (str v)))
    p))

(defn- pool
  [{:keys [subprotocol
           subname
           classname
           excess-timeout
           idle-timeout
           initial-pool-size
           minimum-pool-size
           maximum-pool-size
           test-connection-query
           idle-connection-test-period
           test-connection-on-checkin
           test-connection-on-checkout]
    :or {excess-timeout (* 30 60)
         idle-timeout (* 3 60 60)
         initial-pool-size 3
         minimum-pool-size 3
         maximum-pool-size 15
         test-connection-query nil
         idle-connection-test-period 0
         test-connection-on-checkin false
         test-connection-on-checkout false}
    :as spec}]
  {:datasource
   (doto (resolve-new ComboPooledDataSource)
                 (.setDriverClass classname)
                 (.setJdbcUrl (str "jdbc:" subprotocol ":" subname))
                 (.setProperties (as-properties
                                  (dissoc spec
                                          :classname
                                          :subprotocol
                                          :subname
                                          :naming
                                          :delimiters
                                          :alias-delimiter
                                          :excess-timeout
                                          :idle-timeout
                                          :initial-pool-size
                                          :minimum-pool-size
                                          :maximum-pool-size
                                          :test-connection-query
                                          :idle-connection-test-period
                                          :test-connection-on-checkin
                                          :test-connection-on-checkout)))
                 (.setMaxIdleTimeExcessConnections excess-timeout)
                 (.setMaxIdleTime idle-timeout)
                 (.setInitialPoolSize initial-pool-size)
                 (.setMinPoolSize minimum-pool-size)
                 (.setMaxPoolSize maximum-pool-size)
                 (.setIdleConnectionTestPeriod idle-connection-test-period)
                 (.setTestConnectionOnCheckin test-connection-on-checkin)
                 (.setTestConnectionOnCheckout test-connection-on-checkout)
                 (.setPreferredTestQuery test-connection-query))})

(defn close-pool!
  [{{:keys [datasource]} :pool}]
  (doto ^com.mchange.v2.c3p0.ComboPooledDataSource datasource
    (.close)))

(defn create-pool!
  [config]
  {:pool (-> config (pool))
   :options {:naming {:fields identity :keys identity}
             :delimiters ["\"" "\""]
             :alias-delimiter " AS "}})

(defn get-connection
  [{:keys [pool]}]
  (->> pool))

;;------------------------------------------------------------------;;
;; Query and Transaction Facilities
;;------------------------------------------------------------------;;

(defn format-sql
  [sql-map & params]
  (apply
   sql/format
   sql-map
   (conj
    (into [] params)
    :quoting :ansi
    :allow-dashed-names? true)))

(defn exec!-
  [conn op]
  (jdbc/execute! conn (format-sql op)))

(defn transact!
  [db operation-fn]
  (jdbc/with-db-transaction [conn (get-connection db)]
    (operation-fn conn)))

(defn query-
  [conn sql-map & params]
  (jdbc/query conn (apply format-sql sql-map params)))

(defn query
  [conn-pool sql-map & params]
  (jdbc/with-db-connection [conn (get-connection conn-pool)]
    (apply query- conn sql-map params)))

(defn read-only-transact!
  [db operation-fn]
  (jdbc/with-db-transaction [conn (get-connection db) {:read-only? true}]
    (operation-fn conn)))

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;
