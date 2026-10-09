#!/bin/bash
set -eu

CHECKS="maven pmd adr farley grok"

ARCHITECTURE_DOC="docs/architecture.md"
GUARDIAN_PLUGIN="dev-team@bfinster"
GUARDIAN_SCRIPT="scripts/progress_guardian.py"
INSTALLED_PLUGINS="${HOME:-}/.claude/plugins/installed_plugins.json"

MUTATION_GATE_PATTERN='Mutation gate:([^`[:space:]]|[[:space:]]+[^[:space:]])'
BUILD_PROGRESS_RANGE='/^## Build Progress/,$p'
OPEN_INDENTED_STEP_PATTERN='^[[:space:]]+-[[:space:]]+\[ \]'

EXIT_ERROR=1
EXIT_UNMET=2
EXIT_BLOCK=2

HAS_UNMET_ITEMS=0
IS_INTENDED_EXIT=0
PLAN=""

map_unexpected_exit() {
    local status=$?
    if [ "$status" -ne 0 ] && [ "$status" -ne "$EXIT_ERROR" ] && [ "$IS_INTENDED_EXIT" -eq 0 ]; then
        echo "pr-ready: unexpected failure, exit status $status" >&2
        exit "$EXIT_ERROR"
    fi
}
trap map_unexpected_exit EXIT

die() {
    echo "pr-ready: $*" >&2
    exit "$EXIT_ERROR"
}

is_known_check() {
    local known
    for known in $CHECKS; do
        [ "$known" = "$1" ] && return 0
    done
    return 1
}

enter_repository() {
    local top
    top=$(git rev-parse --show-toplevel 2>/dev/null) || die "not inside a git repository"
    cd "$top"
}

records_dir() {
    git rev-parse --git-path pr-ready
}

tree_after_commit_all() {
    (
        local index_copy
        index_copy=$(mktemp "${TMPDIR:-/tmp}/pr-ready-index.XXXXXX") || exit 1
        trap 'rm -f "$index_copy"' EXIT
        cp -p "$(git rev-parse --git-path index)" "$index_copy" || exit 1
        GIT_INDEX_FILE="$index_copy" git add -u || exit 1
        GIT_INDEX_FILE="$index_copy" git write-tree
    )
}

record() {
    local check="${1:-}"
    local evidence="${*:2}"
    is_known_check "$check" || die "unknown check '$check'; known checks: $CHECKS"
    [ -n "${evidence//[[:space:]]/}" ] || die "no evidence for check '$check'"
    case "$evidence" in
        *$'\n'*) die "evidence must be one line" ;;
    esac
    enter_repository
    local dir tree
    dir=$(records_dir)
    tree=$(tree_after_commit_all)
    mkdir -p "$dir"
    printf '%s %s\n' "$check" "$evidence" >>"$dir/$tree"
}

is_recorded() {
    local file="$1" check="$2"
    [ -f "$file" ] && grep -q -- "^$check " "$file"
}

exit_intended() {
    IS_INTENDED_EXIT=1
    exit "$1"
}

report_unmet() {
    echo "pr-ready: $*" >&2
    HAS_UNMET_ITEMS=1
}

check_records() {
    local file check
    file="$(records_dir)/$1"
    for check in $CHECKS; do
        is_recorded "$file" "$check" || report_unmet "no record for $check"
    done
}

check_plan() {
    local branch plan content changes
    branch=$(git symbolic-ref --quiet HEAD) ||
        { report_unmet "no branch checked out, so the slice plan path is unknown"; return 0; }
    plan="plans/${branch#refs/heads/}.md"
    content=$(git show "HEAD:$plan" 2>/dev/null) ||
        { report_unmet "slice plan $plan is not in HEAD"; return 0; }
    PLAN="$plan"
    changes=$(git status --porcelain -- "$plan") ||
        { report_unmet "slice plan $plan could not be checked for uncommitted changes"; changes=""; }
    [ -z "$changes" ] ||
        report_unmet "slice plan $plan has uncommitted changes"
    printf '%s\n' "$content" | grep -Eq "$MUTATION_GATE_PATTERN" ||
        report_unmet "$PLAN has no text after 'Mutation gate:'"
    printf '%s\n' "$content" | sed -n "$BUILD_PROGRESS_RANGE" | grep -Eq "$OPEN_INDENTED_STEP_PATTERN" &&
        report_unmet "$PLAN has an open indented step in Build Progress"
    return 0
}

installed_guardian() {
    local install_path
    install_path=$(jq -r --arg plugin "$GUARDIAN_PLUGIN" \
        '.plugins[$plugin][0].installPath // empty' "$INSTALLED_PLUGINS" 2>/dev/null) || return 1
    [ -n "$install_path" ] || return 1
    echo "$install_path/$GUARDIAN_SCRIPT"
}

find_guardian() {
    if [ -n "${PR_READY_PROGRESS_GUARDIAN:-}" ]; then
        echo "$PR_READY_PROGRESS_GUARDIAN"
    else
        installed_guardian
    fi
}

check_guardian() {
    [ -n "$PLAN" ] || return 0
    local guardian output status=0
    guardian=$(find_guardian) ||
        { report_unmet "progress guardian not found: set PR_READY_PROGRESS_GUARDIAN"; return 0; }
    [ -f "$guardian" ] || { report_unmet "progress guardian not found at $guardian"; return 0; }
    output=$(python3 "$guardian" --plan "$PLAN" --pre-pr --skip-llm 2>&1) || status=$?
    [ "$status" -eq 0 ] && return 0
    report_unmet "progress guardian failed with exit status $status"
    [ -z "$output" ] || printf '%s\n' "$output" >&2
}

check_architecture() {
    local size
    size=$(git cat-file -s "HEAD:$ARCHITECTURE_DOC" 2>/dev/null) ||
        { report_unmet "$ARCHITECTURE_DOC is not in HEAD"; return 0; }
    [ "$size" -gt 0 ] || report_unmet "$ARCHITECTURE_DOC is empty"
}

check_ready() {
    enter_repository
    local head_tree
    head_tree=$(git rev-parse --verify --quiet 'HEAD^{tree}') || die "no commit in this repository"
    check_records "$head_tree"
    check_plan
    check_guardian
    check_architecture
    [ "$HAS_UNMET_ITEMS" -eq 0 ] || exit_intended "$EXIT_UNMET"
}

is_gated_command() {
    local sq="'"
    local quoted="\"[^\"]*\"|${sq}[^${sq}]*${sq}"
    local escaped='\\.'
    local plain="[^[:space:]\\\\\"${sq}]"
    local part="(${plain}|${escaped}|${quoted})"
    local option_argument="([^-[:space:]\\\\\"${sq}]|${escaped}|${quoted})${part}*"
    local start='(^|[;&|(])[[:space:]]*'
    local prefix="(env[[:space:]]+|[A-Za-z_][A-Za-z0-9_]*=${part}*[[:space:]]+)*([^[:space:];&|()]*/)?"
    local options="([[:space:]]+-(${part})+([[:space:]]+(${option_argument}))?)*"
    local end='([^[:alnum:]_-]|$)'
    local git_push="${start}${prefix}git${options}[[:space:]]+push${end}"
    local gh_pr_create="${start}${prefix}gh${options}[[:space:]]+pr[[:space:]]+(create|new)${end}"
    printf '%s\n' "$1" | grep -Eq -e "$git_push" -e "$gh_pr_create"
}

hook_command() {
    printf '%s' "$1" | jq -r '.tool_input.command // empty' 2>/dev/null ||
        printf '%s' "$1" | sed 's/\\"/@@QUOTE@@/g; s/\\n/;/g; s/"/;/g; s/@@QUOTE@@/"/g'
}

hook() {
    local input status
    input=$(cat)
    is_gated_command "$(hook_command "$input")" || exit 0
    set +e
    ( trap map_unexpected_exit EXIT; set -e; check_ready )
    status=$?
    set -e
    [ "$status" -eq 0 ] && exit 0
    exit_intended "$EXIT_BLOCK"
}

case "${1:-}" in
    "") check_ready ;;
    --hook) hook ;;
    record) shift && record "$@" ;;
    *) die "usage: pr-ready.sh [record <check> <evidence> | --hook]" ;;
esac
