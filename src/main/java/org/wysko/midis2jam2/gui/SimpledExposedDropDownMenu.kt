package org.wysko.midis2jam2.gui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Defines a simple dropdown menu.
 */
@Composable
fun SimpleExposedDropDownMenu(
    values: List<String>,
    selectedIndex: Int,
    onChange: (Int) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier,
    backgroundColor: Color = MaterialTheme.colors.onSurface.copy(alpha = TextFieldDefaults.BackgroundOpacity),
    shape: Shape = MaterialTheme.shapes.small.copy(bottomEnd = ZeroCornerSize, bottomStart = ZeroCornerSize)
) {
    SimpleExposedDropDownMenuImpl(
        values = values,
        selectedIndex = selectedIndex,
        onChange = onChange,
        label = label,
        modifier = modifier,
        backgroundColor = backgroundColor,
        shape = shape,
        decorator = { color, width, content ->
            Box(
                Modifier
                    .drawBehind {
                        val strokeWidth = width.value * density
                        val y = size.height - strokeWidth / 2
                        drawLine(
                            color,
                            Offset(0f, y),
                            Offset(size.width, y),
                            strokeWidth
                        )
                    }
            ) {
                content()
            }
        }
    )
}

@Composable
private fun SimpleExposedDropDownMenuImpl(
    values: List<String>,
    selectedIndex: Int,
    onChange: (Int) -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier,
    backgroundColor: Color,
    shape: Shape,
    decorator: @Composable (Color, Dp, @Composable () -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(Size.Zero) }

    val indicatorColor =
        if (expanded) MaterialTheme.colors.primary.copy(alpha = ContentAlpha.high)
        else MaterialTheme.colors.onSurface.copy(alpha = TextFieldDefaults.UnfocusedIndicatorLineOpacity)
    val indicatorWidth = (if (expanded) 2 else 1).dp
    val labelColor =
        if (expanded) MaterialTheme.colors.primary.copy(alpha = ContentAlpha.high)
        else MaterialTheme.colors.onSurface.copy(ContentAlpha.medium)
    val trailingIconColor = MaterialTheme.colors.onSurface.copy(alpha = TextFieldDefaults.IconOpacity)

    val rotation: Float by animateFloatAsState(if (expanded) 180f else 0f)

    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.width(IntrinsicSize.Min)) {
        decorator(indicatorColor, indicatorWidth) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(color = backgroundColor, shape = shape)
                    .onGloballyPositioned { textFieldSize = it.size.toSize() }
                    .clip(shape)
                    .clickable {
                        expanded = !expanded
                        focusManager.clearFocus()
                    }
                    .padding(start = 16.dp, end = 12.dp, top = 7.dp, bottom = 10.dp)
            ) {
                Column(Modifier.padding(end = 32.dp)) {
                    ProvideTextStyle(value = MaterialTheme.typography.caption.copy(color = labelColor)) {
                        label()
                    }
                    Text(
                        text = values[selectedIndex],
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "Change",
                    tint = trailingIconColor,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(top = 4.dp)
                        .rotate(rotation)
                )

            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(with(LocalDensity.current) { textFieldSize.width.toDp() })
        ) {
            values.forEachIndexed { i, v ->
                val scope = rememberCoroutineScope()
                DropdownMenuItem(
                    onClick = {
                        onChange(i)
                        scope.launch {
                            delay(0)
                            expanded = false
                        }
                    }
                ) {
                    Text(v)
                }
            }
        }
    }
}