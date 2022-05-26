(ns administrator.web.handlers
  (:require
   [clojure.java.io :as io]

   [clojure.string :as string]

   [ring.util.response :as ring-resp]
   [ring.util.io :as ring-io]

   [taoensso.timbre :as timbre]
   [administrator.db.protocol :as db.proto]
   [aero.core :as aero])
  (:import java.util.Base64))

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;

(defn register-channel [db]
  {:name  ::register-channel
   :enter (fn register-channel
            [{request :request :as context}]
            (try
              (let [ch-id       (-> request :json-params :channel-id)
                    fetch-count (-> request :json-params :fetch-rate)
                    g-map       {:channel_id   ch-id
                                 :channel_type "bourse"
                                 :fetch_count  fetch-count
                                 :verified?    false}

                    _ (db.proto/register-channel db g-map)
                    response (ring-resp/response
                               {:message         "Channel Succesfuly Registered"
                                :channel-id      ch-id
                                :succesful?      true})]
                (assoc context :response response))
              (catch Exception e
                (timbre/spy ["Channel Register Failed!" e])
                (assoc
                  context
                  :response
                  (ring-resp/not-found
                    {:message    "Channel Register Failed"
                     :succesful? false})))))})

(defn delete-channel [db]
  {:name  ::delete-channel
   :enter (fn delete-channel
            [{request :request :as context}]
            (try
              (let [ch-id       (-> request :json-params :channel-id)

                    _ (db.proto/delete-channel db ch-id)

                    response (ring-resp/response
                               {:message         "Channel Succesfuly Deleted"
                                :channel-id      ch-id
                                :succesful?      true})]

                (assoc context :response response))
              (catch Exception e
                (timbre/spy ["Channel Deletion Failed!" e])
                (assoc
                  context
                  :response
                  (ring-resp/not-found
                    {:message    "Channel Deletion Failed"
                     :succesful? false})))))})

(defn verify-channel [db]
  {:name  ::verify-channel
   :enter (fn verify-channel
            [{request :request :as context}]
            (try
              (let [ch-id       (-> request :json-params :channel-id)
                    status       (-> request :json-params :verification-status)

                    _ (db.proto/verify-channel db ch-id status)

                    response (ring-resp/response
                               {:message         "Channel Succesfuly Verified"
                                :channel-id      ch-id
                                :succesful?      true})]

                (assoc context :response response))
              (catch Exception e
                (timbre/spy ["Channel Verification Failed!" e])
                (assoc
                  context
                  :response
                  (ring-resp/not-found
                    {:message    "Channel Verification Failed"
                     :succesful? false})))))})

(defn update-channel [db]
  {:name  ::udpate-channel
   :enter (fn update-channel
            [{request :request :as context}]
            (try
              (let [ch-id       (-> request :json-params :channel-id)
                    fetch-count (-> request :json-params :fetch-rate)

                    _ (db.proto/update-channel db ch-id fetch-count)

                    response (ring-resp/response
                               {:message         "Channel Succesfuly Updated"
                                :channel-id      ch-id
                                :succesful?      true})]
                (assoc context :response response))
              (catch Exception e
                (timbre/spy ["Channel Update Failed!" e])
                (assoc
                  context
                  :response
                  (ring-resp/not-found
                    {:message    "Channel Update Failed"
                     :succesful? false})))))})

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;
