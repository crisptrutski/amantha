(ns amantha.components.data-card
  (:require
    [amantha.utils :as u]
    [clojure.string :as str]))

(defn field-width [field]
  (:width field 1))

(defn cell [field]
  (:content field field))

(defn row-width [row]
  (let [base (if (:title row) 1 0)]
    (reduce + base (map field-width (:fields row)))))

(defn data-card-rows [width rows]
  (for [r rows]
    (let [title (:title r)
          fields (:fields r [(:field r)])
          cnt (+ (count fields) (if title 1 0))]
      (into [:tr]
            (concat [(when title [:th (cell title)])]
                    (vec (for [f (butlast fields)] [:td {:col-span (:width f 1)} (cell f)]))
                    [[:td {:col-span (- (inc width) cnt)} (cell (last fields))]])))))

(defn data-card [rows]
  (let [width (apply max (map row-width rows))
        loose (remove :section rows)
        sections (distinct (keep :section rows))]
    [:div.statsDiv
     [:table.statsTable
      (into [:tbody]
            (concat (data-card-rows width loose)
                    (apply concat (for [s sections]
                                    (conj (data-card-rows width (filter (comp #{s} :section) rows))
                                          [:tr [:th.statsDiv--header {:col-span width} s]])))))]]))

(defn map->card [hsh]
  (for [[k v] hsh]
    {:title (u/titlecase (str/replace (u/safe-name k) #"-" " ")) :field v}))

