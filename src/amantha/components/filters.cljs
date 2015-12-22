(ns amantha.components.filters
  (:require [om.core :as om :include-macros true]
            [cljs.core.async :refer [put!]]
            [sablono.core :refer-macros [html]]
            [bss.rampant.utils :refer [e->value]]
            [amantha.components.date-range-picker :refer [date-range-picker]]
            [amantha.components.single-date-picker :refer [single-date-picker]]
            [amantha.utils :refer [register-path-get-and-set enumerate]]
            [clojure.string :as str]
            cljsjs.jquery
            select2.js
            [re-frame.core :refer [subscribe dispatch register-sub register-handler]]))

;; Full Text Filter

(defn handle-full-text-search-change [pattern cursor]
  (fn [e]
    (let [v (e->value e)]
      (om/update! cursor :value v)
      (om/update! cursor :filters [[:include-string v pattern]]))))

(defn full-text-search [pattern data owner]
  (om/component
   (html [:div.form-horiztonal {:style {:padding "5px"}}
          (when-let [l (:label data)] [:label.control-label l])
          [:div.input-group
           (when (= "^%s" pattern) [:span.input-group-addon "starts with"])
           (when (= "%s$" pattern) [:span.input-group-addon "ends with"])
           [:input.form-control {:value       (:value data)
                                 :on-change   (handle-full-text-search-change pattern data)
                                 :placeholder (:placeholder data)}]]])))

;; Date range

(defn date-range [{:keys [start-date end-date] :as data} owner]
  (om/component
   (let [filter-state [[:date-range start-date end-date]]]
     (if (not= (:filters data) (om/value filter-state))
       (om/update! data :filters (om/value filter-state)))
     (om/build date-range-picker data))))

;; Selector

(defn handle-categorical-selector-change [cursor _]
  (fn [e]
    (let [v (e->value e)
          v (if (= v "default") nil v)]
      (om/update! cursor :value v)
      (om/update! cursor :filters [[:equal v]]))))

(defn categorical-selector [data owner]
  (om/component
   (html [:div.form-horizontal {:style {:padding "5px"}}
          (when-let [l (:label data)]
            [:label.control-label l])
          [:div.input-group
           (into [:select.form-control {:value (or (:value data) (:default data))
                                        :on-change (handle-categorical-selector-change data owner)}]
                 (for [option (:options data)]
                   [:option {:value (name option)} (name option)]))]])))

(defn multi-selector
  [data owner]
  (reify
    om/IRender
    (render [_]
      ;; hack to support a static callback to select2
      (if (not= data (om/get-state owner :cursor))
        (om/set-state! owner :cursor data))

      (html
       [:div.form-horizontal.filter-container
        (when-let [label (:label data)]
          [:label.control-label
           label
           [:small.text-muted " - Select one or more."]])
        (into [:select.form-control {:multiple "multiple"
                                     :ref "selector"
                                     :data-placeholder "Any"}]
              (for [option (:options data)]
                [:option {:value (name option)} (name option)]))]))

    om/IDidUpdate
    (did-update [_ _ _]
      ;; hack to force rerender of select2 component, with
      (let [jq-elem (js/$ (om/get-node owner "selector"))]
        (.val jq-elem (clj->js (:value data)))
        (.select2 jq-elem)))

    om/IDidMount
    (did-mount [_]
      (let [select-elem (om/get-node owner "selector")
            select2 (.select2 (js/$ select-elem))]
        (.on select2 "change"
             (fn [elem]
               (let [values (js->clj (.val (js/$ select-elem)))
                     cursor (om/get-state owner :cursor)]
                 (om/update! cursor :value values)
                 (om/update! cursor :filters [[:any-of values]]))))))))

;; Number Range Filter

(defn handle-number-range-change [key cursor owner]
  (fn [e]
    (let [s (str/replace (e->value e) #"\D" "")
          v (if-not (empty? s) s)]
      (om/update! cursor key v)
      (om/refresh! owner)
      (om/update! cursor :filters [(if-let [f (:>= @cursor)] [:>= f])
                                   (if-let [f (:<= @cursor)] [:<= f])]))))

(defn number-range [data owner]
  (om/component
   (html [:div.form-horizontal {:style {:padding "5px"}}
          (when-let [l (:label data)] [:label.control-label l])
          [:div.form-group
           [:div.col-sm-6
            [:div.input-group
             [:span.input-group-addon ">="]
             [:input.form-control {:type        :number
                                   :on-change   (handle-number-range-change :>= data owner)
                                   :value       (:>= data)
                                   :placeholder (:placeholder data)}]]]
           [:div.col-sm-6
            [:div.input-group
             [:span.input-group-addon "<="]
             [:input.form-control {:type        :number
                                   :on-change   (handle-number-range-change :<= data owner)
                                   :value       (:<= data)
                                   :placeholder (:placeholder data)}]]]]])))

; Forecast options (not really filters, but I like the framework)

(register-path-get-and-set
  :filter
  [:filters])

(defn num-days [[page filter-key]]
  (let [filter (subscribe [:filter page filter-key])]
    (fn [_]
      (let [{:keys [label value]} @filter]
        [:div.form-horizontal {:style {:padding "5px"}}
         [:label.control-label label]
         [:div.input-group
          [:input.form-control
           {:type :number
            :value value
            :on-change #(dispatch [:filter page filter-key :value (str/replace (e->value %) #"\D" "")])}]
          [:span.input-group-addon "days"]]]))))

(defn button [[page filter-key]]
  (let [filter (subscribe [:filter page filter-key])]
    (fn [_]
      (let [{:keys [label handler]} @filter]
          [:button.btn.btn-primary.form-control
           {:on-click #(dispatch [handler])
            :style {:margin-top "32px"}}
           label]))))

;; Groups filter components in rows/columns

(declare filter-view)

(defn grouped-filters-base [filter-view-handler]
  (fn [filters]
    [:div.filter-panel
     (for [[rownum filter-group] (enumerate (partition-all 3 filters))]
       [:div.row {:key rownum}
        (for [[colnum filter] (enumerate filter-group)]
          [:div.col-md-4.col-lg-4
           {:key colnum}
             (filter-view-handler filter-view filter)])])]))

(def grouped-filters
  (grouped-filters-base om/build))

(def grouped-filters-reagent
  (grouped-filters-base vector))

(defn grouped-filters-reagent-from-map [page]
  (let [filters @(subscribe [:filter page])]
    (grouped-filters-reagent (for [[key filter] filters]
                               {:type (:type filter) :path [page key]}))))

;; Dispatcher

(defmulti filter-view (fn [x & _] (:type x)))

(defmethod filter-view :full-text-search [& args]
  (apply full-text-search nil args))

(defmethod filter-view :start-text-search [& args]
  (apply full-text-search "^%s" args))

(defmethod filter-view :end-text-search [& args]
  (apply full-text-search "%s$" args))

(defmethod filter-view :date-range [& args]
  (apply date-range args))

(defmethod filter-view :number-range [& args]
  (apply number-range args))

(defmethod filter-view :categorical-single [& args]
  (apply categorical-selector args))

(defmethod filter-view :multi-select [& args]
  (apply multi-selector args))

(defmethod filter-view :num-days [f]
  (num-days (:path f)))

(defmethod filter-view :single-date [f]
  (single-date-picker (:path f)))

(defmethod filter-view :button [f]
  (button (:path f)))
