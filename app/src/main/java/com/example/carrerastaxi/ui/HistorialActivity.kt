package com.example.carrerastaxi.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.carrerastaxi.R
import com.example.carrerastaxi.data.AppDatabase
import com.example.carrerastaxi.data.CarreraEntity
import com.example.carrerastaxi.utils.DistanceUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HistorialActivity : AppCompatActivity() {

    private lateinit var tvTotalHoy: TextView
    private lateinit var tvTotalMes: TextView
    private lateinit var tvTotalAnio: TextView
    private lateinit var rvCarreras: RecyclerView
    private lateinit var btnActualizarHistorial: Button
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        // Inicializar BD
        database = AppDatabase.Companion.getDatabase(this)

        // Inicializar vistas
        tvTotalHoy = findViewById(R.id.tvTotalHoy)
        tvTotalMes = findViewById(R.id.tvTotalMes)
        tvTotalAnio = findViewById(R.id.tvTotalAnio)
        rvCarreras = findViewById(R.id.rvCarreras)
        btnActualizarHistorial = findViewById(R.id.btnActualizarHistorial)

        // Configurar RecyclerView
        rvCarreras.layoutManager = LinearLayoutManager(this)

        // Cargar datos
        cargarHistorial()

        // Listener del botón actualizar
        btnActualizarHistorial.setOnClickListener {
            cargarHistorial()
        }
    }

    private fun cargarHistorial() {
        lifecycleScope.launch {
            try {
                // Obtener fechas
                val hoy = DistanceUtils.getTodayDateISO()
                val primerDiaDelMes = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                }.time
                val fechaPrimerDia = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(primerDiaDelMes)

                // Calcular totales
                val totalHoyDouble = database.carreraDao().getTotalGananciaByDate(hoy) ?: 0.0
                val totalMesDouble = database.carreraDao()
                    .getTotalGananciaByRange(
                        fechaPrimerDia,
                        hoy
                    ) ?: 0.0
                val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
                val primerDiaAnio = "$currentYear-01-01"
                val ultimoDiaAnio = "$currentYear-12-31"
                val totalAnioDouble = database.carreraDao()
                    .getTotalGananciaByRange(primerDiaAnio, ultimoDiaAnio) ?: 0.0

                // Actualizar UI con totales
                runOnUiThread {
                    tvTotalHoy.text = "📅 HOY: ${DistanceUtils.formatMoney(totalHoyDouble)}"
                    tvTotalMes.text = "📊 MES ACTUAL: ${DistanceUtils.formatMoney(totalMesDouble)}"
                    tvTotalAnio.text = "📈 AÑO: ${DistanceUtils.formatMoney(totalAnioDouble)}"
                }

                // Obtener todas las carreras
                database.carreraDao().getAllCarreras().collect { carreras ->
                    runOnUiThread {
                        val adapter = CarreraAdapter(carreras)
                        rvCarreras.adapter = adapter
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    inner class CarreraAdapter(private val carreras: List<CarreraEntity>) :
        RecyclerView.Adapter<CarreraAdapter.CarreraViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarreraViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_carrera, parent, false)
            return CarreraViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: CarreraViewHolder, position: Int) {
            holder.bind(carreras[position])
        }

        override fun getItemCount(): Int = carreras.size

        inner class CarreraViewHolder(itemView: View) :
            RecyclerView.ViewHolder(itemView) {

            private val tvFechahora: TextView = itemView.findViewById(R.id.tvFechaHora)
            private val tvDistancia: TextView = itemView.findViewById(R.id.tvDistancia)
            private val tvGanancia: TextView = itemView.findViewById(R.id.tvGanancia)
            private val tvFormaPago: TextView = itemView.findViewById(R.id.tvFormaPago)
            private val tvDuracion: TextView = itemView.findViewById(R.id.tvDuracion)

            fun bind(carrera: CarreraEntity) {
                tvFechahora.text = "${carrera.fecha} ${carrera.horaInicio} - ${carrera.horaFin}"
                tvDistancia.text = "📏 ${DistanceUtils.formatDistance(carrera.distanciaKm)}"
                tvGanancia.text = "💵 Ganancia: ${DistanceUtils.formatMoney(carrera.ganancia)}"
                tvFormaPago.text = "💳 ${carrera.formaPago}"
                tvDuracion.text = "⏱ ${DistanceUtils.formatTime(carrera.duracionSegundos)}"
            }
        }
    }
}