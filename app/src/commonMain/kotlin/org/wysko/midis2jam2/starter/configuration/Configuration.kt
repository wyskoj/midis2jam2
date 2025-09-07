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

package org.wysko.midis2jam2.starter.configuration

import org.wysko.midis2jam2.domain.HomeScreenModel
import org.wysko.midis2jam2.domain.settings.SettingsRepository
import kotlin.reflect.KClass

/**
 * The parent of all configuration classes.
 */
interface Configuration

/**
 * Returns the first element in the collection that is an instance of the specified type.
 *
 * @param type the class reference representing the type
 * @return the first element of the specified type in the collection, or `null` if no such element is found
 *
 * @suppress This function performs unchecked cast, so it suppresses the unchecked cast warning
 */
@Suppress("kotlin:S6530", "UNCHECKED_CAST")
@Deprecated("Use find instead", ReplaceWith("find()"))
fun <T : Configuration> Collection<Configuration>.getType(type: KClass<T>): T {
    return this.firstOrNull { type.isInstance(it) } as T
}

/**
 * Returns the first element in the collection that is an instance of the specified type.
 * If no such element is found, throws an exception with the specified message.
 *
 * @param type the class reference representing the type
 * @return the first element of the specified type in the collection
 */
@Deprecated("Use find instead", ReplaceWith("find()"))
operator fun <T : Configuration> Collection<Configuration>.get(type: KClass<T>): T = getType(type)

inline fun <reified T : Configuration> Collection<Configuration>.find(): T = getType(T::class)

class ConfigurationService(
    private val homeTabModel: HomeScreenModel,
    private val settingsRepository: SettingsRepository,
) {
    fun getConfigurations(): List<Configuration> {
        return buildList {
            add(
                HomeConfiguration(
                    selectedMidiDevice = homeTabModel.selectedMidiDevice.value.name,
                    selectedSoundbank = homeTabModel.selectedSoundbank.value?.path,
                    isLooping = homeTabModel.isLooping.value,
                )
            )

            add(AppSettingsConfiguration(settingsRepository.appSettings.value))
        }
    }
}