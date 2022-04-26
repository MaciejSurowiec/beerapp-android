package com.example.beerapp
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.Bundle
import android.os.IBinder
import android.os.Messenger
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {
    lateinit var welcomeText: TextView
    lateinit var sharedPref : SharedPreferences
    lateinit var loginButton : Button
    lateinit var registerButton : Button
    lateinit var httpService: Intent
    var isBound = false
    lateinit var mMessenger: Messenger

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            isBound = true
            mMessenger = Messenger(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            //mMessenger = null
            isBound = false
        }
    }
    fun logout() {
        val sharedPref = getSharedPreferences("userInfo", MODE_PRIVATE)
        val editor = sharedPref.edit()

        if (sharedPref.contains("userLogin")) {
            editor.remove("userLogin")
            editor.apply()
        }
        welcomeText.text = ""

        loginButton.setVisibility(View.VISIBLE)
        registerButton.setVisibility(View.VISIBLE)
        invalidateOptionsMenu()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2137) {
            if (resultCode == RESULT_OK) {
                loginSuccess()
            }
        }
    }

    fun loginSuccess() {
        if (sharedPref.contains("userLogin")) {
            welcomeText.text = "Witaj " + sharedPref.getString("userLogin", null)
            loginButton.setVisibility(View.GONE)
            registerButton.setVisibility(View.GONE)

            invalidateOptionsMenu()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPref = getSharedPreferences("userInfo", MODE_PRIVATE)
        welcomeText = findViewById<TextView>(R.id.welcome)
        loginButton = findViewById<Button>(R.id.login)
        registerButton = findViewById<Button>(R.id.register)

        httpService = Intent(this, HttpService::class.java)
        bindService(httpService, serviceConnection, BIND_AUTO_CREATE)

        loginButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                var intent = Intent(this@MainActivity, LoginActivity::class.java)
                startActivityForResult(intent,2137)
            }
        })

        registerButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                var intent = Intent(this@MainActivity, RegisterActivity::class.java)
                startActivityForResult(intent,2137)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.popup_menu, menu)
        if (!sharedPref.contains("userLogin")) {
            menu.setGroupVisible(0, false)
        }

        return true
    }

    override fun onPrepareOptionsMenu(menu : Menu): Boolean {
        if (sharedPref.contains("userLogin")) {
            var logout = menu.findItem(R.id.logout)
            logout.setVisible(true)
        }
        else {
            var logout = menu.findItem(R.id.logout)
            logout.setVisible(false)
        }

        return true;
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.login -> {
                var intent = Intent(this@MainActivity, LoginActivity::class.java)
                startActivityForResult(intent,2137)
                //refreshMessage()
            }
            R.id.register ->{
                var intent = Intent(this@MainActivity, RegisterActivity::class.java)
                startActivityForResult(intent,2137)
                //refreshMessage()
            }
            R.id.logout ->{
                logout()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
        }
    }
}