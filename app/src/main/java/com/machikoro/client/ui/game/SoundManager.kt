package com.machikoro.client.ui.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.machikoro.client.R

object SoundManager {

    private var soundPool: SoundPool? = null
    private val sounds = mutableMapOf<GameSound, Int>()
    private val loaded = mutableSetOf<Int>()

    fun init(context: Context) {
        if (soundPool != null) return // idempotent: onStart can fire repeatedly

        val pool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()

        // load() is async — only mark a sample playable once it is ready
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loaded.add(sampleId)
        }

        sounds[GameSound.DICE_ROLL] = pool.load(context, R.raw.dice, 1)
        sounds[GameSound.PURCHASE] = pool.load(context, R.raw.purchase, 1)
        sounds[GameSound.CARD_FLIP] = pool.load(context, R.raw.card_flip, 1)
        sounds[GameSound.COIN] = pool.load(context, R.raw.coin, 1)
        sounds[GameSound.COIN_DRAWER] = pool.load(context, R.raw.coin_drawer, 1)
        sounds[GameSound.WIN] = pool.load(context, R.raw.win, 1)
        sounds[GameSound.SIUUU] = pool.load(context, R.raw.siuuu, 1)

        soundPool = pool
    }

    fun play(sound: GameSound) {
        val pool = soundPool ?: return
        val id = sounds[sound] ?: return
        if (id in loaded) pool.play(id, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        sounds.clear()
        loaded.clear()
    }
}

enum class GameSound {
    DICE_ROLL, PURCHASE, CARD_FLIP, COIN, COIN_DRAWER, WIN, SIUUU
}
