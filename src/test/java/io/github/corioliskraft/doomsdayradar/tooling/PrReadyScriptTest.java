package io.github.corioliskraft.doomsdayradar.tooling;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

class PrReadyScriptTest {

    private static final Path SCRIPT = Path.of("scripts", "pr-ready.sh").toAbsolutePath();
    private static final List<String> CHECKS = List.of("maven", "pmd", "adr", "farley", "grok");
    private static final String EVIDENCE = "passed";
    private static final String BRANCH = "slice-x";
    private static final String PLAN = "plans/" + BRANCH + ".md";
    private static final String ARCHITECTURE = "docs/architecture.md";
    private static final int EXIT_ERROR = 1;
    private static final int EXIT_UNMET = 2;
    private static final int EXIT_BLOCK = 2;
    private static final long TIMEOUT_SECONDS = 60;

    @TempDir Path dir;

    @Test
    void fiveRecordsForTheHeadTreePassTheCheckSilently() throws Exception {
        var repo = readyRepo();

        var result = runScript(repo);

        assertPassesSilently(result);
    }

    @Test
    void checksWithoutRecordsAreNamedOnStderrAndRecordedChecksAreNot() throws Exception {
        var recorded = List.of("maven", "grok");
        var repo = repoWithRecords(recorded);
        var unrecorded = CHECKS.stream().filter(check -> !recorded.contains(check)).toList();

        var result = runScript(repo);

        assertOnlyUnmet(result, unrecorded.size(), unrecorded.toArray(String[]::new));
        assertThat(result.stderr()).doesNotContain(recorded.toArray(String[]::new));
    }

    @Test
    void aCheckNameInTheEvidenceOfAnotherRecordDoesNotCountAsARecord() throws Exception {
        var repo = newRepo();
        assertSucceeds(runScript(repo, "record", "maven", "pmd adr farley grok"));

        var result = runScript(repo);

        assertOnlyUnmet(result, 4, "pmd", "adr", "farley", "grok");
        assertThat(result.stderr()).doesNotContain("maven");
    }

    @Test
    void recordOfAnUnknownCheckExitsWithAnError() throws Exception {
        var repo = newRepo();

        var result = runScript(repo, "record", "foo", EVIDENCE);

        assertFails(result, "foo");
    }

    @Test
    void anOrdinaryErrorDoesNotReportAnUnexpectedFailure() throws Exception {
        var repo = newRepo();

        var result = runScript(repo, "record", "foo", EVIDENCE);

        assertFails(result, "foo");
        assertThat(result.stderr()).doesNotContain("unexpected failure");
    }

    @Test
    void recordWithoutEvidenceExitsWithAnError() throws Exception {
        var repo = newRepo();

        var result = runScript(repo, "record", "maven");

        assertFails(result, "no evidence");
    }

    @Test
    void recordWithEmptyEvidenceExitsWithAnError() throws Exception {
        var repo = newRepo();

        var result = runScript(repo, "record", "maven", "");

        assertFails(result, "no evidence");
    }

    @Test
    void anUnknownModeExitsWithAnError() throws Exception {
        var repo = newRepo();

        var result = runScript(repo, "frobnicate");

        assertFails(result, "usage");
    }

    @Test
    void recordLeavesTheStatusAndTheStagedDiffUnchanged() throws Exception {
        var repo = newRepo();
        Files.writeString(repo.resolve("tracked.txt"), "changed\n");
        Files.writeString(repo.resolve("staged.txt"), "staged\n");
        git(repo, "add", "staged.txt");
        Files.writeString(repo.resolve("untracked.txt"), "untracked\n");
        var statusBefore = git(repo, "status", "--porcelain");
        var stagedDiffBefore = git(repo, "diff", "--cached");

        var result = runScript(repo, "record", "maven", EVIDENCE);

        assertReady(result);
        assertThat(git(repo, "status", "--porcelain")).isEqualTo(statusBefore);
        assertThat(git(repo, "diff", "--cached")).isEqualTo(stagedDiffBefore);
    }

    @Test
    void recordLeavesNoTemporaryFileBehindAndPrintsNothing() throws Exception {
        var repo = newRepo();

        var result = runScript(repo, "record", "maven", EVIDENCE);

        assertPassesSilently(result);
        try (var left = Files.list(scriptTmpdir())) {
            assertThat(left).isEmpty();
        }
    }

    @Test
    void recordsForTheOldTreeDoNotCountAfterANewCommit() throws Exception {
        var repo = readyRepo();
        Files.writeString(repo.resolve("tracked.txt"), "two\n");
        git(repo, "commit", "-q", "-a", "-m", "change");

        var result = runScript(repo);

        assertAllChecksUnmet(result);
    }

    @Test
    void recordsForAnUncommittedChangeDoNotCountBeforeTheCommit() throws Exception {
        var repo = newRepo();
        Files.writeString(repo.resolve("tracked.txt"), "two\n");
        recordAll(repo, CHECKS);

        var result = runScript(repo);

        assertAllChecksUnmet(result);
    }

    @Test
    void recordsForAChangedTrackedFileAndAnUntrackedFileCountAfterCommitAll() throws Exception {
        var repo = newRepo();
        Files.writeString(repo.resolve("tracked.txt"), "two\n");
        Files.writeString(repo.resolve("untracked.txt"), "untracked\n");
        recordAll(repo, CHECKS);
        git(repo, "commit", "-q", "-a", "-m", "change");

        var result = runScript(repo);

        assertReady(result);
    }

    @Test
    void recordsStillCountAfterAChangeIsUndone() throws Exception {
        var repo = readyRepo();
        Files.writeString(repo.resolve("tracked.txt"), "two\n");
        git(repo, "checkout", "--", "tracked.txt");

        var result = runScript(repo);

        assertReady(result);
    }

    @Test
    void recordsForANewlyStagedFileCountAfterCommitAll() throws Exception {
        var repo = newRepo();
        Files.writeString(repo.resolve("added.txt"), "added\n");
        git(repo, "add", "added.txt");
        recordAll(repo, CHECKS);
        git(repo, "commit", "-q", "-a", "-m", "add");

        var result = runScript(repo);

        assertReady(result);
    }

    @Test
    void recordsSeeASameSizeChangeMadeInTheSecondOfTheLastIndexWrite() throws Exception {
        var repo = newRepo();
        var tracked = repo.resolve("tracked.txt");
        var past = FileTime.from(Instant.parse("2020-01-01T00:00:00Z"));
        git(repo, "config", "core.trustctime", "false");
        Files.setLastModifiedTime(tracked, past);
        git(repo, "add", "tracked.txt");
        Files.setLastModifiedTime(repo.resolve(".git").resolve("index"), past);
        Files.writeString(tracked, "two\n");
        Files.setLastModifiedTime(tracked, past);
        recordAll(repo, CHECKS);
        git(repo, "commit", "-q", "-a", "-m", "change");

        var result = runScript(repo);

        assertThat(git(repo, "show", "HEAD:tracked.txt")).isEqualTo("two\n");
        assertReady(result);
    }

    @Test
    void aSlicePlanMissingFromHeadIsNamedAndNothingElseIs() throws Exception {
        var repo = newRepo();
        removePlanFromHead(repo);
        recordAll(repo, CHECKS);

        var result = runScript(repo);

        assertOnlyUnmet(result, 1, PLAN, "not in HEAD");
        assertThat(result.stderr()).doesNotContain("Mutation gate", "maven");
    }

    @Test
    void anArchitectureDocumentMissingFromHeadIsNamedAndNothingElseIs() throws Exception {
        var repo = newRepo();
        git(repo, "rm", "-q", ARCHITECTURE);
        git(repo, "commit", "-q", "-m", "remove architecture");
        recordAll(repo, CHECKS);

        var result = runScript(repo);

        assertOnlyUnmet(result, 1, ARCHITECTURE);
        assertThat(result.stderr()).doesNotContain("maven", "Mutation gate", "guardian");
    }

    @Test
    void anEmptyArchitectureDocumentIsNamedAndNothingElseIs() throws Exception {
        var repo = newRepo();
        Files.writeString(repo.resolve(ARCHITECTURE), "");
        git(repo, "commit", "-q", "-a", "-m", "empty architecture");
        recordAll(repo, CHECKS);

        var result = runScript(repo);

        assertOnlyUnmet(result, 1, ARCHITECTURE);
        assertThat(result.stderr()).doesNotContain("maven", "Mutation gate", "guardian");
    }

    @Test
    void aSlicePlanOnlyInTheWorkingTreeDoesNotCount() throws Exception {
        var repo = newRepo();
        removePlanFromHead(repo);
        Files.createDirectory(repo.resolve("plans"));
        Files.writeString(repo.resolve(PLAN), "Mutation gate: `N/A`\n");
        recordAll(repo, CHECKS);

        var result = runScript(repo);

        assertOnlyUnmet(result, 1, PLAN, "not in HEAD");
    }

    @Test
    void aSlicePlanWithoutTextAfterTheMutationGateIsNamedAsTheMutationGate() throws Exception {
        var repo = newRepo();
        commitPlan(repo, "# Plan\n\nMutation gate:   \n");
        recordAll(repo, CHECKS);

        var result = runScript(repo);

        assertOnlyUnmet(result, 1, "Mutation gate");
        assertThat(result.stderr()).doesNotContain("maven");
    }

    @Test
    void aSlicePlanWithUncommittedChangesIsAnUnmetItemEvenWhenItIsTheOnlyDirtyFile()
            throws Exception {
        var repo = readyRepo();
        Files.writeString(repo.resolve(PLAN), "Mutation gate: `N/A`\n- [x] ticked later\n");

        var result = runScript(repo);

        assertOnlyUnmet(result, 1, PLAN, "uncommitted");
    }

    @Test
    void aFailingGitStatusMakesTheSlicePlanAnUnmetItem() throws Exception {
        var repo = readyRepo();

        var result = runScript(repo, withGitThatFailsOnStatus());

        assertOnlyUnmet(result, 1, PLAN, "could not be checked for uncommitted changes");
    }

    @Test
    void anOpenIndentedStepInTheBuildProgressIsAnUnmetItem() throws Exception {
        var repo = newRepo();
        commitPlan(repo, planWithBuildProgress("- [x] Slice 1\n  - [ ] Step 1.1\n"));
        recordAll(repo, CHECKS);

        var result = runScript(repo);

        assertOnlyUnmet(result, 1, PLAN, "open indented step");
    }

    @Test
    void indentedStepsThatAreDoneAreNotAnUnmetItem() throws Exception {
        var repo = newRepo();
        commitPlan(repo, planWithBuildProgress("- [x] Slice 1\n  - [x] Step 1.1\n"));
        recordAll(repo, CHECKS);

        var result = runScript(repo);

        assertReady(result);
    }

    @Test
    void anOpenIndentedBoxBeforeTheBuildProgressIsNotAnUnmetItem() throws Exception {
        var repo = newRepo();
        commitPlan(
                repo,
                "Mutation gate: `N/A`\n\n  - [ ] a nested note\n\n## Build Progress\n\n- [x] Slice 1\n");
        recordAll(repo, CHECKS);

        var result = runScript(repo);

        assertReady(result);
    }

    @Test
    void aTagWithTheNameOfTheBranchDoesNotHideTheSlicePlan() throws Exception {
        var repo = newRepo();
        git(repo, "tag", BRANCH);
        recordAll(repo, CHECKS);

        var result = runScript(repo);

        assertReady(result);
    }

    @Test
    void aClosingBacktickAfterTheMutationGateIsNotAResult() throws Exception {
        var repo = newRepo();
        commitPlan(repo, "# Plan\n\n`Mutation gate:` is the line to keep.\n");
        recordAll(repo, CHECKS);

        var result = runScript(repo);

        assertOnlyUnmet(result, 1, "Mutation gate");
    }

    @Test
    void aDetachedHeadNamesTheMissingBranch() throws Exception {
        var repo = newRepo();
        git(repo, "checkout", "-q", "--detach");
        recordAll(repo, CHECKS);

        var result = runScript(repo);

        assertOnlyUnmet(result, 1, "no branch");
        assertThat(result.stderr()).doesNotContain("maven", "Mutation gate");
    }

    @Test
    void aRunFromASubdirectoryPassesAndRunsTheGuardianAtTheTopOfTheRepository() throws Exception {
        var repo = readyRepo();
        var subdirectory = Files.createDirectory(repo.resolve("sub dir"));
        var guardian = stubGuardian(dir.resolve("stub.py"), 0, "");

        var result = runScript(subdirectory, withGuardian(guardian));

        assertReady(result);
        assertThat(workingDirectoryOf(guardian)).isEqualTo(repo.toRealPath().toString());
    }

    @Test
    void aRunFromASubdirectoryNamesTheSameUnmetItemsAsARunFromTheTop() throws Exception {
        var repo = repoWithRecords(List.of("maven"));
        var subdirectory = Files.createDirectory(repo.resolve("sub dir"));

        var fromTop = runScript(repo);
        var fromSubdirectory = runScript(subdirectory);

        assertThat(fromSubdirectory).isEqualTo(fromTop);
        assertThat(fromTop.exitCode()).isEqualTo(EXIT_UNMET);
    }

    @Test
    void oneRunNamesEveryKindOfUnmetItem() throws Exception {
        var repo = newRepo();
        commitPlan(repo, "Mutation gate:\n");
        git(repo, "rm", "-q", ARCHITECTURE);
        git(repo, "commit", "-q", "-m", "remove architecture");
        var guardian = stubGuardian(dir.resolve("stub.py"), 1, "uncommitted changes in the tree");

        var otherItems = List.of("Mutation gate", "progress guardian failed", ARCHITECTURE);
        var expectedCount = CHECKS.size() + otherItems.size();

        var result = runScript(repo, withGuardian(guardian));

        assertOnlyUnmet(result, expectedCount, otherItems.toArray(String[]::new));
        assertThat(result.stderr()).contains(CHECKS);
    }

    @Test
    void aRunOutsideARepositoryExitsWithAnError() throws Exception {
        var plainDirectory = Files.createDirectory(dir.resolve("plain"));

        var result = runScript(plainDirectory);

        assertFails(result, "not inside a git repository");
    }

    @Test
    void aRepositoryWithoutACommitExitsWithAnError() throws Exception {
        var repo = initRepo();

        var result = runScript(repo);

        assertFails(result, "no commit");
    }

    @Test
    void theGuardianReceivesThePlanPrePrAndSkipLlm() throws Exception {
        var repo = readyRepo();
        var guardian = stubGuardian(dir.resolve("stub.py"), 0, "");

        var result = runScript(repo, withGuardian(guardian));

        assertReady(result);
        assertThat(argumentsReceivedBy(guardian))
                .isEqualTo("--plan " + PLAN + " --pre-pr --skip-llm");
    }

    @Test
    void aFailingGuardianShowsItsMessageAndOnlyTheGuardianIsNamed() throws Exception {
        var repo = readyRepo();
        var guardian = stubGuardian(dir.resolve("stub.py"), 1, "uncommitted changes in the tree");

        var result = runScript(repo, withGuardian(guardian));

        assertOnlyUnmet(result, 1, "guardian", "uncommitted changes in the tree");
        assertThat(result.stderr()).doesNotContain("maven", "Mutation gate", "slice plan");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void aGuardianThatExitsWithAnyNonZeroStatusIsAnUnmetItem(int exitCode) throws Exception {
        var repo = readyRepo();
        var guardian = stubGuardian(dir.resolve("stub.py"), exitCode, "internal error");

        var result = runScript(repo, withGuardian(guardian));

        assertOnlyUnmet(result, 1, "guardian");
    }

    @Test
    void aGuardianPathThatDoesNotExistIsAnUnmetItemNamingTheGuardian() throws Exception {
        var repo = readyRepo();

        var result = runScript(repo, withGuardian(dir.resolve("missing.py")));

        assertOnlyUnmet(result, 1, "progress guardian not found");
        assertThat(result.stderr()).doesNotContain("maven", "Mutation gate");
    }

    @Test
    void theGuardianIsFoundThroughTheInstalledPluginsFile() throws Exception {
        var repo = readyRepo();
        var pluginDirectory = dir.resolve("plugin");
        var guardian =
                stubGuardian(
                        pluginDirectory.resolve("scripts").resolve("progress_guardian.py"), 0, "");
        writeInstalledPlugins(pluginDirectory);

        var result = runScript(repo, withoutGuardianVariable());

        assertReady(result);
        assertThat(argumentsReceivedBy(guardian)).contains("--pre-pr");
    }

    @Test
    void noGuardianVariableAndNoInstalledPluginsFileIsAnUnmetItemNamingTheGuardian()
            throws Exception {
        var repo = readyRepo();

        var result = runScript(repo, withoutGuardianVariable());

        assertOnlyUnmet(result, 1, "set PR_READY_PROGRESS_GUARDIAN");
        assertThat(result.stderr()).doesNotContain("maven", "Mutation gate");
    }

    @Test
    void anUnexpectedGitFailureExitsWithAnErrorAndNotWithTheUnmetCode() throws Exception {
        var repo = readyRepo();

        var result = runScript(repo, withGitThatFailsOnGitPath());

        assertFails(result, "unexpected failure");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "git push",
                "git  push",
                "git push -u origin x",
                "git -C dir push",
                "git -c k=v push",
                "/usr/bin/git push",
                "FOO=1 git push",
                "env git push",
                "ls; git push",
                "ls\ngit push",
                "cd a && gh pr create --draft",
                "gh -R o/r pr create",
                "echo $(git push)",
                "git -C \"/a b/c\" push",
                "git -C '/a b' push",
                "FOO=\"a b\" git push",
                "git -C /a\\ b push",
                "git -C \"/a b\"/c push",
                "git -c k='a b' push",
                "FOO=a\\ b git push",
                "gh pr new --draft",
                "false || git push",
                "git -C dir -c k=v push",
                "git --no-pager push",
                "git --git-dir=\"/a b\" push",
                "git --work-tree='/a b' push",
                "git -C\"/a b\" push"
            })
    void hookBlocksAPushOrPullRequestCommandWhileItemsAreUnmet(String command) throws Exception {
        var repo = newRepo();

        var result = runHookWithCommand(repo, command);

        assertHookBlocksAllChecks(result);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "ls",
                "git status",
                "gh pr view",
                "git pushx",
                "git commit -m \"add git push gate\""
            })
    void hookLetsOtherCommandsPassSilently(String command) throws Exception {
        var repo = newRepo();

        var result = runHookWithCommand(repo, command);

        assertPassesSilently(result);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "bash -c \"git push\"",
                "sh -c \"git push\"",
                "if x; then git push; fi",
                "echo `git push`",
                "{ git push; }",
                "git \\\npush",
                "gh \\\npr create",
                "gh pr \\\ncreate",
                "gh pr \\\nnew"
            })
    void hookDoesNotGateWrappedPushes(String command) throws Exception {
        var repo = newRepo();

        var result = runHookWithCommand(repo, command);

        assertPassesSilently(result);
    }

    @Test
    void hookLetsAPushPassOnAReadyRepository() throws Exception {
        var repo = readyRepo();

        var result = runHookWithCommand(repo, "git push");

        assertPassesSilently(result);
    }

    @Test
    void hookBlocksMalformedJsonThatContainsAPush() throws Exception {
        var repo = newRepo();

        var result =
                runHookWithStdin(
                        repo, environment -> {}, "{\"tool_input\":{\"command\":\"git push");

        assertHookBlocksAllChecks(result);
    }

    @Test
    void hookBlocksMalformedJsonWithAQuotedPathBeforeAPush() throws Exception {
        var repo = newRepo();

        var result =
                runHookWithStdin(
                        repo,
                        environment -> {},
                        "{\"tool_input\":{\"command\":\"git -C \\\"/a b\\\" push");

        assertHookBlocksAllChecks(result);
    }

    @Test
    void hookLetsMalformedJsonWithoutAPushPass() throws Exception {
        var repo = newRepo();

        var result = runHookWithStdin(repo, environment -> {}, "{\"tool_input\":{\"command\":\"ls");

        assertPassesSilently(result);
    }

    @Test
    void hookBlocksAPushWhenTheGuardianCannotRun() throws Exception {
        var repo = readyRepo();

        var result =
                runHookWithStdin(
                        repo, withGuardian(dir.resolve("missing.py")), toHookJson("git push"));

        assertHookBlocks(result, "progress guardian not found");
    }

    @Test
    void hookTurnsAnUnexpectedGitFailureIntoTheBlockExitCode() throws Exception {
        var repo = readyRepo();

        var result = runHookWithStdin(repo, withGitThatFailsOnGitPath(), toHookJson("git push"));

        assertHookBlocks(result, "unexpected failure");
    }

    @Test
    void hookOutsideARepositoryLetsOtherCommandsPass() throws Exception {
        var plainDirectory = Files.createDirectory(dir.resolve("plain"));

        var result = runHookWithCommand(plainDirectory, "ls");

        assertPassesSilently(result);
    }

    @Test
    void hookOutsideARepositoryBlocksAPush() throws Exception {
        var plainDirectory = Files.createDirectory(dir.resolve("plain"));

        var result = runHookWithCommand(plainDirectory, "git push");

        assertHookBlocks(result, "not inside a git repository");
    }

    @Test
    void recordWithMultiLineEvidenceExitsWithAnErrorAndWritesNothing() throws Exception {
        var repo = newRepo();

        var result = runScript(repo, "record", "pmd", "ok\nmaven x");

        assertFails(result, "one line");
        assertThat(repo.resolve(".git").resolve("pr-ready")).doesNotExist();
    }

    @Test
    void recordWithBlankEvidenceExitsWithAnErrorAndWritesNothing() throws Exception {
        var repo = newRepo();

        var result = runScript(repo, "record", "maven", "  \t ");

        assertFails(result, "no evidence");
        assertThat(repo.resolve(".git").resolve("pr-ready")).doesNotExist();
    }

    @Test
    void recordFromASubdirectoryCountsForTheWholeRepository() throws Exception {
        var repo = newRepo();
        var subdirectory = Files.createDirectory(repo.resolve("sub dir"));
        recordAll(subdirectory, CHECKS);

        var result = runScript(repo);

        assertReady(result);
    }

    @Test
    void recordWithMultiWordEvidenceWritesExactlyOneLineForTheHeadTree() throws Exception {
        var repo = newRepo();

        var result = runScript(repo, "record", "maven", "ran", "ok");

        assertReady(result);
        var tree = git(repo, "rev-parse", "HEAD^{tree}").trim();
        assertThat(Files.readString(repo.resolve(".git").resolve("pr-ready").resolve(tree)))
                .isEqualTo("maven ran ok\n");
    }

    @Test
    void anInstalledPluginsFileWithoutTheDevTeamPluginIsAnUnmetItemNamingTheGuardianVariable()
            throws Exception {
        var repo = readyRepo();
        writeInstalledPluginsFile("{\"version\": 2, \"plugins\": {}}");

        var result = runScript(repo, withoutGuardianVariable());

        assertOnlyUnmet(result, 1, "set PR_READY_PROGRESS_GUARDIAN");
        assertThat(result.stderr()).doesNotContain("maven", "Mutation gate");
    }

    @Test
    void recordsForADeletedTrackedFileCountAfterCommitAll() throws Exception {
        var repo = newRepo();
        Files.delete(repo.resolve("tracked.txt"));
        recordAll(repo, CHECKS);
        git(repo, "commit", "-q", "-a", "-m", "delete");

        var result = runScript(repo);

        assertReady(result);
    }

    @Test
    void recordOutsideARepositoryExitsWithAnError() throws Exception {
        var plainDirectory = Files.createDirectory(dir.resolve("plain"));

        var result = runScript(plainDirectory, "record", "maven", EVIDENCE);

        assertFails(result, "not inside a git repository");
    }

    @Test
    void aSlicePlanMissingFromHeadDoesNotRunTheGuardian() throws Exception {
        var repo = newRepo();
        removePlanFromHead(repo);
        recordAll(repo, CHECKS);
        var guardian = stubGuardian(dir.resolve("stub.py"), 0, "");

        var result = runScript(repo, withGuardian(guardian));

        assertOnlyUnmet(result, 1, PLAN);
        assertThat(Path.of(guardian + ".args")).doesNotExist();
    }

    @Test
    void hookReadsTheCommandFromTheJsonAndNotFromOtherFields() throws Exception {
        var repo = newRepo();

        var result =
                runHookWithStdin(
                        repo,
                        environment -> {},
                        "{\"tool_input\":{\"command\":\"ls\"},\"description\":\"git push\"}");

        assertPassesSilently(result);
    }

    @Test
    void hookBlocksMalformedJsonWithAnEscapedNewlineBeforeAPush() throws Exception {
        var repo = newRepo();

        var result =
                runHookWithStdin(
                        repo, environment -> {}, "{\"tool_input\":{\"command\":\"ls\\ngit push");

        assertHookBlocksAllChecks(result);
    }

    private static void assertSucceeds(Result result) {
        assertThat(result.exitCode()).as(result.stderr()).isZero();
    }

    private static void assertReady(Result result) {
        assertSucceeds(result);
    }

    private static void assertFails(Result result, String... named) {
        assertThat(result.exitCode()).as(result.stderr()).isEqualTo(EXIT_ERROR);
        assertThat(result.stderr()).as(result.stderr()).contains(named);
    }

    private static void assertAllChecksUnmet(Result result) {
        assertThat(result.exitCode()).as(result.stderr()).isEqualTo(EXIT_UNMET);
        assertThat(result.stderr()).as(result.stderr()).contains(CHECKS);
    }

    private static void assertHookBlocks(Result result, String... named) {
        assertThat(result.exitCode()).as(result.stderr()).isEqualTo(EXIT_BLOCK);
        assertThat(result.stderr()).as(result.stderr()).contains(named);
    }

    private static void assertHookBlocksAllChecks(Result result) {
        assertHookBlocks(result, CHECKS.toArray(String[]::new));
    }

    private static void assertPassesSilently(Result result) {
        assertReady(result);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).isEmpty();
    }

    private static void assertOnlyUnmet(Result result, long messageCount, String... named) {
        assertThat(result.exitCode()).as(result.stderr()).isEqualTo(EXIT_UNMET);
        assertThat(result.stderr()).contains(named);
        assertThat(countMessageLines(result.stderr())).as(result.stderr()).isEqualTo(messageCount);
    }

    private static long countMessageLines(String stderr) {
        return stderr.lines().filter(line -> line.startsWith("pr-ready:")).count();
    }

    private Result runHookWithCommand(Path workingDirectory, String command) throws Exception {
        return runHookWithStdin(workingDirectory, environment -> {}, toHookJson(command));
    }

    private Result runHookWithStdin(
            Path workingDirectory, Consumer<Map<String, String>> customize, String stdin)
            throws Exception {
        var input = Files.writeString(Files.createTempFile(dir, "stdin", ".json"), stdin);
        return run(
                workingDirectory,
                List.of("/bin/bash", SCRIPT.toString(), "--hook"),
                customize,
                input);
    }

    private static String toHookJson(String command) {
        var escaped = command.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return "{\"tool_input\":{\"command\":\"" + escaped + "\"}}";
    }

    private Consumer<Map<String, String>> withGuardian(Path guardian) {
        return environment -> environment.put("PR_READY_PROGRESS_GUARDIAN", guardian.toString());
    }

    private Consumer<Map<String, String>> withoutGuardianVariable() {
        return environment -> environment.remove("PR_READY_PROGRESS_GUARDIAN");
    }

    private void writeInstalledPlugins(Path installPath) throws IOException {
        writeInstalledPluginsFile(
                """
                {"version": 2, "plugins": {"dev-team@bfinster": [{"installPath": "%s"}]}}
                """
                        .formatted(installPath));
    }

    private void writeInstalledPluginsFile(String json) throws IOException {
        var plugins = dir.resolve("home").resolve(".claude").resolve("plugins");
        Files.createDirectories(plugins);
        Files.writeString(plugins.resolve("installed_plugins.json"), json);
    }

    private Consumer<Map<String, String>> withGitThatFailsOnGitPath() {
        return withGitThatFailsWhen("[ \"$2\" = \"--git-path\" ]");
    }

    private Consumer<Map<String, String>> withGitThatFailsOnStatus() {
        return withGitThatFailsWhen("[ \"$1\" = \"status\" ]");
    }

    private Consumer<Map<String, String>> withGitThatFailsWhen(String condition) {
        return environment -> {
            try {
                var realGit = run(dir, List.of("/bin/sh", "-c", "command -v git")).stdout().trim();
                var wrapper = dir.resolve("fake-bin").resolve("git");
                Files.createDirectories(wrapper.getParent());
                Files.writeString(
                        wrapper,
                        """
                        #!/bin/sh
                        %s && exit 2
                        exec "%s" "$@"
                        """
                                .formatted(condition, realGit));
                assertThat(wrapper.toFile().setExecutable(true)).isTrue();
                environment.put("PATH", wrapper.getParent() + ":" + environment.get("PATH"));
            } catch (IOException | InterruptedException e) {
                throw new AssertionError(e);
            }
        };
    }

    private Path passingGuardian() throws IOException {
        var guardian = dir.resolve("passing-guardian.py");
        return Files.exists(guardian) ? guardian : stubGuardian(guardian, 0, "");
    }

    private Path stubGuardian(Path file, int exitCode, String message) throws IOException {
        Files.createDirectories(file.getParent());
        return Files.writeString(
                file,
                """
                import os, pathlib, sys
                pathlib.Path(sys.argv[0] + ".args").write_text(" ".join(sys.argv[1:]))
                pathlib.Path(sys.argv[0] + ".cwd").write_text(os.getcwd())
                sys.stderr.write("%s\\n")
                sys.exit(%d)
                """
                        .formatted(message, exitCode));
    }

    private String argumentsReceivedBy(Path guardian) throws IOException {
        return Files.readString(Path.of(guardian + ".args"));
    }

    private String workingDirectoryOf(Path guardian) throws IOException {
        return Files.readString(Path.of(guardian + ".cwd"));
    }

    private Path scriptTmpdir() throws IOException {
        return Files.createDirectories(dir.resolve("tmp"));
    }

    private Path readyRepo() throws Exception {
        return repoWithRecords(CHECKS);
    }

    private Path repoWithRecords(List<String> checks) throws Exception {
        var repo = newRepo();
        recordAll(repo, checks);
        return repo;
    }

    private void recordAll(Path workingDirectory, List<String> checks) throws Exception {
        for (var check : checks) {
            assertSucceeds(runScript(workingDirectory, "record", check, EVIDENCE));
        }
    }

    private Path newRepo() throws Exception {
        var repo = initRepo();
        Files.writeString(repo.resolve("tracked.txt"), "one\n");
        Files.createDirectory(repo.resolve("plans"));
        Files.writeString(repo.resolve(PLAN), "Mutation gate: `N/A` until a tool is chosen.\n");
        Files.createDirectory(repo.resolve("docs"));
        Files.writeString(repo.resolve(ARCHITECTURE), "# Architecture\n");
        git(repo, "add", "tracked.txt", PLAN, ARCHITECTURE);
        git(repo, "commit", "-q", "-m", "initial");
        return repo;
    }

    private Path initRepo() throws Exception {
        var repo = Files.createDirectory(dir.resolve("ready repo"));
        git(repo, "init", "-q", "-b", BRANCH);
        git(repo, "config", "user.name", "Test");
        git(repo, "config", "user.email", "test@example.com");
        git(repo, "config", "commit.gpgsign", "false");
        return repo;
    }

    private static String planWithBuildProgress(String steps) {
        return "Mutation gate: `N/A`\n\n## Build Progress\n\n" + steps;
    }

    private void commitPlan(Path repo, String content) throws Exception {
        Files.writeString(repo.resolve(PLAN), content);
        git(repo, "commit", "-q", "-a", "-m", "plan");
    }

    private void removePlanFromHead(Path repo) throws Exception {
        git(repo, "rm", "-q", PLAN);
        git(repo, "commit", "-q", "-m", "remove plan");
    }

    private String git(Path repo, String... args) throws Exception {
        var command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(args));
        var result = run(repo, command);
        assertSucceeds(result);
        return result.stdout();
    }

    private Result runScript(Path workingDirectory, String... args) throws Exception {
        return runScript(workingDirectory, environment -> {}, args);
    }

    private Result runScript(
            Path workingDirectory, Consumer<Map<String, String>> customize, String... args)
            throws Exception {
        var command = new ArrayList<>(List.of("/bin/bash", SCRIPT.toString()));
        command.addAll(List.of(args));
        return run(workingDirectory, command, customize, null);
    }

    private Result run(Path workingDirectory, List<String> command)
            throws IOException, InterruptedException {
        return run(workingDirectory, command, environment -> {}, null);
    }

    private Result run(
            Path workingDirectory,
            List<String> command,
            Consumer<Map<String, String>> customize,
            Path stdin)
            throws IOException, InterruptedException {
        var stdout = Files.createTempFile(dir, "stdout", ".txt");
        var stderr = Files.createTempFile(dir, "stderr", ".txt");
        var builder =
                new ProcessBuilder(command)
                        .directory(workingDirectory.toFile())
                        .redirectOutput(stdout.toFile())
                        .redirectError(stderr.toFile());
        if (stdin != null) {
            builder.redirectInput(stdin.toFile());
        }
        var environment = builder.environment();
        environment.keySet().removeIf(name -> name.startsWith("GIT_"));
        environment.put("GIT_CONFIG_GLOBAL", "/dev/null");
        environment.put("GIT_CONFIG_NOSYSTEM", "1");
        environment.put("GIT_CEILING_DIRECTORIES", dir.toRealPath().getParent().toString());
        environment.put("TMPDIR", scriptTmpdir().toString());
        environment.put("HOME", Files.createDirectories(dir.resolve("home")).toString());
        environment.put("PR_READY_PROGRESS_GUARDIAN", passingGuardian().toString());
        customize.accept(environment);
        var process = builder.start();
        if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new AssertionError("Timed out: " + command);
        }
        return new Result(process.exitValue(), Files.readString(stdout), Files.readString(stderr));
    }

    private record Result(int exitCode, String stdout, String stderr) {}
}
