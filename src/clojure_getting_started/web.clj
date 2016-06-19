(ns clojure-getting-started.web
  (:use [selmer.parser])
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]))

(def posts-2 [{:title "newer first post title" :heading "hellooooooooooo"}
              {:title "post title 2" :heading "have a nice day today"}
              {:title "third post" :heading "aaaaaaaaaaaaaaaaaaa"}])

(def index-page-2 (render-file "templates/index.html" {:posts posts-2}))

(defn splash []
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "Hello from Heroku"})

(defroutes app
  (GET "/" [] index-page-2)
  (route/resources "/")
  (route/not-found "Page not found"))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (site #'app) {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
