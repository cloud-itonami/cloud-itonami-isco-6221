(ns aquaculture.store
  "SSoT for the ISCO-08 6221 independent aquaculture operations actor
  (itonami actor pattern, ADR-2607011000 / CLAUDE.md Actors section;
  README's 'Robotics premise' — a pond/tank-monitoring robot performs
  water-quality sensing, feed dispensing and stock counting under
  this advisor/governor pair, which never dispatches hardware
  itself). Modeled on cloud-itonami-isco-4311's bookkeeping.store.

  Domain:

    client — a registered organization (:client-id, :name)
    pond   — a registered pond/tank {:pond-id :client-id :name
             :fish-count number :max-feed-kg-per-fish number
             :min-dissolved-oxygen-mgl number}.
             `:max-feed-kg-per-fish` is the registered daily feed
             ceiling per fish (arithmetic, not judgement);
             `:min-dissolved-oxygen-mgl` is the registered floor a
             proposed feed's measured dissolved oxygen must meet —
             feeding into oxygen-depleted water accelerates die-off,
             that's water chemistry, not judgement.
    record — a committed operating record (approved feed dispense) —
             written ONLY via commit-record!.
    ledger — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (client [s client-id])
  (pond [s pond-id])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (register-pond! [s p])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (pond [_ pond-id] (get-in @a [:ponds pond-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s client]
    (swap! a assoc-in [:clients (:client-id client)] client) s)
  (register-pond! [s p]
    (swap! a assoc-in [:ponds (:pond-id p)] p) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :ponds {} :records [] :ledger []}
                                   seed)))))
