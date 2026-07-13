(ns aquaculture.advisor
  "AquacultureAdvisor — the advisor named in this repository's README,
  proposing a pond operation (approve a feed dispense, approve a
  chemical treatment, approve a deep-water operation) from a stocking
  plan, feed schedule and water-quality protocol. Swappable mock/llm;
  the advisor ONLY proposes — `aquaculture.governor` checks the
  per-fish feed ceiling and dissolved-oxygen floor independently and
  always escalates chemical-treatment/deep-water decisions. Modeled
  on cloud-itonami-isco-4311's advisor.

  A proposal: {:op :approve-feed|:approve-chemical-treatment|:approve-deep-water-operation
               :effect :propose :pond-id str :feed-kg number
               :dissolved-oxygen-mgl number :stake kw :confidence n
               :rationale str}")

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake pond-id feed-kg dissolved-oxygen-mgl] :as request}]
  {:op op
   :effect :propose
   :pond-id pond-id
   :feed-kg feed-kg
   :dissolved-oxygen-mgl dissolved-oxygen-mgl
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are an aquaculture-operations advisor. Given a request, propose
   an :op, the :pond-id, :feed-kg and :dissolved-oxygen-mgl, an honest
   :confidence and a :stake. Never call an over-ceiling feed dose or a
   sub-floor dissolved-oxygen feed conforming — the governor checks
   both against the registered pond record. Chemical-treatment and
   deep-water decisions always require human sign-off regardless of
   confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
