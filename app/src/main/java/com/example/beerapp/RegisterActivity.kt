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


class RegisterActivity : AppCompatActivity() {
    private lateinit var login: TextView
    private lateinit var spinner: ProgressBar
    private lateinit var button: Button
    private lateinit var mMessenger: Messenger
    private lateinit var httpService: Intent
    private val replyMessage = Messenger(IncomingHandler())
    private var isBound = false
    private var userLogin = ""
    private val passwordPattern = Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{4,}$")

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            isBound = true
            mMessenger = Messenger(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }
    }

    private fun endRegistration() {
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
            val json = JSONObject(msg.data.getString("json") ?: "")
            if (json["content"] == "204") {
                endRegistration()
            } else {
                login.error = "Ten login jest już zajęty"
                button.isEnabled = true
                spinner.visibility = View.GONE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        button = findViewById(R.id.button)
        login = findViewById<EditText>(R.id.login)
        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        val password2 = findViewById<EditText>(R.id.password2)
        spinner = findViewById(R.id.progressBar)
        httpService = Intent(this, HttpService::class.java)
        bindService(httpService, serviceConnection, BIND_AUTO_CREATE)

        spinner.visibility = View.GONE
        button.setOnClickListener {
            val emailString = email.text.toString()
            userLogin = login.text.toString()

            if (password.text.toString().isNotEmpty()) {
                if (!passwordPattern.matches(password.text.toString())) {
                    password.error = "Podane hasło jest zbyt słabe"
                    return@setOnClickListener
                } else {
                    if (password.text.toString() != password2.text.toString()) {
                        password2.error = "Podane hasła sa różne"
                        return@setOnClickListener
                    }
                }
            }

            if (!emailString.isEmpty()) {
                if (!Patterns.EMAIL_ADDRESS.matcher(emailString).matches()) {
                    email.error = "To nie jest mail"
                    return@setOnClickListener
                }
            }

            if (emailString.isEmpty() ||
                password.text.toString().isEmpty() ||
                password2.text.toString().isEmpty() ||
                userLogin.isEmpty()
            ) {
                val toast = Toast.makeText(applicationContext, "brakuje danych", Toast.LENGTH_LONG)
                toast.show()
                return@setOnClickListener
            }

            val hashed = BCrypt.hashpw(password.text.toString(), BCrypt.gensalt())
            Log.i("test", hashed)
            val json = JSONObject(
                mapOf(
                    "login" to userLogin,
                    "email" to emailString,
                    "password" to hashed
                )
            )

            button.isEnabled = false
            spinner.visibility = View.VISIBLE

            val message = Message.obtain(null, R.integer.POST_HTTP, R.integer.REGISTER, 0)
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

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
        }
    }
}