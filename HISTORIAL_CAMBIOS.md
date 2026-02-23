# Historial de Cambios - CarrerasTaxi

Fecha: 21/02/2026

## Resumen
Se realizaron mejoras de UI, UX y rendimiento, integracion de perfil y borrado, mejoras de GPS/tiempo en vivo, notificaciones y preparacion para publicacion. Tambien se creo politica de privacidad y repositorio en GitHub Pages.

## Cambios principales (App)
- UI neon y reestructura de pantalla principal.
- Mapa mas grande, boton de centrar ubicacion y modo satelite.
- Tiempo y velocidad en una sola linea para ahorrar espacio.
- Forma de pago y monto en una misma fila.
- Boton de tarifas oculto en la parte superior.
- Boton flotante de IA.
- Panel mini de estado (GPS, bateria, red).
- Eliminacion de controles manuales de +1/+5/+10 km.
- Reloj en tiempo real al abrir la app (sin iniciar carrera).
- Ubicacion en tiempo real al abrir la app (preview sin carrera).
- Vibracion y sonido al iniciar/finalizar.
- Acciones de notificacion (Iniciar/Pausar/Finalizar).
- Perfil del conductor y borrado total local de datos.

## Cambios principales (Historial)
- Filtros por rango, busqueda, estadisticas.
- Boton eliminar historial (borrado local).

## Permisos agregados
- ACCESS_BACKGROUND_LOCATION
- POST_NOTIFICATIONS (Android 13+)
- VIBRATE
- ACCESS_NETWORK_STATE

## Politica de privacidad
- Archivo local: `PRIVACY_POLICY.md`
- Publicada en GitHub Pages: `https://darnekss.github.io/carrerastaxi-privacy/`

## Notas
- La notificacion solo aparece cuando se inicia una carrera (servicio en primer plano).
- Para mostrar notificacion sin carrera, se requiere mantener servicio activo siempre (impacto bateria).

## Archivos modificados o nuevos (resumen)
- `app/src/main/java/com/example/carrerastaxi/ui/MainActivity.kt`
- `app/src/main/java/com/example/carrerastaxi/ui/ProfileActivity.kt`
- `app/src/main/java/com/example/carrerastaxi/ui/HistorialActivity.kt`
- `app/src/main/java/com/example/carrerastaxi/service/TaxiMeterService.kt`
- `app/src/main/java/com/example/carrerastaxi/data/CarreraDao.kt`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/layout/activity_profile.xml`
- `app/src/main/res/layout/activity_historial.xml`
- `app/src/main/AndroidManifest.xml`
- `PRIVACY_POLICY.md`
- `HISTORIAL_CAMBIOS.md`

