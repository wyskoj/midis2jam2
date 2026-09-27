# The behaviour catalogue

`app/src/desktopTest/resources/spec/behaviour.yaml` lists, one entry at a time, everything
midis2jam2 promises to do. Most entries are transcribed from the documentation site
(<https://docs.midis2jam2.xyz/>, source in the sibling `midis2jam2-docs` repository); the rest
describe behaviour the app has but the docs do not yet mention, and are marked `source: code`.

The catalogue exists because the documentation is the closest thing the project has to a
specification, and nothing was holding the app to it. The lyric display is the cautionary
tale: it was fully written, then quietly stopped being constructed during a refactor. It kept
compiling, the settings screen kept offering the toggle, and the docs kept promising the
feature, for months.

## How it is enforced

A test cites the behaviours it covers:

```kotlin
@Test
@Spec("camera.freecam.key.forward")
fun `W moves the free camera forward`() { ... }
```

`SpecCoverageTest` then holds the two sides together. It fails when:

- a catalogued behaviour has no citing test and is neither `waived` nor `pending`;
- a test cites an id that is not in the catalogue (usually a typo);
- a behaviour marked `pending` turns out to have a test after all, so the marker is stale;
- the catalogue is malformed - duplicate ids, unknown platforms, empty claims.

It also prints a coverage line on every run, for example:

```
Spec coverage: 97/97 testable behaviours covered (0 still pending, 13 waived, 110 catalogued)
```

## The three states an entry can be in

**Covered** - a test cites it. Nothing to do.

**`waived: "<reason>"`** - no automated test will ever cover it, and the reason says why.
These are behaviours genuinely out of the suite's reach: a native file dialog, a real display,
real MIDI hardware, Android touch gestures. A waiver is permanent and is reported in the
coverage line, so it is visible rather than silent.

**`pending: "<reason>"`** - not covered *yet*. This is the backlog, and it is meant to shrink
to nothing. When you write the test, delete the marker; the suite
fails if you forget, so the list cannot quietly stop being true. Adding a behaviour without a
test is the normal way this list grows again, and that is fine as long as it keeps shrinking.

## Keeping it in step with the docs

The catalogue is a copy, so it goes stale unless someone re-reads the docs. When a feature
changes:

1. Update the documentation site as usual.
2. Add, change or remove the matching entries here, keeping ids stable where the behaviour
   is unchanged - ids are cited from test code, so renaming one breaks that citation loudly.
3. Write or update the test.

Entries marked `source: code` are the reverse problem: behaviour that works but is not
documented. The free camera's Shift and Ctrl speed modifiers are the current examples. They
are worth either documenting or reconsidering.
