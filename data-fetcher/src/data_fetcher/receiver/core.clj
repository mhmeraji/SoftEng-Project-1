(ns data-fetcher.receiver.core
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.string :as str]
   [tick.core :as tick]
   [clojure.core.async :as async]
   [com.stuartsierra.component :as component]
   [taoensso.timbre :as timbre]
   [clj-http.client :as http]
   [cheshire.core :as cheshire]
   [data-fetcher.state.protocol :as state.proto]
   [data-fetcher.db.protocol :as db.proto]
   [hermes.lib.component.core :as hermes.component]))

;;------------------------------------------------------------------;;
;; Base Functions
;;------------------------------------------------------------------;;

(defn get-order-http-call [base-address token
                           s-timeout c-timeout]
  ;; (-> (try (http/get
  ;;            (str base-address
  ;;                 "/orders?$sortby=-id&$perpage=700&status=3&status=2&status=6&$"
  ;;                 "fromdate.created_at="
  ;;                 (str (tick/today) "T00:00:00")
  ;;                 "&$todate.created_at="
  ;;                 (str (tick/tomorrow) "T00:00:00"))
  ;;            {:headers {:Authorization
  ;;                       (str "Bearer " token)
  ;;                       :content-type "application/json"}
  ;;             :connection-timeout c-timeout
  ;;             :socket-timeout s-timeout})
  ;;          (catch Exception e
  ;;            (timbre/error ["Error In Getting Order Status From Mazdax"
  ;;                           e])
  ;;            (throw e)))
  ;;     :body
  ;;     (cheshire.core/parse-string keyword)
  ;;     vec)
  )

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;

(defn start-polling-signals
  [db-ch state interrupt-ch
   fetch-rate s-timeout c-timeout]
  (try
    (async/thread
      (loop []

        (let
            [[v p] (async/alts!! [interrupt-ch] :default "go")]
             (when (not= v "stop")
               (Thread/sleep fetch-rate)
               (recur)))))
       (catch Exception e
         (timbre/info (ex-data e)))))

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;

(defrecord Telegram-Receiver
    [config
     ;;------------------------------
     state
     db
     interrupt-ch
     fetch-rate
     socket-timeout
     connection-timeout
     ;;------------------------------
     input-ch]

  ;;----------------------------------------------------------------;;
    component/Lifecycle
    ;;----------------------------------------------------------------;;

    (start [component]
      (timbre/info ["Starting Telegram Receiver Component" config])
      (let [interrupt-ch (async/chan)
            input-ch     (async/chan 1024)
            fetch-rate   (-> config
                             :fetch-rate)
            s-timeout    (-> config
                             :socket-timeout)
            c-timeout    (-> config
                             :connection-timeout)

            db-ch        (db.proto/<-output-ch db)]

        (start-polling-signals
          db-ch state
          interrupt-ch fetch-rate
          s-timeout c-timeout)

        (assoc component
               :input-ch input-ch
               :socket-timeout s-timeout
               :connection-timeout c-timeout
               :interrupt-ch interrupt-ch)))

    (stop [component]

      (timbre/info "Stopping Telegram Receiver Component")
      (when (some? input-ch)
        (async/close! input-ch))

      (when (some? interrupt-ch)
        (async/put! interrupt-ch "stop"))

      (when (some? interrupt-ch)
        (async/close! interrupt-ch))
      (-> component)))

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;

(defmethod hermes.component/create-component
  [:receiver :telegram :v-0.0.1]
  [definition]
  (-> {:config (-> definition :component/config)}
      (map->Telegram-Receiver)))

(spec/def ::config any?)

(defmethod hermes.component/config-spec
  [:receiver :telegram :v-0.0.1]
  [_]
  (-> ::config))

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;
