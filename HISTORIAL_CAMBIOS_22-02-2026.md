# Historial de Cambios - 22/02/2026

Este documento registra en detalle los cambios realizados el 22/02/2026 durante la migración y mejora para convertir la app en un taxímetro GPS profesional.

Resumen corto
- Se añadieron motores, gestores y utilidades para cálculo de tarifa profesional, filtrado GPS, gestión de carrera, estadísticas, almacenamiento/exportación y optimizaciones de mapa/consumo.

Cambios completos

1) Arquitectura y módulos nuevos
- `CalculationEngine`: motor híbrido de tarificación (base, por km, por minuto), modos DETENIDO/TRAFFIC/MOVIMIENTO, cálculo incremental por segundos.
- `GPSManager`: filtrado de puntos GPS inválidos (precisión, saltos imposibles, velocidad máxima), evita sumar distancia en parado.
- `TripManager`: ciclo vida (start/stop/save), integra `CalculationEngine` y persiste carreras en Room, expone getters para UI.
- `StatisticsManager`: métricas diarias/semanales/mensuales, ganancias por km/hora, mejores día/hora/carrera, costos de gasolina y ganancia neta.
- `StorageManager`: export CSV, backup JSON, export PDF básico y subida HTTP multipart a URL configurable.
- `MeterViewModel`: expone `StateFlow` con estado en tiempo real (precio, distancia, tiempo, progreso meta diaria).

2) Modelos y utilidades
- `LocationPoint` (modelo de punto GPS normalizado).
- `GeoUtils` (distancia Haversine y conversión ms→kmh).
- `RouteUtils` (Douglas–Peucker) para simplificar rutas antes de guardar/exportar.
- `MapThrottler` para agrupar y batch de puntos antes de redibujar el mapa.

3) Integración con el servicio y mapa
- `TaxiMeterService`: integrado con `GPSManager` y `TripManager`, expone `setFilteredLocationCallback` con puntos filtrados, y ajusta adaptativamente la frecuencia de `FusedLocation` según velocidad (debounce para evitar cambios frecuentes).
- `MainActivity`: ahora consume puntos filtrados para trazar la polyline de forma eficiente (usa `MapThrottler`), integra `MeterViewModel`, añade botones de emergencia y backup, mantiene pantalla activa durante carrera y bloquea controles para evitar toques accidentales.

4) Exportación y nube
- Backup local JSON y export CSV/PDF.
- Subida HTTP multipart a URL configurable por el usuario (`upload_url`) desde el diálogo de tarifas.
- Botón "Probar subida" para verificar la URL desde el diálogo.

5) Tests
- Pruebas unitarias añadidas para `CalculationEngine` (casos: detenido, movimiento, tráfico) en `app/src/test`.

6) Optimización y calidad
- Agrupamiento de puntos para reducir redraws (`MapThrottler`).
- Simplificación de rutas antes de guardar (reducción de tamaño en BD/export).
- Ajuste adaptativo de frecuencia GPS para ahorrar batería en función de velocidad.

Archivos añadidos (lista breve)
- `app/src/main/java/com/example/carrerastaxi/models/LocationPoint.kt`
- `app/src/main/java/com/example/carrerastaxi/utils/GeoUtils.kt`
- `app/src/main/java/com/example/carrerastaxi/core/CalculationEngine.kt`
- `app/src/main/java/com/example/carrerastaxi/core/GPSManager.kt`
- `app/src/main/java/com/example/carrerastaxi/core/TripManager.kt` (ampliado)
- `app/src/main/java/com/example/carrerastaxi/core/StatisticsManager.kt`
- `app/src/main/java/com/example/carrerastaxi/core/StorageManager.kt`
- `app/src/main/java/com/example/carrerastaxi/ui/MeterViewModel.kt`
- `app/src/main/java/com/example/carrerastaxi/utils/RouteUtils.kt`
- `app/src/main/java/com/example/carrerastaxi/utils/MapThrottler.kt`
- `app/src/test/java/com/example/carrerastaxi/core/CalculationEngineTest.kt`

Archivos modificados (lista breve)
- `app/src/main/java/com/example/carrerastaxi/service/TaxiMeterService.kt`
- `app/src/main/java/com/example/carrerastaxi/ui/MainActivity.kt`
- `app/src/main/res/layout/activity_main.xml` (botones emergencia/backup)
- `app/src/main/res/layout/dialog_tarifas.xml` (campo `upload_url`, botón probar subida)
- `HISTORIAL_CAMBIOS.md` (referencia a este archivo)

Notas finales
- Recomendado: probar en campo para ajustar `epsilon` de simplificación, thresholds de GPS y los tiempos de debounce de reconfiguración del `FusedLocation`.
- Próximos pasos sugeridos: integrar proveedores cloud (OAuth), añadir tests para `GPSManager` y `TripManager`, y configurar CI.

Fecha: 22/02/2026

---

