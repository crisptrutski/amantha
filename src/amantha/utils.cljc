(ns bss.rampant.utils
  (:refer-clojure :exclude [read-string])
  #?@(:clj [(:require [clojure.edn :refer [read-string]]
                      [clojure.java.io :as io]
                      [clojure.string :as str])]
      :cljs [(:import [goog.net XhrIo])
             (:require [cljs.reader]
                       [clojure.string :as str]
                       [goog.i18n.DateTimeFormat :as dtf]
                       [goog.i18n.DateTimeFormat.Format])]))

(defn no-op [& _])

(defn group-by-deep [ks data]
  (if-not (seq ks)
    data
    (let [[k & ks'] ks]
      (->> (group-by k data)
           (map (fn [[idx data']] [idx (group-by-deep ks' data')]))
           (into {})))))

#?(:clj
(defn safe-println
  "Threadsafe println. Useful for debugging and basic logging."
  [& more]
  (.write *out* (str (str/join " " more) "\n"))))

#?(:clj
(defn load-fixtures
  "Produce mapping of filenames to data, for EDN files in fixtures folder"
  ([] (load-fixtures "fixtures"))
  ([path]
   (let [f (io/file path)]
     (into {} (for [file (file-seq f)
                    :when (.endsWith (.getName file) ".edn")
                    :let [name (str/replace (.getName file) #".\w+$" "")]]
                [name (vec (read-string (slurp file)))]))))))

(defn positions
  "Determine index positions in sequential collection where predicate holds"
  [pred coll]
  (keep-indexed (fn [idx x] (when (pred x) idx)) coll))

(defn frequencies-by
  "Returns a map from distinct items in coll to the number of times
  they appear."
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


(defn- escape-char? [s]
  (some #{\, \" \return \newline} s))

(defn- escape-char [s]
  (str \" (str/escape s {\"       "\\\""
                         \return  "\\r"
                         \newline "\\n"})
       \"))

(defn- escape [s]
  (let [s (str s)]
    (if (escape-char? s) (escape-char s) s)))

(defn data->csv [data]
  (let [fields (keys (first data))]
    (str/join "\n"
              (cons
               (str/join "," (map (comp escape name) fields))
               (for [row data]
                 (str/join "," (map #(escape (get row %)) fields)))))))

#?(:cljs
(defn set-url!
  "Hash navigation" ;; TODO: use push state where available
  [url]
  (aset js/window.location "hash" url)))

;; Dates

#?(:cljs
(def format-map
  (let [f goog.i18n.DateTimeFormat.Format]
    {:FULL_DATE (.-FULL_DATE f)
     :FULL_DATETIME (.-FULL_DATETIME f)
     :FULL_TIME (.-FULL_TIME f)
     :LONG_DATE (.-LONG_DATE f)
     :LONG_DATETIME (.-LONG_DATETIME f)
     :LONG_TIME (.-LONG_TIME f)
     :MEDIUM_DATE (.-MEDIUM_DATE f)
     :MEDIUM_DATETIME (.-MEDIUM_DATETIME f)
     :MEDIUM_TIME (.-MEDIUM_TIME f)
     :SHORT_DATE (.-SHORT_DATE f)
     :SHORT_DATETIME (.-SHORT_DATETIME f)
     :SHORT_TIME (.-SHORT_TIME f)})))

#?(:cljs
(defn format-date-generic
  "Format a date using either the built-in goog.i18n.DateTimeFormat.Format enum
  or a formatting string like \"dd MMMM yyyy\""
  [date-format date]
  (.format (goog.i18n.DateTimeFormat.
             (or (get format-map date-format) date-format))
           (js/Date. date))))

#?(:cljs
(def format-date (partial format-date-generic "dd MMM yy @ HH:mm")))

;; Debugging

(defn p [x] (prn x) x)
(defn p* [f & args] (let [v (apply f args)] (prn v) v))
(defn pc [xs] (prn (count xs)) xs)
(defn pc* [f & xs] (let [v (apply f xs)] (pc v) v))

;; UI Helpers

;; TODO: just use a (sq)uuid generator

(defonce ^:private last-id (atom 0))
(defn gen-id [] (swap! last-id inc))

(defn e->value [e]
  (-> e .-target .-value))

(defn glyphicon [type & body]
  (apply vector
         (keyword (str "span.glyphicon.glyphicon-" (name type)))
         body))

(defn right-glyphicon [type & body]
  (apply vector
         (keyword (str "span.right.glyphicon.glyphicon-" (name type)))
         body))

;; local storage

;; NOTE: pefer data to interface, replace usage of these helpers perhaps with:
;; https://github.com/dialelo/hodgepodge, transient-like direct usage
;; https://github.com/eneroth/plato, atom which syncs with mapped values
;; https://github.com/alandipert/storage-atom, atom which syncs as whole

#?(:cljs [

(defn read-storage [key]
  (if-let [str (aget js/localStorage key)]
    (cljs.reader/read-string str)))

(defn write-storage [key value]
  (aset js/localStorage key (prn-str value)))

(defn get-local-nav []
  (if-let [nav (read-storage "bss.nav")]
    (keyword nav)))

(defn set-local-nav [section]
  (write-storage "bss.nav" section))

])

;; strings

(defn titlecase [string]
  (->> (str/split (name string) #" |-")
       (map (fn [s] (apply str (.toUpperCase (.substring s 0 1)) (.substring s 1))))
       (str/join " ")))

;; charting

#?(:cljs
(defn ^:deprecated strip-timestamp
  "Convert date to string"
  [date]
  (js/Date. (.toDateString date))))

;; AJAX

#?@(:cljs [

(defn- wrap-edn-callback [callback]
  (if-not callback
    no-op
    (fn [reply]
      (-> (.-target reply)
          (.getResponseText)
          (cljs.reader/read-string)
          (callback)))))

(defn- build-http-vars [payload]
  (str/join "&" (map (fn [[k v]] (str (name k) "=" (pr-str v))) payload)))

(defn ^:deprecated ajax [url callback]
  (.send goog.net.XhrIo url
         (wrap-edn-callback callback)))

(defn ^:deprecated ajax-post [url & [payload callback]]
  (.send goog.net.XhrIo url
         (wrap-edn-callback callback)
         "POST"
         (build-http-vars payload)))

(defn ^:deprecated ajax-put [url & [payload callback]]
  (.send goog.net.XhrIo url
         (wrap-edn-callback callback)
         "PUT"
         (build-http-vars payload)))

])
