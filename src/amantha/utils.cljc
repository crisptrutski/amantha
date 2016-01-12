(ns amantha.utils
  (:refer-clojure :exclude [read-string])
  (:require
    [clojure.string :as str]
    #?@(:cljs
        [[clojure.string :as str]
         [goog.i18n.DateTimeFormat]
         [goog.i18n.DateTimeFormat.Format]
         [goog.string :as gstring]
         [goog.string.format]])))

(defn no-op [& _])

(defn ensure-hash
  "Ensure that URL is absolute-fragment"
  [url]
  (if (= \# (first url))
    (apply str "/#" (rest url))
    (str "/#" url)))

(defn group-by-deep [ks data]
  (if-not (seq ks)
    data
    (let [[k & ks'] ks]
      (->> (group-by k data)
           (map (fn [[idx data']] [idx (group-by-deep ks' data')]))
           (into {})))))

(defn positions
  "Determine index positions in sequential collection where predicate holds"
  [pred coll]
  (keep-indexed (fn [idx x] (when (pred x) idx)) coll))

(defn frequencies-by
  "Returns a map from distinct items in coll to the number of times they appear."
  [f coll]
  (persistent!
    (reduce (fn [counts x]
              (let [v (f x)]
                (assoc! counts v (inc (get counts v 0)))))
            (transient {}) coll)))

(defn merge-by
  "Similar to merge-with, reduces elements over a collection grouped by
  key-fn and merges the value of val-fn as applied to each element with f.

      (merge-by + :year :sales [{:year 2014 :sales 20}
                                {:year 2014 :sales 50}
                                {:year 2015 :sales 10}])
      => {2014 70, 2015 10}"
  [f key-fn val-fn coll]
  (persistent!
    (reduce (fn [acc x]
              (let [v (key-fn x)]
                (assoc! acc v (f (get acc v 0) (val-fn x)))))
            (transient {}) coll)))


;; Dates

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

#?(:cljs
   (defn format-date-generic
     "Format a date using either the built-in goog.i18n.DateTimeFormat.Format enum
     or a formatting string like \"dd MMMM yyyy\""
     [date-format date]
     (.format (goog.i18n.DateTimeFormat.
                (or (get format-map date-format) date-format))
              (js/Date. date))))

#?(:cljs
   #_(defn format-date [& {:keys [date format] :or {format "Do MMM YYYY"}}]
       (.format (js/moment date) format))
   (def format-date (partial format-date-generic "dd MMM yy, HH:mm Z")))

;; Debugging

(defn p [x] (prn x) x)
(defn p* [f & args] (let [v (apply f args)] (prn v) v))

;; strings

(defn safe-name [nameable]
  (if (implements? #?(:cljs INamed :clj clojure.lang.Named) nameable)
    (name nameable)
    (str nameable)))

(defn titlecase [string]
  (as-> (name string) %
        (str/split % #"\s")
        (map (fn [s] (when (pos? (count s)) (apply str (.toUpperCase (.substring s 0 1)) (.substring s 1)))) %)
        (str/join "" (interleave % (concat (re-seq #"\s" (name string)) [""])))))

#?(:cljs
   (do
     (defn format-currency [& {:keys [currency amount] :or {currency "R"}}]
       (str/replace (str currency " " (gstring/format "%.2f" amount))
                    #"\B(?=(\d{3})+(?!\d))"
                    ","))

     (defn e->no-op [e]
       (doto e .preventDefault .stopPropagation))

     (defn handler-fn [f]
       ;; return a non-boolean to avoid "false is deprecated in React" warning
       (comp (constantly 0) f e->no-op))))

(defn zip [& colls]
  "E.g. (zip [1 2 3] [4 5 6]) => ([1 4] [2 5] [3 6])"
  (apply map vector colls))

(defn index-map
  "E.g. (index-map [:a :b :c]) => {:a 0, :b 1, :c 2)"
  [ks]
  (into {} (zip ks (-> ks count range))))

(defn enumerate [coll]
  (map vector (range) coll))

