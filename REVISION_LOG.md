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

## 2026-09-28 — Removed `AdaptDataProcessing`; retired `Bleb_Data_Analysis`

`AdaptDataProcessing` is deprecated — its README now recommends a Python rewrite
for its analysis. Dropped it from ADAPT entirely:

- Removed the `com.github.djpbarry:AdaptDataProcessing` dependency from `pom.xml`.
- Deleted the `Bleb_Data_Analysis` plugin (a thin wrapper over
  `AdaptDataProcessing.DataFileAverager`) and its `plugins.config` entry.
- Removed the inline `DataFileAverager` call in `RunnableOutputGenerator` (the
  per-bleb `mean_data.csv` averaging step). That post-hoc averaging is now a
  Python step, per the upstream README.

ADAPT now has **two** registered plugins (`Analyse Movie`, `Batch Analysis`) and
**two** JitPack dependencies (`IAClassLibrary`, `TrackerLibrary`).

---

## 2026-09-27 — TrackerLibrary bumped to `v4.0.2` (version-skew fix)

`pom.xml` now pins `TrackerLibrary v4.0.2` (was `v4.0.1`). `v4.0.2` re-pins its
`IAClassLibrary` dependency to `v2.0.1` (the prior `v4.0.1` still pointed at the
old commit `37a1be016a`), so ADAPT's direct `IAClassLibrary v2.0.1` and the
transitive one now agree — resolving the version skew flagged during review.

---

## 2026-09-27 — Added Phase H: interactive Fiji run & smoke-test harness

From this point, headless build/test is no longer sufficient to consider a change
done — ADAPT must be exercised interactively inside Fiji. Added **Phase H** to
`DEVELOPMENT_PLAN.md` (staging/launch scripts, a per-change smoke test over the
three `plugins.config` commands against `test_data/ADAPT_Test_Data.zip`, and
entry-point hygiene for the `Main` debug path), plus **M11** in the sequencing
list. M11 is now the immediate gate for every change.

No code changed — planning only.

---

## 2026-09-27 — Switched to latest Fiji (2.18.1); fixed launcher

- Adopted the **latest Fiji** at `C:\Users\barryd\Fiji` (2.18.1, `nojre`). It
  requires **Java 21+** and ships **TrackMate 8.1.6** (matching the modernised
  `IAClassLibrary` `v2.0.1`).
- Launcher confirmed working: the jaunch launcher finds Java 21 via `JAVA_HOME`
  (no bundled JRE or junction needed — an initial `jre` junction was tried and
  then removed as unnecessary).
- Updated `bin/` scripts to default to the new Fiji dir, and `run-fiji.cmd` to
  use the bundled launcher. Updated `DEVELOPMENT_PLAN.md` Phase H1.

---

## 2026-09-27 — Re-pointed dependencies to tagged releases

`pom.xml` now pins the modernised siblings (Decision 2):

- `IAClassLibrary` `fe92f24c6e` → **`v2.0.1`**
- `TrackerLibrary` `99584ec579` → **`v4.0.1`**
- `AdaptDataProcessing` left at `95d31fcec8` (obsolete; still used by
  `Bleb_Data_Analysis`, pending removal).

`mvn clean verify` passes (18 sources compiled at `release 21`). One new
deprecation warning surfaced in `Analyse_Movie` — the modernised IAClassLibrary
marks `ProgressDialog`/`DataStatistics` `@Deprecated`; migrating those calls is a
follow-up, not a blocker.

---

## 2026-09-27 — JDK 21 upgrade applied; TrackMate pin removed

Executed the JDK 21 move in `pom.xml`:

- Added `scijava.jvm.version=21`; `maven.compiler.release` now resolves to **21**.
  Clean `mvn compile` recompiled all 18 sources with `javac [release 21]`
  (BUILD SUCCESS).
- Removed the `TrackMate:7.14.0` `dependencyManagement` pin. TrackMate is now
  resolved transitively (parent manages **TrackMate 8.0.0**), so ADAPT no longer
  pins it at all.

The three sibling dependencies are still the old commit hashes
(`fe92f24c6e`, `99584ec579`, `95d31fcec8`); re-pointing them to the new tags is
the next step once the upstream repos are tagged.

---

## 2026-09-27 — JDK 21 target + output-baseline plan (decisions)

- **JDK target moved to Java 21** (reverses Decision 3's Java 11). Rationale:
  avoid cross-project compatibility issues by aligning ADAPT with the siblings
  (IAClassLibrary, TrackerLibrary), Fiji's runtime, and TrackMate ≥ 8. Actions
  (not yet applied): compile target → 21, drop the `TrackMate:7.14.0` pin,
  re-point deps once upstream tags land. Recorded in `DEVELOPMENT_PLAN.md`
  Decision 3.
- **Output-baseline comparison** recorded as `DEVELOPMENT_PLAN.md` Phase H5: a
  SHA-256 per-file diff against the `ADAPT_Test_Data/` benchmark outputs, with
  normalization for timestamps (`*.properties`), non-deterministic ROI order
  (`labels.zip`, Issue #2), and TIFF/PNG metadata. Behaviour-preserving changes
  must reproduce the benchmark byte-for-byte; changes are only expected once the
  modernised deps land (upstream bug fixes).

No code changed — planning only.

---

## 2026-09-27 — Local Fiji wired up for live testing (M11 / Phase H)

- Fiji is at `C:\Users\barryd\fiji-nojre\Fiji.app` (a **nojre** build, no bundled
  JRE). Verified it runs on the local **JDK 21** (ImageJ 1.54p + Java 21.0.12.1).
- The bundled `fiji-windows-x64.exe` launcher **cannot find Java** (exits 1), so
  Fiji is launched via direct Java (`java -cp "jars\*;plugins\*" ij.ImageJ`).
- Installed `adapt-4.0.0-SNAPSHOT.jar` into `Fiji.app/plugins/`, removed the old
  `adapt-3.0.13.jar`. Sibling deps were already present and match the current
  pins (`iaclasslibrary-1.0.37.jar`, `trackerlibrary-3.0.10.jar`,
  `adaptdataprocessing-1.0.7.jar`).
- Confirmed all three ADAPT plugin classes load on the Fiji classpath.
- Committed `bin/install-to-fiji.cmd`, `bin/run-fiji.cmd`,
  `bin/smoke-test-fiji.cmd` (Phase H1), with the launcher fix left as a
  follow-up.

---

## 2026-09-27 — Plugin framework decision: stay ImageJ 1.x

Resolved (with the maintainer) that ADAPT **stays an ImageJ 1.x plugin**
(`plugins.config` + `ij.plugin.PlugIn`) rather than reimplementing as a modern
SciJava `@Plugin`/`@Parameter` command. A SciJava migration is recorded as a
future option only; if revisited, the preferred route is thin `Command` wrappers
over the existing core (after M4), not a full rewrite. Recorded in
`DEVELOPMENT_PLAN.md` as Decision 6 and Phase H0, with
https://imagej.net/develop/ij1-plugins as the authoritative reference for the
current model.

No code changed — planning only.

---

## 2026-09-27 — Version bump to `4.0.0-SNAPSHOT`

Bumped `pom.xml` `<version>` from `3.0.13` to `4.0.0-SNAPSHOT` — the first step
toward the semver discipline in L2 and the sibling's `4.0.0-SNAPSHOT` convention.
This is the pre-tag working version; the `v4.0.0` tag is only created once the
planned modernisation work lands.

---

## 2026-09-27 — M1 (Foundations) completed

**Phases A1, A2, A4.5, A4.6, and A6 done.** `mvn verify` passes on JDK 21 with
`maven.compiler.release=11`. The package rename (A5.2 / Decision 4) and the
`readParams()` → JSON replacement (Decision 5) are deliberately left to M4, as
the plan sequences them.

| Change | What |
|---|---|
| A1 | CI `actions/checkout@v4` → `@v5` and `actions/setup-java@v4` → `@v5` (Node 20 → Node 24 deprecation). |
| A2 | `pom.xml`: documented the three commit-hash pins (`IAClassLibrary:fe92f24c6e`, `TrackerLibrary:99584ec579`, `AdaptDataProcessing:95d31fcec8`) with the commit each refers to and why; flagged `AdaptDataProcessing` as deprecated. Re-pointing to tags remains M10 / Phase G. |
| A4.5 | Stripped ~300 lines of dead code: `Main.java` (commented experiments); the old Dijkstra segmentation methods (`initDistanceMaps`, `dijkstraDilate`, `buildDistanceMaps`, `expandRegions`); `generateScaleBar`, `drawBlebMovie`, `printParamFile`; and scattered debug `IJ.saveAs` blocks across `Analyse_Movie`, `BlebAnalyser`, `RunnableOutputGenerator`. |
| A4.6 | `pom.xml` license BSD-2 → GPL-3.0 (`license.licenseName=gpl_v3`, name "GNU General Public License v3.0 or later"); replaced the six NetBeans "change this header" stubs with the GPL header. |
| A6 | Replaced 2 empty `catch` blocks and 9 bare `System.out.println(e.toString())` / `IJ.log(e.toString())` with actionable `IJ.log(...)` messages; fixed a log typo ("saved" → "save"). |

### Verification

- `mvn verify` → **BUILD SUCCESS**.
- `mvnw` is tracked with the executable bit set (`100755`) — L10 is satisfied.

### Deliberately deferred

- **A5.2 (package rename)** — sequenced into M4 (see plan note).
- **`AdaptDataProcessing` / `Bleb_Data_Analysis` removal** — flagged as an open
  decision (see the upstream-dependency entry above), not forced inside M1.

---

## 2026-09-27 — Upstream dependencies have moved; `AdaptDataProcessing` is obsolete

The three JitPack dependencies are no longer in the state assumed by the plan.
Two of them have been substantially modernised, and the third is now deprecated.

### The two surviving libraries now publish Javadoc

- **IAClassLibrary** — https://djpbarry.github.io/IAClassLibrary/
- **TrackerLibrary** — https://djpbarry.github.io/TrackerLibrary/

Both have completed their own modernisation pass (GPL-3.0-or-later, Java 21,
Maven wrapper, and Javadoc publishing). Their `REVISION_LOG.md` files are the
authoritative record; their Javadoc sites are now the canonical API surface for
downstream consumers. This **changes Decision 2's reference point**: the
compatibility target for ADAPT is the published Javadoc, not the pinned git
commit hashes currently in `pom.xml`.

ADAPT's pins are therefore stale:
- `IAClassLibrary:fe92f24c6e` — superseded by the modernised tagged/Javadoc
  release.
- `TrackerLibrary:99584ec579` — superseded by the modernised release
  (`4.0.0-SNAPSHOT` in the sibling, tagged per its plan).

### `AdaptDataProcessing` is deprecated (archive candidate)

The [`AdaptDataProcessing` README](https://github.com/djpbarry/AdaptDataProcessing/blob/master/README.md)
now declares the project **obsolete** and recommends a **Python rewrite**
(pandas / NumPy / SciPy / matplotlib) for its analysis. Its three pieces are:

- `DataFileAverager` — the only functional engine (per-timepoint mean/σ across
  track CSVs).
- `DataResampler` — effectively non-functional (`run()` commented out).
- `DetectionMapAnalyser` — TIFF detection-map statistics.

**Impact on ADAPT:**

- The `Bleb_Data_Analysis` plugin is a thin wrapper over
  `AdaptDataProcessing.DataFileAverager`, so one of ADAPT's three registered
  entry points now rests on a deprecated dependency.
- `pom.xml` still pins `AdaptDataProcessing:95d31fcec8`.
- Phase G ("tag all three") and Decision 2 are **moot for `AdaptDataProcessing`**:
  it should not be tagged/fixed, it should be **removed** — with its
  `DataFileAverager` behaviour either reimplemented in ADAPT (or its Python
  equivalent surfaced), or the `Bleb_Data_Analysis` plugin retired.

### Open items this raises

1. Decide the fate of `Bleb_Data_Analysis`: drop it, reimplement
   `DataFileAverager` in ADAPT, or point users at a Python path.
2. Re-point `IAClassLibrary` / `TrackerLibrary` pins to their tagged releases
   using the Javadoc as the compatibility check (completing A2 / Decision 2).
3. Revise `DEVELOPMENT_PLAN.md` Phase G to drop `AdaptDataProcessing` from the
   tagging work.

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

- **License inconsistency (resolved in M1):** `LICENSE` was GPL-3.0 but `pom.xml`
  declared `Simplified BSD License` with `license.licenseName=bsd_2`. Corrected
  to GPL-3.0 in M1 (Decision 1).
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
   NetBeans header stubs. *(Done in M1.)*
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

ADAPT has **no tags** yet. Its version is now `4.0.0-SNAPSHOT` (bumped from the
old-style `3.0.13`); the siblings standardised on `vX.Y.Z`.

**Rule:** adopt `vX.Y.Z` from the first tag onward and decide the major bump
before tagging.

### L3 — Resolve the licence before tagging *(inherited)*

ADAPT had the exact contradiction the siblings fixed: GPL-3.0 `LICENSE` but
BSD-2 `pom.xml`. Fixed in M1 (pom.xml + headers).

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
`.gitattributes` (LF for `mvnw`) in the same pass. The executable bit was
verified in M1: `mvnw` is tracked as `100755`.

**Rule:** confirm `git ls-files -s mvnw` shows `100755` (and LF line endings)
before relying on `./mvnw` in CI. On Windows, set it with
`git update-index --chmod=+x mvnw`.

### L11 — A plugin's build-tooling win is not a completion *(new, ADAPT-specific)*

ADAPT landed the entire build/tooling half of M1 (wrapper, CI, `.gitignore`,
`.gitattributes`, parent bump, TrackMate pin) well before the code-level M1
work (license correction, dead-code removal, error handling — package rename
still deferred to M4). The easy, high-visibility wins came first and could
have been mistaken for a finished M1.

**Rule:** treat "wrapper + CI green" as necessary but far from sufficient;
track code-level milestones explicitly so a green `mvn verify` is not mistaken
for a finished milestone.
