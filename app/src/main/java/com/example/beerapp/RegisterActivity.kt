package com.example.beerapp

import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import org.mindrot.jbcrypt.BCrypt
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI


class RegisterActivity: AppCompatActivity() {

    protected var conError = false
    protected var userLogin = ""

    inner class doAsync(val handler: () -> Unit) : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void?): Void? {
            try {
                handler()
                return null
            }
            catch(e: Exception) {
                conError = true
                return null
            }
        }
    }

    fun addUser(json: JSONObject) : Boolean {
        var url = "https://k4qauqp2v9.execute-api.us-east-1.amazonaws.com/prod/users"
        var success = true
        var done = false
        doAsync {
            val connection = URI(url).toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.setDoOutput(true)

            try {
                connection.getOutputStream().use { os ->
                    val input: ByteArray = json.toString().toByteArray()
                    os.write(input, 0, input.size)
                }
            }
            catch(e : Exception) {
                Log.d("err",e.toString())
                success = false
            }

            try {
                BufferedReader (
                    InputStreamReader(connection.getInputStream(), "utf-8")
                ).use { br ->
                    val response = StringBuilder()
                    var responseLine: String? = null
                    while (br.readLine().also { responseLine = it } != null) {
                        response.append(responseLine!!.trim { it <= ' ' })
                    }
                    Log.i("out",response.toString())
                }
            }
            catch(e : Exception) {
                Log.d("err", e.toString())

            }
            done = true
        }.execute()

        while(!done){}

        return success
    }

    fun endRegistration() {
        val sharedPref = getSharedPreferences("userInfo", MODE_PRIVATE)
        val editor = sharedPref.edit()

        editor.putString("userLogin", userLogin)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        var button = findViewById<Button>(R.id.button)
        var login = findViewById<EditText>(R.id.login)
        var email = findViewById<EditText>(R.id.email)
        var password = findViewById<EditText>(R.id.password)
        var password2 = findViewById<EditText>(R.id.password2)

        button.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                var errors = false

                var emailReg = Regex("^(([^<>()\\[\\]\\\\.,;:\\s@\"]+(\\.[^<>()\\[\\]\\\\.,;:\\s@\"]+)*)|(\".+\"))@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))\$",
                    RegexOption.IGNORE_CASE)
                var passReg = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#\\\$%\\^&\\*])(?=.{8,})")



                if (password.getText().toString() != password2.getText().toString()) {
                    errors = true
                    //wyswietl error odnosnie innych hasel
                }

                if(!password.getText().toString().isEmpty()) {
                    if(!passReg.matches(password.getText().toString())) {
                        errors  = true
                        // wyswietl error o slabym hasle
                    }
                }

                if(!email.getText().toString().isEmpty()) {
                    if(!passReg.matches(email.getText().toString())) {
                        errors  = true
                        // wyswietl error o emailu
                    }
                }

                if(email.getText().toString().isEmpty() ||
                    password.getText().toString().isEmpty() ||
                    password2.getText().toString().isEmpty() ||
                    login.getText().toString().isEmpty() ) {
                    errors = true
                }

                if(!errors) {
                    val hashed = BCrypt.hashpw(password.getText().toString(), BCrypt.gensalt())
                    Log.i("test", hashed)
                    userLogin = login.getText().toString()
                    var json = JSONObject(
                        mapOf(
                            "login" to userLogin,
                            "email" to email.getText().toString(), "password" to hashed
                        )
                    )

                    conError = false
                    addUser(json).toString()
                    if(conError) {
                        //wyswietl error rejestracji
                    }
                    else {
                        endRegistration()
                    }
                }
            }
        })
    }




}