(ns isin-fetcher.sql
  (:require [clojure.java.jdbc :as jdbc]
            [taoensso.timbre :as timbre]
            [isin-fetcher.conn-pool :as gool]

            [honeysql.helpers :as honey]
            [honeysql-postgres.helpers :as psqlh]
            [honeysql.core :as sql]))

(defn create-pool!
  [config]
  (gool/create-pool! config))

(defn close-pool!
  [access]
  (-> access
      (gool/close-pool!)))

(defn transact!
  [access op-fn]
  (gool/transact! access op-fn))

(defn read-only-transact!
  [access op-fn]
  (gool/read-only-transact! access op-fn))

(defn get-connection
  [access]
  (gool/get-connection access))

(defn- ->upsert-isin-map-query
  [{:keys [isin short-name market-type]
    :as isin-map}]
  (-> (honey/insert-into :isins)
      (honey/values
        [isin-map])
      (psqlh/upsert
        (-> (psqlh/on-conflict :isin)
            (psqlh/do-update-set
              :short-name :market-type)))
      (psqlh/returning :isin)))

(defn- ->upsert-isin-map
  [conn isin-map]
  (->> (->upsert-isin-map-query
         isin-map)
       (gool/query- conn)
       first))

(defn- sqlify-data-format
  [isin-map]
  (let [i-map (if (= (count isin-map) 2)
                (second isin-map)
                (->> isin-map))]
    (if (read-string (:faraboors i-map))
      (-> i-map
          (dissoc :faraboors)
          (assoc :market-type "Farabourse"))
      (-> i-map
          (dissoc :faraboors)
          (assoc :market-type "Bourse")))))

(defn- sqlify-data-format
  [[_ i-map]]
  (if (read-string (:faraboors i-map))
    (-> i-map
        (dissoc :faraboors)
        (assoc :market-type "Farabourse"))
    (-> i-map
        (dissoc :faraboors)
        (assoc :market-type "Bourse"))))

(defn upsert-isin-map!
  [access isin-map]
  (when (and (vector? isin-map)
             (some? (:isin (second isin-map))))
    (try
      (let [sql-record (sqlify-data-format isin-map)]
        (when (some? (:isin sql-record))
          (transact!
            access (fn [conn]
                     (->upsert-isin-map
                       conn sql-record)))))
      (catch Exception e
        (timbre/error ["Couldn't Upsert Isin Map" isin-map e])
        (throw e)))))
