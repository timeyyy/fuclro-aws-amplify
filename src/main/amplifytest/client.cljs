(ns amplifytest.client
  (:require [fulcro.client :as fc]
            [amplifytest.ui.root :as root]
            [fulcro.client.network :as net]
            [fulcro.client.localized-dom :as dom]
            [fulcro.client.dom :as c-dom]
            [fulcro.client.data-fetch :as df]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.util :refer [force-children]]
            ["aws-amplify" :refer [Auth]]
            ["aws-amplify-react" :refer [withAuthenticator AuthenticatorWrapper Authenticator SignUp SignIn]]
            ["/aws-exports.js" :default aws-exports]))

(defn component-factory-simple
  "Make a factory to build a React instance from a React class."
  [component]
  (fn
    ([] (dom/create-element component))
    ([props] (dom/create-element component (clj->js props)))
    ([props & children] (apply dom/create-element component (clj->js props) (force-children children)))))


(def ui-auth (component-factory-simple Authenticator))
; (def ui-auth (component-factory-simple Authenticator))
(def ui-root (prim/factory root/Root))

(defsc AppWithAuth
  [this props]
  (ui-auth #js {}
           (ui-root)))

(defonce SPA (atom nil))

(def secure-root AppWithAuth)
; (def secure-root (withAuthenticator root/Root))

(defn mount []
  (.configure Auth (clj->js aws-exports))
  (reset! SPA (fc/mount @SPA secure-root "app")))

(defn start []
  (mount))

(def secured-request-middleware
  ;; The CSRF token is embedded via server_components/html.clj
  (->
    (net/wrap-csrf-token (or js/fulcro_network_csrf_token "TOKEN-NOT-IN-HTML!"))
    (net/wrap-fulcro-request)))

(defn ^:export init []
  (reset! SPA (fc/new-fulcro-client
                :started-callback (fn [amplifytest]
                                    (df/load amplifytest :all-users root/User))
                ;; This ensures your client can talk to a CSRF-protected server.
                ;; See middleware.clj to see how the token is embedded into the HTML
                :networking {:remote (net/fulcro-http-remote
                                       {:url                "/api"
                                        :request-middleware secured-request-middleware})}))
  (start))
