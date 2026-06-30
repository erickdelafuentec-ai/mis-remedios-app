package cl.mersoftware.misremedios

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

/**
 * Genera 5 patrones de sonido distintos usando ToneGenerator en el stream
 * de ALARMA, así suena al volumen de alarma (no el de notificación) y
 * atraviesa el modo vibrador. Repite en bucle hasta detener().
 */
class Reproductor {

    private var tg: ToneGenerator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var sonando = false

    // cada patrón: lista de pares (toneType, duraciónMs)
    private val patrones = listOf(
        // 0 Campana
        listOf(ToneGenerator.TONE_PROP_BEEP to 180, ToneGenerator.TONE_PROP_ACK to 180, ToneGenerator.TONE_PROP_BEEP2 to 300),
        // 1 Timbre
        listOf(ToneGenerator.TONE_CDMA_HIGH_L to 150, ToneGenerator.TONE_CDMA_HIGH_L to 150, ToneGenerator.TONE_CDMA_HIGH_SS to 250),
        // 2 Suave
        listOf(ToneGenerator.TONE_PROP_BEEP to 300, ToneGenerator.TONE_PROP_ACK to 300, ToneGenerator.TONE_PROP_PROMPT to 400),
        // 3 Urgente
        listOf(ToneGenerator.TONE_CDMA_ABBR_ALERT to 120, ToneGenerator.TONE_CDMA_ABBR_ALERT to 120, ToneGenerator.TONE_CDMA_ABBR_ALERT to 200),
        // 4 Gota
        listOf(ToneGenerator.TONE_PROP_PROMPT to 100, ToneGenerator.TONE_PROP_BEEP to 220)
    )

    fun iniciar(indiceSonido: Int, fuerte: Boolean) {
        detener()
        sonando = true
        val vol = if (fuerte) ToneGenerator.MAX_VOLUME else (ToneGenerator.MAX_VOLUME * 0.6).toInt()
        tg = try {
            ToneGenerator(AudioManager.STREAM_ALARM, vol)
        } catch (e: Exception) { null }

        val patron = patrones.getOrElse(indiceSonido) { patrones[0] }
        val reps = if (fuerte) 2 else 1
        val pausaCiclo = if (fuerte) 700L else 1400L

        fun cicloCompleto() {
            if (!sonando) return
            var delay = 0L
            for (r in 0 until reps) {
                for ((tono, dur) in patron) {
                    handler.postDelayed({ if (sonando) tg?.startTone(tono, dur) }, delay)
                    delay += dur + 30
                }
                delay += 120
            }
            // volver a sonar el ciclo
            handler.postDelayed({ cicloCompleto() }, delay + pausaCiclo)
        }
        cicloCompleto()
    }

    fun detener() {
        sonando = false
        handler.removeCallbacksAndMessages(null)
        try { tg?.release() } catch (_: Exception) {}
        tg = null
    }

    companion object {
        val NOMBRES = listOf("Campana", "Timbre", "Suave", "Urgente", "Gota")
    }
}
