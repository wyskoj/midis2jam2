# Tuning keys and the capo

The fretting engine picks each part's tuning and capo. The guitar, bass and banjo show them on the instrument
itself (`instrument/family/guitar/TuningVisuals.kt`, timed by `TuningMotion.kt`):
- **Keys.** Each tuning key is turned by how far its string is tuned from standard.
- **Slack.** A lowered string vibrates wider and slower; a raised one is tighter. This needs no art.
- **Retune.** Just before the instrument's first note, the retuned strings ring and glide from standard into their
  tuning, and the keys turn with them.
- **Capo.** A capo slides down from the nut and clamps behind its fret.

The keys need art: today they are part of each body mesh. Until an instrument has its art, its keys don't turn and
the old rule still applies (the separate drop-D model when the lowest string is lowered).

## Models

All go in `sharedAssets/Assets/`, as OBJ, sharing the body's texture and UVs.

| Instrument | Body today | Body without keys | Key |
|---|---|---|---|
| Electric guitar (all electric types) | `Guitar.obj` | e.g. `GuitarNoKeys.obj` | `GuitarKey.obj` |
| Acoustic guitar | `GuitarAcoustic.obj` | e.g. `GuitarAcousticNoKeys.obj` | `AcousticKey.obj` |
| Bass (standard, synth) | `Bass.obj` | e.g. `BassNoKeys.obj` | `BassKey.obj` |
| Fretless bass | `BassFretless.obj` | e.g. `BassFretlessNoKeys.obj` | `BassKey.obj` |
| Banjo | `Banjo.obj` | e.g. `BanjoNoKeys.obj` | `BanjoKey.obj` |

- **Body without keys.** Export it in exactly the same space as the current body, so the strings, frets and hand
  still line up. In `Guitar.obj`, the keys are the `TopOfKeys`, `BottomOfKeys`, `TwisterSpindles`,
  `TwisterBottoms`, `TuneTateBottoms` and `FacesOfSpindles` groups. The other bodies are single meshes. Leave the
  tuner posts or bushings on the body; only the part that turns moves.
- **Key.** One key, modelled with its pivot (the centre of the post) at the origin and its turning axis along +Y.
  Every key on the headstock is a copy of it.

## Layout

Each instrument with key art gets `sharedAssets/instrument/tuning/<body>.json`, named after its usual body (so
`Guitar.json` for `Guitar.obj`). Having this file is what switches the instrument over:

```json
{
  "body": "GuitarNoKeys.obj",
  "key": "GuitarKey.obj",
  "degreesPerSemitone": 90,
  "keys": [
    { "position": [-1.2, 21.5, 0.3], "rotation": [0, 0, 90], "direction": 1 },
    ...
  ]
}
```

| Key | What it does |
|---|---|
| `body` | The body exported without its keys, used whatever the tuning. |
| `key` | The key model. |
| `degreesPerSemitone` | How far a key turns per semitone from standard. At 90, drop D's low key is half a turn round. |
| `keys` | One entry per string, lowest string first. |
| `position` | Where that key's pivot sits, in the body's model space. |
| `rotation` | How that key is oriented, as Euler angles in degrees. A 3+3 headstock mirrors one side here. |
| `direction` | `1` or `-1`: which way turning the key tightens the string, so lowering turns it the other way. |

In standard tuning every key sits exactly as placed, so the layout should reproduce the current body's look.

## Capo

One `Capo.obj`, modelled:
- 1 unit wide across the neck (X), centred on the origin;
- the clamping face toward −Y (onto the strings), with +Y away from the fretboard;
- its thickness along the neck on Z.

The code stretches it to each neck's width at its fret, so one model fits the guitar, the acoustic and the banjo
(the bass never uses a capo). Until it exists, a plain bar stands in.

## Tuning by eye

These constants are in `TuningMotion.kt`:
- `RETUNE_LEAD`, `RETUNE_GAP`: when the retune happens, relative to the first note.
- `SPEED_EXPONENT`, `WIDTH_EXPONENT`: how strongly slack shows.
- `OVERSHOOT`: how far the glide passes its tuning before settling.

`TuningMotionTest` and `TuningVisualsTest` check the timing, the slack, the ringing strings and the capo's place.
`instrument.fretted.tuning.keys` stays `pending` in the spec until the key art exists.
