package com.example.carrerastaxi.core

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.lang.StringBuilder
import com.example.carrerastaxi.data.AppDatabase
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

/**
 * Provee utilidades de almacenamiento y exportación: CSV, PDF y backup JSON.
 */
class StorageManager(private val context: Context) {

    private val dao by lazy { AppDatabase.getDatabase(context).carreraDao() }

    suspend fun exportAllAsCsv(outFile: File): File = withContext(Dispatchers.IO) {
        val list = dao.getAllCarrerasLimited(1000000).first()
        val sb = StringBuilder()
        sb.append("id,fecha,hora_inicio,hora_fin,duracion_segundos,distancia_km,monto_cobrado,ganancia,velocidad_promedio\n")
        for (c in list) {
            sb.append("${c.id},${c.fecha},${c.horaInicio},${c.horaFin},${c.duracionSegundos},${c.distanciaKm},${c.montoCobrado},${c.ganancia},${c.velocidadPromedio}\n")
        }
        outFile.parentFile?.mkdirs()
        FileOutputStream(outFile).use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }
        outFile
    }

    suspend fun exportTripAsCsv(carreraId: Int, outFile: File): File = withContext(Dispatchers.IO) {
        val c = dao.getCarreraById(carreraId)
        outFile.parentFile?.mkdirs()
        FileOutputStream(outFile).use { fos ->
            if (c != null) {
                val sb = StringBuilder()
                sb.append("id,fecha,hora_inicio,hora_fin,duracion_segundos,distancia_km,monto_cobrado,ganancia,velocidad_promedio\n")
                sb.append("${c.id},${c.fecha},${c.horaInicio},${c.horaFin},${c.duracionSegundos},${c.distanciaKm},${c.montoCobrado},${c.ganancia},${c.velocidadPromedio}\n")
                fos.write(sb.toString().toByteArray(Charsets.UTF_8))
            } else {
                fos.write("".toByteArray())
            }
        }
        outFile
    }

    suspend fun backupDatabaseAsJson(outFile: File): File = withContext(Dispatchers.IO) {
        val list = dao.getAllCarrerasLimited(1000000).first()
        val sb = StringBuilder()
        sb.append("[")
        val it = list.iterator()
        while (it.hasNext()) {
            val c = it.next()
            sb.append("{")
            sb.append("\"id\":${c.id},")
            sb.append("\"fecha\":\"${c.fecha}\",")
            sb.append("\"horaInicio\":\"${c.horaInicio}\",")
            sb.append("\"horaFin\":\"${c.horaFin}\",")
            sb.append("\"duracionSegundos\":${c.duracionSegundos},")
            sb.append("\"distanciaKm\":${c.distanciaKm},")
            sb.append("\"montoCobrado\":${c.montoCobrado},")
            sb.append("\"ganancia\":${c.ganancia},")
            sb.append("\"puntosGPS\":\"${c.puntosGPS.replace('"','\'')}\",")
            sb.append("\"velocidadPromedio\":${c.velocidadPromedio}")
            sb.append("}")
            if (it.hasNext()) sb.append(",")
        }
        sb.append("]")
        outFile.parentFile?.mkdirs()
        FileOutputStream(outFile).use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }
        outFile
    }

    suspend fun exportAllAsPdf(outFile: File, title: String = "Historial de Carreras"): File = withContext(Dispatchers.IO) {
        val list = dao.getAllCarrerasLimited(1000000).first()
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint().apply { textSize = 12f }
        var y = 30f
        canvas.drawText(title, 20f, y, paint)
        y += 20f
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        for (c in list) {
            if (y > 800f) break
            val line = "${c.fecha} ${c.horaInicio}-${c.horaFin} ${String.format(Locale.getDefault(), "%.2fkm", c.distanciaKm)} ${String.format(Locale.getDefault(), "%.2f", c.ganancia)}"
            canvas.drawText(line, 20f, y, paint)
            y += 16f
        }
        doc.finishPage(page)
        outFile.parentFile?.mkdirs()
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        outFile
    }

    // Placeholder for cloud backup: implement provider-specific upload later
    suspend fun uploadBackupToCloud(file: File, provider: String): Boolean = withContext(Dispatchers.IO) {
        // provider is treated as an HTTP upload URL (POST multipart/form-data)
        if (provider.isBlank()) return@withContext false
        try {
            val boundary = "----CarrerasTaxiBoundary${System.currentTimeMillis()}"
            val url = java.net.URL(provider)
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                useCaches = false
                setRequestProperty("Connection", "Keep-Alive")
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connectTimeout = 15000
                readTimeout = 30000
            }

            java.io.DataOutputStream(conn.outputStream).use { dos ->
                // file part
                val fileName = file.name
                dos.writeBytes("--$boundary\r\n")
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n")
                dos.writeBytes("Content-Type: application/json\r\n\r\n")
                file.inputStream().use { it.copyTo(dos) }
                dos.writeBytes("\r\n")
                dos.writeBytes("--$boundary--\r\n")
                dos.flush()
            }

            val code = conn.responseCode
            return@withContext code in 200..299
        } catch (t: Throwable) {
            t.printStackTrace()
            return@withContext false
        }
    }
}
