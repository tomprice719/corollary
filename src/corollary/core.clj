(ns corollary.core
  (require [clojure.java.io :as io]
           [ring.adapter.jetty :as jetty]
           [migratus.core :as migratus]
           [selmer.parser]
           [environ.core :refer [env]]))

(def migratus-config {:store                :database
                      :migration-dir        "migrations/"
                      :migration-table-name "migrations"
                      :db (env :database-url)})

(defn -main [& [port]]
  (require 'corollary.routes :reload-all)
  (if (nil? (env :production))
    (do (println "selmer cache turned off")
      (selmer.parser/cache-off!))
    (println "selmer cache left on"))
  (migratus/migrate migratus-config)
  (let [port (Integer. (or port (env :port) 5000))
        routes-ns (find-ns 'corollary.routes)
        mysite (ns-resolve routes-ns 'mysite)]
    (jetty/run-jetty mysite
                     {:port port :join? false})))

(defn start []
  (def server (-main)))

(defn stop []
  (.stop server))

(defn restart []
  (stop)
  (start))
