(ns amantha.filters)

(defn ->float [s]
  #?(:clj  (Double. (str s))
     :cljs (js/parseFloat s)))

(defmulti filter-check first)

(defmethod filter-check :default [payload]
  (prn "WARNING: unable to filter payload " payload)
  (constantly true))

(defn filter-once
  "Perform single level of filtering (eg. with names starting with 'Cla')"
  [data [key filter-opts]]
  (let [p (second filter-opts)]
    (if (or (not p) (and (string? p) (empty? p)))
      data
      (filter (comp (filter-check filter-opts) key)
              data))))

(defn- apply-filters [data filters]
  (vec (reduce filter-once data filters)))

(defn combine-filters [filter-specs]
  (vec (for [{:keys [key filters]} filter-specs
             filter filters]
         [key filter])))

;; API

(defn filter-data [data filter-specs]
  (if data
    (if-not filter-specs
      data
      (apply-filters data (combine-filters filter-specs)))))
