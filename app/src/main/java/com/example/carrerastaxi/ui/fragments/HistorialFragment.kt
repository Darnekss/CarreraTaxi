package com.example.carrerastaxi.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.carrerastaxi.R
import com.example.carrerastaxi.data.AppDatabase
import com.example.carrerastaxi.data.DailyStatsEntity
import com.example.carrerastaxi.utils.DistanceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HistorialFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_historial, container, false)
        val rv = v.findViewById<RecyclerView>(R.id.rvHistorial)
        rv.layoutManager = LinearLayoutManager(requireContext())
        val adapter = StatsAdapter()
        rv.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            val list = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).dailyStatsDao().statsByDateRange()
            }
            adapter.submit(list)
        }

        v.findViewById<View>(R.id.btnExportCsv)?.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val list = AppDatabase.getDatabase(requireContext()).dailyStatsDao().statsByDateRange()
                val csv = buildString {
                    append("fecha,totalKm,bruta,gasolina,neta,tiempo\n")
                    list.forEach {
                        append("${it.date},${"%.2f".format(it.totalKm)},${it.grossEarnings},${it.fuelCost},${it.netEarnings},${it.totalTimeSec}\n")
                    }
                }
                val filename = "taxbolivia_historial.csv"
                saveToDownloads(filename, csv.toByteArray())
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "CSV exportado a Descargas/$filename", Toast.LENGTH_LONG).show()
                }
            }
        }

        v.findViewById<View>(R.id.btnExportPdf)?.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val list = AppDatabase.getDatabase(requireContext()).dailyStatsDao().statsByDateRange()
                val pdfDoc = android.graphics.pdf.PdfDocument()
                val paint = android.graphics.Paint().apply { textSize = 14f; color = android.graphics.Color.BLACK }
                var pageNumber = 1
                var y = 40
                var page = pdfDoc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                val canvas = page.canvas
                list.forEachIndexed { idx, it ->
                    if (y > 780) {
                        pdfDoc.finishPage(page)
                        pageNumber++
                        y = 40
                        page = pdfDoc.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                    }
                    canvas.drawText("${it.date}  KM:${"%.1f".format(it.totalKm)}  Neta:${"%.1f".format(it.netEarnings)} Bs", 40f, y.toFloat(), paint)
                    y += 24
                }
                pdfDoc.finishPage(page)
                val filename = "taxbolivia_historial.pdf"
                saveToDownloads(filename, pdfDoc)
                pdfDoc.close()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "PDF exportado a Descargas/$filename", Toast.LENGTH_LONG).show()
                }
            }
        }
        return v
    }

    private class StatsAdapter : RecyclerView.Adapter<StatsAdapter.VH>() {
        private val items = mutableListOf<DailyStatsEntity>()
        fun submit(list: List<DailyStatsEntity>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_historial, parent, false)
            return VH(v)
        }
        override fun getItemCount(): Int = items.size
        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
        class VH(view: View) : RecyclerView.ViewHolder(view) {
            private val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
            private val tvSub = view.findViewById<TextView>(R.id.tvSub)
            fun bind(item: DailyStatsEntity) {
                tvTitle.text = "${item.date} - ${DistanceUtils.formatMoney(item.netEarnings)}"
                tvSub.text = "KM ${"%.1f".format(item.totalKm)} | Tiempo ${DistanceUtils.formatTime(item.totalTimeSec)}"
            }
        }
    }

    private fun saveToDownloads(name: String, data: ByteArray) {
        val resolver = requireContext().contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { out -> out.write(data) }
        }
    }

    private fun saveToDownloads(name: String, pdf: android.graphics.pdf.PdfDocument) {
        val resolver = requireContext().contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { out -> pdf.writeTo(out) }
        }
    }
}
