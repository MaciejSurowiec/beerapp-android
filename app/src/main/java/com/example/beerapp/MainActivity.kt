package com.example.beerapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.os.*
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import org.mindrot.jbcrypt.BCrypt


class MainActivity : AppCompatActivity() {
    private lateinit var welcomeText: TextView
    private lateinit var sharedPref: SharedPreferences
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var httpService: Intent
    private var mMessenger: Messenger? = null

    private val replyMessage = Messenger(IncomingHandler())
    private var isBound = false

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            isBound = true
            mMessenger = Messenger(service)

            if (sharedPref.contains("userLogin")) {
                welcomeText.text = "Witaj ${sharedPref.getString("userLogin", null)}"
                loginButton.visibility = View.GONE
                registerButton.visibility = View.GONE

                val message = Message.obtain(null, R.integer.GET_HTTP, R.integer.REVIEWNUMBER_URL, 0)
                message.replyTo = replyMessage
                val bundle = Bundle()
                val json = JSONObject(mapOf("login" to sharedPref.getString("userLogin",null)))
                bundle.putString("json", json.toString())
                message.data = bundle

                mMessenger!!.send(message)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }
    }

    inner class IncomingHandler : Handler() {
        override fun handleMessage(msg: Message) {
            val json = JSONObject(msg.data.getString("json") ?: "")
            findViewById<TextView>(R.id.reviewNumber).text = "Ocenione piwa: ${json["content"].toString()}"
        }
    }

    private fun logout() {
        val sharedPref = getSharedPreferences("userInfo", MODE_PRIVATE)
        val editor = sharedPref.edit()

        if (sharedPref.contains("userLogin")) {
            editor.remove("userLogin")
            editor.apply()
        }
        welcomeText.text = ""
        findViewById<TextView>(R.id.reviewNumber).text = ""
        loginButton.visibility = View.VISIBLE
        registerButton.visibility = View.VISIBLE
        invalidateOptionsMenu()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2137) {
            if (resultCode == RESULT_OK) {
                loginSuccess()
            }
            else{
                if (sharedPref.contains("userLogin")) {
                    val message = Message.obtain(null, R.integer.GET_HTTP, R.integer.REVIEWNUMBER_URL, 0)
                    message.replyTo = replyMessage
                    val bundle = Bundle()
                    val json = JSONObject(mapOf("login" to sharedPref.getString("userLogin",null)))
                    bundle.putString("json", json.toString())
                    message.data = bundle

                    mMessenger!!.send(message)
                }
            }
        }
    }

    fun loginSuccess() {
        if (sharedPref.contains("userLogin")) {
            welcomeText.text = "Witaj ${sharedPref.getString("userLogin", null)}"
            loginButton.visibility = View.GONE
            registerButton.visibility = View.GONE

            val message = Message.obtain(null, R.integer.GET_HTTP, R.integer.REVIEWNUMBER_URL, 0)
            message.replyTo = replyMessage
            val bundle = Bundle()
            val json = JSONObject(mapOf("login" to sharedPref.getString("userLogin",null)))
            bundle.putString("json", json.toString())
            message.data = bundle

            mMessenger!!.send(message)

            invalidateOptionsMenu()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sharedPref = getSharedPreferences("userInfo", MODE_PRIVATE)
        welcomeText = findViewById(R.id.welcome)
        loginButton = findViewById(R.id.login)
        registerButton = findViewById(R.id.register)
        httpService = Intent(this, HttpService::class.java)
        bindService(httpService, serviceConnection, BIND_AUTO_CREATE)

        loginButton.setOnClickListener {
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivityForResult(intent, 2137)
        }

        registerButton.setOnClickListener {
            val intent = Intent(this@MainActivity, RegisterActivity::class.java)
            startActivityForResult(intent, 2137)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.popup_menu, menu)
        if (!sharedPref.contains("userLogin")) {
            menu.setGroupVisible(0, false)
        }

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (sharedPref.contains("userLogin")) {
            val logout = menu.findItem(R.id.logout)
            logout.isVisible = true
        } else {
            val logout = menu.findItem(R.id.logout)
            logout.isVisible = false
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.beerlist -> {
                val intent = Intent(this@MainActivity, BeerListActivity::class.java)
                startActivityForResult(intent,2137)
            }
            R.id.logout -> {
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