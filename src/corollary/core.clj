(ns corollary.core
  (require [compojure.handler :refer [site]]
           [clojure.java.io :as io]
           [ring.adapter.jetty :as jetty]
           [environ.core :refer [env]]
           [migratus.core :as migratus]
           [ring.middleware.session.cookie :refer [cookie-store]]
           [selmer.parser]
           [corollary.routes :refer [app]]))

(def migratus-config {:store                :database
                      :migration-dir        "migrations/"
                      :migration-table-name "migrations"
                      :db (env :database-url)})

(defn -main [& [port]]
  (if (nil? (env :production))
    (do (println "selmer cache turned off")
      (selmer.parser/cache-off!))
    (println "selmer cache left on"))
  (migratus/migrate migratus-config)
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app
                           {:session {:store (cookie-store {:key (.getBytes (env :cookie-store-key))})}})
                     {:port port :join? false})))

(defn start []
  (def server (-main)))

(defn stop []
  (.stop server))

(defn reload []
  (require 'corollary.core :reload-all))

(defn restart []
  (stop)
  (reload)
  (start))
