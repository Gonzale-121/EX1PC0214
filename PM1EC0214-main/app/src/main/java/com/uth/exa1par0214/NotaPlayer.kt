package com.uth.exa1par0214

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import java.io.File
import java.io.FileInputStream

/**
 * Reproductor de notas de voz compartido por MainActivity y ListaActivity.
 * Valida el archivo, configura los atributos de audio y reporta el motivo real del fallo.
 */
class NotaPlayer(private val alFinalizar: () -> Unit = {}) {

    private var player: MediaPlayer? = null

    val reproduciendo: Boolean
        get() = try {
            player?.isPlaying == true
        } catch (e: IllegalStateException) {
            false
        }

    /** Devuelve null si empezó a reproducir, o el mensaje de error para mostrar al usuario. */
    fun reproducir(path: String?): String? {
        if (path.isNullOrEmpty()) return "Debe grabar una nota de voz"

        val archivo = File(path)
        if (!archivo.exists()) return "El archivo de audio ya no existe"
        if (archivo.length() == 0L) return "El archivo de audio está vacío"

        detener()

        val mp = MediaPlayer()
        return try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            // Se usa el FileDescriptor: es lo más fiable para archivos del almacenamiento de la app
            FileInputStream(archivo).use { mp.setDataSource(it.fd) }
            mp.setOnCompletionListener {
                detener()
                alFinalizar()
            }
            mp.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                detener()
                alFinalizar()
                true
            }
            mp.prepare()
            mp.start()
            player = mp
            null
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo reproducir $path", e)
            mp.release()
            "No se pudo reproducir la nota: ${e.message}"
        }
    }

    fun detener() {
        player?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (_: IllegalStateException) {
            }
            it.release()
        }
        player = null
    }

    companion object {
        private const val TAG = "NotaPlayer"
    }
}
