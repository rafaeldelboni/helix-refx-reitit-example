(ns app.core
  (:require ["react-dom/client" :as rdom]
            [app.lib :refer [defnc]]
            [helix.core :refer [$]]
            [helix.dom :as d]
            [reitit.core :as r]
            [reitit.coercion.spec :as rss]
            [reitit.frontend :as rf]
            ;[reitit.frontend.controllers :as rfc]
            [reitit.frontend.easy :as rfe]))

;;; Views ;;;

(defnc home-page []
  (d/div
   (d/h1 "This is home page")
   (d/button
    ;; Dispatch navigate event that triggers a (side)effect.
    ;{:on-click #(re-frame/dispatch [::push-state ::sub-page2])}
    {:on-click #(js/console.log [::push-state ::sub-page2])}
    "Go to sub-page 2")))

(defnc sub-page1 []
  (d/div
   (d/h1 "This is sub-page 1")))

(defnc sub-page2 []
  (d/div
   (d/h1 "This is sub-page 2")))

;;; Routes ;;;

(defn href
  "Return relative url for given route. Url can be used in HTML links."
  ([k]
   (href k nil nil))
  ([k params]
   (href k params nil))
  ([k params query]
   (rfe/href k params query)))

(def routes
  ["/"
   [""
    {:name      ::home
     :view      home-page
     :link-text "Home"
     :controllers
     [{;; Do whatever initialization needed for home page
       ;; I.e (re-frame/dispatch [::events/load-something-with-ajax])
       :start (fn [& _params] (js/console.log "Entering home page"))
       ;; Teardown can be done here.
       :stop  (fn [& _params] (js/console.log "Leaving home page"))}]}]
   ["sub-page1"
    {:name      ::sub-page1
     :view      sub-page1
     :link-text "Sub page 1"
     :controllers
     [{:start (fn [& _params] (js/console.log "Entering sub-page 1"))
       :stop  (fn [& _params] (js/console.log "Leaving sub-page 1"))}]}]
   ["sub-page2"
    {:name      ::sub-page2
     :view      sub-page2
     :link-text "Sub-page 2"
     :controllers
     [{:start (fn [& _params] (js/console.log "Entering sub-page 2"))
       :stop  (fn [& _params] (js/console.log "Leaving sub-page 2"))}]}]])

(defn on-navigate [new-match]
  (when new-match
    ;(re-frame/dispatch [::navigated new-match])))
    (js/console.log [::navigated new-match])))

(def router
  (rf/router
   routes
   {:data {:coercion rss/coercion}}))

(defn init-routes! []
  (js/console.log "initializing routes")
  (rfe/start!
   router
   on-navigate
   {:use-fragment true}))

(defnc nav [{:keys [router current-route]}]
  (d/ul
   (for [route-name (r/route-names router)
         :let       [route (r/match-by-name router route-name)
                     text (-> route :data :link-text)]]
     (d/li {:key route-name}
           (when (= route-name (-> current-route :data :name))
             "> ")
           ;; Create a normal links that user can click
           (d/a {:href (href route-name)} text)))))

(defnc router-component [{:keys [router]}]
  ;(let [current-route @(re-frame/subscribe [::current-route])]
  (let [current-route {:data {:view home-page}}]
    (d/div
     ($ nav {:router router :current-route current-route})
     (when current-route
       (-> current-route :data :view $)))))

;;; Setup ;;;

(def debug? ^boolean goog.DEBUG)

(defn dev-setup []
  (when debug?
    (enable-console-print!)
    (println "dev mode")))

(defn ^:export init []
  (dev-setup)
  (init-routes!)
  (doto (rdom/createRoot (js/document.getElementById "app"))
    (.render ($ router-component {:router router})))
  (println "Hello Helix!"))
