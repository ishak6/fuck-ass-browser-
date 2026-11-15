package com.fuckass.browser.media

enum class PlaybackStatus { IDLE, READY, PLAYING, PAUSED, ENDED, ERROR }

data class PlaybackSnapshot(
    val status: PlaybackStatus,
    val positionMs: Long,
    val durationMs: Long?,
    val speed: Float,
    val muted: Boolean,
    val subtitleTrack: String?,
    val errorCode: String?
)

/** Deterministic playback policy/state machine; decoding and DRM remain platform/provider concerns. */
class PlaybackController {
    private var status = PlaybackStatus.IDLE
    private var positionMs = 0L
    private var durationMs: Long? = null
    private var speed = 1f
    private var muted = false
    private var subtitleTrack: String? = null
    private var errorCode: String? = null

    @Synchronized fun prepare(durationMs: Long?) {
        require(durationMs == null || durationMs >= 0)
        this.durationMs = durationMs
        positionMs = 0
        errorCode = null
        status = PlaybackStatus.READY
    }

    @Synchronized fun play() {
        check(status in setOf(PlaybackStatus.READY, PlaybackStatus.PAUSED, PlaybackStatus.ENDED))
        if (status == PlaybackStatus.ENDED) positionMs = 0
        status = PlaybackStatus.PLAYING
    }

    @Synchronized fun pause() {
        if (status == PlaybackStatus.PLAYING) status = PlaybackStatus.PAUSED
    }

    @Synchronized fun seekTo(position: Long) {
        check(status != PlaybackStatus.IDLE && status != PlaybackStatus.ERROR)
        positionMs = position.coerceAtLeast(0).let { durationMs?.let(it::coerceAtMost) ?: it }
        if (durationMs != null && positionMs >= durationMs!!) status = PlaybackStatus.ENDED
        else if (status == PlaybackStatus.ENDED) status = PlaybackStatus.PAUSED
    }

    @Synchronized fun setSpeed(value: Float) {
        require(value.isFinite() && value in 0.5f..2.0f)
        speed = value
    }

    @Synchronized fun setMuted(value: Boolean) { muted = value }

    @Synchronized fun selectSubtitle(trackId: String?) {
        require(trackId == null || trackId.isNotBlank())
        subtitleTrack = trackId
    }

    @Synchronized fun fail(code: String) {
        require(code.isNotBlank())
        errorCode = code
        status = PlaybackStatus.ERROR
    }

    @Synchronized fun snapshot() = PlaybackSnapshot(
        status, positionMs, durationMs, speed, muted, subtitleTrack, errorCode
    )
}
