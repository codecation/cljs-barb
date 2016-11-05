(ns barb.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [clojure.browser.repl :as repl]
            [goog.string :as gstring]
            [goog.string.format]
            [cljs.spec :as s]
            [cljs.core.async :refer [chan close!]]
            [clojure.test.check :as tc]
            [clojure.spec.test :as stest]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]
            [cljs.spec.impl.gen :as gen]))

(enable-console-print!)

(def image-size 100)
(def max-iterations 30)
(def polygon-count 120)
(def mutation-chance 0.05)
(def mutation-delta 30)
(def mutation-float-delta 0.2)

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
  {
   ::x1 (rand-int image-size) ::y1 (rand-int image-size)
   ::x2 (rand-int image-size) ::y2 (rand-int image-size)
   ::x3 (rand-int image-size) ::y3 (rand-int image-size)
   ::r (rand-int 256) ::g (rand-int 256) ::b (rand-int 256) ::a (rand)
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
    (::r polygon)
    ","
    (::g polygon)
    ","
    (::b polygon)
    ","
    (gstring/format "%.2f" (::a polygon))
    ")"))

(defn draw-polygon [polygon context]
  "Draw a polygon on a context."
  (set! (.-fillStyle context) (polygon->rgba-string polygon))
  (.beginPath context)
  (.moveTo context (::x1 polygon) (::y1 polygon))
  (.lineTo context (::x2 polygon) (::y2 polygon))
  (.lineTo context (::x3 polygon) (::y3 polygon))
  (.closePath context)
  (.fill context))

(defn draw-individual-on-context [individual context]
  (doseq [polygon individual]
    (draw-polygon polygon context))
  context)

(defn make-context []
  "Returns an in-memory (not in the DOM) context"
  (let [canvas (.createElement js/document "canvas")]
    (set! (.-width canvas) image-size)
    (set! (.-height canvas) image-size)
    (let [context (.getContext canvas "2d")]
      context)))

(defn individual->image-data [individual]
  "Take a vector of maps representing an individual and return a vector of rgba
  data."
  (let [context (make-context)]
    (draw-individual-on-context individual context)
    (context->image-data context)))

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

(defn write-image-data-to-context [image-data-to-write context]
  (let [clamped-array (js/Uint8ClampedArray. image-data-to-write)
        new-image-data (js/ImageData. clamped-array image-size image-size)]
    (.putImageData context new-image-data 0 0)))

(defn find-individual-context []
  (let [canvas (.getElementById js/document "individual-canvas")
        context (.getContext canvas "2d")]
    context))

(defn maybe-mutate [x]
  "Returns x or an x that is larger or smaller by mutation-delta."
  (if (> (rand) (- 1 mutation-chance))
    (+ x (rand-nth (range (- mutation-delta)
                          mutation-delta)))
    x))

(defn maybe-mutate-float [x]
  "Returns x or an x that is larger or smaller by mutation-float-delta."
  (if (> (rand) (- 1 mutation-chance))
    (+ x (rand-nth (range (- mutation-float-delta) mutation-float-delta 0.05)))
    x))

(defn mutate-polygon [polygon]
  "Takes a polygon and mutates some of its attributes at random"
    (-> polygon
        (update ::x1 maybe-mutate)
        (update ::y1 maybe-mutate)
        (update ::x2 maybe-mutate)
        (update ::y2 maybe-mutate)
        (update ::x3 maybe-mutate)
        (update ::y3 maybe-mutate)
        (update ::r maybe-mutate)
        (update ::g maybe-mutate)
        (update ::b maybe-mutate)
        (update ::a maybe-mutate-float)))

;; (stest/instrument)

(defn run []
  (println "Starting")
  (let [reference-image-data (reference-image->image-data)
        individual (generate-random-individual)
        individual-image-data (individual->image-data individual)
        context (find-individual-context)]
    (go-loop [x max-iterations
              best-yet-individual individual
              best-yet-image-data individual-image-data
              candidate-individual individual
              candidate-image-data individual-image-data]
             (println "it: " x)
             (when (> x 0)
               (write-image-data-to-context best-yet-image-data context)
               (<! (timeout 0))
               (if (> (calculate-fitness reference-image-data candidate-image-data)
                      (calculate-fitness reference-image-data best-yet-image-data))
                 (let [new-candidate (map mutate-polygon candidate-individual)
                new-image-data (individual->image-data new-candidate)]
            (recur (dec x)
                   candidate-individual
                   candidate-image-data
                   new-candidate
                   new-image-data))
          (let [new-candidate (map mutate-polygon best-yet-individual)
                new-image-data (individual->image-data new-candidate)]
            (recur (dec x)
                   best-yet-individual
                   best-yet-image-data
                   new-candidate
                   new-image-data)))))))

(.addEventListener
  js/window
  "DOMContentLoaded"
  (let [individual (generate-random-individual)
        context (find-individual-context)]
    (run)))
