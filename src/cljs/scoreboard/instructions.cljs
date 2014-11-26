(ns scoreboard.instructions)

(defn tt [& instructions]
  [:div.tooltip
   (into [:ul]
         (for [i instructions]
           [:li i]))])

(def score-header
  (tt "Click to add 1 to score."
      "Right click to remove 1 from score."))

(def score
  (tt "Click to edit score."
      "After changing, press enter."
      "Leave the box empty to cancel."))

(def possession
  (tt "Click to change possession."))

(def timeout
  (tt "Click to remove a timeout."
      "After 0, clicking loops back to 3."))

(def ball-on-label
  (tt "Click to advance the offense 1 yard."
      "Right click for a loss of 1 yard."))

(def ball-on-number
  (tt "Click to edit the yard line."
      "0-100 where 0 is the home team goal line."
      "After changing, press enter."
      "Leave the box empty to cancel."))

(def to-go-label
  (tt "Click to advance the offense 1 yard."
      "Right click for a loss of 1 yard."))

(def to-go-number
  (tt "Click to edit yards to go."
      "After changing, press enter."
      "Leave the box empty to cancel."))

(def down-label
  (tt "Click to next down."
      "Right click to previous down."))

(def down-number
  (tt "Click to edit the down (1-4)."
      "After changing, press enter."
      "Leave the box empty to cancel."))

(def qtr-label
  (tt "Click to next quarter."
      "Right click to previous quarter."))

(def qtr-number
  (tt "Click to edit the quarter (1-4)."
      "After changing, press enter."
      "Leave the box empty to cancel."))
