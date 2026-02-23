package com.example.carrerastaxi.ui

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.carrerastaxi.R
import com.example.carrerastaxi.data.AppDatabase
import com.example.carrerastaxi.data.CarreraEntity
import com.example.carrerastaxi.core.StorageManager
import com.example.carrerastaxi.utils.DistanceUtils
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.LruCache
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Actividad para mostrar el historial de carreras con filtros y busqueda
 */
class HistorialActivity : AppCompatActivity() {

    private lateinit var etBusqueda: EditText
    private lateinit var tvTotalHoy: TextView
    private lateinit var tvTotalMes: TextView
    private lateinit var tvTotalAnio: TextView
    private lateinit var tvEstadisticas: TextView
    private lateinit var rvCarreras: RecyclerView
    private lateinit var btnActualizarHistorial: Button
    private lateinit var btnExportPdf: Button
    private lateinit var btnExportCsv: Button
    private lateinit var btnAtras: Button
    private lateinit var btnBorrarTodo: Button
    private lateinit var chartSemanal: LineChart
    private lateinit var btnFiltroHoy: Button
    private lateinit var btnFiltro7Dias: Button
    private lateinit var btnFiltroMes: Button
    private lateinit var btnFiltroTodo: Button
    private lateinit var etPrecioLitro: EditText
    private lateinit var etKmPorLitro: EditText
    private lateinit var database: AppDatabase

    private var carrerasActuales = listOf<CarreraEntity>()
    private var carrerasMostradas = listOf<CarreraEntity>()
    private var filtroActual = FiltroTiempo.HOY
    private var historialJob: Job? = null
    private val limiteHistorial = 200
    private val croquisCache = object : LruCache<Int, Bitmap>(30) {}
    private val placeholderCroquis: Bitmap by lazy { crearPlaceholderCroquis() }

    enum class FiltroTiempo {
        HOY, SIETE_DIAS, MES, TODO

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        database = AppDatabase.getDatabase(this)

        // Inicializar vistas
        etBusqueda = findViewById(R.id.etBusqueda)
        tvTotalHoy = findViewById(R.id.tvTotalHoy)
        tvTotalMes = findViewById(R.id.tvTotalMes)
        tvTotalAnio = findViewById(R.id.tvTotalAnio)
        tvEstadisticas = findViewById(R.id.tvEstadisticas)
        rvCarreras = findViewById(R.id.rvCarreras)
        btnActualizarHistorial = findViewById(R.id.btnActualizarHistorial)
        btnExportPdf = findViewById(R.id.btnExportPdf)
        btnExportCsv = findViewById(R.id.btnExportCsv)
        btnAtras = findViewById(R.id.btnAtras)
        btnBorrarTodo = findViewById(R.id.btnBorrarTodo)
        chartSemanal = findViewById(R.id.chartSemanal)
        btnFiltroHoy = findViewById(R.id.btnFiltroHoy)
        btnFiltro7Dias = findViewById(R.id.btnFiltro7Dias)
        btnFiltroMes = findViewById(R.id.btnFiltroMes)
        btnFiltroTodo = findViewById(R.id.btnFiltroTodo)
        etPrecioLitro = findViewById(R.id.etPrecioLitro)
        etKmPorLitro = findViewById(R.id.etKmPorLitro)

        // Configurar RecyclerView
        rvCarreras.layoutManager = LinearLayoutManager(this)
        configurarGrafico()
        configurarSwipeBorrado()

        // Listeners de filtros
        btnFiltroHoy.setOnClickListener {
            filtroActual = FiltroTiempo.HOY
            actualizarFiltros()
            cargarHistorial()
        }
        btnFiltro7Dias.setOnClickListener {
            filtroActual = FiltroTiempo.SIETE_DIAS
            actualizarFiltros()
            cargarHistorial()
        }
        btnFiltroMes.setOnClickListener {
            filtroActual = FiltroTiempo.MES
            actualizarFiltros()
            cargarHistorial()
        }
        btnFiltroTodo.setOnClickListener {
            filtroActual = FiltroTiempo.TODO
            actualizarFiltros()
            cargarHistorial()
        }

        // Listener de busqueda
        etBusqueda.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarPorBusqueda(s.toString())
            }
        })

        // Cargar datos
        cargarHistorial()
        actualizarFiltros()

        // Listener del boton actualizar
        btnActualizarHistorial.setOnClickListener {
            cargarHistorial()
        }

        // Exportar PDF
        btnExportPdf.setOnClickListener {
            exportarPdf()
        }
        btnExportCsv.setOnClickListener {
            exportarCsv()
        }

        btnAtras.setOnClickListener {
            finish()
        }

        btnBorrarTodo.setOnClickListener {
            confirmarBorradoFiltro()
        }

        val recalcWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                actualizarEstadisticas(carrerasMostradas)
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        etPrecioLitro.addTextChangedListener(recalcWatcher)
        etKmPorLitro.addTextChangedListener(recalcWatcher)
    }

    /**
     * Actualiza el estado visual de los botones de filtro
     */
    private fun actualizarFiltros() {
        btnFiltroHoy.alpha = if (filtroActual == FiltroTiempo.HOY) 1.0f else 0.5f
        btnFiltro7Dias.alpha = if (filtroActual == FiltroTiempo.SIETE_DIAS) 1.0f else 0.5f
        btnFiltroMes.alpha = if (filtroActual == FiltroTiempo.MES) 1.0f else 0.5f
        btnFiltroTodo.alpha = if (filtroActual == FiltroTiempo.TODO) 1.0f else 0.5f
    }

    /**
     * Carga el historial con filtro de tiempo
     */
    private fun cargarHistorial() {
        historialJob?.cancel()
        historialJob = lifecycleScope.launch {
            try {
                val (hoy, hace7Dias, fechaPrimerDia) = obtenerFechasFiltro()

                // Actualizar totales globales
                val (totalHoyDouble, totalMesDouble, totalAnioDouble) = withContext(Dispatchers.IO) {
                    val totalHoy = database.carreraDao().getTotalGananciaByDate(hoy) ?: 0.0
                    val totalMes = database.carreraDao().getTotalGananciaByRange(fechaPrimerDia, hoy) ?: 0.0
                    val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
                    val totalAnio =
                        database.carreraDao().getTotalGananciaByRange("$currentYear-01-01", "$currentYear-12-31")
                            ?: 0.0
                    Triple(totalHoy, totalMes, totalAnio)
                }

                tvTotalHoy.text = "HOY: ${DistanceUtils.formatMoney(totalHoyDouble)}"
                tvTotalMes.text = "MES: ${DistanceUtils.formatMoney(totalMesDouble)}"
                tvTotalAnio.text = "ANO: ${DistanceUtils.formatMoney(totalAnioDouble)}"

                // Obtener carreras segun filtro
                val carreras = when (filtroActual) {
                    FiltroTiempo.HOY -> database.carreraDao().getCarrerasByDateLimited(hoy, limiteHistorial)
                    FiltroTiempo.SIETE_DIAS ->
                        database.carreraDao().getCarrerasByRangeLimited(hace7Dias, hoy, limiteHistorial)
                    FiltroTiempo.MES ->
                        database.carreraDao().getCarrerasByRangeLimited(fechaPrimerDia, hoy, limiteHistorial)
                    FiltroTiempo.TODO -> database.carreraDao().getAllCarrerasLimited(limiteHistorial)
                }

                // Recolectar carreras
                carreras.collect { listaCarreras ->
                    carrerasActuales = listaCarreras
                    carrerasMostradas = listaCarreras
                    val adapter = CarreraAdapter(listaCarreras)
                    rvCarreras.adapter = adapter
                    actualizarEstadisticas(listaCarreras)
                    actualizarGraficoSemanal(listaCarreras)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Actualiza las estadisticas mostradas
     */
    private fun actualizarEstadisticas(carreras: List<CarreraEntity>) {
        if (carreras.isEmpty()) {
            tvEstadisticas.text = "Sin carreras en este periodo"
            return
        }

        val totalGanancia = carreras.sumOf { it.ganancia }
        val totalCobrado = carreras.sumOf { it.montoCobrado }
        val totalDistancia = carreras.sumOf { it.distanciaKm }
        val totalDuracion = carreras.sumOf { it.duracionSegundos }
        val promedioGanancia = totalGanancia / carreras.size
        val promedioDistancia = totalDistancia / carreras.size
        val maxGanancia = carreras.maxOf { it.ganancia }
        val minGanancia = carreras.minOf { it.ganancia }
        val gananciaPorKm = if (totalDistancia > 0.0) totalGanancia / totalDistancia else 0.0
        val totalHoras = totalDuracion / 3600.0
        val gananciaPorHora = if (totalHoras > 0.0) totalGanancia / totalHoras else 0.0

        val precioLitro = etPrecioLitro.text.toString().toDoubleOrNull() ?: 0.0
        val kmPorLitro = etKmPorLitro.text.toString().toDoubleOrNull() ?: 0.0
        val costoGasolina = if (precioLitro > 0.0 && kmPorLitro > 0.0) {
            (totalDistancia / kmPorLitro) * precioLitro
        } else 0.0
        val gananciaNeta = totalGanancia - costoGasolina

        val agrupadoDia = carreras.groupBy { it.fecha }
        val mejorDia = agrupadoDia.maxByOrNull { it.value.sumOf { c -> c.ganancia } }?.key ?: "-"
        val mejorDiaMonto = agrupadoDia[mejorDia]?.sumOf { it.ganancia } ?: 0.0

        val agrupadoHora = carreras.groupBy { it.horaInicio.take(2).toIntOrNull() ?: 0 }
        val mejorHora = agrupadoHora.maxByOrNull { it.value.sumOf { c -> c.ganancia } }?.key ?: 0

        val mejorCarrera = carreras.maxByOrNull { it.ganancia }
        val peorCarrera = carreras.minByOrNull { it.ganancia }
        val promedioDiario = if (agrupadoDia.isNotEmpty()) totalGanancia / agrupadoDia.size else 0.0
        val prediccionDia = promedioDiario

        tvEstadisticas.text = """
            ESTADISTICAS (${carreras.size} carreras)
            Bruta: ${DistanceUtils.formatMoney(totalGanancia)} | Cobrada: ${DistanceUtils.formatMoney(totalCobrado)}
            Distancia: ${"%.1f".format(totalDistancia)} km | Duracion: ${DistanceUtils.formatTime(totalDuracion)}
            Por km: ${DistanceUtils.formatMoney(gananciaPorKm)} | Por hora: ${DistanceUtils.formatMoney(gananciaPorHora)}
            Promedio carrera: ${DistanceUtils.formatMoney(promedioGanancia)} | Promedio distancia: ${"%.1f".format(promedioDistancia)} km
            Mejor dia: $mejorDia (${DistanceUtils.formatMoney(mejorDiaMonto)}) | Mejor hora: ${"%02d".format(mejorHora)}:00
            Mejor carrera: ${DistanceUtils.formatMoney(mejorCarrera?.ganancia ?: 0.0)} | Peor carrera: ${DistanceUtils.formatMoney(peorCarrera?.ganancia ?: 0.0)}
            Maximo: ${DistanceUtils.formatMoney(maxGanancia)} | Minimo: ${DistanceUtils.formatMoney(minGanancia)}
            Costo gasolina est.: ${DistanceUtils.formatMoney(costoGasolina)} | Neta: ${DistanceUtils.formatMoney(gananciaNeta)}
            Promedio diario: ${DistanceUtils.formatMoney(promedioDiario)} | Prediccion diaria: ${DistanceUtils.formatMoney(prediccionDia)}
        """.trimIndent()
    }

    /**
     * Filtra las carreras por busqueda de texto
     */
    private fun filtrarPorBusqueda(texto: String) {
        val filtrado = if (texto.isEmpty()) {
            carrerasActuales
        } else {
            carrerasActuales.filter { carrera ->
                carrera.fecha.contains(texto, ignoreCase = true) ||
                    carrera.formaPago.contains(texto, ignoreCase = true) ||
                    carrera.distanciaKm.toString().contains(texto) ||
                    carrera.ganancia.toString().contains(texto)
            }
        }

        carrerasMostradas = filtrado
        val adapter = CarreraAdapter(filtrado)
        rvCarreras.adapter = adapter
        actualizarGraficoSemanal(filtrado)
    }

    private fun configurarGrafico() {
        chartSemanal.setTouchEnabled(false)
        chartSemanal.description.isEnabled = false
        chartSemanal.legend.isEnabled = false
        chartSemanal.axisRight.isEnabled = false
        chartSemanal.axisLeft.textColor = getColor(R.color.text_muted)
        chartSemanal.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chartSemanal.xAxis.textColor = getColor(R.color.text_muted)
        chartSemanal.xAxis.setDrawGridLines(false)
        chartSemanal.axisLeft.setDrawGridLines(true)
        chartSemanal.setDrawGridBackground(false)
    }

    private fun actualizarGraficoSemanal(carreras: List<CarreraEntity>) {
        val entradas = mutableListOf<Entry>()
        val labels = mutableListOf<String>()
        val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())

        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val key = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
            val totalDia = carreras.filter { it.fecha == key }.sumOf { it.ganancia }
            entradas.add(Entry((6 - i).toFloat(), totalDia.toFloat()))
            labels.add(sdf.format(cal.time))
        }

        val dataSet = LineDataSet(entradas, "Ganancia").apply {
            color = getColor(R.color.neon_blue)
            valueTextColor = getColor(R.color.text_neon)
            lineWidth = 2f
            setCircleColor(getColor(R.color.neon_blue))
            circleRadius = 3f
            setDrawValues(false)
        }

        chartSemanal.data = LineData(dataSet)
        chartSemanal.xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
        chartSemanal.invalidate()
    }

    private fun configurarSwipeBorrado() {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                val carrera = carrerasMostradas.getOrNull(pos)
                if (carrera == null) {
                    rvCarreras.adapter?.notifyItemChanged(pos)
                    return
                }

                AlertDialog.Builder(this@HistorialActivity)
                    .setTitle("Borrar carrera")
                    .setMessage("¿Deseas eliminar esta carrera del historial?")
                    .setPositiveButton("Borrar") { _, _ ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            database.carreraDao().deleteCarrera(carrera)
                            withContext(Dispatchers.Main) { cargarHistorial() }
                        }
                    }
                    .setNegativeButton("Cancelar") { _, _ ->
                        rvCarreras.adapter?.notifyItemChanged(pos)
                    }
                    .setOnCancelListener {
                        rvCarreras.adapter?.notifyItemChanged(pos)
                    }
                    .show()
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(rvCarreras)
    }

    private fun confirmarBorradoFiltro() {
        if (carrerasActuales.isEmpty()) {
            Toast.makeText(this, "No hay carreras para borrar", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Borrar historial")
            .setMessage("¿Deseas borrar todas las carreras del filtro actual?")
            .setPositiveButton("Borrar") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    borrarSegunFiltro()
                    withContext(Dispatchers.Main) { cargarHistorial() }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private suspend fun borrarSegunFiltro() {
        val (hoy, hace7Dias, fechaPrimerDia) = obtenerFechasFiltro()
        when (filtroActual) {
            FiltroTiempo.HOY -> database.carreraDao().deleteCarrerasByDate(hoy)
            FiltroTiempo.SIETE_DIAS -> database.carreraDao().deleteCarrerasByRange(hace7Dias, hoy)
            FiltroTiempo.MES -> database.carreraDao().deleteCarrerasByRange(fechaPrimerDia, hoy)
            FiltroTiempo.TODO -> database.carreraDao().deleteAllCarreras()
        }
    }

    private fun obtenerFechasFiltro(): Triple<String, String, String> {
        val hoy = DistanceUtils.getTodayDateISO()
        val hace7Dias = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.let {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.time)
        }
        val primerDiaDelMes = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }.time
        val fechaPrimerDia = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(primerDiaDelMes)
        return Triple(hoy, hace7Dias, fechaPrimerDia)
    }

    private fun exportarPdf() {
        val lista = carrerasMostradas
        if (lista.isEmpty()) {
            Toast.makeText(this, "No hay datos para exportar", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val nombre = "historial_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
            val ok = withContext(Dispatchers.IO) {
                guardarPdfEnDescargas(nombre, lista)
            }
            if (ok) {
                Toast.makeText(this@HistorialActivity, "PDF guardado en Descargas", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@HistorialActivity, "Error al guardar PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportarCsv() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val storage = StorageManager(this@HistorialActivity)
                val nombre = "historial_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
                val out = File(cacheDir, nombre)
                val file = storage.exportAllAsCsv(out)
                val ok = guardarCsvEnDescargas(file.name, file.readText(Charsets.UTF_8))
                withContext(Dispatchers.Main) {
                    if (ok) Toast.makeText(this@HistorialActivity, "CSV guardado en Descargas", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(this@HistorialActivity, "Error al guardar CSV", Toast.LENGTH_SHORT).show()
                }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@HistorialActivity, "Error exportando CSV", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun guardarCsvEnDescargas(nombre: String, contenido: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, nombre)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(contenido.toByteArray(Charsets.UTF_8))
                    }
                    true
                } else false
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, nombre)
                FileOutputStream(file).use { out ->
                    out.write(contenido.toByteArray(Charsets.UTF_8))
                }
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun guardarPdfEnDescargas(nombre: String, carreras: List<CarreraEntity>): Boolean {
        val doc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 1f }

        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = margin

        fun drawHeader() {
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = 16f
            canvas.drawText("Historial de Carreras", margin, y, paint)
            y += 18f

            paint.typeface = Typeface.DEFAULT
            paint.textSize = 10f
            canvas.drawText("Filtro: ${getFiltroLabel()}", margin, y, paint)
            y += 12f
            val fechaGen = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText("Generado: $fechaGen", margin, y, paint)
            y += 16f

            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = 10f
            canvas.drawText("Fecha/Hora", margin, y, paint)
            canvas.drawText("Dist (km)", 260f, y, paint)
            canvas.drawText("Ganancia", 340f, y, paint)
            canvas.drawText("Pago", 440f, y, paint)
            y += 8f
            canvas.drawLine(margin, y, (pageWidth - margin), y, linePaint)
            y += 14f
            paint.typeface = Typeface.DEFAULT
        }

        fun newPage() {
            doc.finishPage(page)
            pageNumber++
            page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = margin
            drawHeader()
        }

        drawHeader()

        for (carrera in carreras) {
            if (y > pageHeight - 80) {
                newPage()
            }
            val fecha = "${carrera.fecha} ${carrera.horaInicio}"
            paint.textSize = 10f
            canvas.drawText(fecha, margin, y, paint)
            canvas.drawText(String.format("%.2f", carrera.distanciaKm), 260f, y, paint)
            canvas.drawText(String.format("%.2f", carrera.ganancia), 340f, y, paint)
            canvas.drawText(carrera.formaPago, 440f, y, paint)
            y += 14f
        }

        val totalGanancia = carreras.sumOf { it.ganancia }
        val totalDistancia = carreras.sumOf { it.distanciaKm }
        val totalDuracion = carreras.sumOf { it.duracionSegundos }

        if (y > pageHeight - 80) {
            newPage()
        }
        y += 8f
        canvas.drawLine(margin, y, (pageWidth - margin), y, linePaint)
        y += 16f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 11f
        canvas.drawText("Totales", margin, y, paint)
        y += 14f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Carreras: ${carreras.size}", margin, y, paint)
        y += 12f
        canvas.drawText("Distancia: ${String.format("%.2f", totalDistancia)} km", margin, y, paint)
        y += 12f
        canvas.drawText("Duracion: ${DistanceUtils.formatTime(totalDuracion)}", margin, y, paint)
        y += 12f
        canvas.drawText("Ganancia: ${DistanceUtils.formatMoney(totalGanancia)}", margin, y, paint)

        doc.finishPage(page)

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, nombre)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { out ->
                        doc.writeTo(out)
                    }
                    true
                } else {
                    false
                }
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, nombre)
                FileOutputStream(file).use { out ->
                    doc.writeTo(out)
                }
                true
            }
        } catch (e: Exception) {
            false
        } finally {
            doc.close()
        }
    }

    private fun getFiltroLabel(): String {
        return when (filtroActual) {
            FiltroTiempo.HOY -> "Hoy"
            FiltroTiempo.SIETE_DIAS -> "Ultimos 7 dias"
            FiltroTiempo.MES -> "Mes"
            FiltroTiempo.TODO -> "Todo"
        }
    }

    /**
     * Adaptador para el RecyclerView de carreras
     */
    inner class CarreraAdapter(private val carreras: List<CarreraEntity>) :
        RecyclerView.Adapter<CarreraAdapter.CarreraViewHolder>() {

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): CarreraViewHolder {
            val itemView = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_carrera, parent, false)
            return CarreraViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: CarreraViewHolder, position: Int) {
            holder.bind(carreras[position])
        }

        override fun getItemCount(): Int = carreras.size

        inner class CarreraViewHolder(itemView: android.view.View) :
            RecyclerView.ViewHolder(itemView) {

            private val tvFechahora: TextView = itemView.findViewById(R.id.tvFechaHora)
            private val tvDistancia: TextView = itemView.findViewById(R.id.tvDistancia)
            private val tvGanancia: TextView = itemView.findViewById(R.id.tvGanancia)
            private val tvFormaPago: TextView = itemView.findViewById(R.id.tvFormaPago)
            private val tvDuracion: TextView = itemView.findViewById(R.id.tvDuracion)
            private val ivCroquis: ImageView = itemView.findViewById(R.id.ivCroquis)

            fun bind(carrera: CarreraEntity) {
                tvFechahora.text = "${carrera.fecha} ${carrera.horaInicio} - ${carrera.horaFin}"
                tvDistancia.text = "Distancia: ${DistanceUtils.formatDistance(carrera.distanciaKm)}"
                tvGanancia.text = "Ganancia: ${DistanceUtils.formatMoney(carrera.ganancia)}"
                tvFormaPago.text = "Pago: ${carrera.formaPago}"
                tvDuracion.text = "Duracion: ${DistanceUtils.formatTime(carrera.duracionSegundos)}"

                ivCroquis.setImageBitmap(placeholderCroquis)
                ivCroquis.tag = carrera.id

                val cached = croquisCache.get(carrera.id)
                if (cached != null) {
                    ivCroquis.setImageBitmap(cached)
                    return
                }

                lifecycleScope.launch(Dispatchers.Default) {
                    val puntos = DistanceUtils.stringToLatLngList(carrera.puntosGPS)
                    val bmp = renderCroquis(puntos)
                    croquisCache.put(carrera.id, bmp)
                    withContext(Dispatchers.Main) {
                        if (ivCroquis.tag == carrera.id) {
                            ivCroquis.setImageBitmap(bmp)
                        }
                    }
                }
            }
        }
    }

    private fun renderCroquis(points: List<GeoPoint>): Bitmap {
        val width = 600
        val height = 240
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0E1627.toInt()
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)

        if (points.size < 2) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFF8AA3C4.toInt()
                textSize = 24f
            }
            canvas.drawText("Sin ruta", 20f, height / 2f, p)
            return bmp
        }

        val lats = points.map { it.latitude }
        val lons = points.map { it.longitude }
        val minLat = lats.minOrNull() ?: 0.0
        val maxLat = lats.maxOrNull() ?: 0.0
        val minLon = lons.minOrNull() ?: 0.0
        val maxLon = lons.maxOrNull() ?: 0.0

        val padding = 20f
        val latRange = (maxLat - minLat).takeIf { it != 0.0 } ?: 0.0001
        val lonRange = (maxLon - minLon).takeIf { it != 0.0 } ?: 0.0001

        val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF4FC3FF.toInt()
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }

        val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF26D9A5.toInt()
            style = Paint.Style.FILL
        }

        var prevX = 0f
        var prevY = 0f
        points.forEachIndexed { idx, p ->
            val x = padding + ((p.longitude - minLon) / lonRange).toFloat() * (width - 2 * padding)
            val y = padding + ((maxLat - p.latitude) / latRange).toFloat() * (height - 2 * padding)
            if (idx > 0) {
                canvas.drawLine(prevX, prevY, x, y, pathPaint)
            }
            prevX = x
            prevY = y
        }

        val start = points.first()
        val end = points.last()
        val sx = padding + ((start.longitude - minLon) / lonRange).toFloat() * (width - 2 * padding)
        val sy = padding + ((maxLat - start.latitude) / latRange).toFloat() * (height - 2 * padding)
        val ex = padding + ((end.longitude - minLon) / lonRange).toFloat() * (width - 2 * padding)
        val ey = padding + ((maxLat - end.latitude) / latRange).toFloat() * (height - 2 * padding)
        canvas.drawCircle(sx, sy, 6f, pointPaint)
        canvas.drawCircle(ex, ey, 6f, pointPaint)

        return bmp
    }

    private fun crearPlaceholderCroquis(): Bitmap {
        val width = 600
        val height = 240
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0E1627.toInt() }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF8AA3C4.toInt()
            textSize = 22f
        }
        canvas.drawText("Cargando...", 20f, height / 2f, p)
        return bmp
    }
}
