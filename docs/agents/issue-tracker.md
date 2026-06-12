# Issue tracker

## Where issues live

Issues for this repo live in **GitHub Issues** on the repository's GitHub remote.

The `to-issues`, `triage`, `to-prd`, and `qa` skills read from and write to GitHub using the [`gh`](https://cli.github.com/) CLI.

## Prerequisites

- The repo must be a git repository with a GitHub remote:
  ```bash
  git init
  gh repo create <org>/igw-gateway --source=. --remote=origin --push
  ```
- The `gh` CLI must be authenticated (`gh auth status`) and authorized for the target org/user.
- The triage labels (see `triage-labels.md`) should exist on the repo before the `triage` skill runs. The `triage` skill will create them on first run, but you can pre-create them with:
  ```bash
  gh label create needs-triage    --color "FBCA04" --description "Awaiting maintainer evaluation"
  gh label create needs-info      --color "D93F0B" --description "Waiting on reporter for more info"
  gh label create ready-for-agent --color "0E8A16" --description "Fully specified; an AFK agent can pick it up"
  gh label create ready-for-human --color "1D76DB" --description "Needs a human implementer"
  gh label create wontfix         --color "FFFFFF" --description "Will not be actioned"
  ```

## Conventions for skills

- **Listing issues:** `gh issue list --label <label> --state open`
- **Reading an issue:** `gh issue view <number>`
- **Creating an issue:** `gh issue create --title "..." --body "..." --label <labels>`
- **Applying a triage transition:** `gh issue edit <number> --add-label <new> --remove-label <old>`
- **Closing:** prefer `gh issue close <number> --comment "..."` over a bare close, so the resolution is recorded in the issue thread.

## Scope

This skill only governs how the engineering skills interact with the tracker. It does not change how *humans* file or triage issues — keep your existing team conventions.
