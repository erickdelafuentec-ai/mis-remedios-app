package cl.mersoftware.misremedios

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Cuando el teléfono se reinicia, las alarmas se borran. Aquí se reprograman. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val a = intent.action ?: return
        if (a == Intent.ACTION_BOOT_COMPLETED || a == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            Programador.reprogramarTodo(context)
        }
    }
}
