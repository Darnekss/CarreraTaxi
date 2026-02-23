package com.example.carrerastaxi.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.carrerastaxi.R
import com.example.carrerastaxi.ui.StatsActivity

class StatsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_stats, container, false)
        v.findViewById<Button>(R.id.btnOpenStats)?.setOnClickListener {
            startActivity(Intent(requireContext(), StatsActivity::class.java))
        }
        return v
    }
}
