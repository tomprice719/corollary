;;use forward declaration for mutual recursion
(ns corollary.tree
  (:require [clojure.pprint :refer [pprint]]
            [clojure.set :refer [rename-keys]]
            [clojure.data.priority-map :refer :all]
            [clojure.java.jdbc :refer [query]]
            [corollary.queries :as queries]
            [corollary.utils :refer [db thrush]])) ;;maybe you just need priority-map

(declare add-pos-data)

;;TODO: encode node-data and last-pos in map rather than vector. Or in a record called "reduction"

;;(def test-tree
;;  (hash-map 1 {:children [2 3] :id 1}
;;            2 {:children [4 5]  :id 2}
;;            3 {:children [6 7]  :id 3}
;;            4 {:children [8 9]  :id 4}
;;            5 {:children [10 11]  :id 5}
;;            6 {:children [] :id 6}
;;            7 {:children []  :id 7}
;;            8 {:children [] :id 8}
;;            9 {:children []  :id 9}
;;            10 {:children []  :id 10}
;;            11 {:children []  :id 11}))

(defn reducer [[node-data last-pos] key]
  (let [new-node-data (add-pos-data node-data last-pos key)]
    [new-node-data (get-in new-node-data [key :pos])]))

(defn new-pos [{last-text-row :text-row
                last-bottom :bottom
                last-indent :indent}
               {bottom-bottom :bottom}]
  (hash-map :text-row (+ last-bottom 1)
            :arrow-row last-text-row
            :bottom bottom-bottom
            :indent last-indent))

(defn starting-pos [{:keys [bottom indent]} {:keys [multi-post posts]}]
  (let [parent-size (if multi-post (count posts) 1)]
    (hash-map :text-row (+ bottom parent-size)
              :bottom (+ bottom parent-size)
              :indent (+ indent 1))))

(defn add-pos-data [node-data last-pos key]
  (let
    [[new-node-data bottom-pos] (reduce reducer
                                        [node-data (starting-pos last-pos (node-data key))]
                                        (get-in node-data [key :children]))]
    (assoc-in new-node-data
              [key :pos]
              (new-pos last-pos bottom-pos))))

(def init-pos
  (hash-map :text-row 0
            :bottom 0
            :indent 0))

;(pprint (draw-data-list test-tree 1))

(defn get-child-kvs [parent-post-id parent-key depth]
  (query db ["select posts.id, posts.date, posts.title from posts join edges on posts.id = edges.child_id where edges.parent_id = ?"
             (Integer. parent-post-id)]
         {:row-fn (fn [{post-id :id title :title date :date}]
                    [{:post-id post-id
                      :title title
                      :parent-key parent-key
                      :children []}
                     [(inc depth) (- date)]])}))

(defn add-has-more [node-data queue-seq]
  (if (empty? queue-seq) node-data
    (recur
      (let [[{:keys [parent-key]} _] (first queue-seq)
            has-more (get-in node-data [parent-key :has-more])
            new-key (cons :more parent-key)
            more-node {:post-id (:post-id (node-data parent-key))
                       :title "More ..."
                       :children []}]
        (if has-more
          node-data
          (-> node-data
              (update-in [parent-key :children]
                         #(conj % new-key))
              (assoc-in [parent-key :has-more] true)
              (assoc new-key more-node))))
      (rest queue-seq))))

(defn get-nodes-recurse [chosen queue count]
  (if (or (zero? count) (empty? queue))
    (add-has-more chosen (seq queue))
    (let [[{:keys [post-id parent-key] :as node} [depth _]] (peek queue)
          new-key (cons post-id parent-key)]
      (recur
        (-> chosen
            (assoc new-key node)
            (update-in [parent-key :children]
                       #(conj % new-key)))
        (into (pop queue) (get-child-kvs post-id new-key depth))
        (dec count)))))

(defn get-nodes [selected-post-id]
  (get-nodes-recurse {'() {:children []
                           :post-id selected-post-id
                           :selected true
                           :title (queries/get-one-title selected-post-id)}}
                     (into (priority-map) (get-child-kvs selected-post-id '() 0))
                     10))

(defn node-list [node-data]
  (tree-seq (constantly true)
            #(get-in node-data [% :children])
            :top))

(defn make-parent [node-data top-key]
  (let [{:keys [post-id]} (node-data top-key)
        parent-posts (queries/get-parents post-id)]
    (case (count parent-posts)
      0 nil
      1 (-> (first parent-posts)
            (rename-keys {:id :post-id})
            (assoc :children [top-key]))
      (hash-map :posts parent-posts
                :children [top-key]
                :multi-post true))))

(defn finish [node-data top-key]
  (-> node-data
      (rename-keys {top-key :top})
      (assoc-in [:top :top] true)))

(defn add-ancestors [node-data top-key]
  (if-let [parent (make-parent node-data top-key)]
    (if (:multi-post parent)
      (assoc node-data :top parent)
      (let [key [(:post-id parent) :ancestor]]
        (if (contains? node-data key)
          (finish node-data top-key)
          (add-ancestors (assoc node-data key parent) key))))
    (finish node-data top-key)))

(def base-x 20)
(def base-y 20)
(def indent-width 20)
(def text-height 25)

(defn get-x [indent]
  (+ base-x (* indent indent-width)))

(defn get-y [row]
  (+ base-y (* row text-height)))

(defn arrow-points [{:keys [text-row arrow-row indent]}]
  (let [right-x (- (get-x indent) 3)
        left-x (- right-x 10)
        high-y (+ (get-y arrow-row) 3)
        low-y (- (get-y text-row) 3)]
    (vector left-x high-y
            left-x low-y
            right-x low-y)))

;;use defmulti / defmethod
(defn draw-data [{{:keys [indent text-row] :as pos} :pos :keys [post-id multi-post posts top title] :as node}]
  (if multi-post
    {:multi-post true
     :x (get-x indent)
     :posts (map (fn [post i]
                   (merge post {:y (get-y (+ text-row i))}))
                 posts
                 (range (count posts)))}
    {:x (get-x indent)
     :y (get-y text-row)
     :has-arrow (not top)
     :arrow-points (apply
                     (partial format "%d,%d %d,%d %d,%d")
                     (arrow-points pos))
     :title title
     :post-id post-id
     }))

;;TODO: make changes to add-pos-data, draw-data
;;Also, use "key" instead of id in some variable names
(defn draw-data-list [selected-post-id]
  (let [node-data (-> selected-post-id
                      get-nodes
                      (add-ancestors '())
                      (add-pos-data init-pos :top))]
    (map draw-data (vals node-data))))


;;(draw-data-list 34)
