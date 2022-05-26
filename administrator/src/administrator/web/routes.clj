(ns administrator.web.routes
  (:require [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route :as pedestal-route]
            [io.pedestal.http.ring-middlewares :as ring-mw]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.http.route :as route]

            [taoensso.timbre :as timbre]

            [cheshire.core :as cheshire]

            [io.pedestal.interceptor.chain :as interceptor.chain]
            [io.pedestal.http.content-negotiation :as contneg]

            [ring.util.response :as ring-resp]

            [administrator.web.handlers :as handlers]
            [administrator.db.conn-pool :as conn-pool]))

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;

(defn get-url-id [request]
  (get-in request [:path-params :id]))

(def get-id-interceptor
  {:name ::get-id-interceptor
   :enter (fn [context]
            (let [request (:request context)
                  new-context
                  (assoc-in
                   context
                   [:request :body-params :id]
                   (get-url-id request))]
              new-context))})

(def log-everything-interceptor
  {:name  ::log-everything-interceptor
   :enter (fn [context] (timbre/info "ENTER -> " (-> context :request)) context)
   :leave (fn [context] (timbre/info "LEAVE -> " context) context)
   :error (fn [context ex]
            (timbre/info "Error ->>" (ex-data ex))
            (-> context
                (assoc ::interceptor.chain/error ex)))})

(def body-coercion-interceptor
  {:name  ::body-coercion-interceptor
   :error
   (fn [context e]
     (->> context))
   :leave (fn [context]
            (-> context
                (update-in [:response :body] pr-str)
                (update-in [:response]
                           #(ring-resp/content-type % "application/edn"))))})

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;

(def supported-types
  ["application/edn" "application/json" "application/octet-stream"])

(def content-negotiation-interceptor
  (contneg/negotiate-content
   supported-types
   {:no-match-fn (fn [context]
                   (throw (Exception. "Content Negotiation : No Match!")))}))

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;

(defn routes [db]
  [[:administrator :http
    ["/channel"
     ^:interceptors [body-coercion-interceptor
                     content-negotiation-interceptor
                     ;; log-everything-interceptor
                     (ring-mw/multipart-params)
                     (body-params/body-params)
                     get-id-interceptor]
     ["/register"
      {:post   (interceptor/interceptor
                 (handlers/register-channel db))
       :delete (interceptor/interceptor
                 (handlers/delete-channel db))}]
     ["/verify"
      {:post (interceptor/interceptor
               (handlers/verify-channel db))}]
     ["/update"
      {:post (interceptor/interceptor
               (handlers/update-channel db))}]]]])

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;
