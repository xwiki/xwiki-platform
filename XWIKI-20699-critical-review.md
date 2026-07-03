# Critical review of the XWIKI-20699 finalization work

Self-review of the merge + fixes done on branch `XWIKI-20699` (commits `8c8a68df980`,
`8a17dfc41c6`). Ordered by severity. The goal is to expose weaknesses, not to justify the work.

## High

### H1 — The actual bug fix was never executed end-to-end
The entire point of XWIKI-20699 is "annotations point to the old document after a move". I verified
compilation, unit tests, checkstyle, spoon and Revapi, and I **test-compiled** the functional test —
but I never **ran** `annotationsSurvivePageMove` (it needs a Docker-based XWiki). So the claim
"the bug is fixed" is unproven. All I can honestly assert is that the code is internally consistent
and builds. The one test that would demonstrate the fix end-to-end is exactly the one that did not
run. This is the biggest gap.

**Mitigation:** run the docker IT
(`mvn verify -Pintegration-tests,docker -pl .../xwiki-platform-annotation-test-docker`) before
claiming the regression is resolved.

### H2 — michitux's regression was declared "already fixed" by reasoning, not by a test
I concluded the CHANGES_REQUESTED regression (blank-target annotations being returned for every
object property) is handled by the `&& isDocumentType` gate. That reasoning is sound and I read the
code, but there is **no unit test** that pins the exact scenario michitux described: "loading
annotations for an object-property target must not also return the document-content annotations."
Without that test, a future refactor can silently reintroduce the regression, and my "already
fixed" verdict rests on inspection alone.

**Mitigation:** add a `getAnnotations` test with an object-property target and a blank-target object
present, asserting the blank-target one is *not* returned.

## Medium

### M1 — Cross-branch migration re-run behaviour is under-analysed
I renumbered the migration to `180600000` for master and said older branches get `171011000` /
`180403000` at cherry-pick. That is the right XWiki convention, but I glossed over the consequence:
a user who runs the migration on 17.10.11 (DB version → `171011000`) and later upgrades to 18.6 will
run the master migration **again** (`180600000 > 171011000`). It is idempotent for self-referencing
targets (already blanked → no save), so it is *safe*, but `selectDocuments()` still re-queries and
re-queues every document that has any non-empty target on each such upgrade. I asserted "idempotent,
fine" without spelling out this repeated-work cost or confirming it is acceptable. Worth a conscious
decision rather than a hand-wave.

### M2 — I left (and spread) an inconsistent naming of the XWikiContext variable
`DefaultIOService` now refers to the same context object by three different names:
`xcontext` (addAnnotation), `xwikiContext` (getAnnotations), and `deprecatedContext`
(getAnnotation, removeAnnotation, updateAnnotation, updateObject, setIfNotNull). My merge resolution
kept `xcontext` in `addAnnotation`, which actively *increased* the divergence from the file's
dominant `deprecatedContext`. The reviewers (surli/tmortagne) had explicitly discussed this naming.
Choosing one name for the whole file would have been the clean call; "keep the diff focused" is a
weak justification when the diff already touches this file.

### M3 — The functional test I added is narrower than what it replaced, without calling that out
The PR's original `addAndDeleteAnnotations` asserted the edit/delete buttons per comment. I dropped
those assertions and kept only the move + count + delete flow in `annotationsSurvivePageMove`. That
coverage does live in master's `addEditAndDeleteAnnotations`, so nothing is truly lost — but the
commit message frames it as "add a dedicated test" without noting the trim. A reader diffing against
the PR could think coverage was silently reduced.

## Low

### L1 — Weak assertions in the adapted unit test
- `verify(document).setAuthor(anyString())` — I stub `getUser()` to a concrete value, so I could
  assert the exact string. `anyString()` is looser than necessary.
- `when(document.clone()).thenReturn(document)` returns the *same* mock, so the test does not
  actually prove that the *clone* (rather than the cached instance) is the object saved — which is
  the whole reason master introduced the clone. The test passes but does not guard the intent.

### L2 — Stale `@version $Id: …$` on the renamed migration file
`R180600000XWIKI20699DataMigration.java` keeps the old `$Id: 3bfa9eff…$` from before the rename.
Cosmetic (these keywords are effectively dead post-SVN), but inconsistent with a fresh file.

### L3 — Did not scrutinise the migration HQL
`selectDocuments()` hardcodes `obj.className = 'XWiki.XWikiComments'` and `prop.id.name = 'target'`.
This is pre-existing PR code I did not touch, but a "review of the work" should state that its
correctness (does it capture all annotation-bearing documents? does it behave on subwikis?) was
taken on trust, not checked.

## What holds up well
- Conflict resolution preserved both sides' intent (PR's target-storage logic + master's
  `clone()` and minor-edit flag from XWIKI-24507), and the reasoning is traceable.
- The checkstyle complexity failure was fixed by a real refactor (`matchesTarget`) that also removes
  duplication and documents the blank-target semantics — not suppressed with an annotation.
- Compile coverage is broad: the `-am` docker build compiled the whole annotation tree (core,
  reference, io, rest, scripting, ui, page-objects, docker IT) — so the merge does not break the
  module group, not just the one file I edited.
- Migration renumbering is safe: `R…XWIKI20699` only ever existed on this unmerged branch, so no
  released instance ran the old `160000000` number.

## Recommended next steps before merge
1. Run the docker IT to actually prove the fix (H1).
2. Add the object-property `getAnnotations` regression test (H2).
3. Decide and document the cross-branch re-run behaviour (M1).
4. Unify the context variable name across `DefaultIOService` (M2).
5. Optionally tighten the unit-test assertions (L1).
