package com.example.carrerastaxi.managers

/**
 * Mantiene instancias compartidas para que el estado de carrera no se pierda
 * al recrear fragments/actividades.
 */
object AppSession {
    val tripManager: TripManager = TripManager()
}
