(ns barb.core
  (:require [clojure.browser.repl :as repl]))

;; (defonce conn
;;   (repl/connect "http://localhost:9000/repl"))

(def image-size 100)

(enable-console-print!)

;; (defn make-context []
;;   "Creates the native js context object"
;;   (let [canvas (.getElementById js/document "#canvas")
;;         context (.getContext canvas "2d")]
;;     (set! (.-width canvas) 100)
;;     (set! (.-height canvas) 100)
;;     (set! (.-fillStyle context) "rgb(0, 0, 0)")
;;     (set! (.-lineWidth context) wall-width-in-pixels)
;;     context))
;;

(defn read-reference-image-as-rgb []
  (let [canvas (.getElementById js/document "reference-canvas")
        image (.getElementById js/document "reference")
        context (.getContext canvas "2d")]
    (set! (.-width canvas) image-size)
    (set! (.-height canvas) image-size)
    (.drawImage context image 0 0)
    (.getImageData context 0 0 image-size image-size)))

(defn run []
  (println "Running")
  (let [image-data (read-reference-image-as-rgb)]
    (.log js/console image-data)))

(.addEventListener
  js/window
  "DOMContentLoaded"
  (run))

;; algo:
;; read reference image -> turn into imagedata
;; generate a population of candidates, randomly 
;; sort by fitness
;; keep top N
;; breed those N
;; mutate a little
;; repeat
