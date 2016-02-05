(ns amantha.utils
  (:require
    [clojure.string :as str]
    #?@(:cljs
        [[clojure.string :as str]
         [goog.i18n.DateTimeFormat]
         [goog.i18n.DateTimeFormat.Format]
         [goog.string :as gstring]
         [goog.string.format]]))
  #?(:clj (:import (clojure.lang Named))))

;; Data wrangling

(defn positions
  "Determine index positions in sequential collection where predicate holds"
  [pred coll]
  (keep-indexed (fn [idx x] (when (pred x) idx)) coll))

(defn enumerate
  "Map collection to sequence of [index, value] tuples"
  [coll]
  (map vector (range) coll))

;; Strings

(defn safe-name
  "Extended version of `clojure.core/name`, falls back to `toString`"
  [nameable]
  (if #?(:clj (instance? Named nameable)
         :cljs (implements? INamed nameable))
    (name nameable)
    (str nameable)))

(defn titlecase
  "Ensure Formal Capitalisation Of TXT"
  [string]
  (as-> (name string) %
        (str/split % #"\s")
        (map (fn [s] (when (pos? (count s)) (apply str (.toUpperCase (.substring s 0 1)) (.substring s 1)))) %)
        (str/join "" (interleave % (concat (re-seq #"\s" (name string)) [""])))))

;; Numbers

(defn coerce-long [s]
  (if (number? s) s #?(:cljs (js/parseInt s) :clj (Long/parseLong s))))

(defn coerce-float [s]
  (if (number? s) (float s) #?(:cljs (js/parseFloat s) :clj (Double/parseDouble s))))

;; Formatting

(defn format-currency [& {:keys [currency amount] :or {currency "R"}}]
  #?(:clj
     (assert false "Not implemented")
     :cljs
     (str/replace
       (str currency " " (gstring/format "%.2f" amount))
       #"\B(?=(\d{3})+(?!\d))"
       ",")))

#?(:cljs
   (def format-map
     (let [f goog.i18n.DateTimeFormat.Format]
       {:FULL_DATE       (.-FULL_DATE f)
        :FULL_DATETIME   (.-FULL_DATETIME f)
        :FULL_TIME       (.-FULL_TIME f)
        :LONG_DATE       (.-LONG_DATE f)
        :LONG_DATETIME   (.-LONG_DATETIME f)
        :LONG_TIME       (.-LONG_TIME f)
        :MEDIUM_DATE     (.-MEDIUM_DATE f)
        :MEDIUM_DATETIME (.-MEDIUM_DATETIME f)
        :MEDIUM_TIME     (.-MEDIUM_TIME f)
        :SHORT_DATE      (.-SHORT_DATE f)
        :SHORT_DATETIME  (.-SHORT_DATETIME f)
        :SHORT_TIME      (.-SHORT_TIME f)})))

(defn format-date-generic
  "Format a date using either the built-in goog.i18n.DateTimeFormat.Format enum
  or a formatting string like \"dd MMMM yyyy\""
  [date-format date]
  #?(:clj  (assert false "Not implemented")
     :cljs (.format (goog.i18n.DateTimeFormat.
                      (or (get format-map date-format) date-format))
                    (js/Date. date))))

(def format-date (partial format-date-generic "dd MMM yy, HH:mm Z"))
