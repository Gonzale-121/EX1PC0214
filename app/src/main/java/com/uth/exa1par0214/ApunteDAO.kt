package com.uth.exa1par0214

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

class ApunteDAO(context: Context) {

    private val helper = DBHelper(context)

    private fun toValues(a: Apunte) = ContentValues().apply {
        put("asignatura", a.asignatura)
        put("tema", a.tema)
        put("fecha", a.fecha)
        put("descripcion", a.descripcion)
        put("audio_path", a.audioPath)
    }

    private fun fromCursor(c: Cursor) = Apunte(
        c.getInt(0), c.getString(1), c.getString(2),
        c.getString(3), c.getString(4) ?: "", c.getString(5)
    )

    private val columnas = "id,asignatura,tema,fecha,descripcion,audio_path"

    fun insertar(a: Apunte): Long =
        helper.writableDatabase.insert(DBHelper.T_APUNTES, null, toValues(a))

    fun actualizar(a: Apunte): Int =
        helper.writableDatabase.update(DBHelper.T_APUNTES, toValues(a), "id=?", arrayOf(a.id.toString()))

    fun actualizarDescripcion(id: Int, desc: String): Int {
        val v = ContentValues().apply { put("descripcion", desc) }
        return helper.writableDatabase.update(DBHelper.T_APUNTES, v, "id=?", arrayOf(id.toString()))
    }

    fun eliminar(id: Int): Int =
        helper.writableDatabase.delete(DBHelper.T_APUNTES, "id=?", arrayOf(id.toString()))

    fun buscar(id: Int): Apunte? {
        helper.readableDatabase.rawQuery(
            "SELECT $columnas FROM ${DBHelper.T_APUNTES} WHERE id=?", arrayOf(id.toString())
        ).use { c -> return if (c.moveToFirst()) fromCursor(c) else null }
    }

    fun listarPorAsignatura(asignatura: String): MutableList<Apunte> {
        val lista = mutableListOf<Apunte>()
        helper.readableDatabase.rawQuery(
            "SELECT $columnas FROM ${DBHelper.T_APUNTES} WHERE asignatura=? ORDER BY fecha ASC",
            arrayOf(asignatura)
        ).use { c -> while (c.moveToNext()) lista.add(fromCursor(c)) }
        return lista
    }

    fun listarAsignaturas(): MutableList<String> {
        val lista = mutableListOf<String>()
        helper.readableDatabase.rawQuery(
            "SELECT nombre FROM ${DBHelper.T_ASIG} ORDER BY id", null
        ).use { c -> while (c.moveToNext()) lista.add(c.getString(0)) }
        return lista
    }

    fun insertarAsignatura(nombre: String): Boolean {
        val v = ContentValues().apply { put("nombre", nombre) }
        return helper.writableDatabase.insert(DBHelper.T_ASIG, null, v) != -1L
    }
}
