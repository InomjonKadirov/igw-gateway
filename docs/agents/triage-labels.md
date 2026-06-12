# Triage labels

The `triage` skill moves issues through five canonical states. The label string the skill applies for each role is listed below.

| Role | Label | Meaning |
| --- | --- | --- |
| `needs-triage` | `needs-triage` | Maintainer needs to evaluate |
| `needs-info` | `needs-info` | Waiting on reporter for clarification |
| `ready-for-agent` | `ready-for-agent` | Fully specified; an AFK agent can pick it up with no human context |
| `ready-for-human` | `ready-for-human` | Needs a human implementer |
| `wontfix` | `wontfix` | Will not be actioned |

## Notes

- These are the **default** names. To change one, update the table here and re-run any open triage transitions manually.
- Exactly **one** of these labels should be applied at a time. The `triage` skill is responsible for removing the old label when it adds the new one.
- `wontfix` is terminal — closing an issue with `wontfix` is the expected end state and should be paired with a closing comment explaining why.
- Other labels (e.g. `bug`, `enhancement`, `area:gateway`, `priority:high`) are orthogonal to triage and may coexist with any of the above.
