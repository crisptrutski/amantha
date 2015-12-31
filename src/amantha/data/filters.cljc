(ns amantha.data.filters
  (:require
    [clojure.string :as str]
    [amantha.grids.filters :refer [filter-check]])
  #?(:clj (:import [java.util Date])))

(defn parse-float [s]
  #?(:cljs (js/parseFloat s) :clj (Double/parseDouble s)))

(defmethod filter-check :include-string [[_ match pattern]]
  (let [lower (.toLowerCase match)
        total (if pattern (str/replace pattern #"%s" lower) lower)
        regex (re-pattern total)]
    #(re-find regex (.toLowerCase (or % "")))))

(defmethod filter-check :date-range [[_ start-date end-date]]
  (let [t (fn [date]
            #?(:cljs (.valueOf date)
               :clj  (.getTime ^Date date)))
        a (t start-date)
        b (t end-date)]
    #(<= a (t %) b)))

(defmethod filter-check :equal [[_ value]] #{value})

(defmethod filter-check :>= [[_ n]]
  (let [n (parse-float n)] #(>= (parse-float %) n)))

(defmethod filter-check :<= [[_ n]]
  (let [n (parse-float n)] #(<= (parse-float %) n)))

(defmethod filter-check :any-of [[_ values]]
  (fn [val] (some #{val} values)))
