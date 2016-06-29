(ns corollary.utils)

(defn now []
  (.getTime (new java.util.Date)))

(defn thrush [x] ;;currently unused
  #(% x))
