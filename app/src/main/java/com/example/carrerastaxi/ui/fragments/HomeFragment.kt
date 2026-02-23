package com.example.carrerastaxi.ui.fragments

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.carrerastaxi.R
import com.example.carrerastaxi.data.AppDatabase
import com.example.carrerastaxi.managers.AppSession
import com.example.carrerastaxi.managers.TripManager
import com.example.carrerastaxi.service.LocationService
import com.example.carrerastaxi.utils.DistanceUtils
import com.example.carrerastaxi.utils.Prefs
import com.example.carrerastaxi.ui.viewmodels.HomeViewModel
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class HomeFragment : Fragment() {
    private lateinit var mapView: MapView
    private lateinit var btnMain: Button
    private lateinit var btnCenter: View
    private lateinit var btnEmergency: View
    private lateinit var btnLock: View
    private lateinit var tvEstado: TextView
    private lateinit var tvKmHoy: TextView
    private lateinit var tvGananciaHoy: TextView
    private lateinit var tvVelocidad: TextView
    private lateinit var tvMeta: TextView
    private lateinit var progressMeta: android.widget.ProgressBar
    private lateinit var panel: View
    private lateinit var tvDist: TextView
    private lateinit var tvTiempo: TextView
    private lateinit var tvMonto: TextView
    private lateinit var tvVelPanel: TextView
    private lateinit var tvKmCon: TextView
    private lateinit var tvKmSin: TextView
    private lateinit var tvTimeTot: TextView
    private lateinit var tvTimeCon: TextView
    private lateinit var tvTimeSin: TextView
    private lateinit var tvGas: TextView

    private val tripManager = AppSession.tripManager
    private val vm: HomeViewModel by viewModels()
    private var cfg = Prefs.Config(7.0,5.0,1.0,7.0,10.0,true,true,false,300.0,true)

    private var polyLibre: Polyline? = null
    private var polyPas: Polyline? = null
    private var startMarker: Marker? = null
    private var endMarker: Marker? = null
    private val trafficMarkers = mutableListOf<Marker>()
    private var myLocationOverlay: MyLocationNewOverlay? = null
    private var lastGeo: GeoPoint? = null
    private var locked = false
    private var firstFix = false

    private val snapshotReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return
            if (intent.action == LocationService.ACTION_SNAPSHOT) {
                vm.updateFromIntent(intent)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_home, container, false)
        bind(v)
        setupMap()
        setupButtons()
        observeVm()
        return v
    }

    override fun onStart() {
        super.onStart()
        cfg = Prefs.load(requireContext())
        requireContext().startForegroundService(Intent(requireContext(), LocationService::class.java))
        requireActivity().registerReceiver(snapshotReceiver, IntentFilter(LocationService.ACTION_SNAPSHOT))
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unregisterReceiver(snapshotReceiver)
    }

    private fun bind(v: View) {
        mapView = v.findViewById(R.id.mapView)
        btnMain = v.findViewById(R.id.btnMain)
        btnCenter = v.findViewById(R.id.btnCenter)
        btnEmergency = v.findViewById(R.id.btnEmergency)
        btnLock = v.findViewById(R.id.btnLock)
        tvEstado = v.findViewById(R.id.tvEstadoChip)
        tvKmHoy = v.findViewById(R.id.tvKmHoy)
        tvGananciaHoy = v.findViewById(R.id.tvGananciaHoy)
        tvVelocidad = v.findViewById(R.id.tvVelocidadMapa)
        tvMeta = v.findViewById(R.id.tvMeta)
        progressMeta = v.findViewById(R.id.progressMeta)
        panel = v.findViewById(R.id.panelCarreraActiva)
        tvDist = v.findViewById(R.id.tvPanelDistancia)
        tvTiempo = v.findViewById(R.id.tvPanelTiempo)
        tvMonto = v.findViewById(R.id.tvPanelMonto)
        tvVelPanel = v.findViewById(R.id.tvPanelVelocidad)
        tvKmCon = v.findViewById(R.id.tvKmConPasajero)
        tvKmSin = v.findViewById(R.id.tvKmSinPasajero)
        tvTimeTot = v.findViewById(R.id.tvTiempoTotal)
        tvTimeCon = v.findViewById(R.id.tvTiempoCon)
        tvTimeSin = v.findViewById(R.id.tvTiempoSin)
        tvGas = v.findViewById(R.id.tvGasolina)
    }

    private fun setupMap() {
        org.osmdroid.config.Configuration.getInstance().userAgentValue = "TAXControl"
        mapView.setMultiTouchControls(true)
        polyLibre = Polyline().apply {
            outlinePaint.color = android.graphics.Color.GRAY
            outlinePaint.strokeWidth = 4f
            outlinePaint.alpha = 120
        }
        polyPas = Polyline().apply {
            outlinePaint.color = android.graphics.Color.GREEN
            outlinePaint.strokeWidth = 7f
        }
        mapView.overlays.add(polyLibre)
        mapView.overlays.add(polyPas)

        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), mapView).apply {
            enableMyLocation()
            enableFollowLocation()
            runOnFirstFix {
                val geo = myLocation
                if (geo != null && !firstFix) {
                    mapView.post {
                        firstFix = true
                        lastGeo = geo
                        mapView.controller.setZoom(17.0)
                        mapView.controller.setCenter(geo)
                    }
                }
            }
        }
        mapView.overlays.add(myLocationOverlay)
    }

    private fun setupButtons() {
        btnMain.setOnClickListener {
            if (locked) {
                Toast.makeText(requireContext(), "Pantalla bloqueada", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            when (tripManager.tripState()) {
                TripManager.TripState.CON_PASAJERO, TripManager.TripState.ESPERANDO -> {
                    requireContext().startService(Intent(requireContext(), LocationService::class.java).setAction(LocationService.ACTION_FINALIZE_TRIP))
                }
                else -> {
                    lifecycleScope.launch {
                        val profile = withContext(Dispatchers.IO) {
                            com.example.carrerastaxi.domain.usecases.GetActiveProfileUseCase(
                                com.example.carrerastaxi.data.repository.VehicleProfileRepositoryImpl(com.example.carrerastaxi.data.AppDatabase.getDatabase(requireContext()))
                            ).invoke()
                        }
                        profile?.let { tripManager.setProfile(it) } ?: tripManager.setTariffs(cfg.base, cfg.km, cfg.min)
                        tripManager.startTrip(System.currentTimeMillis())
                        vibrate(true)
                        updateButtonLabel(tripManager.tripState())
                    }
                }
            }
        }
        btnCenter.setOnClickListener { lastGeo?.let { mapView.controller.setCenter(it) } }
        btnEmergency.setOnClickListener { vibrate(false); Toast.makeText(requireContext(), "SOS enviado", Toast.LENGTH_SHORT).show() }
        btnLock.setOnClickListener {
            locked = !locked
            btnLock.isSelected = locked
            btnLock.background = ContextCompat.getDrawable(requireContext(), if (locked) R.drawable.btn_neon_red else R.drawable.btn_neon_blue)
        }
    }

    private fun observeVm() {
        vm.state.observe(viewLifecycleOwner, Observer { snap ->
            tvKmHoy.text = "KM total: ${"%.1f".format(snap.km)}"
            tvGananciaHoy.text = "Ganancia: ${"%.0f".format(snap.gross)} Bs"
            tvVelocidad.text = "Velocidad: ${"%.0f".format(DistanceUtils.convertSpeedMsToKmh(snap.speedMs))} km/h"
            val metaPct = if (cfg.goal <= 0) 0 else ((snap.gross / cfg.goal) * 100).toInt().coerceIn(0, 100)
            progressMeta.progress = metaPct
            tvMeta.text = "Meta: ${"%.0f".format(snap.gross)} / ${"%.0f".format(cfg.goal)} Bs (${metaPct}%)"
            tvKmCon.text = "KM con pasajero: ${"%.1f".format(snap.kmWith)}"
            tvKmSin.text = "KM sin pasajero: ${"%.1f".format(snap.kmWithout)}"
            tvTimeTot.text = "Tiempo total: ${DistanceUtils.formatTime(snap.timeTotal)}"
            tvTimeCon.text = "Tiempo con pasajero: ${DistanceUtils.formatTime(snap.timeWith)}"
            tvTimeSin.text = "Tiempo libre: ${DistanceUtils.formatTime(snap.timeWithout)}"
            val (_, fuelCost) = com.example.carrerastaxi.managers.FuelManager(cfg.kmPerL, cfg.gas).fuelCost(snap.km)
            tvGas.text = "Gasolina estimada: ${"%.1f".format(fuelCost)} Bs"
            updateStateChip(snap.state)
            updateButtonLabel(TripManager.TripState.valueOf(snap.state))
            if (snap.lat != 0.0 || snap.lon != 0.0) {
                lastGeo = GeoPoint(snap.lat, snap.lon)
                mapView.controller.setCenter(lastGeo)
                if (TripManager.TripState.valueOf(snap.state) == TripManager.TripState.CON_PASAJERO) {
                    polyPas?.addPoint(lastGeo)
                } else {
                    polyLibre?.addPoint(lastGeo)
                }
                mapView.invalidate()
            }
        })
    }

    private fun updateStateChip(state: String) {
        tvEstado.text = state
        val bg = when (state) {
            "CON_PASAJERO" -> R.drawable.tag_state_green
            "ESPERANDO" -> R.drawable.tag_state_yellow
            "PAUSADO" -> R.drawable.tag_state_yellow
            else -> R.drawable.tag_state_blue
        }
        tvEstado.background = ContextCompat.getDrawable(requireContext(), bg)
    }

    private fun updateButtonLabel(state: TripManager.TripState) {
        if (state == TripManager.TripState.CON_PASAJERO || state == TripManager.TripState.ESPERANDO) {
            btnMain.text = "FINALIZAR CARRERA"
            btnMain.background = ContextCompat.getDrawable(requireContext(), R.drawable.btn_neon_red)
        } else {
            btnMain.text = "INICIAR CARRERA"
            btnMain.background = ContextCompat.getDrawable(requireContext(), R.drawable.btn_neon_green)
        }
    }

    private fun vibrate(short: Boolean) {
        val vib = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (short) vib.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
        else vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0,120,80,160), -1))
    }

    private fun ensurePermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }
    }
}
