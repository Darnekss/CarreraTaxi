package com.example.carrerastaxi.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.carrerastaxi.R
import com.example.carrerastaxi.ui.StatsActivity
import com.example.carrerastaxi.ui.viewmodels.StatsViewModel

class StatsFragment : Fragment() {
    private val vm: StatsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_stats, container, false)
        v.findViewById<Button>(R.id.btnOpenStats)?.setOnClickListener {
            startActivity(Intent(requireContext(), StatsActivity::class.java))
        }
        val tvKm = v.findViewById<TextView>(R.id.tvStatKmTotal)
        val tvKmCon = v.findViewById<TextView>(R.id.tvStatKmCon)
        val tvKmSin = v.findViewById<TextView>(R.id.tvStatKmSin)
        val tvTiempo = v.findViewById<TextView>(R.id.tvStatTiempoTotal)
        val tvTiempoCon = v.findViewById<TextView>(R.id.tvStatTiempoPasajero)
        val tvTiempoLibre = v.findViewById<TextView>(R.id.tvStatTiempoLibre)
        val tvBruta = v.findViewById<TextView>(R.id.tvStatBruta)
        val tvGas = v.findViewById<TextView>(R.id.tvStatGas)
        val tvNeta = v.findViewById<TextView>(R.id.tvStatNeta)

        vm.ui.observe(viewLifecycleOwner) {
            tvKm.text = it.km
            tvTiempo.text = it.tiempo
            tvBruta.text = it.bruta
            tvGas.text = it.gas
            tvNeta.text = it.neta
            tvKmCon.text = it.kmCon
            tvKmSin.text = it.kmSin
            tvTiempoCon.text = it.tiempoCon
            tvTiempoLibre.text = it.tiempoLibre
        }
        vm.load()
        return v
    }
}
