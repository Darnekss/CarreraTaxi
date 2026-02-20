package com.example.carrerastaxi.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.carrerastaxi.R
import com.example.carrerastaxi.data.AppDatabase
import com.example.carrerastaxi.data.CarreraEntity
import com.example.carrerastaxi.service.TaxiMeterService
import com.example.carrerastaxi.utils.DistanceUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import java.util.Timer
import java.util.TimerTask

/**
 * Actividad principal - Pantalla del taxímetro en vivo
 */
class MainActivity : AppCompatActivity() {

    // Componentes UI
    private lateinit var mapView: MapView
    private lateinit var tvTiempo: TextView
    private lateinit var tvVelocidad: TextView
    private lateinit var tvDistancia: TextView
    private lateinit var tvGasolina: TextView
    private lateinit var tvGanancia: TextView
    private lateinit var etMonto: EditText
    private lateinit var btnIniciar: Button
    private lateinit var btnPausar: Button
    private lateinit var btnFinalizar: Button
    private lateinit var btnHistorial: Button

    // Variables de ubicación
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var mapLibreMap: MapLibreMap? = null
    private var lastLocation: Location? = null
    private var totalDistanceMeters = 0.0
    private var totalSpeedSum = 0.0
    private var speedMeasureCount = 0
    private val pathPoints = mutableListOf<LatLng>()

    // Variables de tiempo
    private var elapsedSeconds = 0L
    private var timerTask: TimerTask? = null
    private var timer: Timer? = null
    private var startTime = 0L

    // Variables de estado
    private var isRunning = false
    private var isPaused = false

    // Servicio
    private var taxiMeterService: TaxiMeterService? = null
    private var serviceConnected = false
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TaxiMeterService.LocalBinder
            taxiMeterService = binder.getService()
            serviceConnected = true
            taxiMeterService?.setLocationUpdateCallback { location ->
                onLocationUpdate(location)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            serviceConnected = false
            taxiMeterService = null
        }
    }

    // Base de datos
    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar DB
        database = AppDatabase.Companion.getDatabase(this)

        // Inicializar vistas
        initializeViews()

        // Inicializar ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Solicitar permisos
        if (!hasLocationPermission()) {
            requestLocationPermission()
        }

        // Inicializar mapa
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            mapLibreMap = map
            map.setStyle(Style.MAPBOX_STREETS) { style ->
                addMapLayers(style)
            }
        }

        // Listeners de botones
        btnIniciar.setOnClickListener { iniciarCarrera() }
        btnPausar.setOnClickListener { pausarCarrera() }
        btnFinalizar.setOnClickListener { finalizarCarrera() }
        btnHistorial.setOnClickListener {
            startActivity(Intent(this, HistorialActivity::class.java))
        }

        // Listener para calcular ganancia en vivo
        etMonto.setOnFocusChangeListener { _, _ -> actualizarGanancia() }
    }

    /**
     * Inicializa referencias a las vistas
     */
    private fun initializeViews() {
        mapView = findViewById(R.id.mapView)
        tvTiempo = findViewById(R.id.tvTiempo)
        tvVelocidad = findViewById(R.id.tvVelocidad)
        tvDistancia = findViewById(R.id.tvDistancia)
        tvGasolina = findViewById(R.id.tvGasolina)
        tvGanancia = findViewById(R.id.tvGanancia)
        etMonto = findViewById(R.id.etMonto)
        btnIniciar = findViewById(R.id.btnIniciar)
        btnPausar = findViewById(R.id.btnPausar)
        btnFinalizar = findViewById(R.id.btnFinalizar)
        btnHistorial = findViewById(R.id.btnHistorial)
    }

    /**
     * Verifica si tiene permiso de ubicación
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Solicita permisos de ubicación
     */
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                } else {
                    Manifest.permission.INTERNET
                }
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Inicia una nueva carrera
     */
    private fun iniciarCarrera() {
        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        isRunning = true
        isPaused = false
        elapsedSeconds = 0L
        totalDistanceMeters = 0.0
        pathPoints.clear()
        startTime = System.currentTimeMillis()

        // Actualizar UI
        btnIniciar.visibility = Button.GONE
        btnPausar.visibility = Button.VISIBLE
        btnFinalizar.visibility = Button.VISIBLE
        etMonto.isEnabled = true

        // Iniciar servicio de ubicación
        val serviceIntent = Intent(this, TaxiMeterService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE)

        // Iniciar contador de tiempo
        startTimer()

        Toast.makeText(this, "✅ Carrera iniciada", Toast.LENGTH_SHORT).show()
    }

    /**
     * Pausa la carrera actual
     */
    private fun pausarCarrera() {
        isPaused = true
        isRunning = false

        // Detener updating
        taxiMeterService?.stopLocationUpdates()
        timerTask?.cancel()

        // Actualizar UI
        btnPausar.text = "▶ REANUDAR"
        btnPausar.setOnClickListener { reanudarCarrera() }

        Toast.makeText(this, "⏸ Carrera pausada", Toast.LENGTH_SHORT).show()
    }

    /**
     * Reanuda una carrera pausada
     */
    private fun reanudarCarrera() {
        isPaused = false
        isRunning = true

        btnPausar.text = "⏸ PAUSAR"
        btnPausar.setOnClickListener { pausarCarrera() }

        // Reiniciar ubicación
        val serviceIntent = Intent(this, TaxiMeterService::class.java)
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE)

        // Reiniciar timer
        startTimer()

        Toast.makeText(this, "▶ Carrera reanudada", Toast.LENGTH_SHORT).show()
    }

    /**
     * Finaliza la carrera y guarda en BD
     */
    private fun finalizarCarrera() {
        isRunning = false

        // Detener servicio
        taxiMeterService?.stopLocationUpdates()
        if (serviceConnected) {
            unbindService(serviceConnection)
            serviceConnected = false
        }
        timerTask?.cancel()

        // Actualizar UI
        btnIniciar.visibility = Button.VISIBLE
        btnPausar.visibility = Button.GONE
        btnFinalizar.visibility = Button.GONE

        // Obtener datos
        val distanciaKm = DistanceUtils.metersToKilometers(totalDistanceMeters)
        val gasolinaConsumida = DistanceUtils.calculateGasolineConsumption(distanciaKm)
        val montoCobrado = try {
            etMonto.text.toString().toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
        val ganancia = DistanceUtils.calculateProfit(montoCobrado, gasolinaConsumida)

        // Guardar en base de datos
        lifecycleScope.launch {
            val carrera = CarreraEntity(
                fecha = DistanceUtils.getTodayDateISO(),
                horaInicio = DistanceUtils.formatTimeOfDay(startTime),
                horaFin = DistanceUtils.getCurrentTimeOfDay(),
                duracionSegundos = elapsedSeconds,
                distanciaKm = distanciaKm,
                gasolinaConsumida = gasolinaConsumida,
                montoCobrado = montoCobrado,
                ganancia = ganancia,
                formaPago = "Efectivo", // Esto se puede mejorar con un selector
                puntosGPS = DistanceUtils.latLngListToString(pathPoints),
                velocidadPromedio = if (speedMeasureCount > 0) {
                    totalSpeedSum / speedMeasureCount
                } else {
                    0.0
                }
            )

            database.carreraDao().insertCarrera(carrera)
        }

        // Limpiar UI
        tvTiempo.text = "00:00:00"
        tvVelocidad.text = "0 km/h"
        tvDistancia.text = "0.00 km"
        tvGasolina.text = "0.00 Bs"
        tvGanancia.text = "0.00 Bs"
        etMonto.text.clear()
        etMonto.isEnabled = false
        pathPoints.clear()

        Toast.makeText(this, "✅ Carrera guardada correctamente", Toast.LENGTH_SHORT).show()
    }

    /**
     * Inicia el contador de tiempo
     */
    private fun startTimer() {
        if (timerTask != null) timerTask?.cancel()
        if (timer != null) timer?.cancel()

        timer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                if (isRunning && !isPaused) {
                    elapsedSeconds++
                    runOnUiThread {
                        tvTiempo.text = DistanceUtils.formatTime(elapsedSeconds)
                    }
                }
            }
        }
        timer?.scheduleAtFixedRate(timerTask!!, 1000, 1000)
    }

    /**
     * Manejador de actualizaciones de ubicación
     */
    private fun onLocationUpdate(location: Location) {
        if (!isRunning || isPaused) return

        // Calcular distancia desde última ubicación
        if (lastLocation != null) {
            val distanceDelta = lastLocation!!.distanceTo(location)
            totalDistanceMeters += distanceDelta
        }

        // Calcular velocidad actual
        val speedKmh = DistanceUtils.convertSpeedMsToKmh(location.speed)
        totalSpeedSum += speedKmh
        speedMeasureCount++

        // Agregar punto al recorrido
        val newPoint = LatLng(location.latitude, location.longitude)
        pathPoints.add(newPoint)

        // Actualizar mapa
        updateMap(newPoint)

        // Actualizar UI
        runOnUiThread {
            val distanciaKm = DistanceUtils.metersToKilometers(totalDistanceMeters)
            tvDistancia.text = DistanceUtils.formatDistance(distanciaKm)
            tvVelocidad.text = DistanceUtils.formatSpeed(speedKmh)

            val gasolinaConsumida = DistanceUtils.calculateGasolineConsumption(distanciaKm)
            tvGasolina.text = DistanceUtils.formatMoney(gasolinaConsumida)

            actualizarGanancia()
        }

        lastLocation = location
    }

    /**
     * Actualiza el mapa con la nueva ubicación
     */
    private fun updateMap(newPoint: LatLng) {
        mapLibreMap?.let { map ->
            // Actualizar posición de la cámara
            map.cameraPosition = com.google.android.gms.maps.model.CameraPosition.Builder()
                .target(com.google.android.gms.maps.model.LatLng(newPoint.latitude, newPoint.longitude))
                .build()

            // Actualizar polyline
            val geoJsonSource = map.getStyle()?.getSourceAs<GeoJsonSource>("route-source")
            if (geoJsonSource != null) {
                val featureCollection = com.mapbox.geojson.FeatureCollection.fromFeatures(
                    listOf(
                        com.mapbox.geojson.Feature.fromGeometry(
                            com.mapbox.geojson.LineString.fromLngLats(
                                pathPoints.map { com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude) }
                            )
                        )
                    )
                )
                geoJsonSource.setGeoJson(featureCollection)
            }
        }
    }

    /**
     * Agrega capas al mapa (routes y markers)
     */
    private fun addMapLayers(style: Style) {
        // Crear source para la ruta
        val routeSource = GeoJsonSource("route-source")
        style.addSource(routeSource)

        // Agregar LineLayer para la ruta (polyline)
        val lineLayer = LineLayer("route-line", "route-source")
            .withProperties(
                Property.LINE_COLOR(Color.BLUE),
                Property.LINE_WIDTH(8f),
                Property.LINE_OPACITY(0.8f)
            )
        style.addLayer(lineLayer)

        // Crear marker source
        val markerSource = GeoJsonSource("marker-source")
        style.addSource(markerSource)

        // Agregar SymbolLayer para el marcador
        val markerLayer = SymbolLayer("marker-layer", "marker-source")
        style.addLayer(markerLayer)
    }

    /**
     * Actualiza el valor de ganancia neta
     */
    private fun actualizarGanancia() {
        val distanciaKm = try {
            tvDistancia.text.toString().replace(" km", "").toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }

        val gasolinaConsumida = DistanceUtils.calculateGasolineConsumption(distanciaKm)
        val montoCobrado = try {
            etMonto.text.toString().toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }

        val ganancia = DistanceUtils.calculateProfit(montoCobrado, gasolinaConsumida)
        tvGanancia.text = DistanceUtils.formatMoney(ganancia)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "✅ Permiso de ubicación concedido", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "❌ Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
        if (serviceConnected) {
            unbindService(serviceConnection)
        }
        timerTask?.cancel()
        timer?.cancel()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }
}