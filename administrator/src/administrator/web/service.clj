(ns administrator.web.service
  (:require [io.pedestal.http :as http]))

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;

(defn get-service-map [{:keys [env host port resource-path allowed-origins]
                        :as config}
                       routes]
  {:pre [(some? env)
         (some? port)
         (some? host)
         (some? resource-path)
         (some? allowed-origins)]}
  {:env                     env
   ::http/port              port
   ::http/host              host
   ::http/resource-path     resource-path
   ::http/routes            routes
   ;; ::http/router            :prefix-tree
   ::http/allowed-origins   allowed-origins
   ::http/join?             false
   ::http/type              :jetty
   ::http/container-options {:h2c? true
                             :h2?  false
                             :ssl? false}})

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;
