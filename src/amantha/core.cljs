(ns amantha.core
  (:require [amantha.components.charts :as charts :refer [custom-chart-view]]
            [amantha.components.filters :as filters]
            [amantha.components.generic :refer [toggle-panel]]
            [amantha.components.grid :as grid]
            [amantha.components.page-titles :refer [page-title]]
            [amantha.components.roll-up-selector :as roll-up]
            [amantha.config.repairs :as repairs]
            [amantha.data.filters]
            [amantha.data.permissions :as perms]
            [amantha.pages :refer [page-view]]
            [amantha.pages.commissions]
            [amantha.pages.customers]
            [amantha.pages.forecast]
            [amantha.pages.payments]
            [amantha.pages.repair]
            [amantha.pages.customer.quote]
            [amantha.pages.sales]
            [amantha.state :refer [app-state]]
            [amantha.utils :refer [reagent-bridge]]
            [bss.rampant.filters :refer [filter-data]]
            [bss.rampant.utils :as utils :refer [gen-id glyphicon right-glyphicon]]
            [om.core :as om :include-macros true]
            [re-frame.core :refer [subscribe]]))

(enable-console-print!)

;; mount
;;---------

(defn main-view [app owner]
  (reify
    om/IDisplayName
    (display-name [_] "App")
    om/IRender
    (render [_]
      (let [nav           (:nav app)
            conf          (get-in app [:grids nav])
            hist-key      (:hist-key conf)
            chart-key     (:chart-key conf)
            filter        (get-in app [:filters nav])
            ;; not mapping any data nested on serverside..
            data-key      (first (:data-key conf))]

        ;; hot reload grid config if changed
        (when-not (= conf (repairs/grids nav))
          (om/update! app [:grids nav] (repairs/grids nav)))
        (html
          [:div
           (om/build reagent-bridge nil {:opts {:component page-title}})
           (flash-message/current app)
           [:br]
           (if-not conf
             (om/build page-view app)
             ;; grid demo
             [:div
              (toggle-panel
                app "Filters" :hide-filters
                (filters/grouped-filters (get-in app [:filters nav])))

              (when hist-key
                (toggle-panel
                  app "Occurrences" :hide-chart
                  [:div.panel-body
                   (roll-up/chart-interval-selector owner)
                   (om/build charts/server-histogram-view
                             {:cursor (get-in app [:temp :histogram])
                              :options {:data-key data-key
                                        :hist-key hist-key
                                        :filter filter
                                        :roll-up (:interval (roll-up/get-interval owner))
                                        :x-axis hist-key
                                        :y-axis "Num. Occurences"}})]))

              (when chart-key
                (toggle-panel
                  app "Charts" :hide-chart
                  (om/build custom-chart-view {:cache     (:temp app)
                                               :chart-key chart-key
                                               :data-key  data-key
                                               :filter    filter})))

              (toggle-panel
                app "Data Grid" :hide-data-grid
                (om/build grid/server-grid-view [(get-in app [:temp :grid])
                                                 (assoc conf :filter filter)
                                                 (get-in app [:grid-state nav])])
                #_(om/build grid/grid-view [filtered-data
                                            conf
                                            (get-in app [:grid-state nav])]))])])))))


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
  [user app]
  (into [:ul.nav.navbar-nav.navbar-right.header-links]
        (when user
          (list
            [:li.dropdown
             [:a.dropdown-toggle {:href "#" :data-toggle "dropdown" :role "button" :aria-expanded false}
              (:name user) " " [:span.caret]]

             [:ul.dropdown-menu {:role "menu"}
              [:li [:a {:href "#" :on-click (goog/sign-out app)}
                    [:i.glyphicon.glyphicon-lock] " "
                    "Sign Out"]]]]
            [:li [:p.navbar-btn
                  [:button.btn.btn-info
                   {:on-click (goog/sign-out app)}
                   "Sign Out"]]]))))


(defn navbar [app sections]
  (let [user (:current-user app)]
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
       (sections-nav sections user app)
       (user-widget user app)]]]))


(defn app-view
  [app _]
  (om/component
    (let [user (:current-user app)]
      (html
        [:div
         [:header
          (navbar app (map explore-nav-item (perms/navbar-sections-for user)))]
         [:div.container
          (if user
            [:div
             (om/build main-view app)]
            [:div
             (goog/sign-in-view)
             #_[:div (om/build login/login-view app)]])
          #_[:p (om/build ankha/inspector app)]]]))))


(defn ^:export main []
  (om/root app-view app-state
           {:target (. js/document (getElementById "app"))
            :tx-listen tx-listen}))

(defn safe-sign-in []
  (if (and js/gapi js/gapi.auth)
    (or (goog/get-token)
        (goog/sign-in))
    (js/setTimeout safe-sign-in 100)))

;; Simulate signed-in Technician
(swap! app-state assoc :current-user {:name "Alex F."
                                      :email "alex@ifix.co.za"
                                      :sales-profile-id false})

(routing/start!)
