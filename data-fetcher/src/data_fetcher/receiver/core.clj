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
   [clojure.set :as clj-set]
   [hermes.lib.component.core :as hermes.component]))

;;------------------------------------------------------------------;;
;; Base Functions
;;------------------------------------------------------------------;;

(defn get-order-http-call
  [group-id fetch-count
   s-timeout c-timeout]
  (-> (try
        (http/get
          (str "https://tg.i-c-a.su/json/"
               group-id "?limit="
               fetch-count)
          {:connection-timeout c-timeout
           :socket-timeout     s-timeout})
        (catch Exception e
          (timbre/error "Encountered Error While Getting Messages")
          (timbre/error e))
        ;; (catch java.net.SocketTimeoutException e
        ;;   (timbre/error ["Encountered Timeout Exception" e]))
        ;; (catch Exception e
        ;;   (timbre/error "Encountered Error While Getting Messages")
        ;;   (throw e))
        )
      :body
      (cheshire/parse-string keyword)
      :messages))

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;

(defn trim-message
  [message g-id]
  (-> message
      (select-keys
        [:id :date :edit_date :pinned :silent
         :edit_hide :noforwards :mentioned :legacy
         :from_scheduled :out :forwards :post
         :message :views])
      (clj-set/rename-keys {:id :m_id})
      (assoc :channel-id g-id)))

(defn trim-messages
  [messages g-id]
  (reduce
    (fn [acc message]
      (conj acc (trim-message message g-id)))
    []
    messages))

(defn <-fetcher-groups
  [db-groups]
  (reduce
    (fn [acc db-group]
      (conj
        acc
        (assoc {}
               :channel-id (get db-group :channel_id)
               :fetch-count (get db-group :fetch_count))))
    []
    db-groups))

(defn start-polling-signals
  [db db-ch state interrupt-ch
   fetch-rate s-timeout c-timeout
   sleep-time]
  (try
    (async/thread
      (loop []
        (let [groups   (db.proto/<-groups db)
              f-groups (<-fetcher-groups groups)]
          (doseq [g f-groups]
            (let [raw-messages (get-order-http-call
                                 (:channel-id g) (:fetch-count g)
                                 s-timeout c-timeout)

                  messages     (trim-messages raw-messages (:channel-id g))]
              (doseq [message messages]
                (when-not (state.proto/repeated-message?! state message)
                  (db.proto/insert-message! db message))))
            (Thread/sleep sleep-time)))
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
     sleep-time
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
            sleep-time   (-> config
                             :sleep-time)

            db-ch        (db.proto/<-output-ch db)]

        (start-polling-signals
          db db-ch state
          interrupt-ch fetch-rate
          s-timeout c-timeout sleep-time)

        (assoc component
               :input-ch input-ch
               :socket-timeout s-timeout
               :sleep-time sleep-time
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
