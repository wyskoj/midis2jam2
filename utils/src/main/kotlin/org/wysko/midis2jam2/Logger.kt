package org.wysko.midis2jam2

import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger

/**
 * Convenience function to obtain the logger.
 */
fun <T : Any> T.logger(): Logger = getLogger(javaClass)
