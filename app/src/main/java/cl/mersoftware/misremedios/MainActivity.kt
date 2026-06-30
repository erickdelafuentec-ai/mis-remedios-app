package cl.mersoftware.misremedios

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var contLista: LinearLayout
    private lateinit var contMeds: LinearLayout
    private lateinit var tvFecha: TextView
    private lateinit var avisoPerm: LinearLayout

    private val repro = Reproductor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvFecha = findViewById(R.id.tvFecha)
        contLista = findViewById(R.id.contHoy)
        contMeds = findViewById(R.id.contMeds)
        avisoPerm = findViewById(R.id.avisoPermisos)

        findViewById<Button>(R.id.fabAgregar).setOnClickListener { dialogoMed(null) }
        findViewById<ImageButton>(R.id.btnConfig).setOnClickListener { dialogoAjustes() }
        findViewById<Button>(R.id.btnArreglarPerm).setOnClickListener { pedirPermisos() }

        pedirNotificaciones()
    }

    override fun onResume() {
        super.onResume()
        render()
        Programador.reprogramarTodo(this)
        revisarAvisoPermisos()
    }

    // ---------- Permisos ----------
    private fun pedirNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
    }

    private fun tieneAlarmaExacta(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            return am.canScheduleExactAlarms()
        }
        return true
    }

    private fun revisarAvisoPermisos() {
        avisoPerm.visibility = if (tieneAlarmaExacta()) View.GONE else View.VISIBLE
    }

    private fun pedirPermisos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !tieneAlarmaExacta()) {
            try {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName")))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")))
            }
        }
    }

    // ---------- Render ----------
    private fun render() {
        val f = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "CL")).format(Date())
        tvFecha.text = f.replaceFirstChar { it.uppercase() }

        val meds = Repo.cargar(this)
        val hoy = Programador.hoyStr()

        // ----- Tomas de hoy -----
        contLista.removeAllViews()
        data class Toma(val medId: String, val nombre: String, val dosis: String, val hora: String, val tomado: Boolean)
        val tomas = mutableListOf<Toma>()
        for (m in meds) for (h in m.horas)
            tomas.add(Toma(m.id, m.nombre, m.dosis, h, m.tomados["${hoy}_$h"] == true))
        tomas.sortBy { it.hora }

        if (tomas.isEmpty()) {
            contLista.addView(textoVacio("No hay remedios programados.\nToca «Agregar remedio»."))
        } else {
            val ahora = Calendar.getInstance()
            val minNow = ahora.get(Calendar.HOUR_OF_DAY) * 60 + ahora.get(Calendar.MINUTE)
            for (t in tomas) {
                val (hh, mm) = t.hora.split(":").map { it.toInt() }
                val esAhora = !t.tomado && Math.abs(minNow - (hh * 60 + mm)) <= 30
                contLista.addView(tarjetaToma(t.medId, t.nombre, t.dosis, t.hora, t.tomado, esAhora))
            }
        }

        // ----- Gestión de medicamentos -----
        contMeds.removeAllViews()
        if (meds.isEmpty()) {
            contMeds.addView(textoVacio("Aún no agregas remedios."))
        } else {
            for (m in meds) contMeds.addView(tarjetaMed(m))
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun textoVacio(txt: String) = TextView(this).apply {
        text = txt; textSize = 19f; gravity = Gravity.CENTER
        setTextColor(0xFF5B6770.toInt()); setPadding(dp(20), dp(30), dp(20), dp(30))
    }

    private fun tarjetaToma(medId: String, nombre: String, dosis: String, hora: String,
                            tomado: Boolean, esAhora: Boolean): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = ContextCompat.getDrawable(context,
                if (esAhora) R.drawable.card_ahora else R.drawable.card_normal)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, 0, dp(12)); layoutParams = lp
            alpha = if (tomado) 0.5f else 1f
        }
        card.addView(TextView(this).apply {
            text = hora; textSize = 30f
            setTextColor(if (tomado) 0xFF5B6770.toInt() else 0xFF0D5C63.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            width = dp(100)
        })
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        info.addView(TextView(this).apply {
            text = nombre; textSize = 22f; setTextColor(0xFF1A2327.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            if (tomado) paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
        })
        if (dosis.isNotEmpty()) info.addView(TextView(this).apply {
            text = dosis; textSize = 17f; setTextColor(0xFF5B6770.toInt())
        })
        card.addView(info)

        if (tomado) {
            card.addView(TextView(this).apply {
                text = "✓ Tomado"; textSize = 15f; setTextColor(0xFF2E8B57.toInt())
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
        } else {
            card.addView(Button(this).apply {
                text = "✓"; textSize = 28f
                setTextColor(0xFFFFFFFF.toInt())
                background = ContextCompat.getDrawable(context, R.drawable.btn_verde)
                val lp = LinearLayout.LayoutParams(dp(70), dp(70)); layoutParams = lp
                setOnClickListener { marcarTomado(medId, hora) }
            })
        }
        return card
    }

    private fun tarjetaMed(m: Medicamento): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = ContextCompat.getDrawable(context, R.drawable.card_normal)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0, 0, 0, dp(12)); layoutParams = lp
        }
        val fila = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val izq = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        izq.addView(TextView(this).apply {
            text = m.nombre; textSize = 21f; setTextColor(0xFF1A2327.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        if (m.dosis.isNotEmpty()) izq.addView(TextView(this).apply {
            text = m.dosis; textSize = 16f; setTextColor(0xFF5B6770.toInt())
        })
        fila.addView(izq)
        fila.addView(Button(this).apply {
            text = "✏"; textSize = 18f
            background = ContextCompat.getDrawable(context, R.drawable.btn_borde)
            layoutParams = LinearLayout.LayoutParams(dp(50), dp(50)).also { it.marginEnd = dp(8) }
            setOnClickListener { dialogoMed(m) }
        })
        fila.addView(Button(this).apply {
            text = "🗑"; textSize = 18f
            background = ContextCompat.getDrawable(context, R.drawable.btn_borde)
            layoutParams = LinearLayout.LayoutParams(dp(50), dp(50))
            setOnClickListener { borrarMed(m) }
        })
        card.addView(fila)

        val horas = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
        }
        val txt = m.horas.joinToString("   ") { "🕐 $it" } +
                "    🔔 ${Reproductor.NOMBRES.getOrElse(m.sonido){ "Campana" }}" +
                if (m.fuerte) " (fuerte)" else ""
        horas.addView(TextView(this).apply {
            text = txt; textSize = 16f; setTextColor(0xFF0D5C63.toInt())
        })
        card.addView(horas)
        return card
    }

    private fun marcarTomado(medId: String, hora: String) {
        val meds = Repo.cargar(this)
        meds.find { it.id == medId }?.let {
            it.tomados["${Programador.hoyStr()}_$hora"] = true
        }
        Repo.guardar(this, meds)
        Programador.reprogramarTodo(this)
        render()
    }

    private fun borrarMed(m: Medicamento) {
        AlertDialog.Builder(this)
            .setMessage("¿Eliminar «${m.nombre}»?")
            .setPositiveButton("Eliminar") { _, _ ->
                val meds = Repo.cargar(this).filter { it.id != m.id }
                Repo.guardar(this, meds)
                Programador.reprogramarTodo(this)
                render()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ---------- Diálogo agregar / editar ----------
    private fun dialogoMed(editar: Medicamento?) {
        val scroll = ScrollView(this)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(16), dp(24), dp(8))
        }
        scroll.addView(box)

        fun etiqueta(t: String) = TextView(this).apply {
            text = t; textSize = 17f; setTextColor(0xFF1A2327.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(14), 0, dp(6))
        }

        box.addView(etiqueta("Nombre del remedio"))
        val inNombre = EditText(this).apply {
            textSize = 20f; hint = "Ej: Losartán"; setText(editar?.nombre ?: "")
        }
        box.addView(inNombre)

        box.addView(etiqueta("Dosis (opcional)"))
        val inDosis = EditText(this).apply {
            textSize = 20f; hint = "Ej: 1 pastilla"; setText(editar?.dosis ?: "")
        }
        box.addView(inDosis)

        box.addView(etiqueta("Horarios"))
        val horasBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(horasBox)
        val horasSel = (editar?.horas?.toMutableList() ?: mutableListOf("08:00"))

        fun pintarHoras() {
            horasBox.removeAllViews()
            horasSel.forEachIndexed { i, h ->
                val fila = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dp(4), 0, dp(4))
                }
                fila.addView(Button(this).apply {
                    text = h; textSize = 22f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener {
                        val (hh, mm) = h.split(":").map { s -> s.toInt() }
                        TimePickerDialog(this@MainActivity, { _, ho, mi ->
                            horasSel[i] = "%02d:%02d".format(ho, mi); pintarHoras()
                        }, hh, mm, true).show()
                    }
                })
                fila.addView(Button(this).apply {
                    text = "✕"; textSize = 18f
                    setOnClickListener { if (horasSel.size > 1) { horasSel.removeAt(i); pintarHoras() } }
                })
                horasBox.addView(fila)
            }
        }
        pintarHoras()
        box.addView(Button(this).apply {
            text = "+ Agregar otro horario"; textSize = 16f
            setOnClickListener {
                TimePickerDialog(this@MainActivity, { _, ho, mi ->
                    horasSel.add("%02d:%02d".format(ho, mi)); pintarHoras()
                }, 12, 0, true).show()
            }
        })

        // sonido
        box.addView(etiqueta("Sonido de la alarma"))
        var sonidoSel = editar?.sonido ?: 0
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Reproductor.NOMBRES)
        spinner.setSelection(sonidoSel)
        box.addView(spinner)

        var fuerteSel = editar?.fuerte ?: true
        box.addView(Button(this).apply {
            text = "▶  Escuchar sonido"; textSize = 16f
            setOnClickListener { repro.iniciar(spinner.selectedItemPosition, fuerteSel)
                postDelayed({ repro.detener() }, 2500) }
        })

        // volumen extra
        val swFila = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(14), 0, 0)
        }
        swFila.addView(TextView(this).apply {
            text = "🔊 Volumen extra fuerte"; textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        val sw = Switch(this).apply { isChecked = fuerteSel }
        sw.setOnCheckedChangeListener { _, b -> fuerteSel = b }
        swFila.addView(sw)
        box.addView(swFila)

        AlertDialog.Builder(this)
            .setTitle(if (editar == null) "Nuevo remedio" else "Editar remedio")
            .setView(scroll)
            .setPositiveButton("Guardar") { _, _ ->
                repro.detener()
                val nombre = inNombre.text.toString().trim()
                if (nombre.isEmpty()) { toast("Escribe el nombre"); return@setPositiveButton }
                horasSel.sort()
                val meds = Repo.cargar(this)
                if (editar == null) {
                    meds.add(Medicamento("m${System.currentTimeMillis()}", nombre,
                        inDosis.text.toString().trim(), horasSel,
                        spinner.selectedItemPosition, fuerteSel))
                } else {
                    meds.find { it.id == editar.id }?.apply {
                        this.nombre = nombre
                        this.dosis = inDosis.text.toString().trim()
                        this.horas = horasSel
                        this.sonido = spinner.selectedItemPosition
                        this.fuerte = fuerteSel
                    }
                }
                Repo.guardar(this, meds)
                Programador.reprogramarTodo(this)
                render()
            }
            .setNegativeButton("Cancelar") { _, _ -> repro.detener() }
            .show()
    }

    private fun dialogoAjustes() {
        val msg = """
            🔋 Para que la alarma suene SIEMPRE:

            1. Ajustes → Aplicaciones → Mis Remedios → Batería → «Sin restricciones».

            2. Permite «Alarmas y recordatorios» si te lo pide.

            3. En Samsung/Xiaomi: quita la app de «apps en suspensión» o «ahorro de batería».

            Así Android no apaga la app y la alarma suena con la pantalla bloqueada.
        """.trimIndent()
        AlertDialog.Builder(this)
            .setTitle("Ajustes")
            .setMessage(msg)
            .setPositiveButton("Abrir ajustes de la app") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")))
            }
            .setNeutralButton("Probar alarma") { _, _ ->
                val i = Intent(this, AlarmaActivity::class.java).apply {
                    putExtra("nombre", "Prueba de alarma")
                    putExtra("dosis", "Así se ve y suena")
                    putExtra("sonido", 0); putExtra("fuerte", true)
                    putExtra("medId", ""); putExtra("hora", "")
                }
                startActivity(i)
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_SHORT).show()

    override fun onDestroy() { super.onDestroy(); repro.detener() }
}
