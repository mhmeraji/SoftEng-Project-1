(ns data-fetcher.core
  (:gen-class)
  (:require
   [aero.core :refer [read-config]]
   [com.stuartsierra.component :as component]
   [taoensso.timbre :as timbre]
   [taoensso.timbre.appenders.core :as appenders]

   [hermes.lib.component.core :as hermes.component]

   [data-fetcher.state.core]
   [data-fetcher.receiver.core]
   [data-fetcher.db.postgres]

   [clojure.core.async :as async])
  (:import
   [clojure.lang IPending]))

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;

(defn prepare-for-doomsday!!
  "This function the uncaughtException Handler thread in order to
  deliver doomsday promise and exit the system in case of receiving
  an uncaught exception"
  [doomsday-promise]
  {:pre [(instance? IPending doomsday-promise)]}
  (Thread/setDefaultUncaughtExceptionHandler
    (reify Thread$UncaughtExceptionHandler
      (uncaughtException [_ thread ex]
        (timbre/error ["It happened .. it finally happened!! "
                       "Uncaught exception on thread "
                       (.getName thread) ex])
        (deliver doomsday-promise :DOOM)
        ;; (System/exit -1)
        ))))

(defn -main
  [config-path & args]
  (let [doomsday-promise (promise)
        config (-> config-path
                   (read-config))
        system (-> config hermes.component/create-component)]
    (prepare-for-doomsday!! doomsday-promise)
    (-> system
        component/start)))

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;
;; sample code for start/stop of the system
(comment

  (timbre/merge-config!
   {:appenders
    {:spit (appenders/spit-appender
            {:fname
             (str "/home/mhmeraji/University_Projects/SoftEng-Project-1/data-fetcher/log/001.log")})}})

  (def started-system
    (-main "./resources/definitions/system.edn"))

  (com.stuartsierra.component/stop started-system)

  )
