(ns amantha.utils.dom)

(defn get-by-id [id]
  (.getElementById js/document id))

(defn get-value-by-id [id]
  (.-value (get-by-id id)))

(defn set-value-by-id [id value]
  (-> id get-by-id .-value (set! value)))

(defn e->value [e]
  (-> e .-target .-value))

