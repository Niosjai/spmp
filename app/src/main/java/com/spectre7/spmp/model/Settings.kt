package com.spectre7.spmp.model

import android.content.SharedPreferences
import com.spectre7.spmp.MainActivity
import com.spectre7.spmp.PlayerAccessibilityService
import java.util.Locale

enum class Settings {
    // Language
    KEY_LANG_UI,
    KEY_LANG_DATA,

    // Themeing
    KEY_ACCENT_COLOUR_SOURCE,
    KEY_NOWPLAYING_THEME_MODE,

    // Lyrics
    KEY_LYRICS_FOLLOW_ENABLED,
    KEY_LYRICS_FOLLOW_OFFSET,
    KEY_LYRICS_DEFAULT_FURIGANA,
    KEY_LYRICS_TEXT_ALIGNMENT,
    KEY_LYRICS_EXTRA_PADDING,
    
    // Audio & Video
    KEY_STREAM_AUDIO_QUALITY,
    KEY_DOWNLOAD_AUDIO_QUALITY,

    // Download
    KEY_AUTO_DOWNLOAD_THRESHOLD,
    KEY_AUTO_DOWNLOAD_QUALITY,
    KEY_AUTO_DOWNLOAD_SIZE_LIMIT,

    // Stats
    KEY_STATS_ENABLED,
    KEY_STATS_LISTEN_THRESHOLD,
    KEY_STATS_LISTEN_THRESHOLD_TYPE,

    // Accessibility Service
    KEY_ACC_VOL_INTERCEPT_MODE,
    KEY_ACC_VOL_INTERCEPT_NOTIFICATION,
    KEY_ACC_SCREEN_OFF,

    // Other
    KEY_VOLUME_STEPS;

    companion object {
        val prefs: SharedPreferences get() = MainActivity.prefs

        fun <T> get(enum_key: Settings, preferences: SharedPreferences = prefs, default: T? = null): T {
            val default_value: T = default ?: getDefault(enum_key)
            return when (default_value!!::class) {
                Boolean::class -> preferences.getBoolean(enum_key.name, default_value as Boolean)
                Float::class -> preferences.getFloat(enum_key.name, default_value as Float)
                Int::class -> preferences.getInt(enum_key.name, default_value as Int)
                Long::class -> preferences.getLong(enum_key.name, default_value as Long)
                String::class -> preferences.getString(enum_key.name, default_value as String)
                else -> throw ClassCastException()
            } as T
        }

        inline fun <reified T: Enum<T>> getEnum(enum_key: Settings, preferences: SharedPreferences = prefs, default: T? = null): T {
            val default_value: Int = default?.ordinal ?: getDefault(enum_key)
            return enumValues<T>()[preferences.getInt(enum_key.name, default_value)]
        }

        fun <T> getDefault(enum_key: Settings): T {
            return when (enum_key) {
                KEY_LANG_UI, KEY_LANG_DATA -> MainActivity.languages.keys.indexOf(Locale.getDefault().language)
                
                KEY_ACCENT_COLOUR_SOURCE -> 0
                KEY_NOWPLAYING_THEME_MODE -> 0
                
                KEY_LYRICS_FOLLOW_ENABLED -> true
                KEY_LYRICS_FOLLOW_OFFSET -> 0.5f
                KEY_LYRICS_DEFAULT_FURIGANA -> true
                KEY_LYRICS_TEXT_ALIGNMENT -> 0 // Left, center, right
                KEY_LYRICS_EXTRA_PADDING -> false
                
                KEY_STREAM_AUDIO_QUALITY -> Song.AudioQuality.MEDIUM.ordinal
                KEY_DOWNLOAD_AUDIO_QUALITY -> Song.AudioQuality.MEDIUM.ordinal

                KEY_STATS_ENABLED -> true
                KEY_STATS_LISTEN_THRESHOLD -> 1f // Minutes or percentage
                KEY_STATS_LISTEN_THRESHOLD_TYPE -> 0 // Absolute, percentage
                
                KEY_AUTO_DOWNLOAD_THRESHOLD -> 3 // Listens
                KEY_AUTO_DOWNLOAD_QUALITY -> Song.AudioQuality.MEDIUM.ordinal
                KEY_AUTO_DOWNLOAD_SIZE_LIMIT -> 1000000000 // Bytes
                
                KEY_ACC_VOL_INTERCEPT_MODE -> PlayerAccessibilityService.VOLUME_INTERCEPT_MODE.NEVER.ordinal
                KEY_ACC_VOL_INTERCEPT_NOTIFICATION -> false
                KEY_ACC_SCREEN_OFF -> false
                
                KEY_VOLUME_STEPS -> 50
            } as T
        }

        fun <T> getDefaultProvider(): (String) -> T {
            return { key: String ->
                getDefault(values().first { it.name == key })
            }
        }
    }
}