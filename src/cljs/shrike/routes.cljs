(ns shrike.routes
  (:require [secretary.core :as secretary :refer-macros [defroute]]
            [accountant.core :as accountant]
            [om.core :as om]
            [shrike.api.build :as build]
            [shrike.state :refer [app-state]]))

(defroute "/" {}
  (swap! app-state assoc :view "/")
  (js/console.log "Dashboard view"))

(defroute "/repos" {:as params}
  (swap! app-state assoc :view "/repos")
  (js/console.log (str "User: " (:id params))))

(defroute repo-dashboard "/gh/:username/:repo"
  {:keys [repo username] :as params}
  (build/get-build username repo)
  (swap! app-state assoc :view "repo-dashboard")
  (js/console.log "Loading main repo view page" repo username))

(defroute "/code" {:as params}
  (swap! app-state assoc :view "/code")
  (js/console.log (str "User: " (:id params))))

(secretary/dispatch! (.-pathname (.-location js/window)))

(accountant/configure-navigation!)
