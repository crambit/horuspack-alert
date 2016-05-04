(ns core.alert.config
  "Alert definition."
  (:require [riemann.common :refer [event-to-json]]
            [taoensso.carmine :as car]
            [taoensso.carmine.message-queue :as car-mq]
            [clojure.tools.logging :as log]))

(defmacro ^:private wcar* [& body] `(car/wcar {:pool {} :spec {:uri "//localhost:6379"}} ~@body))

(defn- publish
  "Publish event to the Redis queue."
  [queue event]
  (try
    (wcar* (car-mq/enqueue queue (event-to-json event)))
    (catch Exception e
      (prn (str "RedisError -> " e)))))

(defn- alert-severe
  "The most urgent alerting level, often escalated to a page to urgently request human attention."
  [env event]
  (log/info (str "SEVERE(" env ") -> " event))
  (publish "severe" event))

(defn- alert-moderate
  "Alerting urgency level for issues that do require intervention, but not right away."
  [env event]
  (log/info (str "MODERATE(" env ") -> " event))
  (publish "moderate" event))

(defn- alert-minor
  "Low-urgency alert that is recorded in your monitoring system for future reference or investigation but does not interrupt anyone’s work."
  [env event]
  (log/info (str "MINOR(" env ") -> " event))
  (publish "minor" event))

(defn- alert-tips
  "Some pointers on how to go about a currently observed situation."
  [env event]
  (log/info (str "TIPS(" env ") -> " event))
  (publish "tips" event))

(defn alert
  "Not all alerts carry the same degree of urgency. Some require
  immediate human intervention, some require eventual human
  intervention, and some point to areas where attention may be
  needed in the future. All alerts should, at a minimum, be logged to
  a central location for easy correlation with other metrics and events.

  4 levels of alerting urgency are available by default:

  - tips
  - minor
  - moderate
  - severe

  Expect other packages to make use of these levels."

  [& {:keys [level env]}]
  (fn [event]
    (let [env (or env "prod")]
      (cond
        (= level "severe")
          (alert-severe env event)
        (= level "moderate")
          (alert-moderate env event)
        (= level "minor")
          (alert-minor env event)
        :else
          (alert-tips env event)))))
