package com.uth.exa1par0214

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class DictadoActivity : AppCompatActivity() {

    private lateinit var controller: ApunteController
    private lateinit var txtDictado: EditText
    private var apunteId = -1

    private val dictado =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            if (r.resultCode == RESULT_OK) {
                val res = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!res.isNullOrEmpty()) {
                    val actual = txtDictado.text.toString().trim()
                    txtDictado.setText(if (actual.isEmpty()) res[0] else "$actual ${res[0]}")
                }
            }
        }

    private val permisoMic =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            if (ok) lanzarDictado()
            else Toast.makeText(this, "Permiso de micrófono denegado", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dictado)
        controller = ApunteController(this)

        apunteId = intent.getIntExtra("id", -1)
        txtDictado = findViewById(R.id.txtDictado)

        controller.buscar(apunteId)?.let { a ->
            findViewById<TextView>(R.id.lblTemaDictado).text = a.tema
            txtDictado.setText(a.descripcion)
        }

        findViewById<Button>(R.id.btnAtrasD).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnMic).setOnClickListener { pedirPermisoYDictar() }
        findViewById<Button>(R.id.btnGuardarDesc).setOnClickListener {
            controller.actualizarDescripcion(apunteId, txtDictado.text.toString().trim())
            Toast.makeText(this, "Descripción guardada", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun pedirPermisoYDictar() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED -> lanzarDictado()

            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) ->
                mostrarExplicacion()

            else -> permisoMic.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun mostrarExplicacion() {
        AlertDialog.Builder(this)
            .setTitle("Permiso necesario")
            .setMessage("El micrófono se usa para dictar la descripción del apunte.")
            .setPositiveButton("Entendido") { _, _ ->
                permisoMic.launch(Manifest.permission.RECORD_AUDIO)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun lanzarDictado() {
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-HN")
        }
        try {
            dictado.launch(i)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "El dispositivo no soporta reconocimiento de voz", Toast.LENGTH_LONG).show()
        }
    }
}
