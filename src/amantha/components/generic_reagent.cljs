(ns amantha.components.generic-reagent
  (:require [amantha.utils :as utils]
            [bss.rampant.utils :refer [glyphicon]]
            [reagent.core :as reagent]
            [re-frame.core :refer [dispatch]]))

(defonce ^{:doc "Reactive atom holding current time. Accurate to 1s"}
  current-time
  (reagent/atom (js/Date.)))

(defn- update-current-time []
  (reset! current-time (js/Date.)))

(defonce update-current-time-interval
  (js/setInterval update-current-time (* 60 1000)))

(defn relative-time-view
  "Simple text widget that shows relative time in natural language."
  [time]
  @current-time ;; deref time to ensure rerender overy tick
  [:span (utils/relative-time time)])

(defn relative-time-cell
  "Component wrapper for using `relative-time-view` in Reagent data grids."
  [time]
  [:span
   [relative-time-view time]])

;; super-dumb style views

(def g glyphicon)

(defn g<- [text] [:span (g :arrow-left) " " text])
(defn g-x [text] [:span (g :ban-circle) " " text])
(defn g-> [text] [:span text " "(g :arrow-right)])

(defn align [type content] [:div {:style {:text-align type}} content])

(defn col-3 [content] [:div.col-md-4 content])

(defn col-3-l [content] (col-3 (align :left   content)))
(defn col-3-m [content] (col-3 (align :center content)))
(defn col-3-r [content] (col-3 (align :right  content)))

(defn col-2 [content] [:div.col-md-6 content])
(defn col-2-m [content] (col-2 (align :center content)))

(defn large-btn [type content & [action]]
  (let [class (str "btn-" (name type))
        click (if action #(dispatch action))]
    [:a.btn.btn-lg {:class class, :on-click click} content]))


;; invisible logic views

(defn dev-view [content]
  (when true ;; js/IS_DEV [showing dev components on staging for now]
    [:div content]))
