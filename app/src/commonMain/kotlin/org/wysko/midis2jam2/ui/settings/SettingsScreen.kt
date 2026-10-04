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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import midis2jam2.app.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.wysko.midis2jam2.domain.SystemInteractionService
import org.wysko.midis2jam2.ui.BasicDeviceScaffold
import org.wysko.midis2jam2.ui.common.navigation.NavigationModel

/** Windows narrower than this show a list of categories that open one at a time, like a phone. */
private val CompactWidthThreshold = 720.dp

private val RailWidth = 232.dp
private val ContentMaxWidth = 680.dp

/**
 * The settings screen.
 *
 * In a wide window it is a category rail beside the selected page. In a narrow one (a phone) it is
 * a list of categories, each of which opens its page as a separate screen.
 */
object SettingsScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val model = koinScreenModel<SettingsModel>()
        val pages = rememberSettingsPages(model, koinScreenModel<SettingsScreenModel>())
        val systemInteractionService = koinInject<SystemInteractionService>()

        val openHelp = { systemInteractionService.openOnlineDocumentation() }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxWidth < CompactWidthThreshold
            CompositionLocalProvider(LocalSettingsCompact provides compact) {
                if (compact) {
                    BasicDeviceScaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(stringResource(Res.string.tab_settings)) },
                                actions = { HelpButton(openHelp) },
                            )
                        }
                    ) {
                        SettingsCategoryList(pages)
                    }
                } else {
                    // The rail runs the full height of the window, so it carries the title.
                    BasicDeviceScaffold {
                        SettingsMasterDetail(pages, openHelp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(Res.drawable.help),
            contentDescription = stringResource(Res.string.help),
        )
    }
}

/** One settings page, opened from the category list in a narrow window. */
internal class SettingsPageScreen(private val page: SettingsPage) : Screen {
    override val key: ScreenKey = "settings-page-${page.name}"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = koinScreenModel<SettingsModel>()
        val content = rememberSettingsPages(model, koinScreenModel<SettingsScreenModel>()).firstOrNull { it.page == page }

        BasicDeviceScaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(page.title)) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                painter = painterResource(Res.drawable.arrow_back),
                                contentDescription = stringResource(Res.string.back),
                            )
                        }
                    },
                )
            }
        ) {
            CompositionLocalProvider(LocalSettingsCompact provides true) {
                content?.let { SettingsPageBody(it, showHeader = false) }
            }
        }
    }
}

@Composable
private fun rememberSettingsPages(model: SettingsModel, screenModel: SettingsScreenModel): List<SettingsPageContent> {
    val settings = model.appSettings.collectAsState()
    return remember(model, screenModel) { settingsPages(settings, model, screenModel) }
}

@Composable
private fun SettingsMasterDetail(pages: List<SettingsPageContent>, onHelp: () -> Unit) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val selected = pages[selectedIndex.coerceIn(pages.indices)]

    // Another screen can send the user straight to a page, such as the one a warning is about.
    val navigationModel = koinInject<NavigationModel>()
    val requestedPage by navigationModel.requestedSettingsPage.collectAsState()
    LaunchedEffect(requestedPage) {
        requestedPage?.let { requested ->
            pages.indexOfFirst { it.page == requested }.takeIf { it >= 0 }?.let { selectedIndex = it }
            navigationModel.clearRequestedSettingsPage()
        }
    }

    Row(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(RailWidth)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(start = 24.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.tab_settings),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                HelpButton(onHelp)
            }
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                pages.forEachIndexed { index, content ->
                    SettingsRailItem(
                        content = content,
                        selected = content === selected,
                        onClick = { selectedIndex = index },
                    )
                }
            }
        }
        VerticalDivider(color = settingsDividerColor())
        SettingsPageBody(selected, showHeader = true, modifier = Modifier.weight(1f))
    }
}

/**
 * A category in the rail. The stock drawer item is 56dp tall, which leaves a small label floating
 * in a large pill, so this one is a snug 44dp.
 */
@Composable
private fun SettingsRailItem(content: SettingsPageContent, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(content.icon), contentDescription = null, tint = contentColor)
        Text(
            text = stringResource(content.page.title),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SettingsCategoryList(pages: List<SettingsPageContent>) {
    val navigator = LocalNavigator.currentOrThrow
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        SettingsCard {
            pages.forEachIndexed { index, content ->
                if (index > 0) HorizontalDivider(color = settingsDividerColor())
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navigator.push(SettingsPageScreen(content.page)) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(content.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(content.page.title),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = stringResource(content.summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        painter = painterResource(Res.drawable.chevron_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsPageBody(content: SettingsPageContent, showHeader: Boolean, modifier: Modifier = Modifier) {
    val compact = LocalSettingsCompact.current
    key(content.page) {
        Box(modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(
                modifier = Modifier
                    .widthIn(max = ContentMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 8.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                if (showHeader) {
                    Column {
                        Text(
                            text = stringResource(content.page.title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = stringResource(content.summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                content.sections.forEach { SettingsSectionBlock(it) }
            }
        }
    }
}
