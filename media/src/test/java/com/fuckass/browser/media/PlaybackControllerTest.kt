package com.fuckass.browser.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackControllerTest {
    @Test fun preparesPlaysPausesAndSeeksWithinDuration() {
        val controller = PlaybackController()
        controller.prepare(120_000)
        controller.play()
        controller.pause()
        controller.seekTo(30_000)
        val state = controller.snapshot()
        assertEquals(PlaybackStatus.PAUSED, state.status)
        assertEquals(30_000L, state.positionMs)
    }

    @Test fun seekPastEndClampsAndMarksEnded() {
        val controller = PlaybackController()
        controller.prepare(10_000)
        controller.play()
        controller.seekTo(20_000)
        assertEquals(10_000L, controller.snapshot().positionMs)
        assertEquals(PlaybackStatus.ENDED, controller.snapshot().status)
    }

    @Test fun replayFromEndedStartsAtZero() {
        val controller = PlaybackController()
        controller.prepare(1_000)
        controller.play()
        controller.seekTo(1_000)
        controller.play()
        assertEquals(PlaybackStatus.PLAYING, controller.snapshot().status)
        assertEquals(0L, controller.snapshot().positionMs)
    }

    @Test fun speedMuteAndSubtitleAreUserControlled() {
        val controller = PlaybackController()
        controller.setSpeed(1.5f)
        controller.setMuted(true)
        controller.selectSubtitle("en")
        val state = controller.snapshot()
        assertEquals(1.5f, state.speed)
        assertTrue(state.muted)
        assertEquals("en", state.subtitleTrack)
        controller.selectSubtitle(null)
        assertFalse(controller.snapshot().subtitleTrack != null)
    }

    @Test fun rejectsUnsupportedSpeedAndInvalidDuration() {
        try {
            PlaybackController().setSpeed(4f)
            throw AssertionError("Expected speed validation")
        } catch (_: IllegalArgumentException) { }
        try {
            PlaybackController().prepare(-1)
            throw AssertionError("Expected duration validation")
        } catch (_: IllegalArgumentException) { }
    }

    @Test fun failureMovesControllerToErrorState() {
        val controller = PlaybackController()
        controller.prepare(null)
        controller.fail("decoder")
        assertEquals(PlaybackStatus.ERROR, controller.snapshot().status)
        assertEquals("decoder", controller.snapshot().errorCode)
    }
}
