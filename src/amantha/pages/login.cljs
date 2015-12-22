(ns amantha.pages.login
  (:require [amantha.utils :as utils :refer [e->value]]
            [om.core :as om :include-macros true]
            [sablono.core :refer-macros [html]]))

(defn bind-input [owner key [tag attrs]]
  [tag (assoc attrs
              :on-change #(om/set-state! owner key (utils/e->value %)))])

(defn login-handler [app owner]
  (fn []
    (let [{:keys [username password]} (om/get-state owner)]
      (om/update! app :current-user username)
      false)))

(defn login-view [app owner]
  (om/component
   (html
    [:div#loginbox.mainbox.col-md-6.col-md-offset-3.col-sm-8.col-sm-offset-2
     {:style {:margin-top "50px"}}
     [:div.panel.panel-info
      [:div.panel-heading
       [:div.panel-title "Sign In"]
       #_[:div
          {:style {:float "right" :font-size "80%" :position "relative" :top "-10px"}}
          [:a {:href "#"} "Forgot password?"]]]
      [:div.panel-body
       {:style {:padding-top "30px"}}
       [:div#login-alert.alert.alert-danger.col-sm-12
        {:style {:display "none"}}]
       [:form#loginform.form-horizontal
        {:role "form", :on-submit (login-handler app owner)}
        [:div.input-group
         {:style {:margin-bottom "25px"}}
         [:span.input-group-addon [:i.glyphicon.glyphicon-user]]
         (bind-input owner :username
                     [:input#login-username.form-control
                      {:placeholder "username",
                       :value      (om/get-state owner :username)
                       :on-change #(om/set-state! owner :username (e->value %))
                       :name        "username",
                       :type        "text"}])]
        [:div.input-group
         {:style {:margin-bottom "25px"}}
         [:span.input-group-addon [:i.glyphicon.glyphicon-lock]]
         (bind-input owner :password
                     [:input#login-password.form-control
                      {:placeholder "password",
                       :value      (om/get-state owner :password)
                       :on-change #(om/set-state! owner :password (e->value %))
                       :name        "password",
                       :type        "password"}])]
        #_[:div.input-group
           [:div.checkbox
            [:label
             [:input#login-remember
              {:value "1", :name "remember", :type "checkbox"}]
             " Remember me"]]]
        [:div.form-group
         {:style {:margin-top "10px"}}
         [:div.col-sm-12.controls
          [:button#btn-login.btn.btn-success "Login"]
          "  "
          #_[:a#btn-fblogin.btn.btn-primary
             {:href "#"}
             "Login with Facebook"]]]
        #_[:div.form-group
           [:div.col-md-12.control
            [:div
             {:style {:border-top "1px solid #888" :padding-top "15px" :font-size "85%"}}
             "Don't have an account!"
             [:a
              {:onclick "$('#loginbox').hide(); $('#signupbox').show()",
               :href    "#"}
              "Sign Up Here"]]]]]]]])))

