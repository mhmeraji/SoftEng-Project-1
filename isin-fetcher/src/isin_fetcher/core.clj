(ns isin-fetcher.core
  (:gen-class)
  (:require
   [cheshire.core :as cheshire]
   [clojure.string :as str]
   [clojure.xml :as xml]
   [clojure.string :as string]
   [net.cgrand.enlive-html :as html]
   [clj-fuzzy.metrics :as str-metrics]
   [net.cgrand.tagsoup :as tagsoup]
   [clj-http.client :as http]
   [aero.core :as aero]
   [taoensso.timbre :as timbre]
   [incanter.core :as i]
   [isin-fetcher.sql :as sql]))

;;-----------------------------------------------------------------;;
;; Utillity functions
;;-----------------------------------------------------------------;;

(defn string->stream
  ([s] (string->stream s "UTF-8"))
  ([s encoding]
   (-> s
       (.getBytes encoding)
       (java.io.ByteArrayInputStream.))))

(defn in?
  "true if coll contains elm"
  [coll elm]
  (some #(= elm %) coll))

(defn get-faraboors-symbol-short-names []
  (let [resources
        (html/html-resource
         (java.net.URL.
          "https://www.fipiran.com/Market/LupBasePTC"))]
    (map
     (fn [x]
       (first (:content x)))
     (html/select resources [:td :a]))))

(defn get-non-base-faraboors-symbol-short-names []
  (let [resources
        (html/html-resource
         (java.net.URL.
          "https://www.fipiran.com/Market/LupOTC"))]
    (map
     (fn [x]
       (first (:content x)))
     (html/select resources [:td :a]))))

(defn get-oragh-akhza []
  (let [resources
        (html/html-resource
         (java.net.URL.
          "https://www.fipiran.com/Market/LupOTCO"))]
    (map
     (fn [x]
       (first (:content x)))
     (html/select resources [:td :a]))))
;;-----------------------------------------------------------------;;
;;-----------------------------------------------------------------;;

(defn get-general-isin-symbol-map []
  (reduce
   (fn [collection x]
     (let [isin 1
           farsi-name 2]
       (conj collection {:isin ((str/split x  #",") isin)
                         :short-name ((str/split x  #",") farsi-name)
                         :faraboors "false"})))
   []
   (-> ((str/split
         (->
          (:body
           (http/get
            "http://www.tsetmc.com/tsev2/data/MarketWatchPlus.aspx?h=0&r=0")))
         #"@") 2) (str/split #";"))))
;;-----------------------------------------------------------------;;
;;-----------------------------------------------------------------;;

(defn find-fara-boors-symbols []
  (let [isins-names (get-general-isin-symbol-map)]
    (->>
     (map
      (fn [x]
        (apply max-key
               :similarity
               (map
                (fn [element]
                  {:similarity (clj-fuzzy.metrics/dice
                                (:short-name element)
                                x)
                   :most-similliar-symbol (:short-name element)
                   :isin    (:isin element)
                   :short-name x
                   :faraboors "true"})
                isins-names)))
      (concat
       (get-faraboors-symbol-short-names)
       (get-non-base-faraboors-symbol-short-names)
       (get-oragh-akhza)))
     (map
      (fn [x]
        (dissoc
         x
         :most-similliar-symbol
         :similarity))))))

(defn populate-database [sql-access]
  (let [general-map (timbre/spy
                      (reduce (fn [acc row]
                                (conj acc {(:isin row) row}))
                              (get-general-isin-symbol-map)))
        faraboors-map (timbre/spy
                        (reduce (fn [acc row]
                                  (conj acc {(:isin row) row}))
                                (find-fara-boors-symbols)))]
    (doseq
        [rec (vec (merge general-map faraboors-map))]
        (sql/upsert-isin-map! sql-access rec))))

;;-----------------------------------------------------------------;;
;;-----------------------------------------------------------------;;

(defn -main [arg]

  (if (-> arg count zero? not)
    (let [{:keys [sql-type sql-classname
                  sql-subprotocol sql-subname
                  sql-user sql-password]
           :as script-config}
          (timbre/spy (aero/read-config arg))

          sql-access (timbre/spy
                       (sql/create-pool!
                         {:type        sql-type
                          :classname   sql-classname
                          :subprotocol sql-subprotocol
                          :subname     sql-subname
                          :user        sql-user
                          :password    sql-password}))]
      (populate-database sql-access)
      (sql/close-pool! sql-access))
    (timbre/spy "Config File not found or not provided please run again")))

;;-----------------------------------------------------------------;;
;;-----------------------------------------------------------------;;

(comment

  (-main "/home/mhmeraji/university-projects/SoftEng-Project-1/isin-fetcher/resources/config.edn")

  (+ 1 1)

  )
