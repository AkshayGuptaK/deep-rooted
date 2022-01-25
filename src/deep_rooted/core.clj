(ns deep-rooted.core
  (:gen-class)
  (:require [deep-rooted.parser :as parser]
            [deep-rooted.market-maker :as market-maker]))

(defn- trade-str [{:keys [demand-id supply-id price quantity]}]
  "Formats a trade map into a string fit for printing"
  (->> ["d" demand-id " s" supply-id " " price "/kg " quantity "kg"]
       (apply str)))

(defn -main
  [& args]
  (->> *in*
       (slurp)
       (parser/parse-input)
       (sort-by :time)
       (market-maker/process-orders)
       (map trade-str)
       (run! println)))
