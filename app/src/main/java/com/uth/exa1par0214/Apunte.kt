package com.uth.exa1par0214

data class Apunte(
    var id: Int = 0,
    var asignatura: String = "",
    var tema: String = "",
    var fecha: String = "",
    var descripcion: String = "",
    var audioPath: String = ""
) {
    override fun toString(): String = "$fecha | $tema"
}
