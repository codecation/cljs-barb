(ns barb.core
  (:require [clojure.browser.repl :as repl]
            [cljs.spec :as s]
            [goog.string :as gstring]
            [goog.string.format]
            ))

(defonce conn
  (repl/connect "http://localhost:9000/repl"))

(def image-size 100)
(def population-count 50)
(def polygon-count 50)

(enable-console-print!)

(defn log [x]
  (.log js/console x))

(defn context->image-data [context]
  "Takes a js context and returns a vector of rgba image data"
  (.from js/Array
         (.-data
           (.getImageData context 0 0 image-size image-size))))

(defn reference-image->image-data []
  (let [canvas (.getElementById js/document "reference-canvas")
        image (.getElementById js/document "reference")
        context (.getContext canvas "2d")]
    (.drawImage context image 0 0)
    (context->image-data context)))

(defn generate-random-polygon []
  ;;  TODO: spec for this
  {
   :x1 (rand-int image-size) :y1 (rand-int image-size)
   :x2 (rand-int image-size) :y2 (rand-int image-size)
   :x3 (rand-int image-size) :y3 (rand-int image-size)
   :r (rand-int 256) :g (rand-int 256) :b (rand-int 256) :a (rand)
   })

(defn generate-random-individual []
  "An individual is a collection of polygons."
  (repeatedly polygon-count generate-random-polygon))

(defn alpha-int->float [i]
  "Take an int representing the alpha (0-255), scale to 0.0-1.0."
  (/ i 255.0))

(defn polygon->rgba-string [polygon]
  "Take a map representing a polygon, return a string of 'rgba(r, g, b, a)',
  where r, g, b are ints 0-255, a is a float 0-1."
  (str
    "rgba("
    (:r polygon)
    ","
    (:g polygon)
    ","
    (:b polygon)
    ","
    (gstring/format "%.2f" (:a polygon))
    ")"))

(defn draw-polygon [polygon context]
  "Draw a polygon on a context."
  (set! (.-fillStyle context) (polygon->rgba-string polygon))
  (.beginPath context)
  (.moveTo context (:x1 polygon) (:y1 polygon))
  (.lineTo context (:x2 polygon) (:y2 polygon))
  (.lineTo context (:x3 polygon) (:y3 polygon))
  (.closePath context)
  (.fill context))

(defn draw-individual-on-context [individual context]
  (doseq [polygon individual]
    (draw-polygon polygon context))
  context)

(defn individual->image-data
  "Take a vector of maps representing an individual and return a vector of rgba
  data."
  [individual]
  (let [canvas (.getElementById js/document "individual-canvas")
        context (.getContext canvas "2d")]
    (draw-individual-on-context individual context)
    (context->image-data context)))

(defn find-individual-context []
  (-> (.getElementById js/document "individual-canvas")
      (.getContext "2d")))

(defn calculate-fitness [reference-image-data individual-image-data]
  "Takes two vectors of ints 0-255, representing the rgba data for our
  reference image and individual-image-data, returns the sum of squares
  difference between the two, which represents how similar the two images are."
  (let [differences (map - reference-image-data individual-image-data)
        squares (map #(* % %) differences)
        sum-of-squares (reduce + squares)
        maximum-difference (* (count reference-image-data)
                              (* 256 256))]
    (- 1 (/ sum-of-squares maximum-difference))))

(defn run []
  (println "Running")
  (let [reference-image-data (reference-image->image-data)
        individual-image-data (context->image-data
                                (draw-individual-on-context
                                  (generate-random-individual)
                                  (find-individual-context)))]
    (println (calculate-fitness reference-image-data individual-image-data))
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
