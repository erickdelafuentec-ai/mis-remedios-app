package cl.mersoftware.misremedios

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

/** Pantalla completa naranja que aparece a la hora del remedio. Suena y vibra. */
class AlarmaActivity : AppCompatActivity() {

    private val repro = Reproductor()
    private var vibrator: Vibrator? = null
    private var medId = ""
    private var hora = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // mostrar sobre la pantalla de bloqueo y encender pantalla
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_alarma)

        medId = intent.getStringExtra("medId") ?: ""
        hora = intent.getStringExtra("hora") ?: ""
        val nombre = intent.getStringExtra("nombre") ?: "Tu remedio"
        val dosis = intent.getStringExtra("dosis") ?: ""
        val sonido = intent.getIntExtra("sonido", 0)
        val fuerte = intent.getBooleanExtra("fuerte", true)

        findViewById<android.widget.TextView>(R.id.aNombre).text = nombre
        val tvDosis = findViewById<android.widget.TextView>(R.id.aDosis)
        if (dosis.isNotEmpty()) tvDosis.text = dosis else tvDosis.visibility = android.view.View.GONE

        // sonar + vibrar
        repro.iniciar(sonido, fuerte)
        vibrar(fuerte)

        findViewById<android.widget.Button>(R.id.btnTomado).setOnClickListener {
            marcarTomado()
            cerrar()
        }
        findViewById<android.widget.Button>(R.id.btnPosponer).setOnClickListener {
            // reprograma esta toma 10 minutos después
            posponer10()
            cerrar()
        }
    }

    private fun vibrar(fuerte: Boolean) {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val patron = if (fuerte)
            longArrayOf(0, 600, 300, 600, 300, 600, 300, 600)
        else
            longArrayOf(0, 400, 400, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(patron, 0)) // 0 = repetir
        } else {
            @Suppress("DEPRECATION") vibrator?.vibrate(patron, 0)
        }
    }

    private fun marcarTomado() {
        val meds = Repo.cargar(this)
        val m = meds.find { it.id == medId } ?: return
        m.tomados["${Programador.hoyStr()}_$hora"] = true
        Repo.guardar(this, meds)
        Programador.reprogramarTodo(this)
    }

    private fun posponer10() {
        val am = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = android.content.Intent(this, AlarmaReceiver::class.java).apply {
            putExtra("medId", medId); putExtra("hora", hora)
        }
        val pi = android.app.PendingIntent.getBroadcast(
            this, ("snooze_$medId$hora").hashCode(), intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val cuando = System.currentTimeMillis() + 10 * 60 * 1000
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, cuando, pi)
        else am.setExact(android.app.AlarmManager.RTC_WAKEUP, cuando, pi)
    }

    private fun cerrar() {
        repro.detener()
        vibrator?.cancel()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(medId.hashCode())
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        repro.detener()
        vibrator?.cancel()
    }
}
