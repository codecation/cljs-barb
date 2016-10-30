(ns barb.core
  (:require [clojure.browser.repl :as repl]
            [cljs.spec :as s]))

(defonce conn
  (repl/connect "http://localhost:9000/repl"))

(def image-size 100)
(def population-count 50)

(enable-console-print!)

(defn log [x]
  (.log js/console x))

(defn read-reference-image-as-rgb []
  (let [canvas (.getElementById js/document "reference-canvas")
        image (.getElementById js/document "reference")
        context (.getContext canvas "2d")]
    (set! (.-width canvas) image-size)
    (set! (.-height canvas) image-size)
    (.drawImage context image 0 0)
    (.-data 
      (.getImageData context 0 0 image-size image-size))))

(defn generate-random-individual []
  ;;   individuals are maps
  ;;   x1 y1 x2 y2 x3 y3 r g b a
  ;;   [0 0 0 0 0 0 128 128 128 128]
  ;;  TODO: spec for this
  { 
   :x1 (rand-int 256)
   :y1 (rand-int 256)
   :x2 (rand-int 256)
   :y2 (rand-int 256)
   :x3 (rand-int 256)
   :y3 (rand-int 256)
   :r (rand-int 256)
   :g (rand-int 256)
   :b (rand-int 256)
   :a (rand-int 256) 
   })

(defn run []
  (println "Running")
  (let [image-data (read-reference-image-as-rgb)]
    (println (doall (repeatedly population-count generate-random-individual)))
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
