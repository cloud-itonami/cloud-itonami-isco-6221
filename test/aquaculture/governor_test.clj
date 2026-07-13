(ns aquaculture.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [aquaculture.store :as store]
            [aquaculture.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Aquaculture"})
    (store/register-pond! st {:pond-id "P-1" :client-id "client-1"
                              :name "pond-3"
                              :fish-count 1000
                              :max-feed-kg-per-fish 0.02
                              :min-dissolved-oxygen-mgl 5.0})
    st))

(defn- feed [kg oxygen]
  {:op :approve-feed :effect :propose :pond-id "P-1"
   :feed-kg kg :dissolved-oxygen-mgl oxygen :confidence 0.9 :stake :low})

(def ^:private req {:client-id "client-1"})

(deftest ok-within-feed-ceiling-and-oxygen-floor
  (let [st (fresh-store)
        v (governor/check req {} (feed 15 6.0) st)]
    (is (:ok? v))))

(deftest ok-at-exact-feed-ceiling-and-oxygen-floor
  (testing "the feed ceiling and oxygen floor boundaries are inclusive"
    (let [st (fresh-store)
          v (governor/check req {} (feed 20 5.0) st)]
      (is (:ok? v)))))

(deftest hard-on-feed-exceeds-per-fish-ceiling
  (testing "overfeeding is arithmetic, not judgement"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (feed 40 6.0) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :feed-exceeds-per-fish-ceiling (:rule %)) (:violations v))))))

(deftest hard-on-dissolved-oxygen-below-floor
  (testing "feeding into oxygen-depleted water is water chemistry, not judgement"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (feed 15 2.0) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :dissolved-oxygen-below-floor (:rule %)) (:violations v))))))

(deftest hard-on-unknown-pond
  (let [st (fresh-store)
        v (governor/check req {} (assoc (feed 15 6.0) :pond-id "P-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-pond (:rule %)) (:violations v)))))

(deftest hard-on-foreign-pond
  (let [st (fresh-store)]
    (store/register-client! st {:client-id "client-2" :name "Other"})
    (let [v (governor/check {:client-id "client-2"} {} (feed 15 6.0) st)]
      (is (:hard? v))
      (is (some #(= :pond-wrong-client (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        v (governor/check {:client-id "nobody"} {} (feed 15 6.0) st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (feed 15 6.0) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest always-escalates-chemical-treatment-even-at-high-confidence
  (testing "no chemical treatment application without the governor gate"
    (let [st (fresh-store)
          v (governor/check req {} {:op :approve-chemical-treatment :effect :propose
                                    :pond-id "P-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest always-escalates-deep-water-operation-even-at-high-confidence
  (testing "deep-water operations require human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} {:op :approve-deep-water-operation :effect :propose
                                    :pond-id "P-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (feed 15 6.0) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))
