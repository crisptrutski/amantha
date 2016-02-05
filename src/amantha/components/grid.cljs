(ns amantha.components.grid
  (:require
    [amantha.grids.pagination :as page]
    [amantha.grids.sorting :as s]
    [amantha.utils :as utils]
    [amantha.components.filters :as f]
    [amantha.components.theme :as t]
    [reagent.core :refer [atom]]))

(def dont-kill-dom-max 10000)

(def default-page-size 10)

;; helpers

(defn update-state [key_s state-value]
  (-> state-value
      (update :sort-keys #(s/update-sort-keys % key_s))
      (assoc :page-cursor nil)))

(defn handle-sort-event-fn [state key_s]
  (fn [_]
    (swap! state (partial update-state key_s))))

(defn update-cursor-fn [state-atom update-f]
  (fn [_]
    (swap! state-atom update :page-cursor update-f)))

(defn- selected-column? [keys sort-keys]
  (when (>= (count sort-keys) (count keys))
    (let [head (take (count keys) sort-keys)]
      (every? identity (map s/match-sort-keys keys head)))))

;; views

(defn sort-icon [key_s sort-keys]
  (when (seq sort-keys)
    (let [keys (if (vector? key_s) key_s [key_s])]
      (if-not (selected-column? keys sort-keys)
        (t/sort-glyph false false)
        (if (s/asc? (first sort-keys))
          (t/sort-glyph true true)
          (t/sort-glyph true false))))))

(defn grid-header-cell [{:keys [headers] :as config}
                        state-atom
                        [title value-fn _ custom-sort-keys]]
  (let [sort? (or (keyword? value-fn) custom-sort-keys)
        sort! (when sort? (handle-sort-event-fn state-atom (or custom-sort-keys value-fn)))]
    (t/header-cell
      {:width (str (/ 100 (inc (count headers))) "%") :sort-fn sort!}
      title
      (when sort? (sort-icon (or custom-sort-keys value-fn) (:sort-keys @state-atom))))))

(defn grid-data-cell [row [title value-fn display-fn]]
  (t/data-cell title (-> row value-fn display-fn)))

(defn grid-action-cell [row {:keys [key label handler disable? hide?]}]
  (let [disabled? (when disable? (disable? row))]
    (t/action-cell
      {:key (name key)
       :disabled? disabled?
       :hidden (cond (= hide? true) disabled? (fn? hide?) (hide? row))
       :action-fn (partial handler row)}
      label)))

(defn- row-id [row]
  (or (:id row) (hash row)))

(defn grid-controls
  "Grid navigation: [prev, page num / total, next]"
  [state-atom prev? next? & [page-num total-pages]]
  (let [handler (partial update-cursor-fn state-atom)]
    (t/grid-header-btns
      (t/grid-header-btn :first (when (not= 1 page-num) (handler (constantly [:first]))))
      (t/grid-header-btn :prev (when prev? (handler (comp page/serialize page/prev page/parse))))
      (t/grid-header-btn :next (when next? (handler (comp page/serialize page/next page/parse))))
      (t/grid-header-btn :last (when (<= page-num total-pages) (handler (constantly [:last]))))
      page-num total-pages)))

(defn grid-row [row [row-id headers actions col-width]]
  [:tr {:key (row-id row)}
   (list
     (map (partial grid-data-cell row) headers)
     (if (seq actions)
       [:td {:key "actions"
             :style {:width col-width, :text-align "left"}}
        (interpose " " (remove nil? (map (partial grid-action-cell row) actions)))]))])

;; TODO: use internal state for table filters
(def att (atom nil))

;; THEME

(defn container-view [{:keys [pending?]} header table footer]
  [:div {:style {:opacity (if pending? 0.7 1), :pointer-events (if pending? "none" "")}}
   [:div.row header]
   table
   [:div.row footer]])

(defn table-view [{:keys [stripes?]} header-rows rows footer-rows]
  [:table.table {:class (when stripes? "table-striped")}
   (when (seq header-rows) (into [:thead] header-rows))
   (into [:tbody] rows)
   (when (seq footer-rows) (into [:tfoor] footer-rows))])

(defn position-view [{:keys [idx total until] :as pagination}]
  [:small.text-muted "Showing " idx " to " until " of " total])

(defn pagination-view
  [state-atom
   {:keys [current-page total-pages prev? next?] :as pagination}
   {:keys [pagination? text-summary?]}]
  (when pagination?
    [:div
     [:div.col-sm-12
      (grid-controls state-atom prev? next? current-page total-pages)
      (when text-summary?
        (position-view pagination))]]))

(defn filter-cell [key filter-atom]
  ^{:key key}
  [:td
   [:input.form-control
    {:value (:value @filter-atom)
     :on-change (f/handle-full-text-search-change "%s" filter-atom)
     :placeholder (:placeholder @filter-atom)}]])

(defn draw-grid [state-atom config headers actions rows pagination]
  (container-view
    config
    (pagination-view state-atom pagination (assoc config :text-summary true))

    (table-view
      {:stripes? (not (:no-stripes config))}

      (t/headers
        (mapv (partial grid-header-cell config state-atom) headers)
        (seq actions)
        (when (:filterable? config)
          (seq (mapv #(filter-cell (first %) att) headers))))

       (if-not (seq rows)
         (let [width (if (seq actions) (inc (count headers)) (count headers))]
           (t/loading (:pending? config) width))
         (let [dupes? (some #(< 1 %) (map count (vals (group-by row-id rows))))
               row-id (if dupes? hash row-id)
               col-width (str (/ 100 (+ (count headers) (if (seq actions) 1 0))) "%")]
           (doall (for [r rows] (grid-row r [row-id headers actions col-width])))))
       (when-let [f (:footer config)] (f rows)))

    (pagination-view state-atom pagination config)))

(defn paginate-client-side [rows sort-keys cursor limit]
  (let [[cursor* rows*] (page/paginate-and-sort cursor rows sort-keys limit)
        idx (inc (first (utils/positions #{(first rows*)} (s/sort-by-keys sort-keys rows))))
        current-page (inc (Math/ceil (/ (dec idx) limit)))
        total-pages (+ current-page
                       (Math/ceil (/ (dec (count (drop-while
                                                   #(not= (last rows*) %)
                                                   (s/sort-by-keys sort-keys rows))))
                                     limit)))
        prev? (> idx 1)
        next? (< idx (inc (- (count rows) (count rows*))))
        total (count rows)
        until (+ idx (dec (count rows*)))]
    {:cursor cursor*
     :rows rows*
     :pagination {:idx idx
                  :current-page current-page
                  :total-pages total-pages
                  :prev? prev?
                  :next? next?
                  :total total
                  :until until}}))

(defn grid-view
  [rows {:keys [headers actions pagination?] :as config} state-atom]
  (let [grid-state @state-atom
        grid-state (or grid-state (reset! state-atom config))
        cursor* (page/parse (:page-cursor grid-state))
        sort-keys (:sort-keys grid-state)
        limit (or (:limit config) (when-not pagination? dont-kill-dom-max) default-page-size)
        {:keys [cursor rows pagination]} (paginate-client-side rows sort-keys cursor* limit)]

    ;; not ideal doing this during render, should perform this after handlers, before next render
    (when (not= cursor cursor*)
      (swap! state-atom assoc :page-cursor (page/serialize cursor)))

    (draw-grid state-atom config headers actions rows pagination)))
