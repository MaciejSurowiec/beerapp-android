package com.example.beerapp
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    lateinit var welcomeText: TextView
    fun logout()
    {
        val sharedPref = getSharedPreferences("userInfo", MODE_PRIVATE)
        val editor = sharedPref.edit()

        if (sharedPref.contains("userLogin")) {
            editor.remove("userLogin")
        }
        welcomeText.text = ""
    }


    fun refreshMessage() {
        val sharedPref = getSharedPreferences("userInfo", MODE_PRIVATE)
        if (sharedPref.contains("userLogin")) {
            welcomeText.text = "Witaj " + sharedPref.getString("userLogin", null)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val sharedPref = getSharedPreferences("userInfo", MODE_PRIVATE)
        welcomeText = findViewById<TextView>(R.id.welcome)
        if (sharedPref.contains("userLogin")) {
            welcomeText.text = "Witaj " + sharedPref.getString("userLogin", null)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.popup_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.login -> {
                var intent = Intent(this@MainActivity, LoginActivity::class.java)
                startActivity(intent)
                refreshMessage()
            }
            R.id.register ->{
                var intent = Intent(this@MainActivity, RegisterActivity::class.java)
                startActivity(intent)
                refreshMessage()
            }
            R.id.logout ->{
                logout()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}