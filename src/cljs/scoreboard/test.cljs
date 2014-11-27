(ns scoreboard.test
  (:require [scoreboard.events :as e :refer [publish!]]
            [scoreboard.core :as s]))

(def score-range (range 0 150))
(def timeouts-range (range 1 4))

(def paths-values
  [[[:home :score] score-range]
   [[:home :timeouts] timeouts-range]
   [[:away :score] score-range]
   [[:away :timeouts] timeouts-range]
   [[:ball-on] (range 0 101)]
   [[:to-go] (range 0 101)]
   [[:down] (range 1 5)]
   [[:qtr] (range 1 5)]])

(defn update-number-events []
  (map
   (fn [[path possible-values]]
     (e/event ::s/update-number {:path path
                                 :max (max possible-values)
                                 :min (min possible-values)
                                 :number (rand-nth possible-values)}))
   paths-values))

(defn event-bag []
  (concat [(e/event ::s/change-possession)]
          (update-number-events)))

(defonce benchmarks (atom []))

(defn add-benchmark [benchmarks]
  (cons (. (js/Date.) (getTime)) (take 999 benchmarks)))

(defn publish-random-event []
  (swap! benchmarks add-benchmark)
  (publish! (rand-nth (event-bag))))

(defn main []
  (.setInterval js/window publish-random-event 10))

(defn bench [& [n]]
  (let [latest-n (take (or 1000 n) @benchmarks)]
    (/ (- (first latest-n) (last latest-n))
       (count latest-n))))
