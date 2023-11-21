package com.toasterofbread.spmp.model.settings

import com.google.gson.Gson
import com.toasterofbread.composekit.platform.PlatformFile
import com.toasterofbread.composekit.platform.PlatformPreferences
import com.toasterofbread.composekit.platform.putAny
import com.toasterofbread.spmp.model.settings.category.SettingsCategory
import com.toasterofbread.spmp.platform.AppContext
import com.toasterofbread.spmp.youtubeapi.fromJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SettingsImportExport {
    data class SettingsExportData(
        val included_categories: List<String>?,
        val values: Map<String, Any>?
    ) {
        fun getCategories(): List<SettingsCategory>? =
            included_categories?.map { SettingsCategory.fromId(it) }
    }

    suspend fun exportSettings(
        context: AppContext,
        file: PlatformFile,
        categories: List<SettingsCategory>
    ) = withContext(Dispatchers.IO) {
        val prefs: PlatformPreferences = context.getPrefs()
        val values: MutableMap<String, Any> = mutableMapOf()

        for (category in categories) {
            for (key in category.keys) {
                val value: Any = key.get(prefs)
                if (value != key.getDefaultValue()) {
                    values[key.getName()] = value
                }
            }
        }

        val data: SettingsExportData = SettingsExportData(
            included_categories = categories.map { it.id },
            values = values
        )

        file.outputStream().writer().use { writer ->
            writer.write(Gson().toJson(data))
        }
    }

    suspend fun loadSettingsFile(file: PlatformFile): SettingsExportData = withContext(Dispatchers.IO) {
        return@withContext file.inputStream().reader().use { reader ->
            Gson().fromJson(reader.readText())
        }
    }

    fun importData(context: AppContext, data: SettingsExportData, categories: List<SettingsCategory>?) {
        if (data.values == null) {
            return
        }

        context.getPrefs().edit {
            val all_categories: List<SettingsCategory> = SettingsCategory.all
            val included_categories: List<SettingsCategory>? = data.included_categories?.map { id ->
                SettingsCategory.fromId(id)
            }

            for (category in included_categories ?: all_categories) {
                if (categories != null && !categories.contains(category)) {
                    continue
                }

                for (key in category.keys) {
                    val name: String = key.getName()
                    putAny(name, data.values[name], key.getDefaultValue())
                }
            }
        }
    }
}
