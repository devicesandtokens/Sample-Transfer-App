package com.interswitchng.interswitchpos.views.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.interswitchng.interswitchpos.R
import com.interswitchng.interswitchpos.utils.hide
import com.interswitchng.smartpos.shared.utilities.reveal

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        NavigationUI.setupWithNavController(bottomNav, navHostFragment!!.findNavController())

        findNavController(R.id.nav_host_fragment).addOnDestinationChangedListener { controller, destination, arguments ->
            if (destination.id == R.id.transactionReceiptFragment) {
                bottomNav.hide()
            } else {
                bottomNav.reveal()
            }
        }
    }
}