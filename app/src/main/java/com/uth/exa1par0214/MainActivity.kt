package com.uth.exa1par0214

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private var player: MediaPlayer? = null
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
    private fun iniciarGrabacion() {
        try {
            val dir = getExternalFilesDir(null) ?: filesDir
            val f = File(dir, "nota_${System.currentTimeMillis()}.3gp")
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(f.absolutePath)
                prepare()
                start()
            }
            audioPath = f.absolutePath
            grabando = true
            btnGrabar.text = "Detener"
            inicio = System.currentTimeMillis()
            handler.post(cronometro)
        } catch (e: Exception) {
            alerta("No se pudo iniciar la grabación")
            liberarRecorder()
            audioPath = null
        }
    }

    private fun detenerGrabacion() {
        handler.removeCallbacks(cronometro)
        try {
            recorder?.stop()
        } catch (e: RuntimeException) {
            audioPath = null
        }
        liberarRecorder()
        grabando = false
        btnGrabar.text = "Grabar"
    }

    private fun liberarRecorder() {
        recorder?.release()
        recorder = null
    }

    private fun reproducir() {
        val path = audioPath
        if (path == null) {
            alerta("Debe grabar una nota de voz")
            return
        }
        try {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(path)
                prepare()
                start()
            }
        } catch (e: Exception) {
            alerta("No se pudo reproducir la nota")
        }
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
        player?.release()
    }
}
