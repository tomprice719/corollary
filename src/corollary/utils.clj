(ns corollary.utils)

(defn now []
  (.getTime (new java.util.Date)))
