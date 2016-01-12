(ns amantha.components.grid
  (:require [amantha.grids.pagination :as page]
            [amantha.grids.sorting :as s]
            [amantha.utils :as utils]
            [amantha.components.basic :refer [glyphicon right-glyphicon]]
            [amantha.components.filters :as f]
            [goog.string :as gstring]))

;; helpers

(defn handle-sort [key_s sort-config]
  (s/update-sort-keys sort-config key_s))

(defn handle-sort-event [state key_s]
  (swap! state #(-> %
                    (update :sort-keys (partial handle-sort key_s))
                    (assoc :page-cursor nil))))

(defn handle-paging [state-atom update-f]
  (fn [_]
    (swap! state-atom update :page-cursor update-f)))

;; views

(defn- selected-column? [keys sort-keys]
  (when (>= (count sort-keys) (count keys))
    (let [head (take (count keys) sort-keys)]
      (every? identity (map s/match-sort-keys keys head)))))

(defn sort-icon [key_s sort-keys]
  (when (seq sort-keys)
    (let [keys (if (vector? key_s) key_s [key_s])]
      (if-not (selected-column? keys sort-keys)
        (right-glyphicon :chevron-up {:class "right" :style {:opacity 0}})
        (if (s/asc? (first sort-keys))
          (right-glyphicon :chevron-down)
          (right-glyphicon :chevron-up))))))

(defn grid-header-cell [{:keys [headers] :as config}
                        state-atom
                        [title value-fn _ custom-sort-keys]]
  (let [{:keys [sort-keys]} @state-atom]
    ;; init grid sort keys from config
    (when-not sort-keys
      (swap! state-atom assoc :sort-keys (:sort-keys config)))

    (if (or (keyword? value-fn) custom-sort-keys)
      ^{:key title}
      [:th.clickable {:width    (str (/ 100 (inc (count headers))) "%")
                      :on-click #(handle-sort-event state-atom (or custom-sort-keys value-fn))}
       title (sort-icon (or custom-sort-keys value-fn) sort-keys)]
      ^{:key title}
      [:th.unsortable title])))

(defn- cell-text [content]
  (if (vector? content)
    (cell-text (last content))
    content))

(defn grid-data-cell [row [title value-fn display-fn]]
  (let [content (-> row value-fn display-fn)]
    [:td {:key title}
     [:div.td-container
      [:div.td-content {:title (cell-text content) :style {:width "100%"}}
       content]
      [:div.td-spacer content]
      [:span (gstring/unescapeEntities "&nbsp;")]]]))

(defn grid-action-cell [row {:keys [key label handler disable? hide?]}]
  (let [key-str   (name key)
        disabled? (when disable? (disable? row))
        hidden?   (cond (= hide? true) disabled?
                        (fn? hide?) (hide? row))]
    (when-not hidden?
      ^{:key key}
      [:span {:style {:padding [0 2 2 0]}}
       [:button.btn.btn-xs {:class    key-str
                            :key      key-str
                            :on-click (partial handler row)
                            ;; :style    (if hidden? {:display "none"})
                            :disabled disabled?}
        label]])))

(defn- row-id [row]
  (or (:id row)
      (:case-number row)
      (hash row)))

(defn spaced [& children]
  (interpose " " children))

(defn grid-header-button [& {:keys [disabled on-click label]}]
  [:button.btn.btn-xs.btn-default {:disabled disabled
                                   :on-click on-click} label])

(defn grid-controls
  "Grid navigation: [prev, page num / total, next]"
  [state-atom prev? next? & [page-num total-pages]]
  (let [handler (partial handle-paging state-atom)]
    (into
      [:div.grid-controls {:key "controls"}]
      (spaced
        (grid-header-button :disabled (= 1 page-num)
                            :on-click (handler (constantly [:first]))
                            :label [:span (glyphicon "step-backward") " First"])
        (grid-header-button :disabled (not prev?)
                            :on-click (handler (comp page/serialize page/prev page/parse))
                            :label [:span (glyphicon "chevron-left") " Prev"])

        page-num (if total-pages " / ") total-pages

        (grid-header-button :disabled (not next?)
                            :on-click (handler (comp page/serialize page/next page/parse))
                            :label [:span "Next " (glyphicon "chevron-right")])
        (grid-header-button :disabled (= page-num total-pages)
                            :on-click (handler (constantly [:last]))
                            :label [:span "Last " (glyphicon "step-forward")])))))

(def dont-kill-dom-max 1000)

(defn grid-row [row [row-id headers actions col-width]]
  [:tr {:key (row-id row)}
   (list
     (map (partial grid-data-cell row) headers)
     (if (seq actions)
       [:td {:key   "actions"
             :style {:width col-width, :text-align "left"}}
        (interpose " " (remove nil? (map (partial grid-action-cell row) actions)))]))])

;; TODO: use internal state for table filters
(def att (reagent.core/atom nil))

(defn draw-grid [state-atom config headers actions rows
                 & [{:keys [idx current-page total-pages prev? next? total until] :as pagination}
                    {:keys [pagination? footer no-stripes] :as opts}]]
  ;; Work around for bad data or config -
  ;; Hash actual data to ensure unique React id.
  (let [dupes?    (some #(< 1 %) (map count (vals (group-by row-id rows))))
        row-id    (if dupes? hash row-id)
        col-width (str (/ 100 (+ (count headers) (if (seq actions) 1 0))) "%")
        pending?  (:pending? config)]
    [:div {:style {:opacity        (if pending? 0.7 1)
                   :pointer-events (if pending? "none" "")}}
     [:div.row
      ;; top nav
      (if pagination?
        [:div
         [:div.col-sm-12
          (grid-controls state-atom prev? next? current-page total-pages)
          [:small.text-muted "Showing " idx " to " until " of " total]]])]
     ;; grid proper
     [:table.table {:class (if-not no-stripes "table-striped")}
      [:thead
       [:tr {:key "header"}
        ^{:key "headers"}
        (list (doall (map (partial grid-header-cell config state-atom) headers))
              (when (seq actions) [:th {:key "actions"}]))]
       ;; parameterise
       (when true
         [:tr {:key "filter"}
          (doall
            (for [h headers]
              ^{:key (first h)}
              [:td
               [:input.form-control {:value       (:value @att)
                                     :on-change   (f/handle-full-text-search-change "%s" att)
                                     :placeholder (:placeholder @att)}]]))])]
      [:tbody
       (if-not (seq rows)
         [:tr {:key "loading"}
          [:td {:col-span (inc (count headers))}
           (if pending? "Loading.." "No Data.")]]
         ;; grid contents
         (doall
           (for [r rows]
             ^{:key (row-id r)}
             (grid-row r [row-id headers actions col-width]))))]
      (if footer
        [:tfoot
         (footer rows)])]
     ;; bottom nav
     (if pagination?
       (grid-controls state-atom prev? next? current-page total-pages))]))

(defn paginate-client-side [rows sort-keys cursor limit]
  (let [[cursor* rows*] (page/paginate-and-sort cursor rows sort-keys limit)
        idx          (inc (first (utils/positions #{(first rows*)} (page/fast-sort sort-keys rows))))
        current-page (inc (Math/ceil (/ (dec idx) limit)))
        total-pages  (+ current-page
                        (Math/ceil (/ (dec (count (drop-while
                                                    #(not= (last rows*) %)
                                                    (s/sort-by-keys sort-keys rows))))
                                      limit)))
        prev?        (> idx 1)
        next?        (< idx (inc (- (count rows) (count rows*))))
        total        (count rows)
        until        (+ idx (dec (count rows*)))]
    {:cursor     cursor*
     :rows       rows*
     :pagination {:idx          idx
                  :current-page current-page
                  :total-pages  total-pages
                  :prev?        prev?
                  :next?        next?
                  :total        total
                  :until        until}}))

(defn grid-view
  [rows {:keys [headers actions pagination?] :as config} state-atom]
  (let [grid-state @state-atom
        grid-state (or grid-state config)
        cursor*    (page/parse (:page-cursor grid-state))
        sort-keys  (:sort-keys grid-state)
        limit      (or (:limit config) (when-not pagination? dont-kill-dom-max) 5)
        {:keys [cursor
                rows
                pagination]} (paginate-client-side rows sort-keys cursor* limit)]

    ;; not ideal doing this during render, should perform this when handling the
    (when (not= cursor cursor*)
      (swap! state-atom assoc :page-cursor (page/serialize cursor)))

    (draw-grid state-atom config headers actions rows
               pagination
               {:footer      (:footer config)
                :pagination? pagination?
                :no-stripes  (:no-stripes config)})))
