# Publishing anonymously

Checklist for pushing a project publicly without identifying yourself. Do the "before the first
commit" part when the repo is young; the "before the first public push" part matters when a
repository with existing history goes public. On this machine the `git-privacy` skill's scripts
automate the scan and scrub steps — the checklist below stands with or without them.

Once a repository is public its history is permanent: anything mirrored, forked, or indexed can
never be fully recalled. Prevent, don't cure.

## The six leak channels

1. **Commit metadata** — author, committer and tagger name + email on every commit and annotated
   tag.
2. **Timestamps** — date offsets (`+0100`) reveal your region; normalize them to `+0000`.
3. **Text in tracked files** — names, emails, usernames, personal domains, absolute home paths —
   in the current tree *and* in every historical revision.
4. **Accidentally committed private files** — plans, screenshots, `.env`, data dumps. Deleting
   them later does not remove them from history.
5. **Repo-level metadata** — the LICENSE copyright holder, `package.json` `author`, docs carrying
   account URLs, SSH/GPG key comments and commit signatures.
6. **Outside git** — the hosting account name (it appears in every clone URL), profile email,
   reused avatars and usernames, donation platforms (all require KYC).

## Before the first commit

- Set a **repo-local identity** so commits can never pick up your personal global git config:

  ```sh
  git config user.name "Project Dev"
  git config user.email "dev@localhost"
  ```

  Never sign commits — GPG and SSH signatures embed or expose the signer's identity.
- Keep a deny list of personal strings in `docs/local/privacy.json` (`deny` = strings that must
  never appear, `replace` = what to rewrite them to) and install a pre-commit hook that scans
  staged changes against it.
- The LICENSE holder is neutral — `<project> contributors` — and `docs/SECURITY.md` reports
  problems through anonymous channels (repository issues or an anonymous email), never a personal
  address.

## Before the first public push (existing history)

1. Commit or stash everything — rewriting does not preserve uncommitted work.
2. Scan every revision for the six channels and build the deny/replace map from what it finds.
3. Scrub with `git-filter-repo`: rewrite author, committer and tagger to the neutral identity,
   normalize date offsets to `+0000`, apply the replace map to every blob **and** to commit/tag
   messages, and drop private paths from every revision.
4. Purge old objects locally: `git reflog expire --expire=now --all && git gc --prune=now`.
5. Re-scan — it must report clean — then push branches and tags to the new home. Every hash has
   changed; anyone with a clone must re-clone.

## Where it lands

- A **fresh account** on the host, created with an anonymous email (Proton, Tuta, or an alias);
  never a reused username, avatar, or password. Hide the profile email — use the platform's
  noreply address.
- Host the repository under a **project or organization name**, not a personal account: the
  account name appears in every clone command.
- A fresh SSH key with a neutral comment (`ssh-keygen -c -f key` edits an existing one — comments
  often read `user@hostname` and are visible in the account's key list).
- Mirror, don't move: pushing to a fresh host transfers only the clean history; old unreachable
  objects stay behind on old hosts. Old remotes stay private forever.

## Verification

```sh
git log --format='%an <%ae> | %cn <%ce>' | sort -u               # only the neutral identity
git log --format='%ad' --date=raw | awk '{print $2}' | sort -u   # only +0000
git grep -i -e <name> -e <email> -e <username> $(git rev-list --all)   # empty
git log --all --diff-filter=ADR --name-only --format= | sort -u  # no private files
```

## If you later want credit

Attaching a real name later is a one-line LICENSE change plus future commits. Going the other way
after publishing is possible but never complete — treat anything public as permanent.
