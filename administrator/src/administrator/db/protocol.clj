(ns administrator.db.protocol
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
  (register-channel [db ch-map])
  (delete-channel [db ch-id])
  (update-channel [db ch-id fetch-count])
  (verify-channel [db ch-id status]))

;;------------------------------------------------------------------;;
;;------------------------------------------------------------------;;
