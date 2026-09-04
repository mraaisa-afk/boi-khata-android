package com.boikhata

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceSetupSpeaker(context: Context) : TextToSpeech.OnInitListener {
    private val textToSpeech = TextToSpeech(context.applicationContext, this)
    private var ready = false

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS &&
            textToSpeech.isLanguageAvailable(Locale("bn", "BD")) >= TextToSpeech.LANG_AVAILABLE
        if (ready) textToSpeech.language = Locale("bn", "BD")
    }

    fun speak(step: String): Boolean {
        if (!ready) return false
        textToSpeech.speak(step, TextToSpeech.QUEUE_FLUSH, null, "boi-khata-setup")
        return true
    }

    fun stop() {
        textToSpeech.stop()
        textToSpeech.shutdown()
        ready = false
    }
}
