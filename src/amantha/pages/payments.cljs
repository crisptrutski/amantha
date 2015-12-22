(ns amantha.pages.payments
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [amantha.components.charts :as charts]
            [amantha.components.generic :refer [start-tracking-server! stop-tracking-server!]]
            [amantha.components.roll-up-selector :as roll-up-sel]
            [amantha.data.api :as api]
            [amantha.state :as state]
            [amantha.utils :as utils]
            [cljsjs.moment]
            [goog.string :as gstring]
            [goog.string.format]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put! chan <! alts!]]))

(defn- money-tooltip []
  (this-as point
           (let [amount (aget point "y")
                 label (aget point "x")]
             (str label " - " (utils/format-currency :amount amount)))))


(defn chart-interval-selector
  [owner]
  (let [interval (om/get-state owner :chart-interval)
        chart-interval-ch (om/get-state owner :chart-interval-ch)]
    [:div.col-md-8
     [:label "Chart interval"]
     [:div.btn-group.chart-interval-dropdown
      [:button.btn.btn-default.dropdown-toggle
       {:type "button" :data-toggle "dropdown" :aria-expanded= "true"}
       (:name interval) " "
       [:span.caret]]
      [:ul.dropdown-menu {:role "menu"}
       (for [{:keys [name formatter] :as interval} roll-up-sel/chart-intervals]
         [:li [:a {:href "#"
                   :key name
                   :on-click #(do
                               (put! chart-interval-ch interval)
                               (.preventDefault %))}
               name]])]]]))


(defn histogram-field-selector
  [filter owner]
  (let [field (om/get-state owner :histogram-field)
        histogram-field-ch (om/get-state owner :histogram-field-ch)]
    [:div.col-md-8
     [:label "Field"]
     [:div.btn-group.chart-interval-dropdown
      [:button.btn.btn-default.dropdown-toggle
       {:type "button" :data-toggle "dropdown" :aria-expanded= "true"}
       (:label field) " "
       [:span.caret]]
      [:ul.dropdown-menu {:role "menu"}
       (for [{:keys [label] :as f} filter]
         [:li [:a {:href "#"
                   :key label
                   :on-click #(do
                               (put! histogram-field-ch f)
                               (.preventDefault %))}
               label]])]]]))


(defn draw-interval-chart
  [number-of-payments payments-map rolling]
  (let [interval-name (:name rolling)
        formatter (:formatter rolling)
        x-axis-data  (map formatter (keys payments-map))
        total-amount (utils/format-currency :amount (reduce + (vals payments-map)))
        title        (str number-of-payments " payments totalling " total-amount)
        sub-title    (str interval-name " Interval")]
    (charts/draw-histogram payments-map
                           nil nil
                           {:colors   ["green"]
                            :title    {:text title}
                            :subtitle {:text sub-title}
                            :xAxis    {:categories x-axis-data}
                            :yAxis    {:title {:text "Amount in ZAR"}}
                            :tooltip  {:formatter money-tooltip}
                            :height   350})))

(defn chart-view
  [cursor owner opts]
  (reify
    om/IRender
    (render
      [_]
      (let [number-of-payments (get-in cursor [:cache :grid :payments :pagination :total])
            payments-map (:interval-hist cursor)
            rolling (:rolling cursor)]
        (html
          [:div.col-md-8
           (draw-interval-chart number-of-payments payments-map rolling)])))))


(defn draw-totals-by
  [summary title]
  (let [summary    (filter first summary)
        totals     (vals summary)
        x-values   (keys summary)
        chart-opts {:chart    {:type "bar"}
                    :colors   ["green"]
                    :title    {:text title}
                    :legend   {:enabled false}
                    :xAxis    {:categories x-values}
                    :yAxis    {:title {:text "Amount in ZAR"}}
                    :series   [{:data totals}]
                    :tooltip  {:formatter money-tooltip}
                    :width    "100%"
                    :height   350}]
    (om/build charts/highchart-view chart-opts)))

(defn histogram-view
  [cursor owner opts]
  (reify
    om/IRender
    (render
      [_]
      (html
        [:div.col-md-4
         (draw-totals-by (:summary cursor) (:title cursor))]))))


(defn load-chart-data
  [owner cache chart-interval filter]
  (api/aggregate (fn [data]
                   (om/set-state! owner :chart-interval chart-interval)
                   (om/update! cache [:payments :intervals] data)
                   (om/update! cache [:payments :rolling] chart-interval))
                 :payments
                 :amount
                 :created-at
                 {:filter  (om/value filter)
                  :roll-up (:interval chart-interval)}))

(defn load-histogram-data
  [owner cache histogram-field filter]
  (api/aggregate (fn [data]
                   (om/set-state! owner :histogram-field histogram-field)
                   (om/update! cache [:payments :temp-data] data))
                 :payments
                 :amount
                 (:key histogram-field)
                 {:filter (om/value filter)}))


(defn render-payments
  [filter cache owner]
  (let [payments        (:payments cache)
        interval-hist   (:intervals payments)
        rolling         (:rolling payments)]
    (html
      [:div.panel-body
       [:div.row
        [:div.col-md-8
         (chart-interval-selector owner)]
        [:div.col-md-4
         (histogram-field-selector filter owner)]]
       [:div.row
        (om/build chart-view {:interval-hist interval-hist
                              :rolling rolling
                              :cache cache})
        (om/build histogram-view {:summary (:temp-data payments)
                                  :title (om/get-state owner [:histogram-field :label])})]])))


(defn correct-nil-branch-filter
  [filter]
  filter)

;; When the filter shows "Any", its :value is nil.
;; That would let api/aggregate return data for all branches
;; but we must only get data for the branches the user can view.
;; We've stored those branches in :default-value.
#_(defn correct-nil-branch-filter
  [filter]
  (if (nil? (:value (filter 0)))
    (let [correct-value (state/branches-for-current-user)
          correct-filters [[:any-of correct-value]]
          correct-branch-filter (-> (filter 0)
                                    (assoc-in [:value] correct-value)
                                    (assoc-in [:filters] correct-filters))]
      (apply vector correct-branch-filter (subvec filter 1)))
    filter))


(defmethod charts/custom-chart-view :custom-payments-chart
  [app owner opts]
  (reify
    om/IInitState
    (init-state
      [_]
      {;; default values:
       :chart-interval (second roll-up-sel/chart-intervals)
       :histogram-field (first (:filter app))

       ;; channels for updates:
       :chart-interval-ch (chan)
       :histogram-field-ch (chan)})

    om/IWillMount
    (will-mount
      [_]
      (start-tracking-server! owner)
      (let [chart-interval (om/get-state owner :chart-interval)
            histogram-field (om/get-state owner :histogram-field)
            chart-interval-ch (om/get-state owner :chart-interval-ch)
            histogram-field-ch (om/get-state owner :histogram-field-ch)]

        ;; Initial data load:

        (let [filter (correct-nil-branch-filter (:filter app))]
        ;(let [filter (:filter app)]
          (load-chart-data owner (:cache app) chart-interval filter)
          (load-histogram-data owner (:cache app) histogram-field filter))

        ;; Individual data load for each chart when its own selector has changed:
        (go (loop []
              (let [[value source] (alts! [chart-interval-ch histogram-field-ch])]
                (condp = source
                  chart-interval-ch (load-chart-data owner (:cache app) value (:filter app))
                  histogram-field-ch (load-histogram-data owner (:cache app) value (:filter app))))
              (recur)))))

    om/IWillReceiveProps
    (will-receive-props
      [_ next-props]
      ;; Reload data when filters have changed:
      (let [previous-filter (:filter (om/get-props owner))
            new-filter (:filter next-props)]
        (when-not (= previous-filter new-filter)
          (let [new-filter (:filter next-props)
                ;_ (println "filter 1:" new-filter)
                new-filter (correct-nil-branch-filter new-filter)
                ;_ (println "filter 2:" new-filter)
                cache (:cache next-props)
                chart-interval (om/get-state owner :chart-interval)
                histogram-field (om/get-state owner :histogram-field)]
            (load-chart-data owner cache chart-interval new-filter)
            (load-histogram-data owner cache histogram-field new-filter)))))

    om/IWillUnmount
    (will-unmount
      [_]
      (stop-tracking-server! owner))

    om/IRender
    (render
      [_]
      (render-payments (:filter app) (:cache app) owner))))
