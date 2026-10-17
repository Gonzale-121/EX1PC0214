package com.uth.exa1par0214

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        const val DB_NAME = "bitacora.db"
        const val DB_VERSION = 1
        const val T_ASIG = "asignaturas"
        const val T_APUNTES = "apuntes"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $T_ASIG (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT UNIQUE NOT NULL)")
        db.execSQL(
            "CREATE TABLE $T_APUNTES (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "asignatura TEXT NOT NULL, tema TEXT NOT NULL, fecha TEXT NOT NULL, " +
                    "descripcion TEXT, audio_path TEXT NOT NULL)"
        )
        listOf("Programación Móvil I", "Base de Datos I", "Redes I", "Inglés Técnico").forEach {
            val v = ContentValues().apply { put("nombre", it) }
            db.insert(T_ASIG, null, v)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $T_APUNTES")
        db.execSQL("DROP TABLE IF EXISTS $T_ASIG")
        onCreate(db)
    }
}
