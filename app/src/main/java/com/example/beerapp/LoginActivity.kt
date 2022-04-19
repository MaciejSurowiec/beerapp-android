package com.example.beerapp

import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import org.mindrot.jbcrypt.BCrypt

class LoginActivity: AppCompatActivity() {
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

    fun endLogin() {
        val sharedPref = getSharedPreferences("userInfo", MODE_PRIVATE)
        val editor = sharedPref.edit()

        editor.putString("userLogin", userLogin)
        editor.apply()
        finish()
    }


    fun getPassword(login : String): String {
        var url = "https://k4qauqp2v9.execute-api.us-east-1.amazonaws.com/prod/users/" + login + "/login"
        var data = ""
        var done = false
        doAsync{
            val connection = URI(url).toURL().openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json; utf-8")
            connection.setRequestProperty("Accept", "application/json")

            var json = JSONObject(connection.inputStream.bufferedReader().readText())
            data = json.getString("content")
            done = true
        }.execute()

        while(!done) {}

        return data
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        var button = findViewById<Button>(R.id.button)
        var login = findViewById<EditText>(R.id.login)
        var password = findViewById<EditText>(R.id.password)

        button.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {

                var passwordDB = getPassword(login.getText().toString())
                Log.i("test",passwordDB)
                if(BCrypt.checkpw(password.getText().toString(), passwordDB)) {
                    userLogin = login.getText().toString()
                    endLogin()
                } else {
                    // wyswietlanie bledu
                }
            }
        })

    }

}