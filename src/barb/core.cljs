(ns barb.core
  (:require [clojure.browser.repl :as repl]
            [cljs.spec :as s]))

(defonce conn
  (repl/connect "http://localhost:9000/repl"))

(def image-size 100)
(def population-count 50)
(def polygon-count 12)

(enable-console-print!)

(defn log [x]
  (.log js/console x))

(defn reference-image->image-data []
  (let [canvas (.getElementById js/document "reference-canvas")
        image (.getElementById js/document "reference")
        context (.getContext canvas "2d")]
    (set! (.-width canvas) image-size)
    (set! (.-height canvas) image-size)
    (.drawImage context image 0 0)
    (.from js/Array
      (.-data
        (.getImageData context 0 0 image-size image-size)))))

(defn generate-random-polygon []
  ;;  TODO: spec for this
  { 
   :x1 (rand-int 256) :y1 (rand-int 256)
   :x2 (rand-int 256) :y2 (rand-int 256)
   :x3 (rand-int 256) :y3 (rand-int 256)
   :r (rand-int 256) :g (rand-int 256) :b (rand-int 256) :a (rand-int 256) 
   })

(defn generate-random-individual []
  "An individual is a collection of polygons"
  (repeatedly polygon-count generate-random-polygon))

(defn individual->image-data
  "Take a vector of maps representing an individual and return a vector of rgba
  data."
  [individual]
  ;; make a canvas -> context
  ;; draw each polygon on context (lines and colors and alpha)
  nil
  )


(defn run []
  (println "Running")
  (let [image-data (reference-image->image-data)]
    (println "Done")))

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
