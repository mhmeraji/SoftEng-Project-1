(ns administrator.web.core
  (:require
   [clojure.spec.alpha :as spec]

   [taoensso.timbre :as timbre]
   [aero.core :refer [read-config]]
   [com.stuartsierra.component :as component]

   [io.pedestal.http :as http]

   [hermes.lib.component.core :as hermes.component]

   [administrator.web.service :as service]
   [administrator.db.protocol :as db.proto]
   [administrator.web.routes :as api.routes]))

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;

(defn test?
  [service-map]
  (= :test (:env service-map)))

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;

(defrecord Web-Server [config runnable-service
                       ;; dependencies
                       db]

  component/Lifecycle
  (start [component]
    (timbre/spy :info ["Starting Web Module" config])
    (let [routes      (api.routes/routes db)
          service-map (service/get-service-map config routes)]
      (if (some? runnable-service)
        component
        (cond-> service-map
          true
          http/create-server

          (not (test? service-map))
          http/start

          true
          ((partial assoc component :runnable-service))))))

  (stop [component]
    (do (timbre/spy :info ["Stopping Web Module" config])
        (when (and (some? runnable-service) (not (test? config)))
          (http/stop runnable-service))
        (assoc component :runnable-service nil))))

;;------------------------------------------------------------------;;

(defmethod hermes.component/create-component
  [:web-server :pedestal]
  [definition]
  (-> {:config (:component/config definition)}
      (map->Web-Server)))

(defmethod hermes.component/config-spec
  [:web-server :pedestal]
  [_]
  (-> any?))

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;
