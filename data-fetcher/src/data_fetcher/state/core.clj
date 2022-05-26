(ns data-fetcher.state.core
  "State component which is used for storing and updating the whole
  world state on each event is implemented in this namespace. All the
  main components will acquire state component as their dependency to
  calculate proper actions upon the receive of different events."
  (:require
   [com.stuartsierra.component :as component]

   [taoensso.timbre :as timbre]

   [hermes.lib.component.core :as hermes.component]
   ))

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;

(defn- initiate-state
  []
  (ref {:signals {}}))

(defrecord STATE-v-0-1-0

    [config ref-state]

  ;;----------------------------------------------------------------;;
  component/Lifecycle
  ;;----------------------------------------------------------------;;

  (start [component]
    (timbre/info ["Starting State Component" config])
    (let [init-state (initiate-state)]
      (-> component
          (assoc :ref-state init-state))))

  (stop [component]
    (timbre/info "Stopping State Component")
    (-> component))
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
