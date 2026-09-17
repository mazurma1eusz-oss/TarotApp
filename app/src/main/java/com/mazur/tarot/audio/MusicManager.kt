package com.mazur.tarot.audio

import android.content.Context
import android.media.MediaPlayer
import com.mazur.tarot.R
import com.mazur.tarot.data.local.datastore.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

private val PLAYLIST = intArrayOf(R.raw.bg_music_1, R.raw.bg_music_2, R.raw.bg_music_3)
private const val MUSIC_VOLUME = 0.35f

/**
 * Cicha muzyka w tle aplikacji - 3 utwory odtwarzane jeden po drugim w kółko (po ostatnim
 * wraca do pierwszego). Wycisza się natychmiast po zmianie [SettingsDataStore.musicMuted]
 * (obserwowanej tu na bieżąco) - UI musi tylko przełączyć flagę w DataStore (ikonka głośnika),
 * reszta dzieje się sama. Cykl życia (start/pauza/release) jest sterowany z MainActivity.
 */
class MusicManager(private val context: Context, private val settingsDataStore: SettingsDataStore) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: MediaPlayer? = null
    private var trackIndex = 0
    private var muted = false
    private var isForeground = false

    init {
        settingsDataStore.settingsFlow
            .map { it.musicMuted }
            .distinctUntilChanged()
            .onEach { isMuted ->
                muted = isMuted
                val volume = if (isMuted) 0f else MUSIC_VOLUME
                player?.setVolume(volume, volume)
            }
            .launchIn(scope)
    }

    /** Wywoływane z Activity.onResume - wznawia bieżący utwór albo startuje playlistę od nowa. */
    fun onForeground() {
        isForeground = true
        val current = player
        if (current == null) {
            startTrack(trackIndex)
        } else if (!current.isPlaying) {
            current.start()
        }
    }

    /** Wywoływane z Activity.onPause - muzyka nie gra, gdy aplikacja jest w tle. */
    fun onBackground() {
        isForeground = false
        player?.takeIf { it.isPlaying }?.pause()
    }

    /** Wywoływane z Activity.onDestroy - zwalnia zasoby MediaPlayera. */
    fun release() {
        player?.release()
        player = null
    }

    private fun startTrack(index: Int) {
        player?.release()
        val resId = PLAYLIST[index.mod(PLAYLIST.size)]
        val volume = if (muted) 0f else MUSIC_VOLUME
        player = MediaPlayer.create(context, resId)?.apply {
            setVolume(volume, volume)
            setOnCompletionListener {
                trackIndex = (index + 1).mod(PLAYLIST.size)
                if (isForeground) startTrack(trackIndex)
            }
            if (isForeground) start()
        }
    }
}
