(ns app.dev
  "A place to add preloads for developer tools!"
  (:require
   [helix.experimental.refresh :as r]
   [refx.alpha :as refx]))

;; inject-hook! needs to run on application start.
;; For ease, we run it at the top level.
;; This function adds the react-refresh runtime to the page
(r/inject-hook!)

;; shadow-cljs allows us to annotate a function name with `:dev/after-load`
;; to signal that it should be run after any code reload. We call the `refresh!`
;; function, which will tell react to refresh any components which have a
;; signature created by turning on the `:fast-refresh` feature flag.

;; The `:dev/after-load` metadata causes this function to be called
;; after shadow-cljs hot-reloads code. We force a UI update by clearing
;; the Reframe subscription cache.
(defn ^:dev/after-load refresh []
  (refx/clear-subscription-cache!)
  (r/refresh!))
