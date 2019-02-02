(ns amplifytest.client
  (:require [fulcro.client :as fc]
            [amplifytest.ui.root :as root]
            [fulcro.client.network :as net]
            [fulcro.client.data-fetch :as df]
            ["aws-amplify" :refer [Auth]]
            ["aws-amplify-react" :refer [withAuthenticator AuthenticatorWrapper Authenticator SignUp SignIn]]
            ["/aws-exports.js" :default aws-exports]))

(defonce SPA (atom nil))

(def secure-root (withAuthenticator root/Root))

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
