(ns amantha-example.app
  (:require
    [amantha-example.roman :as roman]
    [amantha.components.basic :as b]
    [amantha.components.data-card :refer [data-card map->card]]
    [amantha.components.filters :as filters]
    [amantha.components.grid :as grid]
    [amantha.components.theme :as t]
    [amantha.grids.filters :refer [filter-data]]
    [amantha.utils :as u]
    [cljsjs.bootstrap]
    [cljsjs.jquery]
    [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(def app-state (atom {:opts {:filterable? true :pagination true}}))
(def range-atom (atom {:key :b, :label "B", :type :number-range}))
(def grid-atom (atom nil))
(def print-outs (atom []))
(defonce rows (atom []))

(defn index-maps [xs]
  (let [xs* (map-indexed (fn [i x] (assoc x :idx i)) xs)]
    (if-not (vector? xs) xs (vec xs*))))

(def default-rows
  (index-maps
    (into [{:a #inst "2015-01-02", :b 2, :c 3}
           {:a #inst "2016-04-02", :b 3, :c 5}
           {:a #inst "2016-03-09", :b 3, :c 2 :d true}
           {:a #inst "2016-08-10", :b 4, :c 1}
           {:a #inst "2016-07-23", :b 5, :c 4}]
          (for [i (range 100)]
            {:a #inst "2015-06-06", :b i, :c (mod i 5) :d (zero? (rand-int 1))}))))

(defn reset-rows! []
  (reset! rows default-rows))



(defn some-grid []
  (grid/grid-view
    (filter-data @rows [@range-atom])
    {:headers [["#" :idx (comp roman/from-number inc)]
               ["A" :a u/format-date]
               ["B" :b (comp b/right-align #(u/format-currency :amount %))]
               ["C" :c b/points-render]
               ["D" :d b/tick-view]]
     :actions [{:key :delete
                :label "Delete"
                :handler (fn [row] (swap! rows #(vec (remove #{row} %))))}
               {:key :print
                :label "Print"
                :handler (fn [row] (swap! print-outs conj (pr-str row)))
                :disable? (fn [row] (= 3 (:b row)))
                :hide? (fn [row] (= 2 (:c row)))}]
     :sort-keys [:idx]

     :pagination? (get-in @app-state [:opts :pagination])
     :filterable? (get-in @app-state [:opts :filterable?])
     :limit 9}
    grid-atom))

(defn checkbox [label path]
  [:div {:style {:padding 5}}
   [:input {:type :checkbox
            :checked (get-in @app-state path)
            :on-change #(swap! app-state update-in path not)}
    " " label]])

(defn pending-checkbox [label]
  [:div {:style {:padding 5}}
   [:input {:type :checkbox :checked true :disabled true :read-only true}
    " " label]])

(defn app-view []
  [:div
   [:table {:style {:margin 5}}
    [:tr
     [:td {:style {:vertical-align :top}}
      [data-card [{:section "A" :title "Name" :fields ["Deon" "Moolman"]}
                  {:section "A" :title "Age" :field "23-59?"}
                  {:section "G" :fields [[:div {:style {:background :red :height 50 :color :white :padding-top 15}}
                                          [:center "I am a graph"]]]}
                  {:section "Z" :title "Heros" :field [:a {:href "#"} "Go Seigen"]}
                  {:section "Z" :title "Deniros" :fields ["Robert de Niro" "Nirosan"]}
                  {:section "X" :title "Pharoah" :fields [{:width 2 :content [:div {:style {:text-align :center :background :orange}} "ASCII GRAPH"]} "Right label"]}
                  {:section "Z" :title "Zeros" :field [:div {:style {:text-align :right}} "434.21%"]}

                  {:section "A" :title "Children" :fields ["Caleb" "Gurlie" "3dan"]}]]]
     [:td t/nbsp]
     [:td {:style {:vertical-align :top}}
      [data-card [{:title "Name" :fields ["Chris" "Truter"]}
                  {:title "Age" :field 9000}]]
      [:div {:style {:height 5}}]
      [data-card (map->card
                   [[:name "Brian Button"]
                    [:power-level 9999]
                    [:language-preference "PHP"]
                    [:alignment
                     [:div {:style {:background :green :height 50 :color :white :padding-top 15}}
                      [:center "Good"]]]])]]]]

   (b/toggle-panel app-state "Options" :options
     [:div
      (checkbox "Column filters" [:opts :filterable?])
      (checkbox "Pagination" [:opts :pagination])
      (pending-checkbox "Cursor paging")])
   (b/toggle-panel app-state "Filters" :hide-filters
     (filters/grouped-filters [range-atom]))
   [some-grid]
   (when (seq @print-outs) [:h5 "Printouts"])
   (into [:ul] (map-indexed (fn [i p] ^{:key i} [:li p]) @print-outs))
   (when (not= default-rows @rows)
     [:a.btn.btn-danger {:on-click reset-rows!} "Reset!"])])

(defn refresh []
  (reagent/render-component [app-view] (.getElementById js/document "container")))

(defn init []
  (reset-rows!)
  (refresh))
