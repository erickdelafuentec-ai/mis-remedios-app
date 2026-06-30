package cl.mersoftware.misremedios

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar
import kotlin.math.abs

/**
 * Programa las alarmas usando AlarmManager.setAlarmClock(), que es la API
 * que Android garantiza incluso en modo Doze (igual que la alarma del reloj).
 */
object Programador {

    /** Reprograma TODAS las próximas tomas de todos los medicamentos. */
    fun reprogramarTodo(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val meds = Repo.cargar(ctx)

        for (m in meds) {
            for (hora in m.horas) {
                val cal = proximaOcurrencia(hora)
                val fecha = fechaStr(cal)
                val clave = "${m.id}_${fecha}_$hora"

                // si ya fue tomado hoy a esa hora, no programar esa ocurrencia
                if (m.tomados[clave] == true) continue

                val intent = Intent(ctx, AlarmaReceiver::class.java).apply {
                    putExtra("medId", m.id)
                    putExtra("hora", hora)
                    putExtra("clave", clave)
                }
                val reqCode = clave.hashCode()
                val pi = PendingIntent.getBroadcast(
                    ctx, reqCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // setAlarmClock = máxima prioridad, atraviesa Doze
                val showIntent = PendingIntent.getActivity(
                    ctx, reqCode + 1, Intent(ctx, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val info = AlarmManager.AlarmClockInfo(cal.timeInMillis, showIntent)
                try {
                    am.setAlarmClock(info, pi)
                } catch (e: SecurityException) {
                    // Sin permiso de alarma exacta: caemos a setExactAndAllowWhileIdle
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
                    } else {
                        am.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
                    }
                }
            }
        }
    }

    /** Tras sonar una alarma, reprograma la del día siguiente para esa misma hora. */
    fun reprogramarSiguiente(ctx: Context, medId: String, hora: String) {
        // simplemente reprograma todo: barato y a prueba de errores
        reprogramarTodo(ctx)
    }

    private fun proximaOcurrencia(hora: String): Calendar {
        val (hh, mm) = hora.split(":").map { it.toInt() }
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hh)
            set(Calendar.MINUTE, mm)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // si la hora de hoy ya pasó (con 1 min de margen), programar para mañana
        if (cal.timeInMillis <= System.currentTimeMillis() + 60_000) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cal
    }

    private fun fechaStr(cal: Calendar): String {
        val y = cal.get(Calendar.YEAR)
        val mo = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val d = cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        return "$y-$mo-$d"
    }

    fun hoyStr(): String {
        val cal = Calendar.getInstance()
        return fechaStr(cal)
    }
}
