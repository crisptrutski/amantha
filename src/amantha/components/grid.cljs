(ns amantha.components.grid
  (:require [om.core :as om :include-macros true]
            [amantha.components.generic :as generic]
            [amantha.data.api :as api]
            [bss.rampant.pagination :as page]
            [bss.rampant.sorting :as s]
            [bss.rampant.utils :as utils :refer [glyphicon right-glyphicon]]
            [clojure.string :as str]
            [goog.string :as gstring]
            [sablono.core :as html :refer-macros [html]]))

;; helpers

(defn handle-sort [key_s]
  (fn [sort-config]
    (s/update-sort-keys sort-config (om/value key_s))))

(defn handle-sort-event [cursor key_s]
  #(do (om/transact! cursor :sort-keys (handle-sort key_s))
       (om/update! cursor :page-cursor nil)))

(defn handle-paging [grid-state update-f]
  (fn [_]
    (om/transact! grid-state :page-cursor update-f)))

;; views

(defn- selected-column? [keys sort-keys]
  (let [head (take (count keys) sort-keys)]
    (every? identity (map s/match-sort-keys keys head))))

(defn sort-icon [key_s sort-keys]
  (if sort-keys
    (let [keys (if (vector? (om/value key_s)) key_s [key_s])]
      (if-not (selected-column? keys sort-keys)
        (right-glyphicon :chevron-up {:class "right" :style {:opacity 0}})
        (if (s/asc? (first sort-keys))
          (right-glyphicon :chevron-down)
          (right-glyphicon :chevron-up))))))

(defn grid-header-cell [{:keys [headers] :as config}
                        {:keys [sort-keys] :as grid-state}
                        [title value-fn _ custom-sort-keys]]
  ;; init grid sort keys from config
  (when-not sort-keys
    (om/update! grid-state :sort-keys (om/value (:sort-keys config))))

  (if (or (keyword? value-fn) custom-sort-keys)
    [:th.clickable {:key      title,
                    :width    (str (/ 100 (inc (count headers))) "%")
                    :on-click (handle-sort-event grid-state (or custom-sort-keys value-fn))}
     title (sort-icon (or custom-sort-keys value-fn) sort-keys)]
    [:th.unsortable {:key title} title]))

(defn- cell-text [content]
  (if (vector? content)
    (cell-text (last content))
    content))

(defn grid-data-cell [row [title value-fn display-fn]]
  (let [content (-> row value-fn display-fn)]
    [:td {:key title}
     [:div.td-container
      [:div.td-content {:title (cell-text content)  :style {:width "100%"}}
       content]
      [:div.td-spacer content]
      [:span (gstring/unescapeEntities "&nbsp;")]]]))

(defn grid-action-cell [row [key label handler unless hide?]]
  (let [key-str  (name key)
        disabled? (unless row)
        hidden?   (cond (= hide? true) disabled?
                        (fn? hide?)    (hide? row))]
    [:span {:style {:padding [0 2 2 0]}}
     [:button.btn.btn-xs {:class    key-str
                          :key      key-str
                          :on-click (partial handler row)
                          :disabled disabled?
                          :style    (if hidden? {:display "none"})}
      label]]))

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
  [grid-state prev? next? & [page-num total-pages]]
  (let [handler (partial handle-paging grid-state)]
    [:div.grid-controls {:key "controls"}
     (spaced
      (grid-header-button :disabled (= 1 page-num)
                          :on-click (handler (constantly [:first]))
                          :label    [:span (glyphicon "step-backward") " First"])
      (grid-header-button :disabled (not prev?)
                          :on-click (handler (comp page/serialize page/prev page/parse))
                          :label    [:span (glyphicon "chevron-left") " Prev"])

      page-num (if total-pages " / ") total-pages

      (grid-header-button :disabled (not next?)
                          :on-click (handler (comp page/serialize page/next page/parse))
                          :label    [:span "Next " (glyphicon "chevron-right")])
      (grid-header-button :disabled (= page-num total-pages)
                          :on-click (handler (constantly [:last]))
                          :label    [:span "Last " (glyphicon "step-forward")]))]))

(def dont-kill-dom-max 1000)

(defn grid-row [row owner [row-id headers actions col-width]]
  (om/component
   (html
    [:tr {:key (row-id row)}
     (list
      (map (partial grid-data-cell row) headers)
      (if (seq actions)
        [:td {:key "actions"
              :style {:width col-width, :text-align "center"}}
         (map (partial grid-action-cell row) actions)]))])))

(defn draw-grid [grid-state config headers actions rows
                 cursor cursor* owner
                 & [{:keys [idx current-page total-pages prev? next? total until limit]
                     :as pagination}
                    {:keys [no-pagination footer no-stripes]
                     :as opts}]]
  ;; Work around for bad data or config -
  ;; Hash actual data to ensure unique React id.
  (let [dupes?    (some #(< 1 %) (map count (vals (group-by row-id rows))))
        row-id    (if dupes? hash row-id)
        col-width (str (/ 100 (+ (count headers) (if (seq actions) 1 0))) "%")
        pending? (:pending? config)]
    (html
     [:div {:style {:opacity (if pending? 0.7 1)
                    :pointer-events (if pending? "none" "")}}
      [:div.row
       ;; top nav
       (if-not no-pagination
         [:div
          [:div.col-sm-12
           (grid-controls grid-state prev? next? current-page total-pages)
           [:small.text-muted "Showing " idx " to " until " of " total]]])]
      ;; grid proper
      [:table.table {:class (if-not no-stripes "table-striped")}
       [:thead
        [:tr {:key "header"}
         (list (map (partial grid-header-cell config grid-state) headers)
               (when actions [:th {:key "actions"}]))]]
       [:tbody
        (if-not (seq rows)
          [:tr {:key "loading"}
           [:td {:col-span (inc (count headers))}
            (if pending? "Loading.." "No Data.")]]
          ;; grid contents
          (om/build-all grid-row rows {:key row-id
                                       :opts [row-id headers actions col-width]}))]
       (if footer
         [:tfoot
          (footer rows)])]
      ;; bottom nav
      (if-not no-pagination
        (grid-controls grid-state prev? next? current-page total-pages))])))

(defn- request-grid-data [owner grid-state data-cursor data-key api-opts]
  (apply api/grid
         (fn [data]
           ;; ignore non-expected data
           (when (= api-opts (om/get-state owner :api-opts))
             (when (om/get-state owner :pending?)
               (om/set-state! owner :pending? false))
             ;; FIXME when shifting sort-keys, round trip to fix cursor
             (when data
               (om/update! grid-state :page-cursor (:page-cursor data))
               (om/update! data-cursor data-key {:rows       (:data data)
                                                 :cursor     (:page-cursor data)
                                                 :pagination (:pagination data)}))))
         data-key
         api-opts))

(defn server-grid-view
  [[data-cursor {:keys [data-key filter headers actions no-pagination] :as config} grid-state] owner]
  (reify
    om/IDisplayName
    (display-name [_] "ServerBasedGridView")

    om/IWillMount
    (will-mount [_]
      (generic/start-tracking-server! owner))

    om/IWillUnmount
    (will-unmount [_]
      (generic/stop-tracking-server! owner))

    om/IRender
    (render [_]
      (let [data-key   (first data-key)
            grid-state (or grid-state config)
            cursor*    (:page-cursor grid-state)
            sort-keys  (vec (om/value (or (:sort-keys grid-state) (:sort-keys config))))
            limit      (or (:limit config) (if no-pagination dont-kill-dom-max) 10)
            api-opts   [sort-keys {:filter      (om/value filter)
                                   :page-cursor (om/value cursor*)
                                   :limit       (om/value limit)}]
            old-opts   (om/get-state owner :api-opts)
            async-data (get data-cursor data-key)]

        ;; if others have changed, just deprecate
        (when (and (not= api-opts old-opts)
                   ;; avoid re-request if last query required a cursor resolve
                   (not (and (not= :range (first (:page-cursor (second old-opts))))
                             (= :range (first (:page-cursor (second api-opts)))))))
          (generic/dirty! owner)
          (om/set-state! owner :pending? true)
          (om/set-state! owner :api-opts api-opts))

        ;; FIXME: during render is not the place to do this (perhaps IWillUpdate)
        (when (generic/dirty? owner)
          (generic/clean! owner)
          (request-grid-data owner grid-state data-cursor data-key api-opts))

        (when async-data
          (let [{:keys [cursor rows pagination]} async-data]
            (draw-grid grid-state (assoc config :pending? (om/get-state owner :pending?))
                       headers actions rows
                       cursor cursor* owner
                       pagination
                       {:footer        (:footer config)
                        :no-pagination no-pagination
                        :no-stripes    (:no-stripes config)})))))))


;; realm of danger

;; FIXME: this is now common code, perhaps move up to rampant
(defn paginate-client-side [rows sort-keys cursor limit]
  (let [[cursor* rows*] (page/paginate-and-sort cursor rows sort-keys limit)
        idx             (inc (first (utils/positions #{(first rows*)} (page/fast-sort sort-keys rows))))
        current-page    (inc (Math/ceil (/ (dec idx) limit)))
        total-pages     (+ current-page
                           (Math/ceil (/ (dec (count (drop-while
                                                      #(not= (last rows*) %)
                                                      (s/sort-by-keys sort-keys rows))))
                                         limit)))
        prev?           (> idx 1)
        next?           (< idx (inc (- (count rows) (count rows*))))
        total           (count rows)
        until           (+ idx (dec (count rows*)))]
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
  [[rows {:keys [headers actions no-pagination] :as config} grid-state] owner]
  (reify
    om/IDisplayName
    (display-name [_] "GridView")
    om/IRender
    (render [_]
      (let [grid-state           (or grid-state config)
            cursor*              (:page-cursor grid-state)
            sort-keys            (:sort-keys grid-state)
            limit                (or (:limit config) (if no-pagination dont-kill-dom-max) 5)
            {:keys [cursor
                    rows
                    pagination]} (paginate-client-side rows sort-keys cursor* limit)]
        (draw-grid grid-state config headers actions rows
                   cursor cursor* owner
                   pagination
                   {:footer        (:footer config)
                    :no-pagination no-pagination
                    :no-stripes    (:no-stripes config)})))))
