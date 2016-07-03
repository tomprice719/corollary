(ns corollary.utils
  (require [environ.core :refer [env]]))

(def db (env :database-url))

(defn now []
  (.getTime (new java.util.Date)))

(defn thrush [x] ;;currently unused
  #(% x))
