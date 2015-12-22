(ns amantha.flash)

;; re-frame

(defn set-flash! [cursor message])

(defn clear-flash! [cursor])

;; move from bootstrap to material design

(defn render [{:keys [message type] :as flash}]
  (if flash
    [:div.alert {:class (str "alert-" type)}
     message
     [:div.pull-right
      [:button.btn.btn-info.btn-xs
       {:on-click #(clear-flash! nil)}
       [:i.glyphicon.glyphicon-remove]]]]))
