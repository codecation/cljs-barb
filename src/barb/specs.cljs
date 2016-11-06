(ns barb.specs
  (:require [barb.core :as b]
            [cljs.spec :as s]
            [clojure.test.check :as tc]
            [clojure.spec.test :as stest]
            [cljs.spec.impl.gen :as gen]))

(s/def ::x-y-coordinate (s/and integer? #(< % b/image-size)))
(s/def ::x1 ::x-y-coordinate)
(s/def ::x2 ::x-y-coordinate)
(s/def ::x3 ::x-y-coordinate)
(s/def ::y1 ::x-y-coordinate)
(s/def ::y2 ::x-y-coordinate)
(s/def ::y3 ::x-y-coordinate)
(s/def ::rgb-val (s/and integer? #(<= 0 % 256)))
(s/def ::r ::rgb-val)
(s/def ::g ::rgb-val)
(s/def ::b ::rgb-val)
(s/def ::a (s/and float? #(<= 0 % 1)))
(s/def ::polygon (s/keys :req [::x1 ::y2 ::x2 ::y2 ::x3 ::y3 ::r ::g ::b ::a]))

(s/def ::individual (s/coll-of ::polygon))

(s/def ::image-data
  (s/and vector? #(= (count %) (* b/image-size b/image-size 4))))

(s/fdef b/generate-random-polygon :ret ::polygon)

(s/fdef b/generate-random-individual
        :args (s/cat :x int?)
        :ret ::individual)

(s/fdef b/calculate-fitness
        :args (s/cat :reference ::image-data :individual ::image-data)
        :ret (s/and float? #(< 0 % 1)))

(s/fdef b/polygon->rgba-string
        :args (s/cat :poly ::polygon)
        :ret string?)

(s/fdef b/mutate-polygon
        :args (s/cat :poly ::polygon)
        :ret ::polygon)
