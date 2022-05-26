(ns data-fetcher.db.postgres
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
   [data-fetcher.db.protocol :as proto]
   [data-fetcher.db.conn-pool :as gool]))

;;------------------------------------------------------------------;;
;; Query & DB Functions
;;------------------------------------------------------------------;;

;;------------------------------------------------------------------;;
;; Polling Functions
;;------------------------------------------------------------------;;

(defn- process-response
  [component access input-ch interrupt-ch]
  (async/thread
    (try
      (loop []
        (when-let [db-record (async/<!! input-ch)])

        (let [[v p] (async/alts!! [interrupt-ch] :default "continue")]
          (if (= p :default)
            (recur))))
      (catch Exception e
        (timbre/error ["Error In DB Record Processing Thread : " e])
        (component/stop component)))))

;;------------------------------------------------------------------;;

(defrecord SLE-DB-Module
    [config access input-ch interrupt-ch]

    component/Lifecycle

    (start [component]
      (timbre/info ["Starting DB Component" config])
      (let [access (proto/create-pool! component)
            interrupt-ch (async/chan)
            input-ch (async/chan 1024)]
        (process-response component access input-ch interrupt-ch)
        (-> component
            (assoc :access access
                   :interrupt-ch interrupt-ch
                   :input-ch input-ch))))

    (stop [component]
      (timbre/info "Stopping DB Component")
      (when (some? access)
        (proto/close-pool! component))

      (when (some? input-ch)
        (async/close! input-ch))

      (when (some? interrupt-ch)
        (async/put! interrupt-ch "stop"))

      (when (some? interrupt-ch)
        (async/close! interrupt-ch))

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

    (<-output-ch [_]
      (->> input-ch)))

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
