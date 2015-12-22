(ns amantha.components.roll-up-selector
  (:require [amantha.components.generic :as generic]
            [amantha.data.api :as api]
            [amantha.utils :as utils]
            [cljsjs.moment]
            [goog.string :as gstring]
            [goog.string.format]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn format-daily     [date] (utils/format-date :date date :format "Do MMM YYYY"))
(defn format-weekly    [date] (utils/format-date :date date :format "Do MMM YYYY"))
(defn format-monthly   [date] (utils/format-date :date date :format "MMM YYYY"))
(defn format-quarterly [date] (utils/format-date :date date :format "MMM YYYY"))
(defn format-yearly    [date] (utils/format-date :date date :format "YYYY"))

(def chart-intervals
  [{:name "Daily"     :interval :daily     :formatter format-daily}
   {:name "Weekly"    :interval :weekly    :formatter format-weekly}
   {:name "Monthly"   :interval :monthly   :formatter format-monthly}
   {:name "Quarterly" :interval :quarterly :formatter format-quarterly}
   {:name "Yearly"    :interval :yearly    :formatter format-yearly}])

(defn change-chart-interval [owner interval]
  (om/set-state! owner :chart-interval interval))

(defn get-interval [owner]
  (or (om/get-state owner :chart-interval)
      (second chart-intervals)))

(defn chart-interval-selector [owner]
  (let [interval (get-interval owner)]
    [:div.col-md-8
     [:label "Chart interval"]
     [:div.btn-group.chart-interval-dropdown
      [:button.btn.btn-default.dropdown-toggle
       {:type "button" :data-toggle "dropdown" :aria-expanded= "true"}
       (:name interval) " "
       [:span.caret]]
      [:ul.dropdown-menu {:role "menu"}
       (for [{:keys [name formatter] :as interval} chart-intervals]
         [:li [:a {:href "#"
                   :key name
                   :on-click #(change-chart-interval owner interval)}
               name]])]]]))
