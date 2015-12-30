(ns amantha.core
  (:require [amantha.components.filters :as filters]
            [amantha.components.generic :refer [toggle-panel]]
            [amantha.components.grid :as grid]
            [amantha.google :as goog]
            [amantha.utils :as utils]))

(enable-console-print!)

(defn main-view []
  (let [app-state (atom nil)
        grid-state (atom nil)]
    [:div
     (toggle-panel
       app-state "Filters" :hide-filters
       (filters/grouped-filters (get-in app-state :filters)))
     (grid/grid-view [{:item 1}] {:config :stuff} grid-state)]))

(defn- ensure-hash [url]
  (if (= \# (first url))
    (apply str "/#" (rest url))
    (str "/#" url)))

(defn sections-nav
  "The actual navigation bar showing the sections the user may navigate to"
  [sections user app]
  [:ul.nav.navbar-nav
   (when user
     (for [[section-key route] sections]
       [:li {:key section-key, :class (when (= (:nav app) section-key) "active")}
        [:a {:href (ensure-hash route)}
         (utils/titlecase section-key)]]))])

(defn user-widget
  "Dropdown menu and sign-out button for the user"
  [user state]
  (into [:ul.nav.navbar-nav.navbar-right.header-links]
        (when user
          (list
            [:li.dropdown
             [:a.dropdown-toggle {:href "#" :data-toggle "dropdown" :role "button" :aria-expanded false}
              (:name user) " " [:span.caret]]

             [:ul.dropdown-menu {:role "menu"}
              [:li [:a {:href "#" :on-click #(goog/sign-out state)}
                    [:i.glyphicon.glyphicon-lock] " "
                    "Sign Out"]]]]
            [:li [:p.navbar-btn
                  [:button.btn.btn-info
                   {:on-click #(goog/sign-out state)}
                   "Sign Out"]]]))))


(defn navbar [state sections]
  (let [user (:current-user @state)]
    [:nav.navbar.navbar-default {:role "navigation"}
     [:div.container
      [:div.navbar-header
       [:a.navbar-brand {:href "#"} "iFix"]
       [:div.navbar-toggle.collapsed {:type "button" :data-toggle "collapse" :data-target "#navbar-main"}
        [:span.sr-only "Toggle navigation"]
        [:span.icon-bar]
        [:span.icon-bar]
        [:span.icon-bar]]]
      [:div.navbar-collapse.collapse {:id "navbar-main"}
       (sections-nav sections user state)
       (user-widget user state)]]]))

