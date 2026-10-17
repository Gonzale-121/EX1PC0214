package com.uth.exa1par0214

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class ListaActivity : AppCompatActivity() {

    private lateinit var controller: ApunteController
    private lateinit var spFiltro: Spinner
    private lateinit var lista: ListView
    private var apuntes: MutableList<Apunte> = mutableListOf()
    private var seleccion = -1
    private var player: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lista)
        controller = ApunteController(this)

        spFiltro = findViewById(R.id.spFiltro)
        lista = findViewById(R.id.lista)
        lista.choiceMode = ListView.CHOICE_MODE_SINGLE

        spFiltro.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, controller.asignaturas()
        )
        spFiltro.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = cargar()
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        lista.setOnItemClickListener { _, _, pos, _ -> seleccion = pos }

        findViewById<Button>(R.id.btnAtras).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnReproducirNota).setOnClickListener { reproducir() }
        findViewById<Button>(R.id.btnDictar).setOnClickListener { dictar() }
        findViewById<Button>(R.id.btnEliminar).setOnClickListener { eliminar() }
        findViewById<Button>(R.id.btnActualizar).setOnClickListener { actualizar() }
    }

    override fun onResume() {
        super.onResume()
        cargar()
    }

    private fun cargar() {
        val asig = spFiltro.selectedItem as? String ?: return
        apuntes = controller.listar(asig)
        lista.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_activated_1, apuntes)
        seleccion = -1
    }

    private fun actual(): Apunte? {
        if (seleccion < 0 || seleccion >= apuntes.size) {
            Toast.makeText(this, "Seleccione un apunte", Toast.LENGTH_SHORT).show()
            return null
        }
        return apuntes[seleccion]
    }

    private fun reproducir() {
        val a = actual() ?: return
        try {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(a.audioPath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo reproducir la nota", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dictar() {
        val a = actual() ?: return
        startActivity(Intent(this, DictadoActivity::class.java).putExtra("id", a.id))
    }

    private fun actualizar() {
        val a = actual() ?: return
        startActivity(Intent(this, MainActivity::class.java).putExtra("id", a.id))
    }

    private fun eliminar() {
        val a = actual() ?: return
        AlertDialog.Builder(this)
            .setTitle("Eliminar")
            .setMessage("¿Desea eliminar el apunte «${a.tema}»?")
            .setNegativeButton("No", null)
            .setPositiveButton("Sí") { _, _ ->
                controller.eliminar(a.id)
                cargar()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
    }
}
