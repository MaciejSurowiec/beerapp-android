package com.example.beerapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.*
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import okhttp3.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater?.inflate(R.menu.popup_menu, menu)
        return true
    }

    fun logout() {
        val sharedPref = getSharedPreferences("userInfo", MODE_PRIVATE)
        val editor = sharedPref.edit()

        if (sharedPref.contains("userLogin")) {
            editor.remove("userLogin")
            editor.apply()
        }

        val navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val action = LoggedFragmentDirections.actionLoggedFragmentToUnloggedFragment()
        navController.navigate(action)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val sharedPref = getSharedPreferences("userInfo", AppCompatActivity.MODE_PRIVATE)


        when (item.itemId) {
            R.id.beerlist -> {
                val action = LoggedFragmentDirections.actionLoggedFragmentToBeerListFragment(sharedPref.getString("userLogin",null).toString())
                navController.navigate(action)
            }
            R.id.logout -> {
                logout()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}