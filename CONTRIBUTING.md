# Contributing

Standards live in `docs/CONVENTIONS.md`; durable agent policy in `AGENTS.md`.

`docs/local/` is gitignored scratch space whose contents are never committed.

Use one branch per change (`feature/<short-name>`, `fix/<short-name>`) with Conventional Commits
(imperative, lower-case subject, no trailing period), squash-merged into `main`.

Run `make lint` and `make test` before committing. Never commit without the maintainer's explicit
acknowledgement.
