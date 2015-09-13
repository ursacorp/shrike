(ns titan.middleware.auth
  (:require [cemerick.url :refer [map->query url url-encode]]
            [clojure.tools.logging :as log]
            [clout.core :as clout]
            [titan.http.response :as responses]
            [ring.util.request :as req]))

(defn matches-any-path?
  "Does this request match any of the paths?

   Expects paths to be a coll."
  [paths request]
  (some #(clout/route-matches % request) paths))

;; TODO: Clean this up. It could easily go into that function above.
(defn is-site-route?
  "Is this a blacklisted site route?"
  [request site-paths]
  (matches-any-path? site-paths request))

;; TODO: actually check this against the whitelisted routes
(defn route-whitelist-fn
  "A function for whitelisting particular routes.

  Whitelists our major resource routes first, then some API routes."
  [path]
  (cond
    (or
     (= path "/")
     (.startsWith path "/css")
     (.startsWith path "/fonts")
     (.startsWith path "/images")
     (.startsWith path "/js")
     (.startsWith path "/templates")

     (= path "/about")
     (= path "/api")
     (= path "/faq")
     (= path "/integrations")
     (= path "/password_reset")
     (= path "/pricing")
     (.startsWith path "/login")
     (.startsWith path "/new_password")
     (.startsWith path "/signup")

     (= path "/debug")
     (= path "/api/v1/email")
     (= path "/api/v1/email/send")
     (= path "/api/v1/login")
     (= path "/api/v1/user")
     (= path "/api/v1/gravatar")
     (= path "/api/v1/password_reset")
     (= path "/api/v1/new_password")) true
    :else false))

(defn darg-auth-fn
  "The Darg authentication function."
  [{:keys [session] :as request}]
  (if-let [id (:id session)]
    {:id id}))

;; TODO - this is definitely incomplete
(defn github-auth-fn
  "Verify that this Darg user actually has authn/authz with GitHub"
  [{:keys [user] :as request}]
  (log/warn user)
  request)

(defn redirect-to-signin
  [request]
  (let [root-target-url (req/path-info request)
        query-str (map->query (:query-params request))
        redirect-url (str root-target-url
                          (when query-str
                            (str "?" query-str)))]
    (responses/redirect
     (str "/login?redirect="
          (url-encode redirect-url)))))

(defn wrap-authentication
  "Wraps authentication for the handler. If a user is successfully authenticated,
  then a map of their id and email is assoc'd onto the :user key of the request
  map."
  [handler auth-fn & {:keys [site-paths
                             whitelist]
                      :or {site-paths []
                           whitelist #{}}}]
  (fn [request]
    (if (whitelist (req/path-info request))
      (handler request)
      (let [user (auth-fn request)]
        (if user
          (handler (assoc request :user user))
          (if (is-site-route? request site-paths)
            (redirect-to-signin request)
            (responses/unauthorized "User not authenticated.")))))))
