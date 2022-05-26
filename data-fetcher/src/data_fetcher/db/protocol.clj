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
  (<-groups    [db])
  (<-last-records [db])
  (insert-message! [db message])
  (<-output-ch [db]))

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;
