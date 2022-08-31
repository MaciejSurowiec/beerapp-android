package com.beerup.beerapp

import android.content.SharedPreferences
import android.os.*
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.beerup.beerapp.ViewModels.LoggedViewModel
import com.beerup.beerapp.ViewModels.SharedViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONObject
import org.json.JSONArray
import java.util.ArrayList

class MainActivity : AppCompatActivity() {
    public var bottomNavigation : BottomNavigationView? = null
    private lateinit var sharedPref: SharedPreferences

    private lateinit var viewModel: SharedViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        viewModel = ViewModelProvider(this).get(SharedViewModel::class.java)
        viewModel.baseUrl = getString(R.string.baseUrl)
        sharedPref = getSharedPreferences("userInfo", MODE_PRIVATE)

        setContentView(R.layout.activity_main)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        bottomNavigation = findViewById(R.id.bottom_navigation)

        bottomNavigation?.setupWithNavController(navController)

    }

    override fun onBackPressed() {
        if(viewModel.backButtonEnd) {
            finish()
        }

        if(viewModel.enableBackPress) {
            super.onBackPressed()
        }
    }

    fun logout() { // this need to go to profileviewModel
        val editor = sharedPref.edit()

        if (sharedPref.contains("userLogin")) {
            editor.remove("userLogin")
            editor.apply()
        }

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val action = LoggedFragmentDirections.actionLoggedFragmentToUnloggedFragment()
        navController.navigate(action)
    }
}