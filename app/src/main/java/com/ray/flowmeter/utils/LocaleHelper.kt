// Locale manager utility providing context wrapping to dynamically switch language resources at runtime.
package com.ray.flowmeter.utils

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

class LocaleContextWrapper(
    base: Context,
    private val realContext: Context
) : android.content.ContextWrapper(base) {
    override fun getBaseContext(): Context {
        return realContext
    }
}

object LocaleHelper {
    fun applyLocale(context: Context, languageCode: String): Context {
        val locale = if (languageCode.isEmpty()) {
            Locale.getDefault()
        } else {
            Locale.forLanguageTag(languageCode)
        }

        Locale.setDefault(locale)

        val localeManager = context.getSystemService(LocaleManager::class.java)
        if (localeManager != null) {
            val localeList = if (languageCode.isEmpty()) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList(locale)
            }
            if (localeManager.applicationLocales != localeList) {
                localeManager.applicationLocales = localeList
            }
        }

        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))
        config.setLayoutDirection(locale)

        val configContext = context.createConfigurationContext(config)
        return LocaleContextWrapper(configContext, context)
    }
}
