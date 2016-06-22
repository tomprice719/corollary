(ns clojure-getting-started.web
  (:use
        [clojure.java.jdbc])
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]
            [clojure.pprint :refer [pprint]]
            [migratus.core :as migratus]
            [selmer.parser :refer [render-file]]
            [ring.middleware.session.cookie :refer [cookie-store]]))

(def migratus-config {:store                :database
                      :migration-dir        "migrations/"
                      :migration-table-name "migrations"
                      :db (env :database-url)})

(def db (env :database-url))

(def posts (query db
                  ["select title, heading from posts"]))

(def posts-2 [{:title "newer first post title" :heading "hellooooooooooo"}
              {:title "post title 2" :heading "have a nice day today"}
              {:title "third post" :heading "aaaaaaaaaaaaaaaaaaa"}])

(defn recent-posts-page [name]
  (do
    (println "Name: " name)
    (render-file "templates/recent_posts.html" {:posts posts :name name})))

(defn selected-post-page [name]
  (do
    (println "Name: " name)
    (render-file "templates/selected_post.html" {:name name})))

(defn splash []
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "Hello from Heroku"})

(defroutes app
  (GET "/" {{name :name} :session}
       {:session {:name name}
        :body (recent-posts-page name)})
  (GET "/recent" {{name :name} :session}
       {:session {:name name}
        :body (recent-posts-page name)})
  (GET "/selected" {{name :name} :session}
       {:session {:name name}
        :body (selected-post-page name)})
  (GET "/user/:name" [name]
       {:session {:name name}
        :body (selected-post-page name)})
  (route/resources "/")
  (route/not-found "Page not found"))

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

;; For interactive development:
;; (.stop server)
;; (def server (-main))
