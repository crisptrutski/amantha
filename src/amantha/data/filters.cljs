(ns amantha.data.filters
  (:require [clojure.string :as str]
            [amantha.filters :refer [filter-check]]))

(defn ->float [s]
  (js/parseFloat s))

(defmethod filter-check :include-string [[_ match pattern]]
  (let [lower (.toLowerCase match)
        total (if pattern (str/replace pattern #"%s" lower) lower)
        regex (re-pattern total)]
    #(re-find regex (.toLowerCase (or % "")))))

(defmethod filter-check :date-range [[_ start-date end-date]]
  ;; #+clj .getTime #+cljs
  (let [t (fn [date] (.valueOf date))
        a (t start-date)
        b (t end-date)]
    #(<= a (t %) b)))

(defmethod filter-check :equal [[_ value]] #{value})

(defmethod filter-check :>= [[_ n]]
  (let [n (->float n)] #(>= (->float %) n)))

(defmethod filter-check :<= [[_ n]]
  (let [n (->float n)] #(<= (->float %) n)))

(defmethod filter-check :any-of [[_ values]]
  (fn [val]
    (some #{val} values)))
