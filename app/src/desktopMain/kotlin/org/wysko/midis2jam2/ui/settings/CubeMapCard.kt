/*
 * Copyright (C) 2025 Jacob Wysko
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

package org.wysko.midis2jam2.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import midis2jam2.app.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.koin.compose.koinInject
import org.wysko.midis2jam2.domain.BackgroundImageRepository
import org.wysko.midis2jam2.domain.CUBE_MAP_IMAGE_EXTENSIONS
import org.wysko.midis2jam2.domain.SystemInteractionService
import org.wysko.midis2jam2.domain.cubeMapProblems
import org.wysko.midis2jam2.domain.importBackgroundImage
import org.wysko.midis2jam2.domain.isCubeMapImageName
import org.wysko.midis2jam2.domain.settings.AppSettings
import org.wysko.midis2jam2.starter.configuration.BACKGROUND_IMAGES_FOLDER
import org.wysko.midis2jam2.ui.common.component.WarningAmber
import java.awt.datatransfer.DataFlavor
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Which side(s) the image picker is choosing for. */
private sealed interface PickerTarget {
    data class Side(val index: Int) : PickerTarget
    data object All : PickerTarget
}

private val TileShape = RoundedCornerShape(10.dp)

/**
 * The cube map picker: the six sides laid out as an unfolded cube, each showing the image it uses.
 *
 * An image is assigned by dropping a file on a side, or by clicking a side and choosing from the images
 * in the backgrounds folder (or importing one into it).
 */
@Composable
internal fun CubeMapCard(settings: State<AppSettings>, model: SettingsModel) {
    val repository = koinInject<BackgroundImageRepository>()
    val systemInteractionService = koinInject<SystemInteractionService>()
    val folder = repository.getTexturesFolder()

    // Bumped whenever the folder's contents change, to re-list it and re-check each side's file.
    var folderVersion by remember { mutableIntStateOf(0) }
    val images = remember(folderVersion) {
        repository.getAvailableImages().filter(::isCubeMapImageName).sortedBy { it.lowercase() }
    }
    val textures = settings.value.backgroundSettings.cubeMapTextures
    val faceNames = CubeMapFace.entries.associateWith { stringResource(it.label) }

    var pickerTarget by remember { mutableStateOf<PickerTarget?>(null) }

    fun assign(target: PickerTarget, name: String) = when (target) {
        is PickerTarget.Side -> model.setCubeMapTexture(target.index, name)
        PickerTarget.All -> model.setAllCubeMapTextures(name)
    }

    fun import(files: List<File>): String? =
        files.firstNotNullOfOrNull { importBackgroundImage(it, folder) }.also { folderVersion++ }

    val currentTarget by rememberUpdatedState(pickerTarget)
    val importPicker = rememberFilePickerLauncher(
        type = FileKitType.File(CUBE_MAP_IMAGE_EXTENSIONS),
        mode = FileKitMode.Single,
    ) { file ->
        val target = currentTarget
        if (file != null && target != null) {
            import(listOf(file.file))?.let { assign(target, it) }
            pickerTarget = null
        }
    }

    val problems = remember(textures, folderVersion) { cubeMapProblems(settings.value.backgroundSettings, folder) }
    val unset = problems.unassigned.map { CubeMapFace.entries[it] }
    val missing = problems.missing.map { CubeMapFace.entries[it] }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.settings_background_type_cubemap),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f).padding(end = 8.dp),
            )
            OutlinedButton(onClick = { pickerTarget = PickerTarget.All }) {
                Icon(painterResource(Res.drawable.image), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.settings_background_cubemap_all_sides))
            }
            TextButton(onClick = { systemInteractionService.openFolder(BACKGROUND_IMAGES_FOLDER) }) {
                Text(stringResource(Res.string.settings_background_texture_open_folder))
            }
        }

        if (unset.isNotEmpty() || missing.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(painterResource(Res.drawable.warning), contentDescription = null, modifier = Modifier.size(20.dp))
                    Column {
                        if (unset.isNotEmpty()) {
                            Text(
                                stringResource(
                                    Res.string.settings_background_cubemap_unset,
                                    unset.joinToString { faceNames.getValue(it) },
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (missing.isNotEmpty()) {
                            Text(
                                stringResource(
                                    Res.string.settings_background_cubemap_missing,
                                    missing.joinToString { "${faceNames.getValue(it)} (${textures[it.index]})" },
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            stringResource(Res.string.background_warning_consequence_settings),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        CubeNet(
            textures = textures,
            faceNames = faceNames,
            folder = folder,
            folderVersion = folderVersion,
            onChoose = { pickerTarget = PickerTarget.Side(it.index) },
            onRemove = { model.setCubeMapTexture(it.index, "") },
            onDropFiles = { face, files ->
                import(files)?.let { model.setCubeMapTexture(face.index, it) }
            },
        )

        Text(
            text = stringResource(Res.string.settings_background_cubemap_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    pickerTarget?.let { target ->
        ImagePickerDialog(
            title = when (target) {
                is PickerTarget.Side -> stringResource(
                    Res.string.settings_background_cubemap_choose,
                    faceNames.getValue(CubeMapFace.entries[target.index]),
                )

                PickerTarget.All -> stringResource(Res.string.settings_background_cubemap_all_sides)
            },
            images = images,
            selected = (target as? PickerTarget.Side)?.let { textures[it.index] },
            folder = folder,
            folderVersion = folderVersion,
            onSelect = {
                assign(target, it)
                pickerTarget = null
            },
            onClear = (target as? PickerTarget.Side)?.let {
                {
                    assign(target, "")
                    pickerTarget = null
                }
            },
            onImport = { importPicker.launch() },
            onDismiss = { pickerTarget = null },
        )
    }
}

/** The six faces as an unfolded cube: Up above North, Down below it, and West, North, East, South in a row. */
@Composable
private fun ColumnScope.CubeNet(
    textures: List<String>,
    faceNames: Map<CubeMapFace, String>,
    folder: File,
    folderVersion: Int,
    onChoose: (CubeMapFace) -> Unit,
    onRemove: (CubeMapFace) -> Unit,
    onDropFiles: (CubeMapFace, List<File>) -> Unit,
) {
    @Composable
    fun RowScope.Cell(face: CubeMapFace?) {
        if (face == null) {
            Spacer(Modifier.weight(1f))
        } else {
            CubeTile(
                label = faceNames.getValue(face),
                fileName = textures[face.index],
                folder = folder,
                folderVersion = folderVersion,
                onClick = { onChoose(face) },
                onRemove = { onRemove(face) },
                onDropFiles = { onDropFiles(face, it) },
                modifier = Modifier.weight(1f),
            )
        }
    }

    val rows = listOf(
        listOf(null, CubeMapFace.Up, null, null),
        listOf(CubeMapFace.West, CubeMapFace.North, CubeMapFace.East, CubeMapFace.South),
        listOf(null, CubeMapFace.Down, null, null),
    )
    Column(
        modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().align(Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { Cell(it) }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CubeTile(
    label: String,
    fileName: String,
    folder: File,
    folderVersion: Int,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onDropFiles: (List<File>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val file = File(folder, fileName)
    val isUnset = fileName.isBlank()
    val isMissing = !isUnset && remember(fileName, folderVersion) { !file.exists() }
    val thumbnail = rememberThumbnail(file.takeIf { !isUnset && !isMissing }, folderVersion)

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isDragOver by remember { mutableStateOf(false) }
    val currentOnDropFiles by rememberUpdatedState(onDropFiles)
    val dropTarget = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) {
                isDragOver = true
            }

            override fun onExited(event: DragAndDropEvent) {
                isDragOver = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                isDragOver = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                isDragOver = false
                val files = event.droppedFiles()
                currentOnDropFiles(files)
                return files.isNotEmpty()
            }
        }
    }

    val colors = MaterialTheme.colorScheme
    val dashColor = when {
        isDragOver -> colors.primary
        isUnset -> WarningAmber
        else -> colors.outlineVariant
    }
    val tileDescription = when {
        isUnset -> "$label: ${stringResource(Res.string.settings_background_cubemap_add_image)}"
        isMissing -> "$label: $fileName (${stringResource(Res.string.settings_background_texture_file_not_found)})"
        else -> "$label: $fileName"
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(TileShape)
            .background(
                when {
                    isDragOver -> colors.primaryContainer
                    isMissing -> colors.errorContainer.copy(alpha = 0.4f)
                    else -> colors.surfaceContainerHighest
                }
            )
            .then(
                when {
                    isMissing -> Modifier.border(1.5.dp, colors.error, TileShape)
                    isUnset || isDragOver -> Modifier.drawBehind {
                        drawRoundRect(
                            color = dashColor,
                            cornerRadius = CornerRadius(10.dp.toPx()),
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
                            ),
                        )
                    }

                    else -> Modifier
                }
            )
            .hoverable(interactionSource)
            .dragAndDropTarget(shouldStartDragAndDrop = { true }, target = dropTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = tileDescription },
    ) {
        thumbnail?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }

        if (isUnset || isMissing || thumbnail == null) {
            Column(
                modifier = Modifier.matchParentSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val contentColor = if (isMissing) colors.onErrorContainer else colors.onSurfaceVariant
                if (isUnset) {
                    Icon(painterResource(Res.drawable.add_circle), contentDescription = null, tint = contentColor)
                }
                Text(
                    label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMissing) colors.onErrorContainer else colors.onSurface,
                )
                Text(
                    text = when {
                        isUnset -> stringResource(Res.string.settings_background_cubemap_add_image)
                        isMissing -> stringResource(Res.string.settings_background_texture_file_not_found)
                        else -> fileName
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isMissing) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor,
                        textDecoration = TextDecoration.LineThrough,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.alpha(0.8f),
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
                    .padding(start = 8.dp, end = 8.dp, top = 20.dp, bottom = 6.dp),
            ) {
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.White)
                Text(
                    fileName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (!isUnset) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp),
            ) {
                Icon(
                    painterResource(Res.drawable.close),
                    contentDescription = stringResource(Res.string.settings_background_cubemap_remove),
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImagePickerDialog(
    title: String,
    images: List<String>,
    selected: String?,
    folder: File,
    folderVersion: Int,
    onSelect: (String) -> Unit,
    onClear: (() -> Unit)?,
    onImport: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp),
                )
                if (images.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.settings_background_cubemap_no_library),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp).fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(112.dp),
                        modifier = Modifier.heightIn(max = 360.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(images, key = { it }) { name ->
                            PickerThumbnail(
                                name = name,
                                file = File(folder, name),
                                folderVersion = folderVersion,
                                isSelected = name == selected,
                                onClick = { onSelect(name) },
                            )
                        }
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onImport) {
                        Text(stringResource(Res.string.settings_playback_soundbanks_import))
                    }
                    Spacer(Modifier.weight(1f))
                    onClear?.let {
                        TextButton(onClick = it) { Text(stringResource(Res.string.settings_background_cubemap_no_image)) }
                    }
                    TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
                }
            }
        }
    }
}

@Composable
private fun PickerThumbnail(
    name: String,
    file: File,
    folderVersion: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val thumbnail = rememberThumbnail(file, folderVersion)
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceContainerHighest)
            .border(2.dp, if (isSelected) colors.primary else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = name },
    ) {
        thumbnail?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
                .padding(start = 6.dp, end = 6.dp, top = 14.dp, bottom = 4.dp),
        )
    }
}

/** The dropped files, or an empty list if the drop does not carry files. */
@OptIn(ExperimentalComposeUiApi::class)
private fun DragAndDropEvent.droppedFiles(): List<File> {
    val transferable = awtTransferable
    if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return emptyList()
    return (transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>).filterIsInstance<File>()
}

/**
 * A small preview of [file], decoded off the main thread, or `null` while it loads, when [file] is
 * `null`, or when it cannot be read as an image.
 */
@Composable
private fun rememberThumbnail(file: File?, folderVersion: Int): ImageBitmap? =
    produceState<ImageBitmap?>(initialValue = null, file?.path, folderVersion) {
        value = file?.let { withContext(Dispatchers.IO) { Thumbnails.load(it) } }
    }.value

/** Decoded previews, so dragging a slider or reopening the picker does not decode every image again. */
private object Thumbnails {
    private const val MAX_EDGE = 256
    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    fun load(file: File): ImageBitmap? {
        val key = "${file.path}:${file.lastModified()}:${file.length()}"
        cache[key]?.let { return it }
        return runCatching {
            val image = SkiaImage.makeFromEncoded(file.readBytes())
            val scale = min(1f, MAX_EDGE.toFloat() / max(image.width, image.height))
            val width = max(1, (image.width * scale).roundToInt())
            val height = max(1, (image.height * scale).roundToInt())
            val surface = Surface.makeRasterN32Premul(width, height)
            surface.canvas.drawImageRect(image, Rect.makeWH(width.toFloat(), height.toFloat()))
            surface.makeImageSnapshot().toComposeImageBitmap()
        }.getOrNull()?.also { cache[key] = it }
    }
}
