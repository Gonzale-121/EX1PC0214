package com.uth.exa1par0214

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var controller: ApunteController
    private lateinit var spAsignatura: Spinner
    private lateinit var txtTema: EditText
    private lateinit var txtFecha: EditText
    private lateinit var txtDescripcion: EditText
    private lateinit var lblTiempo: TextView
    private lateinit var btnGrabar: Button
    private lateinit var btnReproducir: Button
    private lateinit var btnSalvar: Button

    private var recorder: MediaRecorder? = null
    private val notaPlayer = NotaPlayer { btnReproducir.text = "Reproducir" }
    private var rutaGrabando: String? = null
    private var grabando = false
    private var audioPath: String? = null
    private var inicio = 0L
    private val handler = Handler(Looper.getMainLooper())

    private var editId = -1 // -1 = nuevo, >0 = actualizar

    private val permisoMic =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            if (ok) iniciarGrabacion()
            else Toast.makeText(this, "Permiso de micrófono denegado", Toast.LENGTH_SHORT).show()
        }

    private val cronometro = object : Runnable {
        override fun run() {
            val s = (System.currentTimeMillis() - inicio) / 1000
            lblTiempo.text = String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        controller = ApunteController(this)

        spAsignatura = findViewById(R.id.spAsignatura)
        txtTema = findViewById(R.id.txtTema)
        txtFecha = findViewById(R.id.txtFecha)
        txtDescripcion = findViewById(R.id.txtDescripcion)
        lblTiempo = findViewById(R.id.lblTiempo)
        btnGrabar = findViewById(R.id.btnGrabar)
        btnReproducir = findViewById(R.id.btnReproducir)
        btnSalvar = findViewById(R.id.btnSalvar)

        txtFecha.setText(fechaHoy())
        cargarAsignaturas(null)

        btnGrabar.setOnClickListener { if (grabando) detenerGrabacion() else pedirPermisoYGrabar() }
        btnReproducir.setOnClickListener { reproducir() }
        findViewById<Button>(R.id.btnAddAsignatura).setOnClickListener { dialogoAsignatura() }
        btnSalvar.setOnClickListener { salvar() }
        findViewById<Button>(R.id.btnVer).setOnClickListener {
            startActivity(Intent(this, ListaActivity::class.java))
        }

        editId = intent.getIntExtra("id", -1)
        if (editId > 0) {
            controller.buscar(editId)?.let { a ->
                txtTema.setText(a.tema)
                txtFecha.setText(a.fecha)
                txtDescripcion.setText(a.descripcion)
                audioPath = a.audioPath
                cargarAsignaturas(a.asignatura)
                btnSalvar.text = "Actualizar Apunte"
            }
        }
    }

    private fun fechaHoy() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun cargarAsignaturas(seleccionar: String?) {
        val lista = controller.asignaturas()
        spAsignatura.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, lista)
        if (seleccionar != null) spAsignatura.setSelection(lista.indexOf(seleccionar))
    }

    private fun dialogoAsignatura() {
        val et = EditText(this).apply { hint = "Nombre de la asignatura" }
        AlertDialog.Builder(this)
            .setTitle("Agregar asignatura")
            .setView(et)
            .setPositiveButton("Agregar") { _, _ ->
                val n = et.text.toString().trim()
                if (n.isEmpty()) {
                    alerta("Debe escribir un nombre de asignatura")
                } else if (controller.agregarAsignatura(n)) {
                    cargarAsignaturas(n)
                } else {
                    alerta("La asignatura ya existe")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ----- Grabación -----
    private fun pedirPermisoYGrabar() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED -> iniciarGrabacion()

            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) ->
                AlertDialog.Builder(this)
                    .setTitle("Permiso necesario")
                    .setMessage("La app necesita el micrófono para grabar la nota de voz.")
                    .setPositiveButton("Entendido") { _, _ ->
                        permisoMic.launch(Manifest.permission.RECORD_AUDIO)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()

            else -> permisoMic.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    @Suppress("DEPRECATION")
    private fun crearRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else MediaRecorder()

    private fun iniciarGrabacion() {
        notaPlayer.detener()
        btnReproducir.text = "Reproducir"

        val dir = getExternalFilesDir(null) ?: filesDir
        val f = File(dir, "nota_${System.currentTimeMillis()}.m4a")
        try {
            recorder = crearRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96_000)
                setAudioSamplingRate(44_100)
                setOutputFile(f.absolutePath)
                prepare()
                start()
            }
            rutaGrabando = f.absolutePath
            grabando = true
            btnGrabar.text = "Detener"
            inicio = System.currentTimeMillis()
            handler.post(cronometro)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo iniciar la grabación", e)
            liberarRecorder()
            f.delete()
            rutaGrabando = null
            alerta("No se pudo iniciar la grabación: ${e.message}")
        }
    }

    private fun detenerGrabacion() {
        handler.removeCallbacks(cronometro)
        val ruta = rutaGrabando
        var ok = true
        try {
            recorder?.stop()
        } catch (e: RuntimeException) {
            Log.e(TAG, "recorder.stop() falló", e)
            ok = false
        }
        liberarRecorder()
        grabando = false
        rutaGrabando = null
        btnGrabar.text = "Grabar"

        val archivo = ruta?.let { File(it) }
        if (ok && archivo != null && archivo.exists() && archivo.length() > 0) {
            audioPath = ruta // solo se asigna cuando la grabación quedó bien cerrada
        } else {
            archivo?.delete()
            alerta("No se pudo guardar la grabación (muy corta o sin audio). Grabe de nuevo.")
        }
    }

    private fun liberarRecorder() {
        recorder?.release()
        recorder = null
    }

    private fun reproducir() {
        if (grabando) {
            alerta("Detenga la grabación antes de reproducir")
            return
        }
        if (notaPlayer.reproduciendo) {
            notaPlayer.detener()
            btnReproducir.text = "Reproducir"
            return
        }
        val error = notaPlayer.reproducir(audioPath)
        if (error != null) alerta(error) else btnReproducir.text = "Detener"
    }

    // ----- Guardar -----
    private fun salvar() {
        if (grabando) detenerGrabacion()
        val tema = txtTema.text.toString().trim()
        val fecha = txtFecha.text.toString().trim()
        val error = controller.validar(tema, fecha, audioPath)
        if (error != null) {
            alerta(error)
            return
        }

        val a = Apunte(
            asignatura = spAsignatura.selectedItem as String,
            tema = tema,
            fecha = fecha,
            descripcion = txtDescripcion.text.toString().trim(),
            audioPath = audioPath!!
        )

        if (editId > 0) {
            a.id = editId
            controller.actualizar(a)
            Toast.makeText(this, "Apunte actualizado", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            controller.guardar(a)
            Toast.makeText(this, "Apunte guardado", Toast.LENGTH_SHORT).show()
            limpiar()
        }
    }

    private fun limpiar() {
        txtTema.setText("")
        txtDescripcion.setText("")
        txtFecha.setText(fechaHoy())
        lblTiempo.text = "00:00"
        audioPath = null
    }

    private fun alerta(msg: String) {
        AlertDialog.Builder(this).setTitle("Alerta").setMessage(msg)
            .setPositiveButton("Aceptar", null).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(cronometro)
        liberarRecorder()
        notaPlayer.detener()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
