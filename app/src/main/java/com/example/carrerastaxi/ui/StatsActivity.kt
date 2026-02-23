package com.example.carrerastaxi.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.carrerastaxi.R
import com.example.carrerastaxi.data.AppDatabase
import com.example.carrerastaxi.utils.DistanceUtils
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatsActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        loadToday()
    }

    private fun loadToday() {
        val dao = AppDatabase.getDatabase(this).dailyStatsDao()
        val tvKm = findViewById<TextView>(R.id.tvCardKm)
        val tvTiempo = findViewById<TextView>(R.id.tvCardTiempo)
        val tvBruta = findViewById<TextView>(R.id.tvCardBruta)
        val tvGas = findViewById<TextView>(R.id.tvCardGas)
        val tvNeta = findViewById<TextView>(R.id.tvCardNeta)
        val chart = findViewById<LineChart>(R.id.chartNetos)

        scope.launch {
            val today = DistanceUtils.getTodayDateISO()
            val stats = withContext(Dispatchers.IO) { dao.statsByDate(today) }
            val last7 = withContext(Dispatchers.IO) { dao.statsByDateRange() }
            if (stats != null) {
                tvKm.text = String.format("%.1f km", stats.totalKm)
                tvTiempo.text = DistanceUtils.formatTime(stats.totalTimeSec)
                tvBruta.text = DistanceUtils.formatMoney(stats.grossEarnings)
                tvGas.text = DistanceUtils.formatMoney(stats.fuelCost)
                tvNeta.text = DistanceUtils.formatMoney(stats.netEarnings)
            } else {
                tvKm.text = "0 km"
                tvTiempo.text = "00:00:00"
                tvBruta.text = "0 Bs"
                tvGas.text = "0 Bs"
                tvNeta.text = "0 Bs"
            }
            val entries = last7.reversed().mapIndexed { idx, ds ->
                Entry(idx.toFloat(), ds.netEarnings.toFloat())
            }
            val dataSet = LineDataSet(entries, "Neta diaria").apply {
                color = resources.getColor(R.color.neon_green, theme)
                setCircleColor(resources.getColor(R.color.neon_blue, theme))
                lineWidth = 2f
            }
            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    }
}
