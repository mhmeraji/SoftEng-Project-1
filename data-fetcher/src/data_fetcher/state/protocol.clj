(ns data-fetcher.state.protocol
  "Access protocol for working with state component")

(defprotocol Access
  (repeated-message?! [state message])
  (<-state [state]))
