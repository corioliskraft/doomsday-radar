# Plan: PR readiness gate

**Created**: 2026-10-08
**Branch**: slice-1-one-file
**Status**: implemented
**Gherkin persistence**: plan-file-only

## Goal

The agent cannot run `git push` or `gh pr create` while an item of the "Definition of Done for a slice" in `AGENTS.md` is not met for the HEAD tree. Three rules that were only instructions failed in one session, and each failure came out only when Nikolay asked.

## Decisions

- No Gherkin scenarios: the JUnit tests are the behavior contract (Nikolay, 2026-10-08).
- `scripts/pr-ready.sh` is Bash. Its tests are JUnit tests that run the script with `ProcessBuilder` in a temporary git repository, so `./mvnw verify` runs them locally and in CI (Nikolay, 2026-10-08).
- Each step starts from a failing test (`AGENTS.md`, Workflow). The step lists its cases in **TEST**; the build writes them before **IMPLEMENT**, as in slice 1.
- Mutation gate: `N/A`, no mutation tool for Bash; the alternate evidence is the RED record of each step, and a hand-made mutation run: 37 mutants of `scripts/pr-ready.sh`, each applied alone in a copy. Each survivor of the earlier runs got a test and was killed on rerun (a check name inside another record; an ordinary error that must not print "unexpected failure"; the message "not in HEAD" for a missing plan). The last full run, after the fixes of Grok round 2, killed all 37 on the final script and tests. The mutant list is `dev/mutate-pr-ready.py`, not committed.
- The agent writes a record only after the check passed, so a record is a claim of the agent, not a proof. Risk accepted by Nikolay.
- The gate checks the slice plan `plans/<branch>.md`. This plan is tool work on the same branch, and the gate does not read it.
- When the slice plan is missing from HEAD, the guardian does not run and is not named: it needs the plan, and the plan line names the cause.
- Stances: merge the hook into the existing `.claude/settings.json` (keep the graphify and formatter hooks); touch only the files below; the PR stays a draft without auto-merge.

## Acceptance Criteria

- [x] With each item met for the HEAD tree, `scripts/pr-ready.sh` exits 0. Items, from the Definition of Done: records `maven`, `pmd`, `adr`, `farley`, `grok`; the slice plan with a `Mutation gate:` text; `progress_guardian.py --pre-pr` passes; `docs/architecture.md` not empty. The guardian runs on the working tree and reports uncommitted changes as an error, so a dirty tree is an unmet item. The guardian ignores the slice plan file in that check, so the script checks the plan file itself. Evidence: `fiveRecordsForTheHeadTreePassTheCheckSilently`, `aSlicePlanWithUncommittedChangesIsAnUnmetItemEvenWhenItIsTheOnlyDirtyFile`.
- [x] With an item not met, it exits 2 and names each unmet item, and only those, on stderr. A usage error, or a direct run outside a repository, exits 1; exit 2 means only unmet items. Evidence: `oneRunNamesEveryKindOfUnmetItem`, `assertOnlyUnmet` in each single-item test, `aRunOutsideARepositoryExitsWithAnError`.
- [x] A record counts only for the tree that `git commit -a` makes from the working tree at record time. Evidence: the `records...` tests of step 1.2, including the same-size racy change.
- [x] In `--hook` mode, `git push`, `gh pr create` and `gh pr new` at a command start exit 2 and name the unmet items until the check passes, also when the check fails inside. Other commands exit 0 with no output. The forms listed as not matched in Risks stay unmatched. Evidence: `hookBlocksAPushOrPullRequestCommandWhileItemsAreUnmet` (27 cases), `hookLetsOtherCommandsPassSilently`, `hookDoesNotGateWrappedPushes`, and the hook run with the JSON of `git push --dry-run`: exit 2, output saved in `dev/review-gate-r3-evidence/live-hook-dry-run.out`.

## Slices

### Slice 1: readiness gate

**Depends-on:** none
**Files:** `scripts/pr-ready.sh`, `src/test/java/io/github/corioliskraft/doomsdayradar/tooling/PrReadyScriptTest.java`, `.claude/settings.json`, `AGENTS.md`

The script has three modes. Each goes to `git rev-parse --show-toplevel` before its first file access; `--hook` does so only after a command match.

- `scripts/pr-ready.sh record <check> <evidence>` adds the line `<check> <evidence>` to `$(git rev-parse --git-path pr-ready)/<hash>`. The hash comes from `git add -u` and `git write-tree` on a copy of the index: the tree that `git commit -a` makes. A record means "passed".
- `scripts/pr-ready.sh` checks the HEAD tree, prints each unmet item, and exits 2 if there is one.
- `scripts/pr-ready.sh --hook` reads the PreToolUse JSON from stdin. For a `git push`, `gh pr create` or `gh pr new` command it runs the check; any failure in it exits 2. Other commands exit 0 before any git call.

The check list is defined once, at the top of the script. The script runs on bash 3.2 (macOS) and quotes each path. The tests and the settings call it with `/bin/bash` (3.2 on macOS, 5 on ubuntu), so the exec bit is not needed.

Test setup: one `readyRepo()` helper builds a repository that meets each item; each test removes one item and asserts that stderr names that item only. The repository path contains a space. The process environment has no `GIT_*` variables from the host, `GIT_CONFIG_GLOBAL=/dev/null` and `GIT_CONFIG_NOSYSTEM=1`; the repository sets its own `user.name`, `user.email` and `commit.gpgsign=false`. `PR_READY_PROGRESS_GUARDIAN` points to a stub. The real guardian path comes from `installPath` of `dev-team@bfinster` in `~/.claude/plugins/installed_plugins.json`.

**Steps:**

#### Step 1.1: Records for the HEAD tree

**Complexity**: standard
**TEST**: Five records: exit 0. Records for `maven` and `grok` only: exit 2, stderr names `pmd`, `adr`, `farley` and not `maven` or `grok` (RED: the script does not exist). `record foo x`: exit 1. `record maven` with no evidence: exit 1. After `record`, `git status --porcelain` and `git diff --cached` are unchanged.
**IMPLEMENT**: The check list, `record` mode, and the record check.
**REFACTOR**: `readyRepo()` and the script call helper.
**Files**: `scripts/pr-ready.sh`, `src/test/java/io/github/corioliskraft/doomsdayradar/tooling/PrReadyScriptTest.java`
**Commit**: one commit for the slice, see Risks.

#### Step 1.2: A record counts only for its tree

**Complexity**: standard
**TEST**: Records, then a change to a tracked file and a commit: exit 2, all five named. Records made on a change that is not committed, and no commit: exit 2. Records on a changed tracked file and an untracked file, then `git commit -a`: exit 0. Records, a change to a file, the change undone: exit 0. Records with a new file staged by `git add`, then `git commit -a`: exit 0.
**IMPLEMENT**: Hash from `git add -u` on an index copy for `record`; `HEAD^{tree}` for the check. The index copy is deleted on exit.
**REFACTOR**: Review names in the script and the test.
**Files**: `scripts/pr-ready.sh`, `src/test/java/io/github/corioliskraft/doomsdayradar/tooling/PrReadyScriptTest.java`
**Commit**: one commit for the slice.

#### Step 1.3: Slice plan and mutation gate

**Complexity**: standard
**TEST**: No `plans/<branch>.md` in HEAD: exit 2, stderr names that path. A plan in the working tree only: exit 2. A plan without text after `Mutation gate:`: exit 2, stderr names the mutation gate. Detached HEAD: exit 2, stderr names the missing branch. A run from a subdirectory gives the same result as from the top. A direct run outside a repository: exit 1. A plan with uncommitted changes, a failing `git status`, and an open indented step under `## Build Progress`: exit 2 each. Indented steps that are done, and an open indented box before `## Build Progress`: exit 0.
**IMPLEMENT**: Plan path from the branch name, read with `git show HEAD:<path>`. The script also rejects an open indented checkbox under `## Build Progress`, because the guardian does not read it.
**REFACTOR**: Collect all unmet items before the exit, so one run names each of them.
**Files**: `scripts/pr-ready.sh`, `src/test/java/io/github/corioliskraft/doomsdayradar/tooling/PrReadyScriptTest.java`
**Commit**: one commit for the slice.

#### Step 1.4: Progress guardian

**Complexity**: standard
**TEST**: The stub receives `--plan plans/<branch>.md --pre-pr --skip-llm`. A stub that exits 1 with a message: exit 2, stderr has the message. A stub that exits 3: exit 2. A stub path that does not exist: exit 2, stderr names the guardian.
**IMPLEMENT**: One function finds the guardian: `PR_READY_PROGRESS_GUARDIAN`, else `installed_plugins.json`. A guardian that cannot run is an unmet item.
**REFACTOR**: Review the script for duplication.
**Files**: `scripts/pr-ready.sh`, `src/test/java/io/github/corioliskraft/doomsdayradar/tooling/PrReadyScriptTest.java`
**Commit**: one commit for the slice.

#### Step 1.5: Architecture document

**Complexity**: trivial
**TEST**: No `docs/architecture.md` in HEAD: exit 2, stderr names it. An empty file: exit 2.
**IMPLEMENT**: Size check with `git cat-file -s HEAD:docs/architecture.md`.
**REFACTOR**: Review the order of the items in the output.
**Files**: `scripts/pr-ready.sh`, `src/test/java/io/github/corioliskraft/doomsdayradar/tooling/PrReadyScriptTest.java`
**Commit**: one commit for the slice.

#### Step 1.6: Hook mode

**Complexity**: standard
**TEST**: One parameterized table on a repository that is not ready.
- Exit 2, stderr names the unmet items: `git push`, `git  push`, `git push -u origin x`, `git -C dir push`, `git -c k=v push`, `/usr/bin/git push`, `FOO=1 git push`, `env git push`, `ls; git push`, `ls\ngit push`, `cd a && gh pr create --draft`, `gh -R o/r pr create`, `gh pr new --draft`, `git -C /a\ b push`, `git -C "/a b"/c push`, `git -c k='a b' push`, `git --git-dir="/a b" push`, `FOO=a\ b git push`, `false || git push`, `echo $(git push)`.
- Exit 0 with empty stdout and stderr: `ls`, `git status`, `gh pr view`, `git pushx`, `git commit -m "add git push gate"`, `bash -c "git push"`, `sh -c "git push"`, `if x; then git push; fi`, ``echo `git push` ``, `{ git push; }`.

Also: `git push` on a ready repository exits 0. Malformed JSON that contains `git push` exits 2; malformed JSON without it exits 0. A guardian that cannot run, with `git push`, exits 2. Outside a repository: `ls` exits 0, `git push` exits 2.
**IMPLEMENT**: Command match first: at a command start (line start, `;`, `&`, `|`, `(`), optional `VAR=x` words, `env`, a path, then `git` with options and `push`, or `gh` with options and `pr create`. If `jq` fails, change each double quote in the raw stdin to a command start, then match. Run the check in a subshell; any non-zero exit becomes 2.
**REFACTOR**: The match pattern as one named function.
**Files**: `scripts/pr-ready.sh`, `src/test/java/io/github/corioliskraft/doomsdayradar/tooling/PrReadyScriptTest.java`
**Commit**: one commit for the slice.

#### Step 1.7: Hook in the project settings

**Complexity**: standard
**TEST**: In this session, `git push --dry-run` is blocked and the message names the unmet items (RED: it runs). `git status` runs. `jq` reads `.claude/settings.json`, and the graphify and formatter hooks are still there. Evidence in the step report, no JUnit test: the config is data that Claude Code reads.
**IMPLEMENT**: A PreToolUse entry with matcher `Bash`, command `/bin/bash "$CLAUDE_PROJECT_DIR"/scripts/pr-ready.sh --hook`, and `timeout` 120, through the `update-config` skill. In `AGENTS.md`, two sentences under the Definition of Done: a hook runs `scripts/pr-ready.sh` before `git push` and `gh pr create`, and `scripts/pr-ready.sh record <check> <evidence>` writes a record after a check passed.
**REFACTOR**: Review the `AGENTS.md` sentence with `writing-for-agents`.
**Files**: `.claude/settings.json`, `AGENTS.md`
**Commit**: one commit for the slice.

## Parallelization

One slice, one wave (`plan_waves.py`: no collisions, no scope mismatches).

## Pre-PR Quality Gate

- [x] `./mvnw verify` passes. 2026-10-09: BUILD SUCCESS, 104 unit tests (103 in `PrReadyScriptTest`) and 15 IT, 0 failures; log `dev/mvn-verify-pr.log`.
- [x] PMD passes. 2026-10-09: "Found no violations.", `dev/pmd-pr.log`.
- [ ] Review of the slice diff.

## Risks & Open Questions

- One commit for the slice, not one for each step (Nikolay's decision 3, `dev/handover-2026-10-08-b.md`). The file list of the slice lets the plan gate match the commit.
- The hook also blocks a `git push` of work in progress. Nikolay can push from his own terminal, where the hook does not run.
- The guardian reads the working tree, not HEAD. It reports uncommitted changes itself, except in the slice plan file, which the script checks.
- The guardian reads only list lines that start at column 0. The plans from `/dev-team:plan` indent the steps under the slice line, so the guardian sees the slice line only, and an open step under a ticked slice passes. The script closes this for an open indented checkbox under `## Build Progress` in the HEAD plan. A step that has no checkbox, for example a `####` heading, is not checked.
- The script checks only that `docs/architecture.md` is in HEAD and not empty. The `adr` and Grok reviews cover the content.
- Not matched: `bash -c`, `sh -c`, backticks, `{ ...; }`, shell keywords such as `then`, `command`, `env` with options such as `env -i` (bare `env` and `VAR=value` prefixes are matched), a quoted or escaped command word such as `"/usr/bin/git" push`, and a backslash-newline between the words of `git push`, `gh pr create` or `gh pr new` (a command whose words stay on one line, as in `git push \` followed by a newline, is matched). The gate catches a forgotten rule, not a hidden push.
- Claude Code blocks only on exit 2, so a hook timeout or a missing script lets the command run. The guardian runs with `--skip-llm`, so the check stays far below the timeout.
- The gate checks the HEAD tree, not the ref in the command, so `git push origin other` pushes a ref that the gate did not check. Accepted: a refspec parser would grow the matcher, and the matcher stays frozen.
- The agent can edit `scripts/pr-ready.sh`, the hook entry in `.claude/settings.json` and the variable `PR_READY_PROGRESS_GUARDIAN`. This is the same risk as the self-written records, and the same decision (Decisions). The gate catches a forgetful agent, not a hostile one.
- A false block on a quoted text after `;` or `|` is possible. It is the safe direction.
- A settings change may need a new session before the hook runs. Then step 1.7 runs its check in a new session.
- No `shellcheck`: it is not installed. The tests run `/bin/bash`: 3.2 on macOS, 5 in CI.

## Plan Review Summary

Plan tier: standard. Reviewers: Acceptance, Design (UX skipped: no UI; Parallelization skipped: one slice).

- Round 1: both `needs-revision`. Blockers fixed: hook fail-open, no hook pass case, record hash `git add -A` against `git commit -a`, plan path, no `Mutation gate:` line in this plan.
- Round 2: both `approve`. Warnings applied: hook bypass forms and fail-open outside the script in Risks, malformed JSON without a push, "names only that item" in each test, `cd` only after a hook match, `/bin/bash`.
- Not applied: a JUnit test for `.claude/settings.json`. The config is data; step 1.7 checks it with `jq` and a live block.

## Build Progress

### Slices (grouped by wave)

#### Wave 1
- [x] Slice 1: readiness gate
  - [x] Step 1.1: Records for the HEAD tree
  - [x] Step 1.2: A record counts only for its tree
  - [x] Step 1.3: Slice plan and mutation gate
  - [x] Step 1.4: Progress guardian
  - [x] Step 1.5: Architecture document
  - [x] Step 1.6: Hook mode
  - [x] Step 1.7: Hook in the project settings
