(ns corollary.ajax
  (require [corollary.queries :refer [pandoc]]))

(defn get-preview-html [{:keys [selection]}]
  (pandoc selection))
