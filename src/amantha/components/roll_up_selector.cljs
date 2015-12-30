(ns amantha.components.roll-up-selector
  (:require [amantha.utils :as utils]))

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

(defn chart-interval-selector [state]
  (let [interval (:interval @state)]
    [:div.col-md-8
     [:label "Chart interval"]
     [:div.btn-group.chart-interval-dropdown
      [:button.btn.btn-default.dropdown-toggle
       {:type "button" :data-toggle "dropdown" :aria-expanded= "true"}
       (:name interval) " "
       [:span.caret]]
      [:ul.dropdown-menu {:role "menu"}
       (for [{:keys [name] :as interval} chart-intervals]
         [:li [:a {:href "#"
                   :key name
                   :on-click #(swap! state assoc :interval % interval)}
               name]])]]]))
