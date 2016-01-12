(ns amantha.components.basic
  (:require [amantha.utils :as u]))

(defn label [x] [:span.label.label-default x])

(defn labels
  ([options colors x]
   (let [idx (mod (first (u/positions #{x} options)) 7)]
     [:span.label.label-defaul {:style {:background-color (nth colors idx)}} x]))
  ([options x]
   (let [idx (inc (mod (first (u/positions #{x} options)) 7))]
     [:span.label.label-defaul {:class (str "bg-col" idx)} x])))

(defn link-to [url-fn x]
  [:a {:href (url-fn x)} x])

(defn right-align [content]
  [:div {:style {:width "100%" :text-align "right" :padding-right 10}}
   content])

(defn glyphicon [type & body]
  (apply vector
         (keyword (str "span.glyphicon.glyphicon-" (name type)))
         body))

(defn right-glyphicon [type & body]
  (apply vector
         (keyword (str "span.right.glyphicon.glyphicon-" (name type)))
         body))

(defn tick-view [tick?]
  (glyphicon (if tick? :ok :remove)))

(defn toggle-panel
  "Panel element with hide/show toggle."
  [state title hide-key content]
  (let [path [:preferences hide-key]
        show (not (get-in @state path))]
    [:div.panel.panel-default
     [:div.panel-heading.clickable
      {:on-click #(swap! state update-in path not)}
      [:div.panel-title (if show (glyphicon "minus") (glyphicon "plus")) " " title " "]]
     (when show content)]))

(def g glyphicon)

(def full-stars (take 5 (repeat (g "star"))))

(def empty-stars (take 5 (repeat (g "star-empty"))))

(defn points-render [num]
  (let [num   (u/coerce-long num)
        full  (take num full-stars)
        empty (take (- 5 num) empty-stars)
        stars (concat full empty)]
    (into [:span] (interpose " " stars))))

(defn g<- [text] [:span (g :arrow-left) " " text])
(defn g-x [text] [:span (g :ban-circle) " " text])
(defn g-> [text] [:span text " " (g :arrow-right)])

(defn align [type content] [:div {:style {:text-align type}} content])

