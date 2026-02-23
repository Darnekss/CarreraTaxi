package com.example.carrerastaxi.ui.fragments

import android.Manifest
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.carrerastaxi.R
import com.example.carrerastaxi.data.AppDatabase
import com.example.carrerastaxi.data.TrafficEntity
import com.example.carrerastaxi.managers.FuelManager
import com.example.carrerastaxi.managers.TripManager
import com.example.carrerastaxi.models.LocationPoint
import com.example.carrerastaxi.service.LocationService
import com.example.carrerastaxi.utils.DistanceUtils
import com.example.carrerastaxi.utils.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class HomeFragment : Fragment() {

    private lateinit var btnMain: Button
    private lateinit var tvHeader: TextView
    private lateinit var tvEstadoChip: TextView
    private lateinit var tvKmHoy: TextView
    private lateinit var tvGananciaHoy: TextView
    private lateinit var panelCarrera: View
    private lateinit var tvDist: TextView
    private lateinit var tvTime: TextView
    private lateinit var tvMonto: TextView
    private lateinit var tvVel: TextView
    private lateinit var tvVelMapa: TextView
    private lateinit var tvKmCon: TextView
    private lateinit var tvKmSin: TextView
    private lateinit var tvTimeCon: TextView
    private lateinit var tvTimeSin: TextView
    private lateinit var tvTimeTot: TextView
    private lateinit var tvGas: TextView
    private lateinit var tvMeta: TextView
    private lateinit var progressMeta: android.widget.ProgressBar
    private lateinit var mapView: MapView
    private lateinit var btnCenter: View
    private lateinit var btnEmergency: View
    private lateinit var btnLock: View

    private val tripManager = com.example.carrerastaxi.managers.AppSession.tripManager
    private val fuelManager = FuelManager()
    private var polylineLibre: Polyline? = null
    private var polylinePasajero: Polyline? = null
    private var startMarker: Marker? = null
    private var endMarker: Marker? = null
    private var lastGeo: GeoPoint? = null
    private var autoFollow = true
    private var firstFixDone = false
    private var myLocationOverlay: MyLocationNewOverlay? = null
    private var cfg = Prefs.Config(7.0,5.0,1.0,7.0,10.0,true,true,false,300.0,true)
    private val db by lazy { AppDatabase.getDatabase(requireContext()) }
    private var locked = false
    private val trafficMarkers = mutableListOf<Marker>()

    private var locationService: LocationService? = null
    private var serviceBound = false
    private var autosaveJob: Job? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val b = binder as LocationService.LocalBinder
            locationService = b.getService()
            locationService?.setListener { onLocationPoint(it) }
            locationService?.startUpdates()
            serviceBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            serviceBound = false
            locationService = null
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_home, container, false)
        bindViews(v)
        setupMap()
        setupButtons()
        loadConfig()
        return v
    }

    override fun onStart() {
        super.onStart()
        loadConfig()
        if (cfg.journeyActive && !tripManager.journeyActive()) {
            tripManager.startJourney(System.currentTimeMillis())
        }
        ensureGpsRunning()
        startAutosave()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        autosaveJob?.cancel()
        if (serviceBound) requireContext().unbindService(serviceConnection)
    }

    private fun bindViews(v: View) {
        btnMain = v.findViewById(R.id.btnMain)
        tvHeader = v.findViewById(R.id.tvHeader)
        tvEstadoChip = v.findViewById(R.id.tvEstadoChip)
        tvKmHoy = v.findViewById(R.id.tvKmHoy)
        tvGananciaHoy = v.findViewById(R.id.tvGananciaHoy)
        panelCarrera = v.findViewById(R.id.panelCarreraActiva)
        tvDist = v.findViewById(R.id.tvPanelDistancia)
        tvTime = v.findViewById(R.id.tvPanelTiempo)
        tvMonto = v.findViewById(R.id.tvPanelMonto)
        tvVel = v.findViewById(R.id.tvPanelVelocidad)
        tvVelMapa = v.findViewById(R.id.tvVelocidadMapa)
        tvKmCon = v.findViewById(R.id.tvKmConPasajero)
        tvKmSin = v.findViewById(R.id.tvKmSinPasajero)
        tvTimeCon = v.findViewById(R.id.tvTiempoCon)
        tvTimeSin = v.findViewById(R.id.tvTiempoSin)
        tvTimeTot = v.findViewById(R.id.tvTiempoTotal)
        tvGas = v.findViewById(R.id.tvGasolina)
        tvMeta = v.findViewById(R.id.tvMeta)
        progressMeta = v.findViewById(R.id.progressMeta)
        mapView = v.findViewById(R.id.mapView)
        btnCenter = v.findViewById(R.id.btnCenter)
        btnEmergency = v.findViewById(R.id.btnEmergency)
        btnLock = v.findViewById(R.id.btnLock)
    }

    private fun setupMap() {
        org.osmdroid.config.Configuration.getInstance().userAgentValue = "TAXControl"
        mapView.setMultiTouchControls(true)
        polylineLibre = Polyline().apply {
            outlinePaint.color = android.graphics.Color.GRAY
            outlinePaint.strokeWidth = 4f
            outlinePaint.alpha = 120
        }
        polylinePasajero = Polyline().apply {
            outlinePaint.color = android.graphics.Color.GREEN
            outlinePaint.strokeWidth = 7f
        }
        mapView.overlays.add(polylineLibre)
        mapView.overlays.add(polylinePasajero)

        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(requireContext()), mapView).apply {
            enableMyLocation()
            enableFollowLocation()
            runOnFirstFix {
                val geo = myLocation
                if (geo != null && !firstFixDone) {
                    mapView.post {
                        firstFixDone = true
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
        btnMain.setOnClickListener { handleButtonClick() }
        btnMain.setOnLongClickListener { handleLongPress() }
        btnCenter.setOnClickListener {
            autoFollow = true
            lastGeo?.let { mapView.controller.setCenter(it) }
        }
        btnEmergency.setOnClickListener { handleEmergency() }
        btnLock.setOnClickListener { toggleLock() }
    }

    private fun onLocationPoint(pt: LocationPoint) {
        val point = GeoPoint(pt.latitude, pt.longitude)
        lastGeo = point
        if (!firstFixDone) {
            firstFixDone = true
            mapView.controller.setZoom(17.0)
        }
        val state = tripManager.tripState()
        if (state == TripManager.TripState.CON_PASAJERO || state == TripManager.TripState.ESPERANDO) {
            polylinePasajero?.addPoint(point)
        } else {
            polylineLibre?.addPoint(point)
        }
        if (cfg.autoStart && state == TripManager.TripState.LIBRE && pt.speedMs > 0.8f) {
            tripManager.setTariffs(cfg.base, cfg.km, cfg.min)
            tripManager.startTrip(System.currentTimeMillis())
            placeStartMarker()
            vibrate(true)
            updateStateChip()
            updateButtonLabel()
        }
        if (autoFollow && mapView.controller != null) {
            mapView.controller.setCenter(point)
        }
        mapView.invalidate()
        tripManager.onLocation(pt)
        val speedKmh = DistanceUtils.convertSpeedMsToKmh(pt.speedMs)
        if (speedKmh < 10) saveTrafficPoint(point, speedKmh)
        updateUi(speedKmh)
        updateStateChip()
    }

    private fun updateUi(speedKmh: Double) {
        val snap = tripManager.snapshot(System.currentTimeMillis())
        val (_, fuelCost) = fuelManager.fuelCost(snap.totalKm)
        val net = snap.gross - fuelCost
        tvKmHoy.text = "KM total: ${"%.1f".format(snap.totalKm)}"
        tvGananciaHoy.text = "Ganancia: ${"%.0f".format(net)} Bs"
        tvVelMapa.text = "Velocidad: ${"%.0f".format(speedKmh)} km/h"
        tvKmCon.text = "KM con pasajero: ${"%.1f".format(snap.kmWithPassenger)}"
        tvKmSin.text = "KM sin pasajero: ${"%.1f".format(snap.kmWithoutPassenger)}"
        tvTimeTot.text = "Tiempo total: ${DistanceUtils.formatTime(snap.timeTotal)}"
        tvTimeCon.text = "Tiempo con pasajero: ${DistanceUtils.formatTime(snap.timeWithPassenger)}"
        tvTimeSin.text = "Tiempo libre: ${DistanceUtils.formatTime(snap.timeWithoutPassenger)}"
        tvGas.text = "Gasolina estimada: ${"%.1f".format(fuelCost)} Bs"
        val metaPct = if (cfg.goal <= 0) 0 else ((net / cfg.goal) * 100).toInt().coerceIn(0, 100)
        progressMeta.progress = metaPct
        tvMeta.text = "Meta: ${"%.0f".format(net)} / ${"%.0f".format(cfg.goal)} Bs (${metaPct}%)"

        val showPanel = tripManager.tripState() == TripManager.TripState.CON_PASAJERO ||
                tripManager.tripState() == TripManager.TripState.ESPERANDO
        panelCarrera.visibility = if (showPanel) View.VISIBLE else View.GONE
        if (showPanel) {
            tvDist.text = "Distancia: ${"%.1f".format(tripManager.currentTripMeters()/1000.0)} km"
            tvTime.text = "Tiempo: ${DistanceUtils.formatTime(tripManager.currentTripSeconds())}"
            tvMonto.text = "Monto: ${"%.0f".format(tripManager.currentTripEarnings())} Bs"
            tvVel.text = "Velocidad: ${"%.0f".format(speedKmh)} km/h"
        }
        updateButtonLabel()
    }

    private fun vibrate(short: Boolean = true) {
        if (!cfg.vibra) return
        val vib = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (short) {
            vib.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 120, 80, 160), -1))
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureGpsRunning() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }
        val ctx = requireContext()
        ctx.startService(Intent(ctx, LocationService::class.java))
        ctx.bindService(Intent(ctx, LocationService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            ensureGpsRunning()
        }
    }

    private fun handleButtonClick() {
        if (locked) {
            Toast.makeText(requireContext(), "Pantalla bloqueada", Toast.LENGTH_SHORT).show()
            return
        }
        when (tripManager.tripState()) {
            TripManager.TripState.CON_PASAJERO, TripManager.TripState.ESPERANDO -> {
                Toast.makeText(requireContext(), "Mantén 2s para finalizar", Toast.LENGTH_SHORT).show()
            }
            else -> {
                tripManager.setTariffs(cfg.base, cfg.km, cfg.min)
                tripManager.startTrip(System.currentTimeMillis())
                placeStartMarker()
                vibrate(true)
                updateStateChip()
                updateButtonLabel()
            }
        }
    }

    private fun handleLongPress(): Boolean {
        if (locked) return true
        if (tripManager.tripState() == TripManager.TripState.CON_PASAJERO ||
            tripManager.tripState() == TripManager.TripState.ESPERANDO) {
            val summary = tripManager.finishTrip()
            placeEndMarker()
            showSummaryDialog(summary)
            vibrate(false)
            panelCarrera.visibility = View.GONE
            updateStateChip()
            updateButtonLabel()
            return true
        }
        return false
    }

    private fun updateButtonLabel() {
        when (tripManager.tripState()) {
            TripManager.TripState.CON_PASAJERO, TripManager.TripState.ESPERANDO -> {
                btnMain.text = "FINALIZAR CARRERA"
                btnMain.background = ContextCompat.getDrawable(requireContext(), R.drawable.btn_neon_red)
            }
            else -> {
                btnMain.text = "INICIAR CARRERA"
                btnMain.background = ContextCompat.getDrawable(requireContext(), R.drawable.btn_neon_green)
            }
        }
    }

    private fun updateStateChip() {
        val state = tripManager.tripState()
        when (state) {
            TripManager.TripState.CON_PASAJERO -> {
                tvEstadoChip.text = "CON PASAJERO"
                tvEstadoChip.background = ContextCompat.getDrawable(requireContext(), R.drawable.tag_state_green)
            }
            TripManager.TripState.LIBRE -> {
                tvEstadoChip.text = "LIBRE"
                tvEstadoChip.background = ContextCompat.getDrawable(requireContext(), R.drawable.tag_state_blue)
            }
            TripManager.TripState.PAUSADO, TripManager.TripState.ESPERANDO -> {
                tvEstadoChip.text = "ESPERANDO"
                tvEstadoChip.background = ContextCompat.getDrawable(requireContext(), R.drawable.tag_state_yellow)
            }
            TripManager.TripState.FINALIZADO -> {
                tvEstadoChip.text = "FINALIZADO"
                tvEstadoChip.background = ContextCompat.getDrawable(requireContext(), R.drawable.tag_state_red)
            }
        }
    }

    private fun placeStartMarker() {
        val geo = lastGeo ?: return
        if (startMarker == null) {
            startMarker = Marker(mapView)
            startMarker?.icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.presence_online)
            startMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(startMarker)
        }
        startMarker?.position = geo
    }

    private fun placeEndMarker() {
        val geo = lastGeo ?: return
        if (endMarker == null) {
            endMarker = Marker(mapView)
            endMarker?.icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.presence_busy)
            endMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            mapView.overlays.add(endMarker)
        }
        endMarker?.position = geo
    }

    private fun startAutosave() {
        val dao = AppDatabase.getDatabase(requireContext()).dailyStatsDao()
        autosaveJob?.cancel()
        autosaveJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                val snap = tripManager.snapshot(System.currentTimeMillis())
                val (_, fuelCost) = fuelManager.fuelCost(snap.totalKm)
                withContext(Dispatchers.IO) {
                    dao.upsert(
                        com.example.carrerastaxi.data.DailyStatsEntity(
                            date = DistanceUtils.getTodayDateISO(),
                            totalKm = snap.totalKm,
                            kmWithPassenger = snap.kmWithPassenger,
                            kmWithoutPassenger = snap.kmWithoutPassenger,
                            totalTimeSec = snap.timeTotal,
                            timeWithPassengerSec = snap.timeWithPassenger,
                            timeWithoutPassengerSec = snap.timeWithoutPassenger,
                            grossEarnings = snap.gross,
                            fuelCost = fuelCost,
                            netEarnings = snap.gross - fuelCost
                        )
                    )
                }
                delay(4000)
            }
        }
    }

    private fun loadConfig() {
        cfg = Prefs.load(requireContext())
        fuelManager.updateConfig(cfg.kmPerL, cfg.gas)
    }

    private fun showSummaryDialog(summary: TripManager.TripSummary) {
        val msg = "Distancia: ${"%.1f".format(summary.distanceKm)} km\n" +
                "Tiempo: ${DistanceUtils.formatTime(summary.durationSec)}\n" +
                "Parado: ${DistanceUtils.formatTime(summary.stoppedSec)}\n" +
                "Monto: ${"%.1f".format(summary.earnings)} Bs"
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Carrera finalizada")
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun saveTrafficPoint(point: GeoPoint, speedKmh: Double) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            db.trafficDao().insert(
                TrafficEntity(
                    lat = point.latitude,
                    lon = point.longitude,
                    speedKmh = speedKmh,
                    level = if (speedKmh < 5) "RED" else "YELLOW",
                    timestamp = System.currentTimeMillis()
                )
            )
            val recent = db.trafficDao().trafficSince(System.currentTimeMillis() - 3_600_000)
            withContext(Dispatchers.Main) {
                renderTraffic(recent)
            }
        }
    }

    private fun renderTraffic(list: List<TrafficEntity>) {
        trafficMarkers.forEach { mapView.overlays.remove(it) }
        trafficMarkers.clear()
        list.forEach {
            val m = Marker(mapView)
            m.position = GeoPoint(it.lat, it.lon)
            val color = if (it.level == "RED") android.graphics.Color.RED else android.graphics.Color.YELLOW
            m.icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.presence_online)
            m.icon?.setTint(color)
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            trafficMarkers.add(m)
            mapView.overlays.add(m)
        }
        mapView.invalidate()
    }

    private fun toggleLock() {
        locked = !locked
        val bg = if (locked) R.drawable.btn_neon_red else R.drawable.btn_neon_blue
        btnLock.background = ContextCompat.getDrawable(requireContext(), bg)
        Toast.makeText(requireContext(), if (locked) "Pantalla bloqueada" else "Desbloqueada", Toast.LENGTH_SHORT).show()
    }

    private fun handleEmergency() {
        val geo = lastGeo
        val msg = if (geo != null) "SOS enviado con ubicación" else "SOS enviado"
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            // Simple backup local del último punto
            geo?.let {
                db.trafficDao().insert(
                    TrafficEntity(
                        lat = it.latitude,
                        lon = it.longitude,
                        speedKmh = 0.0,
                        level = "SOS",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        vibrate(false)
    }
}
