(ns administrator.db.postgres
  "This namespace hosts the db component main record which is used
  by the journalist component to store state snapshots that will
  be later used by request-receiver component to recover from a
  failure."
  (:require
   [com.stuartsierra.component :as component]
   [taoensso.timbre :as timbre]

   [honeysql.helpers :as honey]
   [honeysql-postgres.helpers :as psqlh]
   [honeysql.core :as sql]
   [clojure.core.async :as async]

   [hermes.lib.component.core :as hermes.component]
   [administrator.db.protocol :as proto]
   [administrator.db.conn-pool :as gool]))

;;------------------------------------------------------------------;;
;; Query & DB Functions
;;------------------------------------------------------------------;;

(defn ->verify-channel-query
  [ch-id status]
  (-> (honey/update :groups_info)
      (honey/sset {:verified? status})
      (honey/where [:= :channel_id ch-id])
      (psqlh/returning :channel_id)))

(defn- ->verify-channel [conn ch-id status]
  (->> (->verify-channel-query ch-id status)
       (gool/query- conn)
       first))

(defn- ->register-channel-query
  [ch-map]
  (-> (honey/insert-into :groups_info)
      (honey/values [ch-map])
      (psqlh/returning :channel_id)))

(defn- ->register-channel [conn ch-map]
  (->> (->register-channel-query ch-map)
       (gool/query- conn)
       first))

(defn ->delete-channel-query
  [ch-id]
  (-> (honey/delete-from :groups_info)
      (honey/where [:= :channel_id ch-id])
      (psqlh/returning :channel_id)))

(defn- ->delete-channel [conn ch-id]
  (->> (->delete-channel-query ch-id)
       (gool/query- conn)
       first))

(defn ->udpate-channel-query
  [ch-id fetch-count]
  (-> (honey/update :groups_info)
      (honey/sset {:fetch_count fetch-count})
      (honey/where [:= :channel_id ch-id])
      (psqlh/returning :channel_id)))

(defn- ->update-channel [conn ch-id fetch-count]
  (->> (->udpate-channel-query ch-id fetch-count)
       (gool/query- conn)
       first))

;;------------------------------------------------------------------;;

(defrecord SLE-DB-Module
    [config access]

    component/Lifecycle

    (start [component]
      (timbre/info ["Starting DB Component" config])
      (let [access (proto/create-pool! component)]
        (-> component
            (assoc :access access))))

    (stop [component]
      (timbre/info "Stopping DB Component")
      (when (some? access)
        (proto/close-pool! component))

      (assoc component
             :access nil))

    ;;--------------------------------------------------------;;

    proto/Pooling

    (create-pool!
      [db]
      (gool/create-pool! config))

    (close-pool!
      [db]
      (-> access
          (gool/close-pool!)))

    (transact!
      [db op-fn]
      (gool/transact! access op-fn))

    (read-only-transact!
      [db op-fn]
      (gool/read-only-transact! access op-fn))

    (get-connection
      [db]
      (gool/get-connection access))

    ;;--------------------------------------------------------;;

    proto/Access

    (register-channel [db ch-map]
      (try
        (proto/transact!
          db (fn [conn]
               (->register-channel conn ch-map)))
        (catch Exception e
          (timbre/error ["Encountered Error While Registering Channel"
                         ch-map e])
          (throw e))))

    (delete-channel [db ch-id]
      (try
        (proto/transact!
          db (fn [conn]
               (->delete-channel conn ch-id)))
        (catch Exception e
          (timbre/error ["Encountered Error While Deleting Channel"
                         ch-id e])
          (throw e))))

    (update-channel [db ch-id fetch-count]
      (try
        (proto/transact!
          db (fn [conn]
               (->update-channel conn ch-id fetch-count)))
        (catch Exception e
          (timbre/error ["Encountered Error While Updating Channel"
                         ch-id fetch-count e])
          (throw e))))

    (verify-channel [db ch-id status]
      (try
        (proto/transact!
          db (fn [conn]
               (->verify-channel conn ch-id status)))
        (catch Exception e
          (timbre/error ["Encountered Error Verifying Channel"
                         ch-id status e])
          (throw e))))

    )

;;------------------------------------------------------------------;;

(defmethod hermes.component/create-component
  [:db :postgres :v-0.0.1]
  [definition]
  (-> {:config (-> definition :component/config)}
      (map->SLE-DB-Module)))

(defmethod hermes.component/config-spec
  [:db :postgres :v-0.0.1]
  [_config]
  (-> any?))

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;
