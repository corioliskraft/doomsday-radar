# Doomsday Radar

A public website that compares the current aircraft count per region with a baseline. More signal sources follow later. The product vision is in [docs/vision.md](docs/vision.md), and the first version is described in [docs/walking-skeleton.md](docs/walking-skeleton.md).

## Stack

- Java 25.
- Now: Docker, Kafka, and ClickHouse.
- Later, each when a slice needs it: Spark, S3, Kubernetes.

## Workflow

dev-team is the main workflow. For each task, use the installed skill that fits it best, and name it in one line. superpowers is turned off for this project. These rules apply to all tasks:

- Work in slices, one slice at a time.
- Each behavior change starts from a failing test, run with the project's Java test runner (`tdd`). Until a JVM mutation tool is chosen, record the mutation gate as `N/A` with the alternate evidence that `tdd` allows.

Commits: the agent commits after the user approves the commit. The agent also creates branches, pushes, and opens pull requests, each after the user approves it. This overrides the global rule that the user runs git writes. The commit message has no `Co-Authored-By` line and does not name the agent.

Before a slice that depends on external data, run a quick check with real data and record the result in the slice plan.

## Writing

Write thin text: documents, code comments, and pull request descriptions. Long text goes unread, and filler hides the part that matters. Tell the reader something that the code, the diff, or the text above does not show. One short sentence is often enough. Use more when the content needs more.

A commit message is the subject line only. Add a body only for a reason that the diff cannot show, in one or two sentences.

A document is short and has short sections. It holds what is important for the reader, for example why the team took a decision or how the system works.

Documents:

- Architecture decisions: ADRs in `docs/adr/`, written with the `adr` agent, only for decisions that its rules say need one.
- Architecture overview: `docs/architecture.md`, made with the `diagrams` skill. It shows only components that exist in the project.

## Stack skills

Add each one when its component enters the project:

- Kafka: the rules of `developing-kafka-java-client` from [confluentinc/agent-skills](https://github.com/confluentinc/agent-skills), as a reference. Its project generator writes tests after code, so `tdd` governs the build.
- Docker: `dev-team:docker-image-create` and `dev-team:docker-image-audit`.
- Kubernetes, Flink, Spark, S3: the official documentation. Each Kubernetes deployment sets resource requests and limits, liveness and readiness probes, and a non-root container.
