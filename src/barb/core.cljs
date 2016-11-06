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
(def max-iterations 100)
(def polygon-count 120)
(def mutation-chance 0.10)
(def mutation-delta 40)
(def mutation-float-delta 0.3)

(defn log [x]
  (.log js/console x))

(defn context->image-data [context]
  "Takes a js context and returns a vector of rgba image data"
  (array-seq
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
  reference image and individual-image-data. Returns the sum of squares
  difference between the two, which represents how similar the two images are."
  (let [maximum-difference (* (count reference-image-data)
                              (* 256 256))]
    (loop [r-data reference-image-data
           i-data individual-image-data
           sum 0]
      (if (empty? r-data)
        (- 1 (/ sum maximum-difference))
        (let [diff (- (first r-data) (first i-data))
              square (* diff diff)]
          (recur (rest r-data)
                 (rest i-data)
                 (+ sum square)))))))

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

(def state (atom {}))

(defn update-state []
  (let [{reference-image-data ::reference-image-data
         best-yet-individual ::best-yet-individual
         best-yet-image-data ::best-yet-image-data
         best-yet-fitness ::best-yet-fitness
         candidate-individual ::candidate-individual
         candidate-image-data ::candidate-image-data
         context ::context} @state
        candidate-fitness (calculate-fitness reference-image-data candidate-image-data)]
    (if (> candidate-fitness best-yet-fitness)
        (let [new-candidate (map mutate-polygon candidate-individual)
              new-image-data (individual->image-data new-candidate)]
          (swap! state assoc
                ::best-yet-individual candidate-individual
                ::best-yet-image-data candidate-image-data
                ::best-yet-fitness candidate-fitness
                ::candidate-individual new-candidate
                ::candidate-image-data new-image-data))
        (let [new-candidate (map mutate-polygon best-yet-individual)
              new-image-data (individual->image-data new-candidate)]
          (swap! state assoc
                ::candidate-individual new-candidate
                ::candidate-image-data new-image-data)))))

(defn setup []
  "Sets up the initial state atom"
  (let [individual (generate-random-individual)
        individual-image-data (individual->image-data individual)
        reference-image-data (reference-image->image-data)]
    (reset! state
            {
             ::reference-image-data (reference-image->image-data)
             ::individual (generate-random-individual)
             ::individual-image-data (individual->image-data individual)
             ::context (find-individual-context)
             ::best-yet-individual individual
             ::best-yet-image-data individual-image-data
             ::best-yet-fitness (calculate-fitness reference-image-data individual-image-data)
             ::candidate-individual individual
             ::candidate-image-data individual-image-data
             })))


(defn randoms []
  (take 40000 (repeatedly #(rand-int 255))))

(defn recursive-update []
  "Updates state, updates the canvas, and calls itself again."
  (update-state)
  (println (::best-yet-fitness @state))
  (write-image-data-to-context
    (::candidate-image-data @state)
    (find-individual-context))
  (.requestAnimationFrame js/window recursive-update))

(.addEventListener
  js/window
  "DOMContentLoaded"
  (do
    (setup)
    (.requestAnimationFrame js/window recursive-update)))
