(ns amantha.components.charts
  (:require [clojure.string :as str]
            [amantha.data.api :as api]
            [amantha.data.api :as api]
            [amantha.components.generic :as generic]
            [amantha.utils :as formatter]
            [amantha.utils :refer [gen-id] :as utils]
            [highcharts.js]
            [om.core :as om :include-macros true]
            [sablono.core :refer-macros [html]]))

(defn -get [name obj]
  (aget obj name))

(defn- -call [fname obj & args]
  (let [f (aget obj fname)]
    (.apply f obj (clj->js args))))

(defn create-highchart [owner chart-opts]
  (js/Highcharts.Chart. (clj->js (assoc-in chart-opts [:chart :renderTo] (om/get-node owner "chart")))))

(defn highchart-view [cursor owner]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [width height]} cursor]
        (html
         [:div {:class-name (if (:loading? cursor) "data-loading")
                :ref "chart" :style {:width width :height height}}])))
    om/IDidMount
    (did-mount [_]
      (create-highchart owner cursor))
    om/IDidUpdate
    (did-update [_ _ _]
      (create-highchart owner cursor))))

(defn- histogram-tooltip [x-axis y-axis]
  (this-as point
           (let [y-label (aget point "y")
                 x-label (aget point "x")]
             (str x-label " - " y-label))))

(defn- titlecase-word [word]
  (apply str (.toUpperCase (first word))
         (rest word)))

(defn- histogram-label [value]
  (cond
    (= js/Date (type value))
    (formatter/format-date :date value :format "Do MMM 'YY")

    (keyword? value)
    (as-> value x
      (name x)
      (str/split x #"-")
      (map titlecase-word x)
      (str/join " " x))

    :else
    value))

(defn draw-histogram [chart-data x-axis y-axis & [overrides]]
  (let [x-axis-data (map histogram-label (map first chart-data))
        y-axis-data (map histogram-label (map last chart-data))
        chart-opts (merge {:loading? (empty? x-axis-data)
                           :chart    {:type "column"}
                           :legend   {:enabled false}
                           :title    {:text ""}
                           :xAxis    {:title {:text (histogram-label x-axis)} :categories x-axis-data}
                           :yAxis    {:title {:text (histogram-label y-axis)}}
                           :series   [{:data y-axis-data}] ;; animation false
                           :tooltip  {:formatter histogram-tooltip}
                           :width    "100%"
                           :height   250}
                          overrides)]
    (om/build highchart-view chart-opts)))

(defn server-histogram [cursor {:keys [data-key hist-key x-axis y-axis] :as opts} owner]
  (when (not= opts(om/get-state owner :old-opts))
    (om/set-state! owner :old-opts opts)
    (generic/dirty! owner))

  (when (generic/dirty? owner)
    (api/histogram (fn [data]
                     (generic/clean! owner)
                     (om/update! cursor data-key data))
                   data-key
                   hist-key
                   {:filter  (om/value (:filter opts))
                    :roll-up (om/value (:roll-up opts))}))

  (html
   [:div (if (generic/dirty? owner) {:style {:opacity 0.5}})
    ;; FIXME: use axis labels from data - empower that API
    (draw-histogram (get cursor data-key)
                    x-axis
                    y-axis)]))

(defn server-histogram-view [{:keys [options cursor]} owner _]
  (let [korks [:global-sha (:data-key options)]]
    (generic/tracked-component-shell owner
                                     server-histogram cursor options
                                     owner)))

(defn build-histogram [data bucket-fn x-axis y-axis]
  (let [bucket-fn* (comp #(or % "(no data)" %) bucket-fn)
        chart-data (into (sorted-map)
                         (utils/frequencies-by bucket-fn* data))]
    (draw-histogram chart-data x-axis y-axis)))

(defmulti custom-chart-view (fn [config] (:chart-key config)))

(defmethod custom-chart-view
  :default
  [app owner _]
  (om/component (html [:div "Bad config"])))
