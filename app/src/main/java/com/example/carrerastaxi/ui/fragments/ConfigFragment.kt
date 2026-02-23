package com.example.carrerastaxi.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.carrerastaxi.R
import com.example.carrerastaxi.managers.AppSession
import com.example.carrerastaxi.ui.viewmodels.ConfigViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConfigFragment : Fragment() {
    private val vm: ConfigViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_config, container, false)
        val etBase = v.findViewById<EditText>(R.id.etTarifaBase)
        val etKm = v.findViewById<EditText>(R.id.etPrecioKm)
        val etMin = v.findViewById<EditText>(R.id.etPrecioMin)
        val etGas = v.findViewById<EditText>(R.id.etPrecioGas)
        val etKml = v.findViewById<EditText>(R.id.etKmPorLitro)
        val swSound = v.findViewById<Switch>(R.id.switchSonido)
        val swVibra = v.findViewById<Switch>(R.id.switchVibra)
        val swAuto = v.findViewById<Switch>(R.id.switchAuto)
        val etMeta = v.findViewById<EditText>(R.id.etMetaDiaria)
        val btnSave = v.findViewById<Button>(R.id.btnGuardarConfig)
        val btnIniciarJornada = v.findViewById<Button>(R.id.btnIniciarJornada)
        val btnFinalizarJornada = v.findViewById<Button>(R.id.btnFinalizarJornada)
        val tvEstadoJornada = v.findViewById<TextView>(R.id.tvEstadoJornada)
        val btnReset = v.findViewById<Button>(R.id.btnReiniciarJornada)

        vm.ui.observe(viewLifecycleOwner) { cfg ->
            etBase.setText(cfg.base.toString())
            etKm.setText(cfg.km.toString())
            etMin.setText(cfg.min.toString())
            etGas.setText(cfg.gas.toString())
            etKml.setText(cfg.kml.toString())
            swSound.isChecked = cfg.sound
            swVibra.isChecked = cfg.vibra
            swAuto.isChecked = cfg.auto
            etMeta.setText(cfg.goal.toString())
            tvEstadoJornada.text = if (cfg.journeyActive) "Jornada: Activa" else "Jornada: Inactiva"
        }
        vm.load()

        btnSave.setOnClickListener {
            val state = ConfigViewModel.UiState(
                base = etBase.text.toString().toDoubleOrNull() ?: 7.0,
                km = etKm.text.toString().toDoubleOrNull() ?: 5.0,
                min = etMin.text.toString().toDoubleOrNull() ?: 1.0,
                gas = etGas.text.toString().toDoubleOrNull() ?: 7.0,
                kml = etKml.text.toString().toDoubleOrNull() ?: 10.0,
                sound = swSound.isChecked,
                vibra = swVibra.isChecked,
                auto = swAuto.isChecked,
                goal = etMeta.text.toString().toDoubleOrNull() ?: 300.0,
                journeyActive = vm.ui.value?.journeyActive ?: true
            )
            vm.save(state)
            Toast.makeText(requireContext(), "Configuración guardada", Toast.LENGTH_SHORT).show()
        }

        btnIniciarJornada.setOnClickListener {
            AppSession.tripManager.startJourney(System.currentTimeMillis())
            vm.updateJourney(true)
            tvEstadoJornada.text = "Jornada: Activa"
            Toast.makeText(requireContext(), "Jornada iniciada", Toast.LENGTH_SHORT).show()
        }

        btnFinalizarJornada.setOnClickListener {
            val tm = AppSession.tripManager
            val snap = tm.endJourney(System.currentTimeMillis())
            vm.updateJourney(false)
            tvEstadoJornada.text = "Jornada: Inactiva"
            viewLifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val (liters, fuelCost) = com.example.carrerastaxi.managers.FuelManager(vm.ui.value?.kml ?: 10.0, vm.ui.value?.gas ?: 7.0).fuelCost(snap.totalKm)
                    val net = snap.gross - fuelCost
                    com.example.carrerastaxi.data.AppDatabase.getDatabase(requireContext()).dailyStatsDao().upsert(
                        com.example.carrerastaxi.data.DailyStatsEntity(
                            date = com.example.carrerastaxi.utils.DistanceUtils.getTodayDateISO(),
                            totalKm = snap.totalKm,
                            kmWithPassenger = snap.kmWithPassenger,
                            kmWithoutPassenger = snap.kmWithoutPassenger,
                            totalTimeSec = snap.timeTotal,
                            timeWithPassengerSec = snap.timeWithPassenger,
                            timeWithoutPassengerSec = snap.timeWithoutPassenger,
                            grossEarnings = snap.gross,
                            fuelCost = fuelCost,
                            netEarnings = net
                        )
                    )
                }
            }
            Toast.makeText(requireContext(), "Jornada finalizada y guardada", Toast.LENGTH_SHORT).show()
        }

        btnReset.setOnClickListener {
            val ctx = requireContext()
            val today = com.example.carrerastaxi.utils.DistanceUtils.getTodayDateISO()
            viewLifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    com.example.carrerastaxi.data.AppDatabase.getDatabase(ctx).dailyStatsDao().upsert(
                        com.example.carrerastaxi.data.DailyStatsEntity(
                            date = today,
                            totalKm = 0.0,
                            kmWithPassenger = 0.0,
                            kmWithoutPassenger = 0.0,
                            totalTimeSec = 0,
                            timeWithPassengerSec = 0,
                            timeWithoutPassengerSec = 0,
                            grossEarnings = 0.0,
                            fuelCost = 0.0,
                            netEarnings = 0.0
                        )
                    )
                }
            }
            Toast.makeText(requireContext(), "Jornada reiniciada", Toast.LENGTH_SHORT).show()
        }

        return v
    }
}
