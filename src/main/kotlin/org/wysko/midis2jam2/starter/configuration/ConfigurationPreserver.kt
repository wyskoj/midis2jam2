/*
 * Copyright (C) 2024 Jacob Wysko
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

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.util.logger
import java.io.File


private val USER_HOME = File(System.getProperty("user.home"))

val APPLICATION_CONFIG_HOME = File(USER_HOME, ".midis2jam2").also {
    it.mkdirs() // Create the application home directory if it doesn't exist.
}

class ConfigurationPreserver<T>(
    private val serializer: SerializationStrategy<T>,
    private val deserializer: DeserializationStrategy<T>,
    private val file: File
) {
    fun saveConfiguration(configuration: T) =
        file.run {
            try {
                writeText(Json.encodeToString(serializer, configuration))
            } catch (e: Exception) {
                this@ConfigurationPreserver.logger().error("Failed to save configuration to file: $absolutePath", e)
            }
        }

    fun getConfiguration(): T? {
        if (!file.exists()) {
            return null
        }
        return try {
            Json.decodeFromString(deserializer, file.readText())
        } catch (e: Exception) {
            logger().error("Failed to load configuration from file: ${file.absolutePath}", e)
            null
        }
    }
}