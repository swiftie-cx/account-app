package com.swiftiecx.timeledger.ui.localization

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Create a Context whose Resources resolve strings in the given [languageTag].
 *
 * - languageTag == "" means follow system.
 * - languageTag should be a BCP-47 tag, e.g. "ja", "es-MX", "pt-BR", "hi".
 */
fun createLocalizedContext(base: Context, languageTag: String): Context {
    val tag = languageTag.trim()
    if (tag.isEmpty() || tag == "system") return base

    val locale = Locale.forLanguageTag(tag)
    val config = Configuration(base.resources.configuration).apply {
        setLocales(LocaleList(locale))
    }
    return base.createConfigurationContext(config)
}

@Composable
fun LocalizedApp(languageTag: String, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val localized = remember(base, languageTag) { createLocalizedContext(base, languageTag) }
    CompositionLocalProvider(LocalContext provides localized) {
        content()
    }
}
