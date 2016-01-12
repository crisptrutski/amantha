(ns amantha.components.forms
  (:require
    [amantha.utils :refer [no-op handler-fn]]
    [amantha.utils.dom :refer [e->value]]
    [reagent.ratom :as ratom]))

(defn serialize-form
  "Convert from `{:a/b x}` to `{:a {:b x}}` to "
  [state]
  (reduce (fn [h [k v]]
            (let [a  (namespace k)
                  b  (name k)
                  ks (if a [a b] [b])]
              (assoc-in h (map keyword ks) v)))
          {}
          state))

(defn deserialize-form
  "Convert from `{:a {:b x}}` to `{:a/b x}`"
  [data]
  (reduce (fn [h [k v]]
            (if-not (map? v)
              (assoc h k v)
              (reduce (fn [h' [k' v']]
                        (assoc h' (keyword (name k) (name k')) v')) h v)))
          {}
          data))

(defn state [data]
  (ratom/atom (deserialize-form data)))

(defn handle-submit [state f]
  (handler-fn (fn [_] (-> state deref serialize-form f))))

(defn form-input
  ([state key]
   (form-input state key {}))
  ([state key {:keys [type placeholder on-change]
               :or {type "text"
                    placeholder ""
                    on-change no-op} :as opts}]
   (merge opts
          {:class       "form-control"
           :type        type
           :value       (get @state key)
           :placeholder placeholder
           :on-change   #(comp on-change (swap! state assoc key (e->value %)))})))
