package cl.mersoftware.misremedios

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Un medicamento con sus horarios. Se guarda como JSON en SharedPreferences.
 * Cada "toma" del día se identifica como id_med + "_" + fecha + "_" + hora.
 */
data class Medicamento(
    val id: String,
    var nombre: String,
    var dosis: String,
    var horas: MutableList<String>,   // ["08:00","20:00"]
    var sonido: Int,                  // índice 0..4
    var fuerte: Boolean,
    var tomados: MutableMap<String, Boolean> = mutableMapOf()
) {
    fun toJson(): JSONObject {
        val o = JSONObject()
        o.put("id", id)
        o.put("nombre", nombre)
        o.put("dosis", dosis)
        o.put("horas", JSONArray(horas))
        o.put("sonido", sonido)
        o.put("fuerte", fuerte)
        val t = JSONObject()
        tomados.forEach { (k, v) -> t.put(k, v) }
        o.put("tomados", t)
        return o
    }

    companion object {
        fun fromJson(o: JSONObject): Medicamento {
            val horas = mutableListOf<String>()
            val ha = o.optJSONArray("horas") ?: JSONArray()
            for (i in 0 until ha.length()) horas.add(ha.getString(i))
            val tomados = mutableMapOf<String, Boolean>()
            val to = o.optJSONObject("tomados") ?: JSONObject()
            to.keys().forEach { k -> tomados[k] = to.getBoolean(k) }
            return Medicamento(
                id = o.getString("id"),
                nombre = o.getString("nombre"),
                dosis = o.optString("dosis", ""),
                horas = horas,
                sonido = o.optInt("sonido", 0),
                fuerte = o.optBoolean("fuerte", true),
                tomados = tomados
            )
        }
    }
}

object Repo {
    private const val PREF = "mis_remedios"
    private const val KEY = "meds"

    fun cargar(ctx: Context): MutableList<Medicamento> {
        val s = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val arr = JSONArray(s)
        val lista = mutableListOf<Medicamento>()
        for (i in 0 until arr.length()) lista.add(Medicamento.fromJson(arr.getJSONObject(i)))
        return lista
    }

    fun guardar(ctx: Context, lista: List<Medicamento>) {
        val arr = JSONArray()
        lista.forEach { arr.put(it.toJson()) }
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString(KEY, arr.toString()).apply()
    }

    fun buscar(ctx: Context, id: String): Medicamento? = cargar(ctx).find { it.id == id }
}
