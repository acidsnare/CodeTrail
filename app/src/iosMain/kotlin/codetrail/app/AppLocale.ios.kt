package codetrail.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import platform.Foundation.NSUserDefaults

/**
 * Compose resources on iOS follow NSLocale.preferredLanguages, which an app can only steer
 * through the "AppleLanguages" default, read at process start. The choice is stored here and
 * takes effect the next time the app launches.
 */
@Composable
actual fun applyLanguage(language: AppLanguage) {
    LaunchedEffect(language) {
        val defaults = NSUserDefaults.standardUserDefaults
        val wanted = language.tag ?: ""
        val stored = defaults.stringForKey(LANG_KEY) ?: ""
        if (wanted == stored) return@LaunchedEffect
        if (wanted.isEmpty()) {
            defaults.removeObjectForKey(LANG_KEY)
            defaults.removeObjectForKey("AppleLanguages")
        } else {
            defaults.setObject(wanted, LANG_KEY)
            defaults.setObject(listOf(wanted), "AppleLanguages")
        }
    }
}

private const val LANG_KEY = "codetrail.lang"
