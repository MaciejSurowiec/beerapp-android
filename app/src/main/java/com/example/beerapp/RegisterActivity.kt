package com.example.beerapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import org.mindrot.jbcrypt.BCrypt


class RegisterActivity: AppCompatActivity() {

    protected var userLogin = ""
    var isBound = false
    lateinit var login: TextView
    lateinit var spinner : ProgressBar
    lateinit var button : Button
    lateinit var mMessenger: Messenger
    lateinit var httpService: Intent
    val replyMessage = Messenger(IncomingHandler())

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

    protected fun endRegistration() {
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
            if(json["content"] == "204") {
                endRegistration()
            } else {
                login.setError("ten login jest już zajęty")
                button.setEnabled(true)
                spinner.setVisibility(View.GONE)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        button = findViewById<Button>(R.id.button)
        login = findViewById<EditText>(R.id.login)
        var email = findViewById<EditText>(R.id.email)
        var password = findViewById<EditText>(R.id.password)
        var password2 = findViewById<EditText>(R.id.password2)
        spinner = findViewById<ProgressBar>(R.id.progressBar)
        httpService = Intent(this, HttpService::class.java)
        bindService(httpService, serviceConnection, BIND_AUTO_CREATE)

        spinner.setVisibility(View.GONE)
        button.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                var errors = false

                val passwordPattern = Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{4,}$")

                var emailString = email.getText().toString()
                userLogin = login.getText().toString()

                if(!password.getText().toString().isEmpty()) {
                    if(!passwordPattern.matches(password.getText().toString())) {
                        errors  = true
                        password.setError("podane hasło jest słabe")
                    }
                    else{
                        if (password.getText().toString() != password2.getText().toString()) {
                            errors = true
                            password2.setError("podane hasła sa różne")
                        }
                    }
                }

                if(!emailString.isEmpty()) {
                    if(!Patterns.EMAIL_ADDRESS.matcher(emailString).matches()) {
                        errors  = true
                        email.setError("to nie jest mail")
                    }
                }

                if(emailString.isEmpty() ||
                    password.getText().toString().isEmpty() ||
                    password2.getText().toString().isEmpty() ||
                    userLogin.isEmpty() ) {
                    errors = true
                    val toast = Toast.makeText(applicationContext, "brakuje danych", Toast.LENGTH_LONG)
                    toast.show()
                }

                if(!errors) {
                    val hashed = BCrypt.hashpw(password.getText().toString(), BCrypt.gensalt())
                    Log.i("test", hashed)
                    var json = JSONObject(mapOf(
                            "login" to userLogin,
                            "email" to emailString, "password" to hashed
                        ))

                    button.setEnabled(false)
                    spinner.setVisibility(View.VISIBLE)

                    if(mMessenger != null) {
                        var message = Message.obtain(null, R.integer.POST_HTTP, R.integer.REGISTER, 0)
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
                else {
                    Log.i("errors","coś nie pykło")
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