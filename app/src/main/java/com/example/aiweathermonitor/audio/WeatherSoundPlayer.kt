package com.example.aiweathermonitor.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.aiweathermonitor.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Plays layered, looping ambient weather sound that matches the current weather code.
 *
 * Each weather condition resolves (via [getWeatherSoundscape]) to one or more looping
 * [SoundLayer]s that are mixed together. Switching conditions crossfades layers in and
 * out so transitions are smooth.
 *
 * Lifecycle: the owner is responsible for calling [pause]/[resume] when the app goes to
 * the background/foreground and [release] when done (e.g. from a Compose DisposableEffect).
 *
 * Always construct with the *application* context to avoid leaking an Activity.
 */
class WeatherSoundPlayer(context: Context) {

    private val TAG = "WeatherSoundPlayer"
    private val appContext: Context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private class Active(
        val player: MediaPlayer,
        var current: Float = 0f,
        var target: Float = 0f,
        var job: Job? = null
    )

    /** res id -> currently playing layer */
    private val active = ConcurrentHashMap<Int, Active>()

    @Volatile private var masterVolume: Float = 1f
    @Volatile private var currentCode: Int? = null
    @Volatile private var paused: Boolean = false

    /** Overall volume multiplier (0f..1f) applied on top of each layer's own volume. */
    fun setMasterVolume(v: Float) {
        masterVolume = v.coerceIn(0f, 1f)
        active.values.forEach { applyVolume(it) }
    }

    /**
     * Switch the soundscape to match [code], crossfading layers that change.
     * Safe to call repeatedly; redundant calls for the same code are ignored.
     */
    fun play(code: Int) {
        if (code == currentCode && !paused && active.isNotEmpty()) return
        currentCode = code
        paused = false

        val target = getWeatherSoundscape(code).associate { it.res to it.volume }

        // Fade out + release any layer no longer needed
        active.keys.filter { it !in target }.forEach { res -> fadeOutAndRelease(res) }

        // Add new layers or re-target existing ones
        target.forEach { (res, vol) ->
            val a = active[res]
            if (a == null) startLayer(res, vol) else fadeTo(a, vol)
        }
    }

    /** Pause all layers (keeps them ready to resume from the same position). */
    fun pause() {
        paused = true
        active.values.forEach { a ->
            try {
                if (a.player.isPlaying) a.player.pause()
            } catch (e: IllegalStateException) {
                AppLogger.error("pause failed", TAG, e)
            }
        }
    }

    /** Resume all layers previously paused. */
    fun resume() {
        if (currentCode == null) return
        paused = false
        active.values.forEach { a ->
            try {
                if (!a.player.isPlaying) a.player.start()
            } catch (e: IllegalStateException) {
                AppLogger.error("resume failed", TAG, e)
            }
        }
    }

    /** Stop and release every layer (the player can still be reused via [play]). */
    fun stop() {
        currentCode = null
        paused = false
        active.keys.toList().forEach { res ->
            active.remove(res)?.let { a ->
                a.job?.cancel()
                releasePlayer(a.player)
            }
        }
    }

    /** Permanently release all resources. The instance must not be used afterwards. */
    fun release() {
        stop()
        scope.cancel()
    }

    // ── internals ────────────────────────────────────────────────────────────

    private fun startLayer(res: Int, targetVol: Float) {
        scope.launch {
            try {
                // Build manually so audio attributes are set BEFORE prepare().
                // MediaPlayer.create() prepares immediately, which would make a
                // later setAudioAttributes() call throw on some Android versions.
                val mp = MediaPlayer()
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                appContext.resources.openRawResourceFd(res).use { afd ->
                    mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                }
                mp.isLooping = true
                mp.setVolume(0f, 0f)
                mp.prepare()

                // We may have been superseded (stopped / code changed) while preparing.
                val stillWanted = currentCode?.let { code ->
                    getWeatherSoundscape(code).any { it.res == res }
                } ?: false
                if (!stillWanted) {
                    releasePlayer(mp)
                    return@launch
                }

                val a = Active(mp, current = 0f, target = targetVol)
                active[res] = a
                if (!paused) {
                    try {
                        mp.start()
                    } catch (e: IllegalStateException) {
                        AppLogger.error("start failed", TAG, e)
                    }
                }
                fadeTo(a, targetVol, fromZero = true)
            } catch (e: Exception) {
                AppLogger.error("startLayer($res) failed", TAG, e)
            }
        }
    }

    private fun fadeTo(a: Active, target: Float, durationMs: Long = 1200, fromZero: Boolean = false) {
        a.target = target
        a.job?.cancel()
        a.job = scope.launch {
            val steps = 24
            val start = if (fromZero) 0f else a.current
            for (i in 1..steps) {
                val f = i.toFloat() / steps
                setLayerVol(a, start + (target - start) * f)
                delay(durationMs / steps)
            }
            setLayerVol(a, target)
        }
    }

    private fun fadeOutAndRelease(res: Int) {
        val a = active.remove(res) ?: return
        a.job?.cancel()
        a.job = scope.launch {
            val steps = 18
            val start = a.current
            for (i in 1..steps) {
                val f = i.toFloat() / steps
                setLayerVol(a, start * (1f - f))
                delay(700L / steps)
            }
            releasePlayer(a.player)
        }
    }

    private fun setLayerVol(a: Active, v: Float) {
        a.current = v.coerceIn(0f, 1f)
        applyVolume(a)
    }

    private fun applyVolume(a: Active) {
        val vol = (a.current * masterVolume).coerceIn(0f, 1f)
        try {
            a.player.setVolume(vol, vol)
        } catch (e: IllegalStateException) {
            AppLogger.error("setVolume failed", TAG, e)
        }
    }

    private fun releasePlayer(mp: MediaPlayer) {
        try {
            if (mp.isPlaying) mp.stop()
        } catch (e: IllegalStateException) {
            // already stopped / not started — ignore
        }
        try {
            mp.release()
        } catch (e: Exception) {
            AppLogger.error("release failed", TAG, e)
        }
    }
}
