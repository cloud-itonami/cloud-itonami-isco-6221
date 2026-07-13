# cloud-itonami-isco-6221

Open Occupation Blueprint for **ISCO-08 6221**: Aquaculture Workers.

**Maturity: `:implemented`** — AquacultureAdvisor ⊣ AquacultureGovernor
as a langgraph StateGraph (`intake → advise → govern → decide →
commit/hold`, human-approval interrupt), modeled on
cloud-itonami-isco-4311's bookkeeping actor. 14 tests / 29 assertions
green. The governor never dispatches hardware — it only gates what
the pond/tank-monitoring robot below may execute.

The feed HARD invariants — arithmetic and water chemistry, not
judgement:

1. **Per-fish feed ceiling** — a proposed feed dose divided by the
   registered fish count must not exceed the registered per-fish
   ceiling.
2. **Dissolved-oxygen floor** — the measured dissolved oxygen must
   meet the registered floor before a feed is approved (feeding into
   oxygen-depleted water accelerates die-off).

`:approve-chemical-treatment` and `:approve-deep-water-operation`
**always** escalate to human sign-off regardless of confidence, per
this repo's Trust Controls (business-model.md) — no chemical treatment
or deep-water operation is ever auto-committed.

This repository designs a forkable OSS business for an independent aquaculture operator: a pond/tank-monitoring robot performs water-quality sensing and feed dispensing under a governor-gated actor, so the operator keeps their own stock and water-quality records instead of renting a closed aquaculture-management SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a pond/tank-monitoring robot performs water-quality sensing, feed dispensing and stock counting under an actor that proposes
actions and an independent **Aquaculture Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
operating near deep water, or chemical water-treatment application) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
stocking plan + feed schedule + water-quality protocol
        |
        v
Aquaculture Advisor -> Aquaculture Governor -> feed/harvest, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `6221`). Required capabilities:

- :robotics
- :telemetry
- :dmn
- :bpmn
- :audit-ledger
- :forms

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
