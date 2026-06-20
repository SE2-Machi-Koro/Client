package com.machikoro.client.ui.game

import android.content.Context
import android.media.SoundPool
import com.machikoro.client.R

object SoundManager {

    private lateinit var soundPool: SoundPool

    private val sounds = mutableMapOf<GameSound, Int>()

    fun init(context: Context) {
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .build()

        sounds[GameSound.DICE_ROLL] =
            soundPool.load(context, R.raw.dice, 1)

        sounds[GameSound.CARD_FLIP] = soundPool.load(context, R.raw.card_flip, 1)

        sounds[GameSound.COIN_DRAWER] = soundPool.load(context, R.raw.coin_drawer, 1)

        sounds[GameSound.PURCHASE] = soundPool.load(context, R.raw.purchase, 1)

        //   sounds[GameSound.BUTTON_CLICK] = soundPool.load(context, R.raw.button_click, 1)

 //       sounds[GameSound.DOUBLES] = soundPool.load(context, R.raw.doubles, 1)

        sounds[GameSound.COIN] =
            soundPool.load(context, R.raw.coin, 1)

        sounds[GameSound.WIN] =
            soundPool.load(context, R.raw.win, 1)
    }

    fun play(sound: GameSound) {
        sounds[sound]?.let {
            soundPool.play(it, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        soundPool.release()
    }
}

enum class GameSound {
    DICE_ROLL, COIN, BUTTON_CLICK, DOUBLES, WIN, CARD_FLIP, PURCHASE, COIN_DRAWER
}
