package com.example.beerapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import org.mindrot.jbcrypt.BCrypt

class LoginActivity: AppCompatActivity() {
    protected var conError = false
    protected var userLogin = ""
    var isBound = false
    lateinit var mMessenger: Messenger
    lateinit var httpService: Intent
    val replyMessage = Messenger(IncomingHandler())
    lateinit var button : Button
    lateinit var login : EditText
    lateinit var password : EditText
    lateinit var spinner : ProgressBar


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

    fun endLogin() {
            val sharedPref = getSharedPreferences("userInfo", MODE_PRIVATE)
            val editor = sharedPref.edit()

            editor.putString("userLogin", userLogin)
            editor.apply()
            val resultIntent = Intent()
            setResult(RESULT_OK, resultIntent)
            finish()
    }

    inner class IncomingHandler : Handler() {
        override fun handleMessage(msg: Message) {
            var json = JSONObject(msg.getData().getString("json"))
            if(!json["content"].toString().isEmpty()) {
                var passwordDB = json["content"].toString()

                if (BCrypt.checkpw(password.getText().toString(), passwordDB)) {
                    userLogin = login.getText().toString()
                    endLogin()
                } else {
                    password.setError("złe hasło")
                    button.setEnabled(true)
                    spinner.setVisibility(View.GONE)
                }
            } else {
                login.setError("nie ma takiego użytkownika")
                button.setEnabled(true)
                spinner.setVisibility(View.GONE)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        button = findViewById<Button>(R.id.button)
        login = findViewById<EditText>(R.id.login)
        password = findViewById<EditText>(R.id.password)
        spinner = findViewById<ProgressBar>(R.id.progressBar)
        httpService = Intent(this, HttpService::class.java)
        bindService(httpService, serviceConnection, BIND_AUTO_CREATE)

        button.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                userLogin = login.getText().toString()
                var json = JSONObject(mapOf("login" to userLogin))

                if(mMessenger != null) {
                    button.setEnabled(false)
                    spinner.setVisibility(View.VISIBLE)
                    var message = Message.obtain(null, R.integer.GET_HTTP, R.integer.LOGIN, 0)
                    val bundle = Bundle()

                    bundle.putString("json", json.toString())
                    message.data = bundle
                    message.replyTo = replyMessage
                    try {
                        mMessenger.send(message)
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
        }
    }
}