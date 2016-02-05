(ns amantha.components.filters
  (:require
    [cljs.core.async :refer [put!]]
    [amantha.utils :as u]
    [amantha.utils.dom :as dom]
    [amantha.data.filters]
    [clojure.string :as str]))

;; Full Text Filter

(defn handle-full-text-search-change [pattern state]
  (fn [e]
    (let [v (dom/e->value e)]
      (swap! state assoc :value v)
      (swap! state assoc :filters [[:include-string v pattern]]))))

(defn full-text-search [pattern state]
  (let [data @state]
    [:div.form-horiztonal {:style {:padding "5px"}}
     (when-let [l (:label data)] [:label.control-label l])
     [:div.input-group
      (when (= "^%s" pattern) [:span.input-group-addon "starts with"])
      (when (= "%s$" pattern) [:span.input-group-addon "ends with"])
      [:input.form-control {:value       (:value data)
                            :on-change   (handle-full-text-search-change pattern state)
                            :placeholder (:placeholder data)}]]]))

;; TODO Date range

;; Selector

(defn handle-categorical-selector-change [state _]
  (fn [e]
    (let [v (dom/e->value e)
          v (if (= v "default") nil v)]
      (swap! state assoc :value v)
      (swap! state assoc :filters [[:equal v]]))))

(defn categorical-selector [data owner]
  [:div.form-horizontal {:style {:padding "5px"}}
   (when-let [l (:label data)]
     [:label.control-label l])
   [:div.input-group
    (into [:select.form-control {:value     (or (:value data) (:default data))
                                 :on-change (handle-categorical-selector-change data owner)}]
          (for [option (:options data)]
            [:option {:value (name option)} (name option)]))]])

;; Number Range Filter

(defn handle-number-range-change [key state owner]
  (fn [e]
    (let [s (str/replace (dom/e->value e) #"\D" "")
          v (if-not (empty? s) s)]
      (swap! state assoc key v)
      (swap! state assoc
             :filters [(when-let [f (:>= @state)] [:>= f])
                       (when-let [f (:<= @state)] [:<= f])]))))

(defn number-range [data owner]
  (let [value @data]
    [:div.form-horizontal {:style {:padding "5px"}}
     (when-let [l (:label value)] [:label.control-label l])
     [:div.form-group
      [:div.col-sm-6
       [:div.input-group
        [:span.input-group-addon ">="]
        [:input.form-control {:type        :number
                              :on-change   (handle-number-range-change :>= data owner)
                              :value       (:>= value)
                              :placeholder (:placeholder value)}]]]
      [:div.col-sm-6
       [:div.input-group
        [:span.input-group-addon "<="]
        [:input.form-control {:type        :number
                              :on-change   (handle-number-range-change :<= data owner)
                              :value       (:<= value)
                              :placeholder (:placeholder value)}]]]]]))

;; Groups filter components in rows/columns

(declare filter-view)

(defn grouped-filters-base [filter-view-handler]
  (fn [filters]
    [:div.filter-panel
     (for [[rownum filter-group] (u/enumerate (partition-all 3 filters))]
       [:div.row {:key rownum}
        (for [[colnum filter] (u/enumerate filter-group)]
          [:div.col-md-4.col-lg-4
           {:key colnum}
           (filter-view-handler filter-view filter)])])]))

(def grouped-filters
  (grouped-filters-base vector))

;; Dispatcher

(defmulti filter-view (fn [x & _] (:type @x)))

(defmethod filter-view :full-text-search [& args]
  (apply full-text-search nil args))

(defmethod filter-view :start-text-search [& args]
  (apply full-text-search "^%s" args))

(defmethod filter-view :end-text-search [& args]
  (apply full-text-search "%s$" args))

(defmethod filter-view :number-range [& args]
  (apply number-range args))

(defmethod filter-view :categorical-single [& args]
  (apply categorical-selector args))

(defmethod filter-view :single-date [f]
  [:div "TODO" (:type @f)])

(defmethod filter-view :date-range [& args]
  [:div "TODO" (:type @(first args))])

(defmethod filter-view :multi-select [& args]
  [:div "TODO" (:type @(first args))])
