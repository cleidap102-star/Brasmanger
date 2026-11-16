package com.example.ui.components

import android.media.AudioManager
import android.media.ToneGenerator

object MatchSoundEffects {

    private var toneGenerator: ToneGenerator? = null

    private fun getOrCreateToneGenerator(): ToneGenerator? {
        return runCatching {
            toneGenerator ?: ToneGenerator(AudioManager.STREAM_MUSIC, 65).also {
                toneGenerator = it
            }
        }.getOrNull()
    }

    fun playGoal() {
        runCatching {
            getOrCreateToneGenerator()?.startTone(ToneGenerator.TONE_PROP_ACK, 340)
        }
    }

    fun playWhistle() {
        runCatching {
            getOrCreateToneGenerator()?.startTone(ToneGenerator.TONE_DTMF_9, 220)
        }
    }

    fun playCard() {
        runCatching {
            getOrCreateToneGenerator()?.startTone(ToneGenerator.TONE_PROP_BEEP2, 160)
        }
    }

    fun release() {
        runCatching {
            toneGenerator?.release()
            toneGenerator = null
        }
    }
}
