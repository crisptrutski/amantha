(ns amantha.components.charts
  (:require
    [clojure.string :as str]
    [amantha.utils :as formatter]
    [amantha.utils :refer [gen-id] :as utils]
    #_[highcharts.js]))

(defn -get [name obj]
  (aget obj name))

(defn- -call [fname obj & args]
  (let [f (aget obj fname)]
    (.apply f obj (clj->js args))))

(defn create-highchart [chart-opts]
  (js/Highcharts.Chart.
    (clj->js
      (assoc-in
        chart-opts [:chart :renderTo]
        nil #_(om/get-node owner "chart")))))

(defn highchart-view [chart-opts]
  {:render     (fn []
                 (let [{:keys [width height]} chart-opts]
                   [:div {:class-name (when (:loading? chart-opts) "data-loading")
                          :ref        "chart"
                          :style      {:width width :height height}}]))
   :did-mount  (fn [_] (create-highchart chart-opts))
   :did-update (fn [_ _ _] (create-highchart chart-opts))})

(defn- histogram-tooltip [x-axis y-axis]
  (this-as point
    (let [y-label (aget point "y")
          x-label (aget point "x")]
      (str x-axis ": " x-label ", " y-axis ": " y-label))))

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
        chart-opts  (merge {:loading? (empty? x-axis-data)
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
    (highchart-view chart-opts)))

(defn build-histogram [data bucket-fn x-axis y-axis]
  (let [bucket-fn* (comp #(or % "(no data)" %) bucket-fn)
        chart-data (into (sorted-map)
                         (utils/frequencies-by bucket-fn* data))]
    (draw-histogram chart-data x-axis y-axis)))
