package com.uth.exa1par0214

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Locale

class ApunteController(context: Context) {

    private val dao = ApunteDAO(context)

    /** Devuelve el mensaje de alerta o null si todo es válido. */
    fun validar(tema: String, fecha: String, audioPath: String?): String? = when {
        tema.isBlank() -> "Debe escribir un tema"
        !fechaValida(fecha) -> "La fecha debe tener el formato AAAA-MM-DD"
        audioPath.isNullOrEmpty() -> "Debe grabar una nota de voz"
        else -> null
    }

    private fun fechaValida(f: String): Boolean {
        if (!f.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return false
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(f)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun guardar(a: Apunte) = dao.insertar(a)
    fun actualizar(a: Apunte) = dao.actualizar(a)
    fun actualizarDescripcion(id: Int, d: String) = dao.actualizarDescripcion(id, d)
    fun eliminar(id: Int) = dao.eliminar(id)
    fun buscar(id: Int) = dao.buscar(id)
    fun listar(asignatura: String) = dao.listarPorAsignatura(asignatura)
    fun asignaturas() = dao.listarAsignaturas()
    fun agregarAsignatura(n: String) = dao.insertarAsignatura(n.trim())
}
