(ns shrike.event
  "Namespace for handling events"
  (:require [jayq.core :refer [$]]
            [shrike.chart :as chart]))

(defn trigger-chart-redraw
  "Trigger Chart.js to remove all existing charts and re-draw new ones."
  []
  (chart/nuke-charts)
  (.trigger ($ js/document) "redraw.bs.charts"))
