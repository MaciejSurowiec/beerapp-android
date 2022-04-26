package com.example.beerapp

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI

class IncomingHandler(looper: Looper?) : Handler(looper!!) {
    private val baseUrl = "https://k4qauqp2v9.execute-api.us-east-1.amazonaws.com/prod"
    private val baseUrlRegister = "$baseUrl/users"

    override fun handleMessage(msg: Message) {
        val json = JSONObject(msg.data.getString("json") ?: "")

        val url = when (msg.arg1) {
            R.integer.REGISTER -> baseUrlRegister
            R.integer.LOGIN -> "$baseUrlRegister/${json["login"]}/login"
            else -> baseUrl
        }

        when (msg.what) {
            R.integer.GET_HTTP -> {
                val connection = URI(url).toURL().openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json; utf-8")
                connection.setRequestProperty("Accept", "application/json")
                val data = try {
                    JSONObject(connection.inputStream.bufferedReader().readText())
                } catch (e: Exception) {
                    JSONObject(mapOf("content" to ""))
                }
                connection.disconnect()

                val message = Message.obtain(null, 0, 0, 0)
                val bundle = Bundle()
                bundle.putString("json", data.toString())
                message.data = bundle
                try {
                    msg.replyTo.send(message)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }

            R.integer.POST_HTTP -> {
                val connection = URI(url).toURL().openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; utf-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true

                try {
                    connection.outputStream.use { os ->
                        val input: ByteArray = json.toString().toByteArray()
                        os.write(input, 0, input.size)
                    }
                } catch (e: Exception) {
                    Log.d("err", e.toString())
                }


                val data = JSONObject(mapOf("content" to connection.responseCode.toString()))

                connection.disconnect()
                val replyTo = msg.replyTo
                val message = Message.obtain(null, 0, 0, 0)//w what moge dac czy sie udalo czy nie
                val bundle = Bundle()
                bundle.putString("json", data.toString())
                message.data = bundle
                try {
                    replyTo.send(message)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }

        }
    }
}


class HttpService : Service() {

    private var mServiceLooper: Looper? = null
    private var mServiceHandler: IncomingHandler? = null
    private lateinit var thread: HandlerThread
    private lateinit var mMessenger: Messenger

    override fun onCreate() {
        super.onCreate()
        thread = HandlerThread(
            "ServiceStartArguments",
            Process.THREAD_PRIORITY_BACKGROUND
        )
        thread.start()
        mServiceLooper = thread.looper
        mServiceHandler = IncomingHandler(mServiceLooper)
        mMessenger = Messenger(mServiceHandler)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return mMessenger.binder
    }

    override fun onDestroy() {
    }
}