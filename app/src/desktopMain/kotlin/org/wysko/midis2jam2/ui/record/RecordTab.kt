/*
 * Copyright (C) 2026 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.wysko.midis2jam2.ui.record

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.layout.layout
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import io.github.vinceglb.filekit.PlatformFile
import midis2jam2.app.generated.resources.Res
import midis2jam2.app.generated.resources.cancel
import midis2jam2.app.generated.resources.check_circle
import midis2jam2.app.generated.resources.error
import midis2jam2.app.generated.resources.folder
import midis2jam2.app.generated.resources.music_note
import midis2jam2.app.generated.resources.record_audio_note
import midis2jam2.app.generated.resources.record_cancelled
import midis2jam2.app.generated.resources.record_change_file
import midis2jam2.app.generated.resources.record_choose_file
import midis2jam2.app.generated.resources.record_drop_hint
import midis2jam2.app.generated.resources.record_empty_body
import midis2jam2.app.generated.resources.record_empty_title
import midis2jam2.app.generated.resources.record_failed_title
import midis2jam2.app.generated.resources.record_frame_rate
import midis2jam2.app.generated.resources.record_in_folder
import midis2jam2.app.generated.resources.record_open_video
import midis2jam2.app.generated.resources.record_output
import midis2jam2.app.generated.resources.record_preparing
import midis2jam2.app.generated.resources.record_quality
import midis2jam2.app.generated.resources.record_quality_high
import midis2jam2.app.generated.resources.record_quality_high_detail
import midis2jam2.app.generated.resources.record_quality_low
import midis2jam2.app.generated.resources.record_quality_low_detail
import midis2jam2.app.generated.resources.record_quality_maximum
import midis2jam2.app.generated.resources.record_quality_medium
import midis2jam2.app.generated.resources.record_quality_medium_detail
import midis2jam2.app.generated.resources.record_recording
import midis2jam2.app.generated.resources.record_resolution
import midis2jam2.app.generated.resources.record_saved
import midis2jam2.app.generated.resources.record_section_audio
import midis2jam2.app.generated.resources.record_section_video
import midis2jam2.app.generated.resources.record_show_in_folder
import midis2jam2.app.generated.resources.record_start
import midis2jam2.app.generated.resources.record_time_left
import midis2jam2.app.generated.resources.record_unreadable
import midis2jam2.app.generated.resources.record_window_note
import midis2jam2.app.generated.resources.soundbank
import midis2jam2.app.generated.resources.soundbank_default
import midis2jam2.app.generated.resources.tab_record
import midis2jam2.app.generated.resources.videocam
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.wysko.midis2jam2.domain.BackgroundWarning
import org.wysko.midis2jam2.domain.SystemInteractionService
import org.wysko.midis2jam2.midi.search.MIDI_FILE_EXTENSIONS
import org.wysko.midis2jam2.record.RecordingState
import org.wysko.midis2jam2.record.VideoQuality
import org.wysko.midis2jam2.ui.common.component.BackgroundWarningDialog
import org.wysko.midis2jam2.util.FileDragAndDrop
import java.io.File
import kotlin.time.Duration

/*
 * Spacing follows one scale (4, 8, 12, 16, 24, 32, 48 dp), with more space around a group than inside it, so the
 * groups read without needing borders.
 */
private val LABEL_GAP = 8.dp
private val CONTROL_GAP = 20.dp
private val GROUP_GAP = 32.dp

/** How far a clickable row's highlight reaches past its content, which stays aligned with everything else. */
private val ROW_BLEED_HORIZONTAL = 12.dp
private val ROW_BLEED_VERTICAL = 8.dp

/** How faded a control is while it can't be used. */
private const val DISABLED_ALPHA = 0.38f

object RecordTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 0u,
            title = stringResource(Res.string.tab_record),
            icon = null,
        )

    @Composable
    override fun Content() {
        val model = koinScreenModel<RecordTabModel>()
        val systemInteractionService = koinInject<SystemInteractionService>()

        val midiFile by model.midiFile.collectAsState()
        val songInfo by model.songInfo.collectAsState()
        val outputFile by model.outputFile.collectAsState()
        val resolution by model.resolution.collectAsState()
        val fps by model.fps.collectAsState()
        val quality by model.quality.collectAsState()
        val soundbank by model.soundbank.collectAsState()
        val soundbanks by model.soundbanks.collectAsState(initial = emptyList())
        val recordingState by model.recordingState.collectAsState()
        val isRecordEnabled by model.isRecordEnabled.collectAsState(initial = false)
        val backgroundWarning by model.backgroundWarning.collectAsState(initial = null)
        var pendingBackgroundWarning by remember { mutableStateOf<BackgroundWarning?>(null) }
        val isRendering = recordingState is RecordingState.Rendering

        val midiFilePicker = model.midiFilePicker()
        val outputFilePicker = model.outputFilePicker()

        val dragAndDropTarget = remember {
            FileDragAndDrop {
                if (MIDI_FILE_EXTENSIONS.contains(it.extension.lowercase())) model.setMidiFile(it)
            }
        }

        pendingBackgroundWarning?.let { warning ->
            BackgroundWarningDialog(
                warningType = warning,
                onConfirm = {
                    pendingBackgroundWarning = null
                    model.startRecording()
                },
                onDismiss = { pendingBackgroundWarning = null },
            )
        }

        Scaffold(
            modifier = Modifier.dragAndDropTarget(
                shouldStartDragAndDrop = { true },
                target = dragAndDropTarget,
            )
        ) { _ ->
            val file = midiFile
            if (file == null) {
                EmptyState(onChooseFile = { midiFilePicker.launch() })
                return@Scaffold
            }

            Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(GROUP_GAP),
                    modifier = Modifier.align(Alignment.Center).widthIn(max = 512.dp)
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(CONTROL_GAP)) {
                        SongHeader(file, songInfo, enabled = !isRendering) { midiFilePicker.launch() }
                        OutputRow(outputFile, enabled = !isRendering) {
                            outputFilePicker.launch(
                                suggestedName = outputFile?.nameWithoutExtension ?: "midis2jam2",
                                defaultExtension = "mp4",
                                directory = outputFile?.parentFile?.let { PlatformFile(it) },
                            )
                        }
                    }

                    Group(stringResource(Res.string.record_section_video)) {
                        Labelled(stringResource(Res.string.record_resolution)) {
                            SelectableCards(
                                options = model.resolutions,
                                selected = resolution,
                                enabled = !isRendering,
                                title = { it.label.substringBefore(' ') },
                                detail = { "${it.width} × ${it.height}" },
                                onSelect = model::setResolution,
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.height(IntrinsicSize.Min),
                        ) {
                            Labelled(
                                stringResource(Res.string.record_frame_rate),
                                Modifier.weight(1f).fillMaxHeight(),
                            ) {
                                CompactDropdown(
                                    options = model.frameRates,
                                    selected = fps,
                                    optionLabel = { "$it fps" },
                                    enabled = !isRendering,
                                    modifier = Modifier.weight(1f),
                                    onSelect = model::setFps,
                                )
                            }
                            Labelled(stringResource(Res.string.record_quality), Modifier.weight(3f)) {
                                SelectableCards(
                                    options = model.qualities,
                                    selected = quality,
                                    enabled = !isRendering,
                                    title = { stringResource(it.label) },
                                    detail = { stringResource(it.detail) },
                                    onSelect = model::setQuality,
                                )
                            }
                        }
                    }

                    Group(stringResource(Res.string.record_section_audio)) {
                        Labelled(stringResource(Res.string.soundbank)) {
                            val default = stringResource(Res.string.soundbank_default)
                            CompactDropdown(
                                options = listOf<File?>(null) + soundbanks,
                                selected = soundbank,
                                optionLabel = { it?.name ?: default },
                                enabled = !isRendering,
                                onSelect = model::setSoundbank,
                            )
                            Note(stringResource(Res.string.record_audio_note))
                        }
                    }

                    ActionArea(
                        state = recordingState,
                        isRecordEnabled = isRecordEnabled,
                        onRecord = {
                            when (val warning = backgroundWarning) {
                                null -> model.startRecording()
                                else -> pendingBackgroundWarning = warning
                            }
                        },
                        onCancel = model::cancelRecording,
                        onOpenVideo = { systemInteractionService.openFolder(it) },
                        onShowInFolder = { systemInteractionService.openFolder(it.absoluteFile.parentFile) },
                    )
                }
            }
        }
    }
}

/** Before a file is chosen, the one thing to do is choose one; nothing else is worth showing yet. */
@Composable
private fun EmptyState(onChooseFile: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 400.dp)) {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(Res.drawable.videocam),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(stringResource(Res.string.record_empty_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(Res.string.record_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Button(onClick = onChooseFile, modifier = Modifier.height(48.dp)) {
                Text(stringResource(Res.string.record_choose_file))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(Res.string.record_drop_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The chosen song, which heads the form: its name, and how long it plays (or why it can't be recorded). */
@Composable
private fun SongHeader(file: File, info: SongInfo?, enabled: Boolean, onChange: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .bleed(ROW_BLEED_HORIZONTAL, ROW_BLEED_VERTICAL)
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onChange)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = ROW_BLEED_HORIZONTAL, vertical = ROW_BLEED_VERTICAL),
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(Res.drawable.music_note),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                file.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            when {
                info == null -> Text("", style = MaterialTheme.typography.bodySmall)
                !info.isReadable -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(Res.drawable.error),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(Res.string.record_unreadable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                else -> Text(
                    info.duration?.let(::formatDuration) ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Just a label: the whole row is the click target, and gives the hover feedback.
        Text(
            stringResource(Res.string.record_change_file),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp).alpha(if (enabled) 1f else DISABLED_ALPHA),
        )
    }
}

/** Where the video goes: the file name first, and the folder after it in a quieter colour. */
@Composable
private fun OutputRow(file: File?, enabled: Boolean, onClick: () -> Unit) {
    Labelled(stringResource(Res.string.record_output)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .bleed(ROW_BLEED_HORIZONTAL, ROW_BLEED_VERTICAL)
                .clip(MaterialTheme.shapes.medium)
                .clickable(enabled = enabled, onClick = onClick)
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = ROW_BLEED_HORIZONTAL, vertical = ROW_BLEED_VERTICAL),
        ) {
            Icon(
                painterResource(Res.drawable.folder),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(file?.name ?: "", style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Spacer(Modifier.width(8.dp))
            file?.absoluteFile?.parentFile?.let { folder ->
                Text(
                    stringResource(Res.string.record_in_folder, abbreviateHome(folder)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** A group of controls under a small, letter-spaced heading. */
@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CONTROL_GAP)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.1.em),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

/** A control with its label above it. Every control in the tab is labelled the same way, so they line up. */
@Composable
private fun Labelled(label: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(LABEL_GAP)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

/**
 * A row of cards, one per option, each with a title over a detail line. The chosen card is filled, outlined and
 * ticked, so the choice doesn't rest on colour alone.
 */
@Composable
private fun <T> SelectableCards(
    options: List<T>,
    selected: T,
    enabled: Boolean,
    title: @Composable (T) -> String,
    detail: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup()
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerLow
                },
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .clip(MaterialTheme.shapes.medium)
                    .selectable(
                        selected = isSelected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    ),
            ) {
                Column(Modifier.padding(12.dp)) {
                    // The tick shares the title's line, so the detail below keeps the card's full width.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            title(option),
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                painterResource(Res.drawable.check_circle),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Text(
                        detail(option),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** A drop-down drawn as a tile, like an unselected card, so the tab has one visual language for choices. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> CompactDropdown(
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = isExpanded, onExpandedChange = { }, modifier = modifier) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth().fillMaxHeight()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .clip(MaterialTheme.shapes.medium)
                .clickable(enabled = enabled) { isExpanded = !isExpanded }
                .alpha(if (enabled) 1f else DISABLED_ALPHA),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            ) {
                Text(
                    optionLabel(selected),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
            }
        }
        ExposedDropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

/** The Record button, or the progress of the recording in its place, and how the last one went. */
@Composable
private fun ActionArea(
    state: RecordingState,
    isRecordEnabled: Boolean,
    onRecord: () -> Unit,
    onCancel: () -> Unit,
    onOpenVideo: (File) -> Unit,
    onShowInFolder: (File) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        when (state) {
            is RecordingState.Finished -> ResultPanel(
                icon = Res.drawable.check_circle,
                tint = MaterialTheme.colorScheme.primary,
                title = stringResource(Res.string.record_saved),
                detail = state.file.name,
            ) {
                FilledTonalButton(onClick = { onOpenVideo(state.file) }) {
                    Text(stringResource(Res.string.record_open_video))
                }
                TextButton(onClick = { onShowInFolder(state.file) }) {
                    Text(stringResource(Res.string.record_show_in_folder))
                }
            }

            is RecordingState.Failed -> ResultPanel(
                icon = Res.drawable.error,
                tint = MaterialTheme.colorScheme.error,
                title = stringResource(Res.string.record_failed_title),
                detail = state.message,
            )

            RecordingState.Cancelled -> Text(
                stringResource(Res.string.record_cancelled),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> Unit
        }

        if (state is RecordingState.Rendering) {
            Progress(state, onCancel)
        } else {
            Button(
                onClick = onRecord,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = isRecordEnabled,
            ) {
                Box(Modifier.size(12.dp).background(LocalContentColor.current, CircleShape))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(Res.string.record_start), fontSize = 16.sp)
            }
        }
        // Once there's a result to show, the reminder about the preview window has done its job.
        if (state !is RecordingState.Finished && state !is RecordingState.Failed) {
            Note(stringResource(Res.string.record_window_note))
        }
    }
}

@Composable
private fun Progress(state: RecordingState.Rendering, onCancel: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row {
                Text(
                    when (val progress = state.progress) {
                        null -> stringResource(Res.string.record_preparing)
                        else -> "${stringResource(Res.string.record_recording)} ${(progress * 100).toInt()}%"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.alignByBaseline(),
                )
                Spacer(Modifier.weight(1f))
                state.remaining?.let {
                    Text(
                        stringResource(Res.string.record_time_left, formatDuration(it)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
            when (val progress = state.progress) {
                null -> LinearProgressIndicator(Modifier.fillMaxWidth())
                else -> LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
        }
        OutlinedButton(onClick = onCancel) { Text(stringResource(Res.string.cancel)) }
    }
}

/** How a recording turned out, with an icon so the outcome isn't told by colour alone. */
@Composable
private fun ResultPanel(
    icon: DrawableResource,
    tint: Color,
    title: String,
    detail: String,
    actions: (@Composable () -> Unit)? = null,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            actions?.let {
                Spacer(Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    it()
                }
            }
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Lets this element reach [horizontal] and [vertical] past the space it's given, without taking up any more of it.
 *
 * Followed by a `clickable` and a matching `padding`, it gives the click target's highlight room around its content
 * while the content itself stays exactly where it was, lined up with the rest of the tab.
 */
private fun Modifier.bleed(horizontal: Dp, vertical: Dp): Modifier = layout { measurable, constraints ->
    val h = horizontal.roundToPx()
    val v = vertical.roundToPx()
    val placeable = measurable.measure(constraints.offset(horizontal = 2 * h, vertical = 2 * v))
    layout(placeable.width - 2 * h, placeable.height - 2 * v) {
        placeable.place(-h, -v)
    }
}

/** Shortens a path in the user's home folder to start with `~`, so more of it fits. */
private fun abbreviateHome(file: File): String {
    val home = System.getProperty("user.home") ?: return file.path
    val path = file.absolutePath
    return if (path == home || path.startsWith(home + File.separator)) "~" + path.removePrefix(home) else path
}

/** A song or time-remaining length, as `m:ss`, or `h:mm:ss` from an hour up. */
internal fun formatDuration(duration: Duration): String {
    val total = duration.inWholeSeconds.coerceAtLeast(0)
    val hours = total / 3600
    val minutes = total % 3600 / 60
    val seconds = total % 60
    return when {
        hours > 0 -> "%d:%02d:%02d".format(hours, minutes, seconds)
        else -> "%d:%02d".format(minutes, seconds)
    }
}

private val VideoQuality.label: StringResource
    get() = when (this) {
        VideoQuality.Low -> Res.string.record_quality_low
        VideoQuality.Medium -> Res.string.record_quality_medium
        VideoQuality.High -> Res.string.record_quality_high
        VideoQuality.Maximum -> Res.string.record_quality_maximum
    }

private val VideoQuality.detail: StringResource
    get() = when (this) {
        VideoQuality.Low -> Res.string.record_quality_low_detail
        VideoQuality.Medium -> Res.string.record_quality_medium_detail
        VideoQuality.High, VideoQuality.Maximum -> Res.string.record_quality_high_detail
    }
