# cloud-itonami-isco-6221

Open Occupation Blueprint for **ISCO-08 6221**: Aquaculture Workers.

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
