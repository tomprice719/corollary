;;use forward declaration for mutual recursion
(ns corollary.tree
  (:require [clojure.pprint :refer [pprint]]
            [clojure.set :refer [rename-keys]]
            [clojure.data.priority-map :refer :all]
            [clojure.java.jdbc :refer [query]]
            [corollary.queries :as queries]
            [corollary.utils :refer [db thrush]])) ;;maybe you just need priority-map

(declare add-pos-data)

;;Code problem: it's annoying to find the different places that nodes are created
;;This mess seriously need refactoring
;;TODO: make different "classes" for each node type.
;;Also, instead of merging post properties with node properties, give each node a corresponding post

(defn reducer [[node-data last-pos] key]
  (let [new-node-data (add-pos-data node-data last-pos key)]
    [new-node-data (get-in new-node-data [key :pos])]))

(defn new-pos [{last-text-row :text-row
                last-bottom :bottom
                last-indent :indent
                was-last-starter? :starter}
               {bottom-bottom :bottom}]
  (hash-map :text-row (+ last-bottom 1)
            :arrow-row last-text-row
            :bottom bottom-bottom
            :indent last-indent
            :first-child was-last-starter?))

(defn starter-pos [{:keys [bottom indent]} {:keys [node-type posts]}]
  (let [parent-size (if (= node-type "multipost") (count posts) 1)]
    (hash-map :text-row (+ bottom parent-size)
              :bottom (+ bottom parent-size)
              :indent (+ indent 1)
              :starter true)))

(defn add-pos-data [node-data last-pos key]
  (let
    [[new-node-data bottom-pos] (reduce reducer
                                        [node-data (starter-pos last-pos (node-data key))]
                                        (get-in node-data [key :children]))]
    (assoc-in new-node-data
              [key :pos]
              (new-pos last-pos bottom-pos))))

(def init-pos
  (hash-map :text-row 0
            :bottom 0
            :indent 0))

(defn get-child-kvs [parent-post-id parent-key depth]
  (queries/get-children parent-post-id
                        #(vector
                           (-> %
                               (rename-keys {:id :post-id})
                               (assoc :node-type "descendant" :children [] :parent-key parent-key))
                           [(inc depth) (- (:date %))])))

(defn add-has-more [node-data queue-seq]
  (if (empty? queue-seq) node-data
    (recur
      (let [[{:keys [parent-key]} _] (first queue-seq)
            has-more (get-in node-data [parent-key :has-more])
            new-key (cons :more parent-key)
            more-node {:post-id (:post-id (node-data parent-key))
                       :title "More ..."
                       :children []
                       :node-type "descendant"}]
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
                           :node-type "selected"
                           :prefix "Selected: "
                           :title (queries/get-one-title selected-post-id)
                           :root (queries/is-root selected-post-id)}}
                     (into (priority-map) (get-child-kvs selected-post-id '() 0))
                     15))

(defn make-parent [node-data top-key]
  (let [{:keys [post-id]} (node-data top-key)
        parent-posts (queries/get-parents post-id identity)]
    (case (count parent-posts)
      0 nil
      1 (-> (first parent-posts)
            (rename-keys {:id :post-id})
            (assoc :children [top-key] :prefix "Ancestor: " :node-type "ancestor"))
      (hash-map :posts parent-posts
                :children [top-key]
                :node-type "multipost"))))

(defn finish [node-data top-key]
  (-> node-data
      (rename-keys {top-key :top})
      (assoc-in [:top :top] true)))

(defn add-ancestors [node-data top-key max-to-add]
  (if-let [parent (and (> max-to-add 0) (make-parent node-data top-key))]
    (if (= (:node-type parent) "multipost")
      (assoc node-data :top parent)
      (let [key [(:post-id parent) :ancestor]]
        (if (contains? node-data key)
          (finish node-data top-key)
          (add-ancestors (assoc node-data key parent) key (- max-to-add 1)))))
    (finish node-data top-key)))

(def base-x 20)
(def base-y 40)
(def indent-width 20)
(def text-height 25)

(defn get-x [indent]
  (+ base-x (* indent indent-width)))

(defn get-y [row]
  (+ base-y (* row text-height)))

(defn arrow-points [{:keys [text-row arrow-row indent first-child]} node-type]
  (case node-type
    "descendant" (let [right-x (- (get-x indent) 3)
                       left-x (- right-x 10)
                       high-y (if first-child
                                (+ (get-y arrow-row) 3)
                                (- (get-y arrow-row) 4))
                       low-y (- (get-y text-row) 6)]
                   (vector left-x high-y
                           left-x low-y
                           right-x low-y))
    "ancestor" (let [right-x (- (get-x (+ indent 1)) 3)
                     left-x (- right-x 10)
                     high-y (+ (get-y text-row) 3)
                     low-y (- (get-y (+ text-row 1)) 6)]
                 (vector left-x high-y
                         left-x low-y
                         right-x low-y))))

;;use defmulti / defmethod
(defn draw-data [{{:keys [indent text-row] :as pos} :pos :keys [post-id title prefix node-type root] :as node}]
  {:x            (get-x indent)
   :y            (get-y text-row)
   :arrow-points (if-not (= node-type "selected")
                   (apply
                     (partial format "%d,%d %d,%d %d,%d")
                     (arrow-points pos node-type)))
   :title        (str prefix title)
   :post-id      post-id
   :node-type    node-type
   :rect-y       (- (get-y text-row) 15)
   :rect-height  19
   :rect-x       (- (get-x indent) 3)
   :root         root
   })

;;TODO: make changes to add-pos-data, draw-data
;;Also, use "key" instead of id in some variable names
(defn draw-data-list [selected-post-id]
  (let [node-data (-> selected-post-id
                      get-nodes
                      (add-ancestors '() 4)
                      (add-pos-data init-pos :top))]
    (map draw-data (vals node-data))))


;;(draw-data-list 34)
