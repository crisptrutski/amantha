(ns amantha.app
  (:require [reagent.core :as reagent :refer [atom]]
            [amantha.components.grid :as grid]
            [amantha.grids.repairs :as grid-utils]
            [amantha.utils :as u]
            [cljsjs.jquery]
            [cljsjs.bootstrap]))

(enable-console-print!)

(def grid-atom (atom nil))

(def print-outs (atom []))

(defonce rows (atom []))

(defn n->roman [num]
  (if-not (pos? num)
    "--"
    (let [svm (into (sorted-map-by >) {1000 "M", 500 "D", 350 "LC", 100 "C", 50 "L", 40 "XL", 10 "X", 5 "V", 4 "IV", 1 "I", 9 "IX", 900 "CM", 90 "XC"})]
      (letfn [(rn [r o]
                (if (= r 0)
                  o
                  (let [d (first (filter #(<= (first %) r) svm))]
                    (rn (- r (first d)) (str o (second d))))))]
        (rn num "")))))

(defn index-maps [xs]
  (let [xs* (map-indexed (fn [i x] (assoc x :idx i)) xs)]
    (if-not (vector? xs)
      xs
      (vec xs*))))

(def default-rows
  (index-maps
    (into [{:a #inst "2015-01-02", :b 2, :c 3}
           {:a #inst "2016-04-02", :b 3, :c 5}
           {:a #inst "2016-03-09", :b 3, :c 2}
           {:a #inst "2016-08-10", :b 4, :c 1}
           {:a #inst "2016-07-23", :b 5, :c 4}]
          (for [i (range 100)]
            {:a #inst "2015-06-06", :b i, :c (mod i 5)}))))

(defn reset-rows! []
  (reset! rows default-rows))

(defn some-grid []
  (grid/grid-view
    @rows
    {:headers     [["#" :idx (comp n->roman inc)]
                   ["A" :a u/format-date]
                   ["B" :b (comp grid-utils/right-align #(u/format-currency :amount %))]
                   ["C" :c grid-utils/points-render]]
     :actions     [{:key     :delete
                    :label   "Delete"
                    :handler (fn [row] (swap! rows #(vec (remove #{row} %))))}
                   {:key      :print
                    :label    "Print"
                    :handler  (fn [row] (swap! print-outs conj (pr-str row)))
                    :disable? (fn [row] (= 3 (:b row)))
                    :hide?    (fn [row] (= 2 (:c row)))}]
     :sort-keys   [:idx]
     :pagination? true
     :filerable?  true
     :limit       7}
    grid-atom))

(defn calling-component []
  [:div "Parent component"
   [some-grid]
   (when (seq @print-outs) [:h5 "Printouts"])
   (into [:ul] (map-indexed (fn [i p] ^{:key i} [:li p]) @print-outs))
   (when (not= default-rows @rows)
     [:a.btn.btn-danger {:on-click reset-rows!} "Reset!"])])

(defn refresh []
  (reagent/render-component [calling-component] (.getElementById js/document "container")))

(defn init []
  (reset-rows!)
  (refresh))
