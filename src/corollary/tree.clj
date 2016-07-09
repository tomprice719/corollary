;;use forward declaration for mutual recursion
(ns corollary.tree
  (:require [clojure.pprint :refer [pprint]]
            [clojure.data.priority-map :refer :all]
            [clojure.java.jdbc :refer [query]]
            [corollary.utils :refer [db]])) ;;maybe you just need priority-map

(declare add-pos-data)

;;TODO: encode node-data and last-pos in map rather than vector. Or in a record called "reduction"

(def test-tree
  (hash-map 1 {:children [2 3] :id 1}
            2 {:children [4 5]  :id 2}
            3 {:children [6 7]  :id 3}
            4 {:children [8 9]  :id 4}
            5 {:children [10 11]  :id 5}
            6 {:children [] :id 6}
            7 {:children []  :id 7}
            8 {:children [] :id 8}
            9 {:children []  :id 9}
            10 {:children []  :id 10}
            11 {:children []  :id 11}))

(defn reducer [[node-data last-pos] id]
  (let [new-node-data (add-pos-data node-data last-pos id)]
    [new-node-data (get-in new-node-data [id :pos])]))

(defn new-pos [{last-text-row :text-row
                last-bottom :bottom
                last-indent :indent}
               {bottom-bottom :bottom}
               id]
  (hash-map :text-row (+ last-bottom 1)
            :arrow-row last-text-row
            :bottom bottom-bottom
            :indent last-indent
            :id id))

(defn starting-pos [{:keys [bottom indent]}]
  (hash-map :text-row (+ bottom 1)
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
  (hash-map :text-row 0
            :arrow-row 0
            :bottom 0
            :indent 0))

(defn node-list [node-data parent]
  (tree-seq (constantly true)
            #(get-in node-data [% :children])
            parent))

(def base-x 20)
(def base-y 20)
(def indent-width 20)
(def text-height 30)

(defn get-x [indent]
  (+ base-x (* indent indent-width)))

(defn get-y [row]
  (+ base-y (* row text-height)))

(defn arrow-points [{:keys [text-row arrow-row indent]}]
  (let [right-x (- (get-x indent) 3)
        left-x (- right-x 10)
        high-y (get-y arrow-row)
        low-y (- (get-y text-row) 5)]
    (vector left-x high-y
            left-x low-y
            right-x low-y)))

(defn draw-data [{id :id {:keys [indent text-row] :as pos} :pos}]
  {:x (get-x indent)
   :y (get-y text-row)
   :arrow-points (apply
                   (partial format "%d,%d %d,%d %d,%d")
                   (arrow-points pos))
   :title (str "Post id number " id)
   })

;(pprint (draw-data-list test-tree 1))

(defn get-child-kvs [id depth]
  (query db ["select posts.id, posts.date, posts.title from posts join edges on posts.id = edges.child_id where edges.parent_id = ?"
             (Integer. id)]
         {:row-fn #(vector (-> %
                               (select-keys [:id :title])
                               (assoc :children [] :parent id))
                           [(inc depth) (- (:date %))])}))

(defn get-nodes-recurse [chosen queue count]
  (if (or (zero? count) (empty? queue))
    chosen
    (let [[{:keys [id parent] :as node} [depth _]] (peek queue)]
      (recur
        (-> chosen
            (assoc id node)
            (update-in [parent :children]
                       #(conj % id)))
        (into (pop queue) (get-child-kvs id depth))
        (dec count)))))

(defn get-nodes [root-id]
  (get-nodes-recurse {root-id {:children [] :id root-id}}
                     (into (priority-map) (get-child-kvs root-id 0))
                     10))

(defn draw-data-list
  ([node-data parent]
   (for [id (node-list node-data parent)
         node-data-with-pos [(add-pos-data node-data init-pos parent)]]
     (draw-data (node-data-with-pos id))))
  ([root-id]
   (draw-data-list (get-nodes root-id) root-id)))

;;(draw-data-list 34)
