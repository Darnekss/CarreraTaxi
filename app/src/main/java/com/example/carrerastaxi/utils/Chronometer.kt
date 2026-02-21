package com.example.carrerastaxi.utils

import java.util.concurrent.TimeUnit

class Chronometer {
    private var startTime: Long = 0
    private var pausedTime: Long = 0
    var isRunning: Boolean = false
        private set

    fun start() {
        startTime = System.currentTimeMillis()
        isRunning = true
    }

    fun pause() {
        if (isRunning) {
            pausedTime = System.currentTimeMillis()
            isRunning = false
        }
    }

    fun resume() {
        if (!isRunning) {
            val elapsedPausedTime = System.currentTimeMillis() - pausedTime
            startTime += elapsedPausedTime
            isRunning = true
        }
    }

    fun stop() {
        isRunning = false
        startTime = 0
        pausedTime = 0
    }

    fun getElapsedTime(): Long {
        return if (isRunning) {
            System.currentTimeMillis() - startTime
        } else {
            pausedTime - startTime
        }
    }

    fun getFormattedTime(): String {
        val millis = getElapsedTime()
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
