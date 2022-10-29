(ns app.login
  (:require ["react-dom/client" :as rdom]
            [app.lib :refer [defnc]]
            [clojure.pprint :refer [pprint]]
            [helix.core :refer [$]]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [refx.alpha :as refx]
            [refx.db :as db]
            [reitit.coercion.schema :as rsc]
            [reitit.frontend :as rf]
            [reitit.frontend.controllers :as rfc]
            [reitit.frontend.easy :as rfe]
            [schema.core :as s]))

;;; Effects ;;;

;; Triggering navigation from events.

(refx/reg-fx :push-state
             (fn [route]
               (apply rfe/push-state route)))

;;; Events ;;;

(refx/reg-event-db ::initialize-db
                   (fn [db _]
                     (if db
                       db
                       {:current-route nil
                        :current-user nil})))

(refx/reg-event-fx ::push-state
                   (fn [_ [_ & route]]
                     {:push-state route}))

(refx/reg-event-db ::navigated
                   (fn [db [_ new-match]]
                     (let [old-match   (:current-route db)
                           controllers (rfc/apply-controllers (:controllers old-match) new-match)]
                       (assoc db :current-route (assoc new-match :controllers controllers)))))

(refx/reg-event-fx
 ::login-done
 (fn
   [{db :db} [_ [_ response]]]
   {:db (-> db
            (assoc :login-loading? false)
            (assoc :current-user response))}))

(refx/reg-event-db
 ::login-error
 (fn
   [db _]
   (-> db
       (assoc :login-loading? false)
       (assoc :current-user nil))))

(refx/reg-fx
 :login
 (fn [{:keys [user on-success]} _]
   (js/setTimeout #(refx/dispatch (conj on-success user)) 500)))

(refx/reg-event-fx
 ::login
 (fn
   [{db :db} user]
    ;; we return a map of (side) effects
   {:login {:user user
            :on-success [::login-done]
            :on-failure [::login-error]}
    :db  (assoc db :login-loading? true)}))

(refx/reg-event-db
 ::logout
 (fn
   [db _]
   (-> db
       (assoc :current-user nil))))

;;; Subscriptions ;;;

(refx/reg-sub ::current-user
              (fn [db]
                (:current-user db)))

(refx/reg-sub ::current-route
              (fn [db]
                (:current-route db)))

(refx/reg-sub ::login-loading
              (fn [db]
                (:login-loading? db)))

;;; Views ;;;

(defnc home-page []
  (d/div
   (d/h2 "Welcome to frontend")
   (d/p "Look at console log for controller calls.")))

(defnc item-page [match]
  (let [{:keys [path query]} (:parameters match)
        {:keys [id]} path]
    (d/div
     (d/ul
      (d/li (d/a {:href (rfe/href ::item {:id 1})} "Item 1"))
      (d/li (d/a {:href (rfe/href ::item {:id 2} {:foo "bar"})} "Item 2")))
     (when id
       (d/h2 "Selected item " id))
     (when (:foo query)
       (d/p "Optional foo query param: " (:foo query))))))

(defnc login-view []
  (let [loading? (refx/use-sub [::login-loading])
        [state set-state] (hooks/use-state {:username "" :password ""})]
    (d/div
     (d/form
      {:disabled loading?
       :on-submit (fn [e]
                    (.preventDefault e)
                    (when (and (:username state)
                               (:password state))
                      (refx/dispatch [::login state])))}

      (d/label "Username")
      (d/input
       {:value (:username state)
        :disabled loading?
        :on-change #(set-state assoc :username (.. % -target -value))})

      (d/label "Password")
      (d/input
       {:value (:password state)
        :disabled loading?
        :on-change #(set-state assoc :password (.. % -target -value))})

      (d/button
       {:type "submit"}
       "Login")

      (when loading?
        (d/p "Loading..."))))))

(defnc about-page []
  (d/div
   (d/p "This view is public.")))

(defnc main-view []
  (let [{:keys [username] :as user} (refx/use-sub [::current-user])
        match (refx/use-sub [::current-route])
        route-data (:data match)]
    (d/div
     (d/ul
      (d/li (d/a {:href (rfe/href ::frontpage)} "Frontpage"))
      (d/li (d/a {:href (rfe/href ::about)} "About (public)"))
      (d/li (d/a {:href (rfe/href ::item-list)} "Item list"))
      (when user
        (d/div
         (d/li (d/a {:on-click (fn [e]
                                 (.preventDefault e)
                                 (refx/dispatch [::logout]))
                     :href "#"}
                    (str "Logout (" username ")"))))))
     ;; If user is authenticated
     ;; or if this route has been defined as public, else login view
     (if (or user (:public? route-data))
       (when-let [view (:view route-data)]
         ($ view {:match match}))
       ($ login-view))
     (d/pre (with-out-str (pprint @db/app-db))))))

;;; Routes ;;;

(defn log-fn [& params]
  (fn [_]
    (apply js/console.log params)))

(def routes
  (rf/router
   ["/"
    [""
     {:name ::frontpage
      :view home-page
      :controllers [{:start (log-fn "start" "frontpage controller")
                     :stop (log-fn "stop" "frontpage controller")}]}]

    ["about"
     {:name ::about
      :view about-page
      :public? true}]

    ["items"
      ;; Shared data for sub-routes
     {:view item-page
      :controllers [{:start (log-fn "start" "items controller")
                     :stop (log-fn "stop" "items controller")}]}

     [""
      {:name ::item-list
       :controllers [{:start (log-fn "start" "item-list controller")
                      :stop (log-fn "stop" "item-list controller")}]}]
     ["/:id"
      {:name ::item
       :parameters {:path {:id s/Int}
                    :query {(s/optional-key :foo) s/Keyword}}
       :controllers [{:identity (fn [match]
                                  (:path (:parameters match)))
                      :start (fn [parameters]
                               (js/console.log "start" "item controller" (:id parameters)))
                      :stop (fn [parameters]
                              (js/console.log "stop" "item controller" (:id parameters)))}]}]]]
   {:data {:controllers [{:start (log-fn "start" "root-controller")
                          :stop (log-fn "stop" "root controller")}]
           :coercion rsc/coercion
           :public? false}}))

(defn on-navigate [new-match]
  (when new-match
    (refx/dispatch [::navigated new-match])))

(defn init-routes! []
  (js/console.log "initializing routes")
  (rfe/start!
   routes
   on-navigate
   {:use-fragment true}))

;;; Setup ;;;

(def debug? ^boolean goog.DEBUG)

(defn dev-setup []
  (when debug?
    (enable-console-print!)
    (println "dev mode")))

(defn ^:export init []
  (refx/clear-subscription-cache!)
  (refx/dispatch-sync [::initialize-db])
  (dev-setup)
  (init-routes!)
  (doto (rdom/createRoot (js/document.getElementById "app"))
    (.render ($ main-view))))
