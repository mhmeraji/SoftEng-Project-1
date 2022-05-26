(ns data-fetcher.db.protocol
  "Access protocol for working with db component")

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;

(defprotocol Pooling
  (create-pool! [db])
  (close-pool! [db])
  (transact! [db fn])
  (read-only-transact! [db fn])
  (get-connection [db]))

(defprotocol Access
  (<-output-ch [db]))

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;
