# Copyright (C) 2026 Jacob Wysko
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program. If not, see https://www.gnu.org/licenses/.

"""
Reading, checking and writing the voice catalogues in sharedAssets/voices, with no UI.

The catalogues are a fixed, simple shape of YAML: comment lines, and entries of `key: value` lines. This module
reads and writes that shape directly rather than through a YAML library, so that the comments (which record where
in the specifications each section was transcribed from) survive an edit, and an unchanged file saves byte for byte.

Run it on its own to check every catalogue the way the app's integrity test does:

    python tools/voice_editor/catalogue.py --check
"""

from __future__ import annotations

import json
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

REPOSITORY = Path(__file__).resolve().parents[2]
SHARED_ASSETS = REPOSITORY / "sharedAssets"
ASSIGNMENT_SOURCES = REPOSITORY / "app/src/commonMain/kotlin/org/wysko/midis2jam2/instrument/algorithmic/assignment"

NONE = "None"
"""The look that puts nothing on stage, rather than falling back."""

GM = "voices/gm.yaml"
GM_KITS = "voices/kits/gm.yaml"


class ParseError(Exception):
    pass


@dataclass(eq=False)  # Entries are compared by identity, so two identical ones can be told apart when editing.
class Entry:
    """One voice or kit. `program` is 1-based, as the specifications print it."""

    program: int
    name: str
    msb: int = 0
    lsb: int = 0
    look: str | None = None
    written: set[str] = field(default_factory=set)
    """The optional keys (msb, lsb) the file spelled out, so an unchanged entry saves exactly as it was."""

    section: str = ""
    """The comment this entry sits under, for display."""

    @property
    def key(self) -> tuple[int, int, int]:
        return self.msb, self.lsb, self.program


@dataclass
class Comment:
    """A comment or blank line, kept exactly as written."""

    text: str


@dataclass
class Catalogue:
    path: str
    """Relative to sharedAssets, e.g. voices/gs.yaml."""

    kits: bool
    items: list[Entry | Comment]
    newline: str = "\n"
    writes_msb: bool = True
    """Whether new entries spell out `msb`, following the rest of the file."""

    @property
    def entries(self) -> list[Entry]:
        return [item for item in self.items if isinstance(item, Entry)]

    @property
    def file(self) -> Path:
        return SHARED_ASSETS / self.path

    def update_sections(self) -> None:
        section = ""
        for item in self.items:
            if isinstance(item, Comment):
                text = item.text.strip()
                if text.startswith("#") and len(text) > 1:
                    section = text.lstrip("#").strip()
            else:
                item.section = section


# Reading ---------------------------------------------------------------------------------------------------------------

_KEY_VALUE = re.compile(r"^(\w+):\s*(.*?)\s*$")


def _value(raw: str, line_number: int):
    if raw.startswith('"'):
        try:
            return json.loads(raw)
        except json.JSONDecodeError as error:
            raise ParseError(f"line {line_number}: can't read the quoted value {raw}") from error
    if re.fullmatch(r"-?\d+", raw):
        return int(raw)
    return raw


def parse(path: str, kits: bool, text: str) -> Catalogue:
    newline = "\r\n" if "\r\n" in text else "\n"
    lines = text.replace("\r\n", "\n").split("\n")
    if lines and lines[-1] == "":
        lines.pop()

    items: list[Entry | Comment] = []
    fields: dict | None = None
    start = 0

    def finish():
        nonlocal fields
        if fields is None:
            return
        missing = [key for key in ("program", "name") if key not in fields]
        if missing:
            raise ParseError(f"line {start}: the entry has no {' or '.join(missing)}")
        unknown = set(fields) - {"program", "name", "msb", "lsb", "look"}
        if unknown:
            raise ParseError(f"line {start}: unknown keys {sorted(unknown)}")
        items.append(Entry(
            program=fields["program"],
            name=str(fields["name"]),
            msb=fields.get("msb", 0),
            lsb=fields.get("lsb", 0),
            look=None if fields.get("look") is None else str(fields["look"]),
            written={key for key in ("msb", "lsb") if key in fields},
        ))
        fields = None

    for number, line in enumerate(lines, start=1):
        stripped = line.strip()
        if line.startswith("- "):
            finish()
            if line[2:].lstrip().startswith("{"):
                raise ParseError(f"line {number}: inline {{...}} entries aren't supported; write one key per line")
            fields, start = {}, number
            content = line[2:]
        elif line.startswith("  ") and fields is not None and stripped and not stripped.startswith("#"):
            content = stripped
        elif stripped == "" or stripped.startswith("#"):
            finish()
            items.append(Comment(line))
            continue
        elif stripped == "[]":
            finish()
            continue
        else:
            raise ParseError(f"line {number}: can't read {line!r}")

        match = _KEY_VALUE.match(content.strip())
        if not match:
            raise ParseError(f"line {number}: expected `key: value`, got {content.strip()!r}")
        key, raw = match.groups()
        if key in fields:
            raise ParseError(f"line {number}: {key} appears twice in one entry")
        fields[key] = _value(raw, number)
    finish()

    catalogue = Catalogue(path, kits, items, newline)
    entries = catalogue.entries
    catalogue.writes_msb = any("msb" in entry.written for entry in entries) if entries else path != GM
    catalogue.update_sections()
    return catalogue


# Writing ---------------------------------------------------------------------------------------------------------------

def render(catalogue: Catalogue) -> str:
    lines: list[str] = []
    for item in catalogue.items:
        if isinstance(item, Comment):
            lines.append(item.text)
            continue
        lines.append(f"- program: {item.program}")
        if "msb" in item.written or item.msb != 0:
            lines.append(f"  msb: {item.msb}")
        if "lsb" in item.written or item.lsb != 0:
            lines.append(f"  lsb: {item.lsb}")
        lines.append(f"  name: {json.dumps(item.name, ensure_ascii=False)}")
        if item.look:
            lines.append(f"  look: {item.look}")
    if not catalogue.entries:
        lines.append("[]")
    return catalogue.newline.join(lines) + catalogue.newline


def save(catalogue: Catalogue) -> None:
    with open(catalogue.file, "w", encoding="utf-8", newline="") as f:
        f.write(render(catalogue))


# The app's own lists ----------------------------------------------------------------------------------------------------

def catalogue_files() -> dict[str, bool]:
    """Every catalogue the app loads (VoiceCatalogues.FILES), and whether it lists kits."""
    source = (ASSIGNMENT_SOURCES / "VoiceCatalogue.kt").read_text(encoding="utf-8")
    files = {path: kits == "true" for path, kits in re.findall(r'"(voices/[^"]+\.yaml)" to (true|false)', source)}
    if not files:
        raise RuntimeError("Couldn't find VoiceCatalogues.FILES in VoiceCatalogue.kt")
    return files


def looks() -> list[str]:
    """Every look id in Looks.kt, for melodic catalogues."""
    source = (ASSIGNMENT_SOURCES / "Looks.kt").read_text(encoding="utf-8")
    return sorted(set(re.findall(r'\bLook\(\s*"([^"]+)"', source)) | {NONE})


def kit_looks() -> list[str]:
    """Every kit look id in KitLooks.kt, for kit catalogues."""
    source = (ASSIGNMENT_SOURCES / "KitLooks.kt").read_text(encoding="utf-8")
    return sorted(set(re.findall(r'\b(?:KitLook|typical)\(\s*"([^"]+)"', source)) | {NONE})


def load_all() -> dict[str, Catalogue]:
    return {
        path: parse(path, kits, (SHARED_ASSETS / path).read_text(encoding="utf-8", newline=""))
        for path, kits in catalogue_files().items()
    }


# What a voice shows as --------------------------------------------------------------------------------------------------

def shows_as(entry: Entry, catalogue: Catalogue, catalogues: dict[str, Catalogue]) -> tuple[str, bool]:
    """
    What the app draws for [entry], following VoiceResolver: its own look, or else the General MIDI voice (or kit)
    with the same program. Returns the description, and whether it comes from a fallback.
    """
    if entry.look == NONE:
        return "nothing", False
    if entry.look:
        return entry.look, False

    base_path = GM_KITS if catalogue.kits else GM
    if catalogue.path == base_path:
        return ("Standard" if catalogue.kits else "nothing"), True

    base = catalogues.get(base_path)
    fallback = next((e for e in base.entries if e.program == entry.program and e.msb == 0 and e.lsb == 0), None) \
        if base else None
    if fallback and fallback.look and fallback.look != NONE:
        return fallback.look, True
    return ("Standard" if catalogue.kits else "nothing"), True


# Checking ---------------------------------------------------------------------------------------------------------------

def problems(catalogue: Catalogue, known_looks: list[str], known_kit_looks: list[str]) -> dict[int, list[str]]:
    """Problems by entry index (into catalogue.entries), mirroring VoiceCatalogueIntegrityTest. -1 is file-wide."""
    found: dict[int, list[str]] = {}
    valid = set(known_kit_looks if catalogue.kits else known_looks)
    seen: dict[tuple[int, int, int], int] = {}

    def add(index: int, message: str):
        found.setdefault(index, []).append(message)

    for index, entry in enumerate(catalogue.entries):
        if not 1 <= entry.program <= 128:
            add(index, f"program {entry.program} is outside 1-128")
        if not 0 <= entry.msb <= 127:
            add(index, f"MSB {entry.msb} is outside 0-127")
        if not 0 <= entry.lsb <= 127:
            add(index, f"LSB {entry.lsb} is outside 0-127")
        if not entry.name.strip():
            add(index, "has no name")
        if entry.look and entry.look not in valid:
            add(index, f"{entry.look} isn't a {'kit ' if catalogue.kits else ''}look")
        if entry.key in seen:
            add(index, f"same bank and program as entry {seen[entry.key] + 1}")
        else:
            seen[entry.key] = index

    if catalogue.path == GM and [e.program for e in catalogue.entries] != list(range(1, 129)):
        add(-1, "gm.yaml should list programs 1 to 128, in order")
    if catalogue.path == "voices/gm2.yaml":
        for index, entry in enumerate(catalogue.entries):
            if entry.msb != 121:
                add(index, "GM2 melodic voices are in bank 121 (79H)")
    if catalogue.path == "voices/kits/gm2.yaml":
        for index, entry in enumerate(catalogue.entries):
            if entry.msb != 120:
                add(index, "GM2 kits are in bank 120 (78H)")
    return found


def _check() -> int:
    catalogues = load_all()
    known, known_kits = looks(), kit_looks()
    failures = 0
    for path, catalogue in catalogues.items():
        original = catalogue.file.read_text(encoding="utf-8", newline="")
        if render(catalogue) != original:
            print(f"{path}: would not save back unchanged")
            failures += 1
        for index, messages in sorted(problems(catalogue, known, known_kits).items()):
            where = "file" if index < 0 else f"entry {index + 1} ({catalogue.entries[index].name})"
            for message in messages:
                print(f"{path}: {where}: {message}")
                failures += 1
        print(f"{path}: {len(catalogue.entries)} entries")
    print("OK" if failures == 0 else f"{failures} problem(s)")
    return 1 if failures else 0


if __name__ == "__main__":
    if sys.argv[1:] == ["--check"]:
        sys.exit(_check())
    print(__doc__)
