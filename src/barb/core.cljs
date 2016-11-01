(ns barb.core
  (:require [clojure.browser.repl :as repl]
            [cljs.spec :as s]
            [goog.string :as gstring]
            [goog.string.format]
            [goog.string.format]
            [clojure.test.check :as tc]
            [clojure.spec.test :as stest]
            [cljs.spec.impl.gen :as gen]))

;; (defonce conn
;;   (repl/connect "http://localhost:9000/repl"))

(enable-console-print!)

(def image-size 100)

(def individuals-to-breed-each-iteration 3)
(def individuals-to-start-with 3)
(def polygon-count 5)

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

(s/def ::x1 (s/and integer? #(< % image-size)))
(s/def ::x2 (s/and integer? #(< % image-size)))
(s/def ::x3 (s/and integer? #(< % image-size)))
(s/def ::y1 (s/and integer? #(< % image-size)))
(s/def ::y2 (s/and integer? #(< % image-size)))
(s/def ::y3 (s/and integer? #(< % image-size)))
(s/def ::r (s/and integer? #(<= 0 % 256)))
(s/def ::g (s/and integer? #(<= 0 % 256)))
(s/def ::b (s/and integer? #(<= 0 % 256)))
(s/def ::a (s/and float? #(<= 0 % 1)))
(s/def ::polygon (s/keys :req [::x1 ::y2 ::x2 ::y2 ::x3 ::y3 ::r ::g ::b ::a]))

(s/fdef generate-random-polygon :args (s/cat :x int?) :ret ::polygon)

;; (println (stest/check `generate-random-polygon))

(defn generate-random-individual [n]
  "An individual is a collection of polygons."
  (repeatedly n (generate-random-polygon)))

(s/fdef generate-random-individual
        :args (s/and (s/cat :x int?) #(> (:x %) 0))
        :ret (s/and coll? #(= (count %) x)))

(println (stest/check `generate-random-individual))

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

(defn individual->image-data [individual]
  "Take a vector of maps representing an individual and return a vector of rgba
  data."
  (let [canvas (.createElement js/document "canvas")]
    (set! (.-width canvas) image-size)
    (set! (.-height canvas) image-size)
    (let [context (.getContext canvas "2d")]
      (draw-individual-on-context individual context)
      (context->image-data context))))

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

(def number-of-individuals-to-breed 3)

(defn generate-individuals [n]
  (repeatedly n generate-random-individual))

(defn select-fittest [individuals-image-data reference-image-data]
  "Takes a collection of image data representing multiple individuals and the
  image data for the reference image. Returns the fittest N individuals as
  determined by calculate-fitness."
  (take
    number-of-individuals-to-breed
    (reverse
      (sort-by #(calculate-fitness reference-image-data %)
               individuals-image-data))))

(defn breed [mother father]
  "Takes a mother and a father that are both vectors of image data. Return a
  new individual whose image data is created by choosing values from mom and
  dad at random."
  (map
    #(if (> 0.5 (rand)) %1 %2)
    mother
    father))

(defn breed-generation [individuals-image-data]
  "Given a collection of image data for highly-fit individuals, breed new
  individuals, choosing parents at random."
  (for [x (range individuals-to-breed-each-iteration)]
    (let [mom (rand-nth individuals-image-data)
          dad (rand-nth (remove #{mom} individuals-image-data))]
      (breed mom dad))))

(defn write-image-data-to-context [image-data-to-write context]
  (let [clamped-array (js/Uint8ClampedArray. image-data-to-write)
        new-image-data (js/ImageData. clamped-array image-size image-size)]
    (.putImageData context new-image-data 0 0)))

(defn find-individual-context []
  (let [canvas (.getElementById js/document "individual-canvas")
        context (.getContext canvas "2d")]
    context))

(defn run []
  (println "Starting")
  (let [reference-image-data (reference-image->image-data)
        individuals (generate-individuals individuals-to-start-with)
        individuals-image-data (map individual->image-data individuals)
        context (find-individual-context)]
    (loop [x 50
           population individuals-image-data]
      (when (> x 0)
        (let [fittest (select-fittest population reference-image-data)
              new-generation (breed-generation fittest)]
          (write-image-data-to-context (first fittest) context)
          (recur (dec x) new-generation))))))

(.addEventListener
  js/window
  "DOMContentLoaded"
  (log "Done loading"))

(s/def ::image-data (s/and vector? #(= (count %) (* image-size image-size 4))))
