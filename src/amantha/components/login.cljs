(ns amantha.components.login
  (:require [amantha.utils.dom :refer [e->value]))

(defn set-state! [a key val]
  (swap! a assoc key val))

(defn bind-input [state key [tag attrs]]
  [tag (assoc attrs :on-change #(set-state! state key (e->value %)))])

(defn login-view [state]
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
      {:role "form", :on-submit #(prn :login %)}
      [:div.input-group
       {:style {:margin-bottom "25px"}}
       [:span.input-group-addon [:i.glyphicon.glyphicon-user]]
       (bind-input state :username
                   [:input#login-username.form-control
                    {:placeholder "username",
                     :value       (get @state :username)
                     :on-change   #(set-state! state :username (e->value %))
                     :name        "username",
                     :type        "text"}])]
      [:div.input-group
       {:style {:margin-bottom "25px"}}
       [:span.input-group-addon [:i.glyphicon.glyphicon-lock]]
       (bind-input state :password
                   [:input#login-password.form-control
                    {:placeholder "password",
                     :value       (get @state :password)
                     :on-change   #(set-state! state :password (e->value %))
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
            "Sign Up Here"]]]]]]]])
