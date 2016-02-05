(ns amantha.components.theme
  (:require
    [amantha.components.basic :refer [glyphicon right-glyphicon]]
    #?@(:cljs
        [[goog.string :as gstring]
         [goog.string.format]])))

(defn- spaced [& children]
  (interpose " " children))

(defn- cell-text [content]
  (if (vector? content) (cell-text (last content)) content))

(def nbsp #?(:clj "&nbsp;" :cljs (gstring/unescapeEntities "&nbsp;")))

;; headers

(defn sort-glyph [active? up?]
  (if-not active?
    (right-glyphicon :chevron-up {:style {:opacity 0}})
    (if up?
      (right-glyphicon :chevron-down)
      (right-glyphicon :chevron-up))))

(defn header-cell [{:keys [width sort-fn]} title glyph]
  (if sort-fn
    [:th.clickable {:key title :width width :on-click sort-fn} title glyph]
    [:th.unsortable {:key title} title]))

(defn headers [header-cells actions? filter-cells]
  [[:tr {:key "header"}
    (seq (conj (vec header-cells)
               (when actions? [:th {:key "actions"}])))]
   (when filter-cells
     [:tr {:key "filter"} filter-cells])])

;; content

(defn data-cell [title content]
  [:td {:key title}
   [:div.td-container
    [:div.td-content {:title (cell-text content) :style {:width "100%"}}
     content]
    [:div.td-spacer content]
    [:span nbsp]]])

(defn action-cell [{:keys [key disabled? hidden? action-fn]} label]
  (when-not hidden?
    ^{:key key}
    [:span {:style {:padding [0 2 2 0]}}
     [:button.btn.btn-xs
      {:class key
       :key key
       :on-click action-fn
       :disabled disabled?}
      label]]))

;; grid buttons

(defn grid-btn [on-click label]
  [:button.btn.btn-xs.btn-default {:disabled (not on-click) :on-click on-click} label])

(defn grid-header-btn [type on-click]
  (grid-btn
    on-click
    (case type
      :first [:span (glyphicon "step-backward") " First"]
      :prev [:span (glyphicon "chevron-left") " Prev"]
      :next [:span "Next " (glyphicon "chevron-right")]
      :last [:span "Last " (glyphicon "step-forward")])))

(defn grid-header-btns [btn-first btn-prev btn-next btn-last page-num total-pages]
  (into
    [:div.grid-controls {:key "controls"}]
    (spaced btn-first btn-prev
            page-num (if total-pages " / ") total-pages
            btn-next btn-last)))

;; misc

(defn loading [pending? width]
  [:tr {:key "loading"}
   [:td {:col-span width}
    (if pending? "Loading.." "No Data.")]])
