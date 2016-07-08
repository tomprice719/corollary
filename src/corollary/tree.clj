;;use forward declaration for mutual recursion
(ns corollary.tree
  (:require [clojure.pprint :refer [pprint]]))

(declare pos-list)

(defn reducer [tree [pos lst] id]
  (let [new-lst (pos-list tree pos id)]
    [(first new-lst) (concat lst new-lst)]))

;;position: text height, arrow height, bottom, indent
;;Parent previous sibling -> first child:
;;
;;child -> next child:
;;
;;

(defn new-pos [{last-text-height :text-height
                last-bottom :bottom
                last-indent :indent}
               {bottom-bottom :bottom}
               id]
  (hash-map :text-height last-bottom
            :arrow-height (+ last-text-height 1)
            :bottom bottom-bottom
            :indent last-indent
            :id id))

(defn starting-pos [{:keys [bottom indent]}]
  (hash-map :text-height bottom
           :bottom (+ bottom 1)
           :indent (+ indent 1)))

(defn pos-list [tree last-pos id]
  (let
    [[bottom-pos lst] (reduce (partial reducer tree)
                              [(starting-pos last-pos) '()]
                              (get tree id))]
    (cons (new-pos last-pos bottom-pos id) lst)))

(def init-pos
  (hash-map :text-height 0
            :arrow-height 0
            :bottom 0
            :indent 0))

(pprint (pos-list test-tree init-pos 1))

(clojure.tools.namespace.repl/refresh)

(def test-tree
  (hash-map 1 [2 3]
            2 [4 5]
            3 [6 7]
            4 [8 9]
            5 [10 11]
            6 []
            7 []
            8 []
            9 []
            10 []
            11 []))

;(defn maketree-iter [[chosen queue]]
;  (let [[[id parent] priority] (peek queue)]
;    (vect
;      (assoc chosen
;        id []
;        parent (conj (get chosen parent) id)))
;    (into (pop queue) (get-children id)))
