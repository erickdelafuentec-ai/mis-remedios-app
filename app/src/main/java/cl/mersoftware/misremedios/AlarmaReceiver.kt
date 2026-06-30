package cl.mersoftware.misremedios

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Se ejecuta exactamente a la hora programada (incluso en Doze).
 * Lanza la AlarmaActivity a pantalla completa y muestra una notificación
 * de alta prioridad como respaldo.
 */
class AlarmaReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medId = intent.getStringExtra("medId") ?: return
        val hora = intent.getStringExtra("hora") ?: ""
        val med = Repo.buscar(context, medId)
        val nombre = med?.nombre ?: "Tu remedio"
        val dosis = med?.dosis ?: ""
        val sonido = med?.sonido ?: 0
        val fuerte = med?.fuerte ?: true

        crearCanal(context)

        // 1) Lanzar pantalla completa de alarma
        val full = Intent(context, AlarmaActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("medId", medId)
            putExtra("hora", hora)
            putExtra("nombre", nombre)
            putExtra("dosis", dosis)
            putExtra("sonido", sonido)
            putExtra("fuerte", fuerte)
        }

        // 2) Notificación con full-screen intent (respaldo si el sistema no abre la activity)
        val fsPi = PendingIntent.getActivity(
            context, medId.hashCode(), full,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = androidx.core.app.NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_pastilla)
            .setContentTitle("💊 Hora de tu remedio")
            .setContentText(nombre + if (dosis.isNotEmpty()) " · $dosis" else "")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fsPi, true)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(medId.hashCode(), notif)

        // Intentar abrir la activity directamente también
        try {
            context.startActivity(full)
        } catch (_: Exception) { /* el full-screen intent la abrirá */ }

        // 3) Dejar lista la ocurrencia del día siguiente
        Programador.reprogramarSiguiente(context, medId, hora)
    }

    private fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CANAL) == null) {
                val canal = NotificationChannel(
                    CANAL, "Alarmas de remedios",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Avisos a la hora de tomar el medicamento"
                    enableVibration(true)
                    setBypassDnd(true) // suena aunque esté en No molestar
                }
                nm.createNotificationChannel(canal)
            }
        }
    }

    companion object {
        const val CANAL = "alarmas_remedios"
    }
}
