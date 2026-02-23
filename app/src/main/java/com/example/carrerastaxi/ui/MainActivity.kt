package com.example.carrerastaxi.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.carrerastaxi.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.Fragment
import com.example.carrerastaxi.ui.fragments.HomeFragment
import com.example.carrerastaxi.ui.fragments.StatsFragment
import com.example.carrerastaxi.ui.fragments.HistorialFragment
import com.example.carrerastaxi.ui.fragments.ConfigFragment
import android.content.Intent
import com.example.carrerastaxi.service.LocationService

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Ensure foreground service stays even if all fragments destroyed
        startService(Intent(this, LocationService::class.java))
        val nav = findViewById<BottomNavigationView>(R.id.bottomNav)
        nav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> switchTo(HomeFragment())
                R.id.nav_stats -> switchTo(StatsFragment())
                R.id.nav_history -> switchTo(HistorialFragment())
                R.id.nav_settings -> switchTo(ConfigFragment())
            }
            true
        }
        if (savedInstanceState == null) {
            nav.selectedItemId = R.id.nav_home
        }
    }

    private fun switchTo(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
