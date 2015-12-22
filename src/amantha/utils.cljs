(ns amantha.utils
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction]])
  (:require #_[ajax.core :refer [GET]]
            [cljs.core.async :refer [<!]]
            [cljsjs.moment]
            [clojure.string :as str]
            [goog.string :as gstring]
            [goog.string.format]
            [reagent.core :as reagent]
            [re-frame.core :refer [subscribe dispatch register-sub register-handler]]
            [sablono.core :refer-macros [html]]))

(declare GET)

(def present? (comp not str/blank?))

(defn format-currency [& {:keys [currency amount] :or {currency "R"}}]
  (str/replace
    (str currency " " (gstring/format "%.2f" amount))
    #"\B(?=(\d{3})+(?!\d))"
    ","))

(defn format-date [& {:keys [date format] :or {format "Do MMM YYYY"}}]
  (.format (js/moment date) format))

;; UUID4 generator adapted from
;; http://stackoverflow.com/questions/105034/create-guid-uuid-in-javascript

(def ^:private uuid-4-pattern "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx")

(defn- fill-char [c]
  (let [r (bit-or (* (js/Math.random) 16) 0)]
    (.toString (if (= c "x") r (bit-or (bit-and r 3) 8)) 16)))

(defn tempid
  "UUID 4"
  []
  (UUID. (str/replace uuid-4-pattern #"[xy]" fill-char)))

;;

(defn relative-time [date-time]
  (.fromNow (js/moment date-time)))

(defn str->int [value]
  (js/parseInt value))

(defn str->currency [currency]
  ((aget js/accounting "parse") currency))

(defn get-by-id [id]
  (js/document.getElementById id))

(defn get-value-by-id [id]
  (.-value (get-by-id id)))

(defn set-value-by-id [id value]
  (-> id get-by-id .-value (set! value)))

(defn get-data
  ([url params handler]
   (get-data url params handler println))
  ([url params handler error-handler]
   (GET url
        {:handler handler
         :params params
         :error-handler error-handler
         :response-format :edn})))

(defn listen [channel handler]
  (go (while true
        (handler (<! channel)))))

(defn zip [& colls]
  "E.g. (zip [1 2 3] [4 5 6]) => ([1 4] [2 5] [3 6])"
  (apply map vector colls))

(defn index-map
  "E.g. (index-map [:a :b :c]) => {:a 0, :b 1, :c 2)"
  [ks]
  (into {} (zip ks (-> ks count range))))

(defn enumerate [coll]
  (map vector (range) coll))

(defn format
  "Formats a string using goog.string.format."
  [fmt & args]
  (apply gstring/format fmt args))

(defn handler-fn [f]
  (fn [e]
    (.preventDefault e)
    (.stopPropagation e)
    (f e)
    ;; return a non-boolean to avoid "false is deprecated in React" warning
    0))

(defn dispatcher [event]
  (handler-fn #(dispatch (if (fn? event)
                           (event %)
                           event))))

(defn register-path-sub [key path]
  (register-sub
    key
    (fn [db [_ & subpath]]
      (reaction (get-in @db (concat path subpath))))))

(defn register-set-path-handler [key path]
  (register-handler
    key
    (fn [db [_ & args]]
      (let [val (last args)
            subpath (butlast args)]
        (assoc-in db (concat path subpath) val)))))

(defn register-path-get-and-set [& key-path-pairs]
  (doseq [[key path] (partition 2 key-path-pairs)]
    (register-path-sub key path)
    (register-set-path-handler key path)))

(defn truncate [text]
  (str (subs text 0 40) "..."))

(defn reagent-bridge
  "Mount a Reagent component inside an Om component tree.

  Example:

  (om/build reagent-bridge cursor {:opts {:component reagent-component-fn}})"
  [_ owner {:keys [component]}]
  (reify
    om/IDidMount
    (did-mount [_]
      (let [elem (om/get-node owner "reagent-component")]
        (reagent/render-component [component] elem)))

    om/IRender
    (render [_]
      (html
       [:span {:ref "reagent-component"}]))))

(defn on-enter [callback]
  (fn [evt]
    (when (= 13 (.-keyCode evt))
      (callback evt))))

(defn map-function-on-map-vals [m f]
  (reduce (fn [altered-map [k v]] (assoc altered-map k (f v))) {} m))
