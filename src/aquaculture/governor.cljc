(ns aquaculture.governor
  "AquacultureGovernor — the independent safety/traceability layer
  named in this repository's README/business-model.md, gating the
  robot-dispensed physical work (water-quality sensing, feed
  dispensing, stock counting) an advisor may propose. The governor
  never dispatches hardware itself. Modeled on
  cloud-itonami-isco-4311's bookkeeping.governor. Feed twist: a
  proposed feed dose divided by the registered fish count is
  arithmetic comparison against the registered per-fish ceiling, and
  the measured dissolved oxygen must meet the registered floor before
  feeding is approved — feeding into oxygen-depleted water accelerates
  die-off, that's water chemistry, not judgement.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance — the organization must be registered.
    2. no-actuation      — proposal :effect must be :propose (the
                           governor never dispatches hardware; it only
                           gates what the robot may execute).
    3. pond basis           — a feed approval must cite a REGISTERED
                           pond belonging to this client.
    4. per-fish feed ceiling — (feed-kg / fish-count) must not exceed
                           the pond's registered
                           :max-feed-kg-per-fish (arithmetic, not
                           judgement).
    5. dissolved-oxygen floor — the proposed dissolved-oxygen-mgl must
                           be >= the pond's registered
                           :min-dissolved-oxygen-mgl (water chemistry,
                           not judgement).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off per
  business-model.md's Trust Controls — these are :high/
  :safety-critical regardless of confidence):
    6. :op :approve-chemical-treatment (no chemical treatment
                           application without the governor gate).
    7. :op :approve-deep-water-operation (deep-water operations
                           require human sign-off).
    8. low confidence (< `confidence-floor`)."
  (:require [aquaculture.store :as store]))

(def confidence-floor 0.6)

(def ^:private always-escalate-ops #{:approve-chemical-treatment
                                     :approve-deep-water-operation})

(defn- hard-violations [{:keys [request proposal]} client-record p]
  (let [{:keys [op feed-kg dissolved-oxygen-mgl]} proposal
        feed? (= :approve-feed op)]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor はハードウェアを直接起動しない）"})

      (and feed? (nil? p))
      (conj {:rule :unknown-pond :detail "未登録 pond への給餌承認は不可"})

      (and feed? p (not= (:client-id p) (:client-id request)))
      (conj {:rule :pond-wrong-client :detail "pond が別 client のもの"})

      (and feed? p (number? feed-kg) (pos? (:fish-count p))
           (> (/ feed-kg (:fish-count p)) (:max-feed-kg-per-fish p)))
      (conj {:rule :feed-exceeds-per-fish-ceiling
             :detail (str "1尾あたり給餌量 " (double (/ feed-kg (:fish-count p)))
                          "kg > 登録済み上限 " (:max-feed-kg-per-fish p)
                          "kg（過給餌は算術であって判断ではない）")})

      (and feed? p (number? dissolved-oxygen-mgl)
           (< dissolved-oxygen-mgl (:min-dissolved-oxygen-mgl p)))
      (conj {:rule :dissolved-oxygen-below-floor
             :detail (str "溶存酸素 " dissolved-oxygen-mgl "mg/L < 登録済み下限 "
                          (:min-dissolved-oxygen-mgl p)
                          "mg/L（酸素欠乏水域への給餌はへい死を加速させる。水質化学であって判断ではない）")}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `aquaculture.store/Store`. Pure — never mutates
  the store, never dispatches the robot."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        p (some->> (:pond-id proposal) (store/pond store))
        hard (hard-violations {:request request :proposal proposal}
                              client-record p)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
