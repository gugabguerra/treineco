# Conventions

Human-facing standards for this project. Durable agent policy lives in `AGENTS.md`, which defers
here; if the two ever disagree, fix both.

## Git workflow

- `main` is stable. One branch per change: `feature/<short-name>`, `fix/<short-name>`.
  Squash-merge into `main`, then delete the branch.
- Conventional Commits: `feat:`, `fix:`, `refactor:`, `chore:`, `docs:`. Subject in the imperative,
  lower-case, no trailing period.
- Tag stable checkpoints: `git tag -a vX.Y.Z -m "..."`. The changelog's version section is added at
  the tag; versions match the git tags and the root `VERSION` file.
- Never commit without the maintainer's explicit acknowledgement.
- Branch from the **merged tip**. Squash-merging a stack of branches conflicts (both sides re-add
  the same files); merge each branch and delete it before branching the next.
- A release push may prefix branches (`v1-feature/<short-name>`) and drop the prefix after the
  release.

## Versioning & release

- The root **`VERSION` file is the single source of truth**: at a release it matches the git tag
  (`vX.Y.Z`) and the changelog section — the three move together, and nothing else hardcodes a
  version (build scripts inject it: Go `-ldflags`, a synced `package.json`, and so on).
- The changelog's `[Unreleased]` becomes the version section at the tag.
- The `[Unreleased]` section is the norm for small projects. If maintaining it becomes toil,
  generate entries from Conventional Commits at release time (e.g. `git-cliff`) and hand-edit the
  highlights.
- Release artifacts — binaries, container images, app bundles — are built by CI from the tag and
  published as release assets. **Never commit them**: `dist/`, `build/`, and `out/` stay ignored.

## Documentation routing

| Change                              | Document                                  |
|-------------------------------------|-------------------------------------------|
| User-visible behavior               | `README.md` § Features                    |
| Support / maintenance posture       | `README.md` § Project status              |
| Storage, permissions, or network    | `docs/SECURITY.md`                        |
| Release notes                       | `docs/CHANGELOG.md` under `[Unreleased]`  |
| Narrow implementation details       | code comments and unit tests              |
| Planning, issues, private notes     | `docs/local/` (gitignored, never commit)  |
| License or copyright holder         | `LICENSE` + `README.md` § License         |
| Publishing without identifying you  | `docs/PUBLISHING-ANONYMOUSLY.md`          |

## Markdown style

- Wrap prose at 100 columns; keep code blocks and tables unchanged.
- Headings sentence case. Feature bullets describe user-visible behavior, one to three lines each.


## Code style

- Kotlin + Jetpack Compose. Official Kotlin code style (`kotlin.code.style=official`).
- Pure rules over injected time — logic that depends on "now" takes the clock as a parameter so it
  is testable without waiting.
- Atomic persistence: write to a temp file and rename; coalesce bursts of saves into one write so a
  crash never leaves a half-written file.
- Never log user data (plans, weights, notes).
- One vocabulary across layers: the same concept keeps the same name in the model, the JSON file,
  and the UI. Renaming a JSON key is a breaking change for saved data — read the old key too.
- Data-model rules (the `Data.*` functions in `Model.kt`) stay pure so it runs as a plain JVM unit test.
- Formatters and linters are canonical — run them before every commit.
  Android Lint (`make lint`) is the linter; no formatter is configured yet.

## Dependencies

- Every dependency earns its place — prefer the standard library, and justify additions in the
  commit or PR body.
- Manifests declare ranges; lockfiles are pinned and committed.
- Automated dependency-update PRs (Dependabot / Renovate) are welcome — merge when CI is green,
  security updates first.
- Scheduled version-update PRs are a standing chore. Prefer **security-only** updates (GitHub's
  Dependabot security updates need no config file) and bump versions only when a change needs
  them — unless the project is actively maintained and wants the noise.


## Tests

- One test file per module under `app/src/test/` (JVM) or `app/src/androidTest/` (device),
  named after it (`Model.kt` → `ModelTest.kt`).
- Prefer pure unit tests of the rules; integration tests go through the public API surface.
- Suites must be independent of machine state and of each other; use temp directories.
- Assertions state their intent in the message argument so a failure reads as a sentence.
