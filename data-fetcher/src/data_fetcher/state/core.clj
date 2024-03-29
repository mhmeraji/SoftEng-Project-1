(ns data-fetcher.state.core
  "State component which is used for storing and updating the whole
  world state on each event is implemented in this namespace. All the
  main components will acquire state component as their dependency to
  calculate proper actions upon the receive of different events."
  (:require
   [com.stuartsierra.component :as component]

   [taoensso.timbre :as timbre]
   [data-fetcher.db.protocol :as db.proto]

   [hermes.lib.component.core :as hermes.component]
   [data-fetcher.state.protocol :as proto]))

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;

(defn in?
  "true if coll contains elm"
  [coll elm]
  (some #(= elm %) coll))

(defn- initiate-state
  [init-value]
  (ref {:signals init-value}))

(defn- fetch-db-last-records
  [db]
  (let [last-records (db.proto/<-last-records db)
        grouped      (group-by :channel-id last-records)]
    (reduce
      (fn [acc ch]
        (let [ids (get grouped ch)]
          (assoc acc ch (map :m_id ids))))
      {}
      (keys grouped))))

(defrecord STATE-v-0-1-0

    [config ref-state db]

  ;;----------------------------------------------------------------;;
  component/Lifecycle
  ;;----------------------------------------------------------------;;

  (start [component]
    (timbre/info ["Starting State Component" config])
    (let [db-initial (fetch-db-last-records db)
          init-state (initiate-state db-initial)]
      (-> component
          (assoc :ref-state init-state))))

  (stop [component]
    (timbre/info "Stopping State Component")
    (-> component))

  proto/Access

  (<-state [state]
    (deref ref-state))

  (repeated-message?! [state message]
    (let [st-map     (proto/<-state state)
          id         (:m_id message)
          channel-id (:channel-id message)
          messages   (get-in st-map [:signals channel-id])
          result-map (if (nil? channel-id)
                       (assoc st-map channel-id [])
                       (->> st-map))]
      (if (in? messages id)
        (->> true)
        (do
          (dosync
            (ref-set
              ref-state
              (update-in result-map
                         [:signals channel-id]
                         conj id)))
          (->> false)))))
  )


;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;

(defmethod hermes.component/create-component
  [:state :ref :v-0.0.1]
  [definition]
  (-> {:config (-> definition :component/config)}
      (map->STATE-v-0-1-0)))

(defmethod hermes.component/config-spec
  [:state :ref :v-0.0.1]
  [_]
  (-> any?))

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;
