---
name: pr
description: Open a pull request for Doomsday Radar. Runs the Maven and PMD gates, then hands over to /dev-team:pr. Use it instead of /dev-team:pr in this project.
argument-hint: "[--draft] [--base <branch>]"
---

# Pull request for Doomsday Radar

`/dev-team:pr` detects no Maven tests and no Java linter. Run these gates first, from the repository root. Stop at the first failure and report it.

1. `./mvnw verify`: unit tests and the Testcontainers ITs. Docker must run. Keep `verify` as the second word, or dev-team's mutation hook does not see the test run.
2. `.pmd/pmd-bin-*/bin/pmd check -d src -R rulesets/java/quickstart.xml -f text --no-progress`: exit 4 means findings, and findings stop the pull request.

Then invoke `/dev-team:pr --no-auto-merge --draft $ARGUMENTS`. Its gate step detects neither command and fills "Checks run" only from its own results. Complete the body while the pull request is a draft:

1. Save the current body: `gh pr view <number> --json body --jq .body > <file>`.
2. In that file, add both commands with their results under "Checks run", and replace any "not applicable" entry for tests or lint. Keep every other section unchanged.
3. `gh pr edit <number> --body-file <file>`, then `gh pr view <number>` to confirm the new body.
4. `gh pr ready <number>`, only when `$ARGUMENTS` does not contain `--draft`. If step 3 fails, the pull request stays a draft and you report the failure.
