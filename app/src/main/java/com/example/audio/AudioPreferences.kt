package com.example.audio

import android.content.Context

object AudioPreferences {
    private const val PREFS_NAME = "reclish_audio_prefs"
    private const val KEY_FOLLOW_VOLUME = "follow_device_volume"
    private const val KEY_BOOST_MULTIPLIER = "boost_multiplier"
    private const val KEY_FORMAT = "selected_format"
    private const val KEY_SOURCE = "selected_source"

    const val DEFAULT_BOOST = 3.5f

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isFollowDeviceVolume(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FOLLOW_VOLUME, true)

    fun setFollowDeviceVolume(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_FOLLOW_VOLUME, enabled).apply()
    }

    fun getBoostMultiplier(context: Context): Float =
        prefs(context).getFloat(KEY_BOOST_MULTIPLIER, DEFAULT_BOOST)

    fun setBoostMultiplier(context: Context, boost: Float) {
        prefs(context).edit().putFloat(KEY_BOOST_MULTIPLIER, boost.coerceIn(1.0f, 5.0f)).apply()
    }

    fun getFormat(context: Context): String =
        prefs(context).getString(KEY_FORMAT, "MP3") ?: "MP3"

    fun setFormat(context: Context, format: String) {
        prefs(context).edit().putString(KEY_FORMAT, format).apply()
    }

    fun getSource(context: Context): AudioCaptureSource {
        val raw = prefs(context).getString(KEY_SOURCE, AudioCaptureSource.BROWSER_INTERNAL.name)
        return try {
            AudioCaptureSource.valueOf(raw ?: AudioCaptureSource.BROWSER_INTERNAL.name)
        } catch (_: Exception) {
            AudioCaptureSource.BROWSER_INTERNAL
        }
    }

    fun setSource(context: Context, source: AudioCaptureSource) {
        prefs(context).edit().putString(KEY_SOURCE, source.name).apply()
    }
}
