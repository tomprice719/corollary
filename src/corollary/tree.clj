;;use forward declaration for mutual recursion
(ns corollary.tree
  (:require [clojure.pprint :refer [pprint]]))

(declare add-pos-data)

;;TODO: encode node-data and last-pos in map rather than vector. Or in a record called "reduction"

(defn reducer [[node-data last-pos] id]
  (let [new-node-data (add-pos-data node-data last-pos id)]
    [new-node-data (get-in new-node-data [id :pos])]))

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

(defn add-pos-data [node-data last-pos id]
  (let
    [[new-node-data bottom-pos] (reduce reducer
                                        [node-data (starting-pos last-pos)]
                                        (get-in node-data [id :children]))]
    (assoc-in new-node-data
              [id :pos]
              (new-pos last-pos bottom-pos id))))

(def init-pos
  (hash-map :text-height 0
            :arrow-height 0
            :bottom 0
            :indent 0))

(pprint (add-pos-data test-tree init-pos 1))

(defn node-list [node-data parent]
  (tree-seq (constantly true)
            #(get-in node-data [% :children])
            parent))

;;TODO: make this give you the data that Selmer needs.
(defn draw-data [pos]
  pos)

(defn draw-data-list [node-data parent]
  (for [id (node-list node-data parent)
        with-pos-data [(add-pos-data node-data init-pos parent)]]
    (draw-data (get-in with-pos-data [id :pos]))))

(pprint (draw-data-list test-tree 1))

(def test-tree
  (hash-map 1 {:children [2 3]}
            2 {:children [4 5]}
            3 {:children [6 7]}
            4 {:children [8 9]}
            5 {:children [10 11]}
            6 {:children []}
            7 {:children []}
            8 {:children []}
            9 {:children []}
            10 {:children []}
            11 {:children []}))

(defn maketree-iter [[chosen queue]]
  (let [[{:keys [id parent] :as node} priority] (peek queue)]
    (vect
      (->chosen
        (assoc id node)
        (update-in [parent :children]
                   #(conj % id)))
      (into (pop queue) (get-children id)))))
