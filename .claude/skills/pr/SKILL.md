---
name: pr
description: Open a pull request for Doomsday Radar. Runs the Maven and PMD gates and a Grok review, then hands over to /dev-team:pr. Use it instead of /dev-team:pr in this project.
argument-hint: "[--draft] [--base <branch>]"
---

# Pull request for Doomsday Radar

`/dev-team:pr` detects no Maven tests and no Java linter. Run these gates first, from the repository root. Stop at the first failure of gate 1 or 2 and report it.

1. `./mvnw verify`: unit tests and the Testcontainers ITs. Docker must run. Keep `verify` as the second word, or dev-team's mutation hook does not see the test run.
2. `.pmd/pmd-bin-*/bin/pmd check -d src -R rulesets/java/quickstart.xml -f text --no-progress`: exit 4 means findings, and findings stop the pull request.
3. The `double-check` skill with the default Grok profile on `git diff $(git merge-base origin/<base> HEAD)`, which includes uncommitted fixes (`<base>` is `main` unless `--base` names another). Use only a reviewer from another provider: if none can run, the review did not finish. Follow the skill to its end: each finding is fixed, closed by the reviewer, or deferred by Nikolay. A finding that is still open stops the pull request. If the review leads to a change, run gates 1 and 2 again and review the changed working tree in the next round. After the last round, commit all changes only after gates 1 and 2 pass and Nikolay approves the commit. If Nikolay does not approve the commit, stop and report. If the review does not finish, report why and continue.

Then invoke `/dev-team:pr --no-auto-merge --draft $ARGUMENTS`. Its gate step runs neither Maven nor PMD and fills "Checks run" only from its own results. Complete the body while the pull request is a draft:

1. Save the current body: `gh pr view <number> --json body --jq .body > <file>`.
2. In that file, add the Maven and PMD commands with their results under "Checks run", and replace any "not applicable" entry for tests or lint. If at least one review round finished, replace the line that is only `NOT RUN` under "Cross-provider review" with one line: the commit that holds the state of the last finished round, the reviewer and model, the number of finished rounds, and the final verdict. If a later round did not finish, add "did not finish in round <n>" to that line. Put each finding that Nikolay deferred on its own line below it. If no round finished, keep `NOT RUN`. Keep every other section and the HTML comments unchanged.
3. `gh pr edit <number> --body-file <file>`, then `gh pr view <number>` to confirm the new body.
4. `gh pr ready <number>`, only when `$ARGUMENTS` does not contain `--draft`. If step 3 fails, the pull request stays a draft and you report the failure.
