# Revision Log

A chronological record of significant development decisions and changes to
ADAPT, and — more importantly — the mistakes made and lessons learned, so that
the modernisation of this repository does not repeat the missteps already
documented for its two upstream siblings
([`IAClassLibrary`](https://github.com/djpbarry/IAClassLibrary/blob/development/REVISION_LOG.md)
and
[`TrackerLibrary`](https://github.com/djpbarry/TrackerLibrary/blob/development/REVISION_LOG.md)).

ADAPT is the **end consumer** of the chain (`IAClassLibrary` →
`TrackerLibrary` → `AdaptDataProcessing` → ADAPT): it is a Fiji/ImageJ plugin,
not a library, so the "public API" concerns that dominate the sibling logs
apply only lightly here. Its modernisation is instead about robustness,
testability, and UX, as laid out in `DEVELOPMENT_PLAN.md`.

Git commit messages provide the fine-grained record; this file is the distilled,
dated narrative plus the "what not to do again" notes.

## Conventions

- Entries are dated and reference commits and/or `DEVELOPMENT_PLAN.md` phases.
- "Lesson" entries state what went wrong and the rule to apply next time.
- Lessons marked *(inherited)* are carried forward from the sibling
  `REVISION_LOG.md` files, whose modernisation explicitly lists ADAPT as the
  final downstream consumer and intends these rules to guide it.

---

## 2026-09-27 — Review & plan kick-off; M1 build tooling landed (partial)

Initial full review of ADAPT and creation of the modernisation plan
(`AGENTS.md` + `DEVELOPMENT_PLAN.md`). This is the last of the three siblings
to be brought into the coordinated pass, so the plan adopts the
already-resolved sibling decisions (GPL-3.0, JitPack pin-to-tags) but **diverges
deliberately on the Java target** (see Decision 3 below).

### What has already landed (build/tooling half of M1)

| Commit | What |
|---|---|
| `5384b31` | Added `AGENTS.md` (architecture, conventions, gotchas). |
| `97d4fd6` | Added `DEVELOPMENT_PLAN.md` (multi-phase roadmap), refined across the subsequent commits. |
| `ad4a749` | Added the Maven wrapper (`mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`) pinned to Maven **3.9.16**. |
| `27b4f53` | Removed the vestigial `mvn_settings.xml`; rewrote CI to use the wrapper with a JDK **17/21 matrix**, `checkout@v4` / `setup-java@v4`, and Maven caching. |
| `5fbdf0b` | Added `.gitignore` (`target/`, IDE files, OS files). |
| `e076702` | Added `.gitattributes` (`mvnw` → LF, `*.cmd`/`*.bat` → CRLF) so Linux CI sees correct line endings. |
| `6dbea74` | Bumped parent to `pom-scijava:45.1.0`, declared `central` first in `<repositories>`, and pinned `TrackMate:7.14.0`. |

### Review findings (recorded for the plan — see `DEVELOPMENT_PLAN.md`)

- **License inconsistency (unresolved):** `LICENSE` is GPL-3.0 but `pom.xml`
  declares `Simplified BSD License` with `license.licenseName=bsd_2`. This is
  the same contradiction the siblings resolved; ADAPT's `pom.xml` has **not yet
  been corrected** (Decision 1, still open).
- **No tests, no tags, no lint/format tooling.** `mvn verify` is
  compile-only. ADAPT has no git tags at all.
- **Brittle `Analyse_Batch.readParams()`** — positional `Scanner` + hard-coded
  `br.readLine()` skips; silently breaks on reordering (Decision 5: replace
  with JSON).
- **Mutable static/config state:** `GUI.UV` is a genuine static singleton
  (`GUI.getUv()`); `Analyse_Movie` carries many mutated `protected` fields
  through the class hierarchy and protrusion-analysis recursion.
- **Non-deterministic output:** `labels.zip` ROI ordering (Issue #2) — the
  per-frame threads share an unsynchronized `Overlay` (Phase B1).
- **Large dead-code blocks:** `Main.java` is almost entirely commented-out
  experiments; stale comment blocks in `Analyse_Movie`, `BlebAnalyser`,
  `RunnableOutputGenerator`.
- **Mixed-case package dirs** (`Adapt`, `Output`, `Visualisation`, `ui`) —
  Decision 4: rename to lowercase.
- **Effective exception handling missing:** bare `catch (Exception e) { IJ.log(e); }`
  and empty `catch` blocks (Phase A6).

### The five decisions (resolved 2024-09-24, authoritative)

1. **License — GPL-3.0.** Correct `pom.xml` (BSD-2 → GPL-3.0) and the six
   NetBeans header stubs. *(Open — not yet applied here.)*
2. **Dependencies — stay on JitPack, pin to tags.** Requires Phase G (tagging
   the three upstream libs) first. *(Open.)*
3. **Target JDK — Java 11 compile target, build on a modern JDK.** This is the
   deliberate divergence from the siblings (both Java 21): ADAPT is a Fiji
   plugin and Fiji "latest" bundles Java 21 at runtime, so targeting 11 keeps
   the plugin compatible. `TrackMate:7.14.0` is the stopgap that makes this
   possible. Future goal: Java 21 + TrackMate 8 as one coordinated bump
   (Phase D5). *(Implemented — build tooling matches.)*
4. **Package names — rename to lowercase.** *(Open.)*
5. **Params file — JSON.** Replace `readParams()` CSV parsing. *(Open.)*

### Early history (pre-modernisation, from git)

| Commit | What |
|---|---|
| `5445fde` | Added an error check to ensure time-lapse data is being input (start of pre-flight validation). |
| `b634de7` | Added an error check to `Analyse_Movie.saveFluorData()`. |
| `2772ba4` | Additional error checks + `pom.xml` update. |
| `843d5f2` | Fixed a bug introduced by `5445fde`. |
| `120a1dd` / `211654c` | `LICENSE` updated to GPL-3.0 (the source of today's BSD-2/GPL-3 contradiction). |

---

## Lessons learned (mistakes to avoid)

These are inherited from the sibling logs and re-stated here only as they apply
to ADAPT. The sibling logs remain the canonical write-ups.

### L1 — Decide the Java target *before* touching build/CI *(inherited)*

The siblings reversed a Java 11 → Java 21 decision mid-stream. ADAPT **has**
settled this (Java 11 now, coordinated Java 21 + TrackMate 8 later — Decision 3),
but the coordinated bump must be a single, planned change, not an incremental
drift.

**Rule:** confirm the cross-project Java / parent-POM / TrackMate version and
dependency pins in writing before any build or CI edit.

### L2 — Use consistent semver tag names *(inherited)*

ADAPT has **no tags** and its version is still old-style `3.0.13`. The siblings
standardised on `vX.Y.Z`.

**Rule:** adopt `vX.Y.Z` from the first tag onward and decide the major bump
before tagging.

### L3 — Resolve the licence before tagging *(inherited)*

ADAPT has the exact contradiction the siblings fixed: GPL-3.0 `LICENSE` but
BSD-2 `pom.xml`. It has **not** been fixed here yet.

**Rule:** align `pom.xml` (`<licenses>`, `license.licenseName`,
`license.copyrightOwners`) with the root `LICENSE` before tagging; treat the
source-header tidy-up as a separate, deferred item.

### L4 — "No references in this repo" ≠ "dead code" *(inherited, lightly)*

Less applicable to a plugin than a library, but the principle maps onto ADAPT's
large commented-out blocks in `Main.java`/`Analyse_Movie`: commented code is
historical context, not spec, and is often stale.

**Rule:** strip commented/experimental code (recoverable from git) rather than
leaving it to mislead.

### L5 — Check JitPack compatibility when moving the parent POM *(inherited)*

The parent `pom-scijava:45.1.0` move broke JitPack for the siblings; ADAPT
already worked around one consequence by declaring `central` **first** in
`<repositories>` (commit `6dbea74`).

**Rule:** verify the parent-POM version is consumable via JitPack, and keep
`central` declared first.

### L6 — Experimental code must not leak into `main` *(inherited)*

ADAPT's `Main.java` is almost entirely commented experiments; `Analyse_Movie`
and others carry large stale blocks.

**Rule:** gate experiments behind a flag or branch; strip debug/hardcoded-path
code before merge.

### L7 — Clean up *all* legacy IDE artifacts, not just the obvious ones *(inherited)*

`.gitignore` now covers `.idea/`, `*.iml`, etc., but the NetBeans-generated
`GUI.form` coupling remains (Phase B1).

**Rule:** after removing an IDE/legacy build, sweep for remaining config/output
files.

### L10 — Commit the Maven wrapper with its executable bit set *(inherited)*

ADAPT adopted the wrapper in `ad4a749`. The sibling hit "Permission denied" on
Linux CI because `mvnw` was committed as mode `100644`. ADAPT also added
`.gitattributes` (LF for `mvnw`) in the same pass — but the executable bit has
not been verified here yet.

**Rule:** confirm `git ls-files -s mvnw` shows `100755` (and LF line endings)
before relying on `./mvnw` in CI. On Windows, set it with
`git update-index --chmod=+x mvnw`.

### L11 — A plugin's build-tooling win is not a completion *(new, ADAPT-specific)*

ADAPT has landed the entire build/tooling half of M1 (wrapper, CI, `.gitignore`,
`.gitattributes`, parent bump, TrackMate pin) but **none** of the code-level
M1 work — license correction, dead-code removal, package rename, error-handling.
The easy, high-visibility wins are done; the remaining M1 items are the ones
that touch source.

**Rule:** treat "wrapper + CI green" as necessary but far from sufficient;
track code-level milestones explicitly so a green `mvn verify` is not mistaken
for a finished milestone.
