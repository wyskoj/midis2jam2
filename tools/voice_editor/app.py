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
A small editor for the voice catalogues in sharedAssets/voices.

    pip install -r tools/requirements.txt
    python tools/voice_editor/app.py

Pick a catalogue on the left. Every voice is listed with what it shows as on stage: its own look, or (in grey) the
General MIDI instrument it falls back to. Edit a cell to change it; the look column offers every look in Looks.kt or
KitLooks.kt. Problems the app's integrity test would reject are shown in red. Ctrl+S saves the catalogue, keeping its
comments.
"""

from __future__ import annotations

import sys

from PySide6.QtCore import Qt
from PySide6.QtGui import QAction, QBrush, QColor, QKeySequence
from PySide6.QtWidgets import (
    QAbstractItemView, QApplication, QCheckBox, QComboBox, QCompleter, QHBoxLayout, QHeaderView, QLabel, QLineEdit,
    QListWidget, QListWidgetItem, QMainWindow, QMessageBox, QPushButton, QSplitter, QStyledItemDelegate,
    QTableWidget, QTableWidgetItem, QVBoxLayout, QWidget,
)

import catalogue as data

PROGRAM, MSB, LSB, NAME, LOOK, SHOWS_AS, SECTION = range(7)
HEADERS = ["Program", "MSB", "LSB", "Name", "Look", "Shows as", "Section"]
EDITABLE = {PROGRAM, MSB, LSB, NAME, LOOK}

TITLES = {
    "voices/gm.yaml": "General MIDI",
    "voices/gs.yaml": "GS",
    "voices/xg.yaml": "XG",
    "voices/gm2.yaml": "General MIDI 2",
    "voices/kits/gm.yaml": "Kits · General MIDI",
    "voices/kits/gs.yaml": "Kits · GS",
    "voices/kits/xg.yaml": "Kits · XG",
    "voices/kits/gm2.yaml": "Kits · General MIDI 2",
}

PROBLEM = QBrush(QColor(255, 200, 200))
FALLBACK = QBrush(QColor(130, 130, 130))


class LookDelegate(QStyledItemDelegate):
    """An editable drop-down of every valid look, with completion."""

    def __init__(self, window: "Editor"):
        super().__init__(window)
        self.window = window

    def createEditor(self, parent, option, index):
        box = QComboBox(parent)
        box.setEditable(True)
        box.addItems([""] + self.window.valid_looks())
        box.completer().setCompletionMode(QCompleter.CompletionMode.PopupCompletion)
        box.completer().setFilterMode(Qt.MatchFlag.MatchContains)
        return box

    def setEditorData(self, editor, index):
        editor.setCurrentText(index.data() or "")

    def setModelData(self, editor, model, index):
        model.setData(index, editor.currentText().strip())


class Editor(QMainWindow):
    def __init__(self):
        super().__init__()
        self.catalogues = data.load_all()
        self.looks = data.looks()
        self.kit_looks = data.kit_looks()
        self.dirty: set[str] = set()
        self.current: data.Catalogue | None = None
        self.rows: list[data.Entry] = []
        self.filling = False

        self.files = QListWidget()
        for path in self.catalogues:
            item = QListWidgetItem()
            item.setData(Qt.ItemDataRole.UserRole, path)
            self.files.addItem(item)
        self.files.currentItemChanged.connect(lambda item, _: self.open(item.data(Qt.ItemDataRole.UserRole)))
        self.files.setMaximumWidth(220)

        self.search = QLineEdit(placeholderText="Search names, looks and sections…")
        self.search.textChanged.connect(self.apply_filter)
        self.only_looks = QCheckBox("Only voices with a look")
        self.only_looks.toggled.connect(self.apply_filter)
        self.only_problems = QCheckBox("Only problems")
        self.only_problems.toggled.connect(self.apply_filter)

        self.table = QTableWidget(0, len(HEADERS))
        self.table.setHorizontalHeaderLabels(HEADERS)
        self.table.verticalHeader().setVisible(False)
        self.table.verticalHeader().setDefaultSectionSize(24)
        self.table.setWordWrap(False)
        self.table.setSelectionBehavior(QAbstractItemView.SelectionBehavior.SelectRows)
        self.table.setAlternatingRowColors(True)
        self.table.setItemDelegateForColumn(LOOK, LookDelegate(self))
        header = self.table.horizontalHeader()
        for column in (PROGRAM, MSB, LSB):
            header.setSectionResizeMode(column, QHeaderView.ResizeMode.ResizeToContents)
        header.setSectionResizeMode(NAME, QHeaderView.ResizeMode.Interactive)
        header.setSectionResizeMode(SECTION, QHeaderView.ResizeMode.Stretch)
        self.table.setColumnWidth(NAME, 220)
        self.table.setColumnWidth(LOOK, 190)
        self.table.setColumnWidth(SHOWS_AS, 190)
        self.table.itemChanged.connect(self.edited)

        add = QPushButton("Add voice")
        add.clicked.connect(self.add)
        remove = QPushButton("Delete")
        remove.clicked.connect(self.delete)
        save = QPushButton("Save")
        save.clicked.connect(self.save_current)
        self.summary = QLabel()

        filters = QHBoxLayout()
        filters.addWidget(self.search, 1)
        filters.addWidget(self.only_looks)
        filters.addWidget(self.only_problems)
        buttons = QHBoxLayout()
        buttons.addWidget(add)
        buttons.addWidget(remove)
        buttons.addStretch(1)
        buttons.addWidget(self.summary)
        buttons.addWidget(save)

        right = QWidget()
        layout = QVBoxLayout(right)
        layout.addLayout(filters)
        layout.addWidget(self.table, 1)
        layout.addLayout(buttons)

        splitter = QSplitter()
        splitter.addWidget(self.files)
        splitter.addWidget(right)
        splitter.setStretchFactor(1, 1)
        self.setCentralWidget(splitter)

        for text, shortcut, slot in [
            ("Save", QKeySequence.StandardKey.Save, self.save_current),
            ("Save all", QKeySequence("Ctrl+Shift+S"), self.save_all),
            ("Find", QKeySequence.StandardKey.Find, self.search.setFocus),
            ("Delete", QKeySequence("Ctrl+Delete"), self.delete),
        ]:
            action = QAction(text, self, shortcut=shortcut)
            action.triggered.connect(slot)
            self.addAction(action)

        self.setWindowTitle("midis2jam2 voice catalogues")
        self.resize(1250, 780)
        self.refresh_file_list()
        self.files.setCurrentRow(0)

    # Showing ----------------------------------------------------------------------------------------------------------

    def valid_looks(self) -> list[str]:
        return self.kit_looks if self.current and self.current.kits else self.looks

    def refresh_file_list(self):
        for row in range(self.files.count()):
            item = self.files.item(row)
            path = item.data(Qt.ItemDataRole.UserRole)
            entries = self.catalogues[path].entries
            mark = " •" if path in self.dirty else ""
            item.setText(f"{TITLES.get(path, path)}{mark}\n  {len(entries)} entries")
            item.setToolTip(str(self.catalogues[path].file))

    def open(self, path: str):
        self.current = self.catalogues[path]
        self.fill()

    def fill(self):
        catalogue = self.current
        self.filling = True
        self.rows = catalogue.entries
        self.table.setRowCount(len(self.rows))
        for row, entry in enumerate(self.rows):
            values = [str(entry.program), str(entry.msb), str(entry.lsb), entry.name, entry.look or "", "", entry.section]
            for column, value in enumerate(values):
                item = QTableWidgetItem(value)
                if column not in EDITABLE:
                    item.setFlags(item.flags() & ~Qt.ItemFlag.ItemIsEditable)
                if column in (PROGRAM, MSB, LSB):
                    item.setTextAlignment(Qt.AlignmentFlag.AlignRight | Qt.AlignmentFlag.AlignVCenter)
                self.table.setItem(row, column, item)
        self.filling = False
        self.recheck()

    def recheck(self):
        """Recomputes what every row shows as, and marks problems."""
        catalogue = self.current
        found = data.problems(catalogue, self.looks, self.kit_looks)
        self.filling = True
        for row, entry in enumerate(self.rows):
            shown, fallback = data.shows_as(entry, catalogue, self.catalogues)
            cell = self.table.item(row, SHOWS_AS)
            cell.setText(f"↳ {shown}" if fallback else shown)
            cell.setForeground(FALLBACK if fallback else QBrush())
            cell.setToolTip("Falls back to General MIDI" if fallback else "")
            messages = found.get(row, [])
            for column in range(len(HEADERS)):
                self.table.item(row, column).setBackground(PROBLEM if messages else QBrush())
            self.table.item(row, NAME).setToolTip("\n".join(messages))
        self.filling = False

        total = len(found.get(-1, [])) + sum(len(m) for i, m in found.items() if i >= 0)
        with_look = sum(1 for e in self.rows if e.look)
        text = f"{len(self.rows)} entries · {with_look} with a look"
        if total:
            text += f" · <span style='color:#c00'>{total} problem(s)</span>"
        self.summary.setText(text)
        self.summary.setToolTip("\n".join(found.get(-1, [])))
        self.apply_filter()

    def apply_filter(self):
        needle = self.search.text().strip().lower()
        for row, entry in enumerate(self.rows):
            haystack = f"{entry.name} {entry.look or ''} {entry.section} {self.table.item(row, SHOWS_AS).text()}"
            hidden = (
                (needle and needle not in haystack.lower())
                or (self.only_looks.isChecked() and not entry.look)
                or (self.only_problems.isChecked() and self.table.item(row, PROGRAM).background() != PROBLEM)
            )
            self.table.setRowHidden(row, bool(hidden))

    # Editing ----------------------------------------------------------------------------------------------------------

    def edited(self, item: QTableWidgetItem):
        if self.filling:
            return
        entry = self.rows[item.row()]
        text = item.text().strip()
        column = item.column()
        if column in (PROGRAM, MSB, LSB):
            try:
                value = int(text)
            except ValueError:
                self.filling = True
                item.setText(str({PROGRAM: entry.program, MSB: entry.msb, LSB: entry.lsb}[column]))
                self.filling = False
                return
            setattr(entry, {PROGRAM: "program", MSB: "msb", LSB: "lsb"}[column], value)
        elif column == NAME:
            entry.name = item.text()
        elif column == LOOK:
            entry.look = text or None
        self.mark_dirty()
        self.recheck()

    def mark_dirty(self):
        self.dirty.add(self.current.path)
        self.refresh_file_list()

    def add(self):
        catalogue = self.current
        selected = self.selected_entries()
        after = selected[-1] if selected else None
        entry = data.Entry(
            program=after.program if after else 1,
            name="New voice",
            msb=after.msb if after else (catalogue.entries[0].msb if catalogue.entries else 0),
            lsb=after.lsb if after else 0,
            written={"msb"} if catalogue.writes_msb else set(),
        )
        position = catalogue.items.index(after) + 1 if after else len(catalogue.items)
        catalogue.items.insert(position, entry)
        catalogue.update_sections()
        self.mark_dirty()
        self.fill()
        row = self.rows.index(entry)
        self.table.setCurrentCell(row, NAME)
        self.table.editItem(self.table.item(row, NAME))

    def delete(self):
        selected = self.selected_entries()
        if not selected:
            return
        names = ", ".join(e.name for e in selected[:5]) + ("…" if len(selected) > 5 else "")
        if QMessageBox.question(self, "Delete", f"Delete {len(selected)} entr{'y' if len(selected) == 1 else 'ies'}? "
                                               f"({names})") != QMessageBox.StandardButton.Yes:
            return
        for entry in selected:
            self.current.items.remove(entry)
        self.mark_dirty()
        self.fill()

    def selected_entries(self) -> list[data.Entry]:
        rows = sorted({index.row() for index in self.table.selectionModel().selectedRows()})
        return [self.rows[row] for row in rows]

    # Saving -----------------------------------------------------------------------------------------------------------

    def save(self, path: str) -> bool:
        catalogue = self.catalogues[path]
        found = data.problems(catalogue, self.looks, self.kit_looks)
        if found:
            count = sum(len(m) for m in found.values())
            answer = QMessageBox.warning(
                self, "Save anyway?",
                f"{TITLES.get(path, path)} has {count} problem(s) the app's integrity test will reject. Save anyway?",
                QMessageBox.StandardButton.Save | QMessageBox.StandardButton.Cancel,
            )
            if answer != QMessageBox.StandardButton.Save:
                return False
        data.save(catalogue)
        self.dirty.discard(path)
        self.refresh_file_list()
        self.statusBar().showMessage(f"Saved {catalogue.file}", 4000)
        return True

    def save_current(self):
        if self.current:
            self.save(self.current.path)
            self.recheck()  # Another catalogue's fallbacks may depend on this one.

    def save_all(self):
        for path in sorted(self.dirty):
            if not self.save(path):
                return

    def closeEvent(self, event):
        if self.dirty:
            answer = QMessageBox.question(
                self, "Unsaved changes", "Save changes to " + ", ".join(TITLES.get(p, p) for p in sorted(self.dirty)) + "?",
                QMessageBox.StandardButton.Save | QMessageBox.StandardButton.Discard | QMessageBox.StandardButton.Cancel,
            )
            if answer == QMessageBox.StandardButton.Cancel:
                event.ignore()
                return
            if answer == QMessageBox.StandardButton.Save:
                self.save_all()
                if self.dirty:
                    event.ignore()
                    return
        event.accept()


def main():
    app = QApplication(sys.argv)
    window = Editor()
    window.show()
    sys.exit(app.exec())


if __name__ == "__main__":
    main()
