package com.example.beerapp

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI

class IncomingHandler(looper: Looper?) : Handler(looper!!) {
    var urlRegister = "https://k4qauqp2v9.execute-api.us-east-1.amazonaws.com/prod/users"
    override fun handleMessage(msg: Message) {
        var url = ""
        var data : JSONObject
        var json = JSONObject(msg.getData().getString("json"))

        when(msg.arg1){
            R.integer.REGISTER -> {
                url = urlRegister
            }

            R.integer.LOGIN -> {
                url = urlRegister + "/" + json["login"] +"/login"
            }
        }

        when(msg.what)
        {
            R.integer.GET_HTTP -> {

                val connection = URI(url).toURL().openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json; utf-8")
                connection.setRequestProperty("Accept", "application/json")
                try {
                    data = JSONObject(connection.inputStream.bufferedReader().readText())
                }
                catch(e: Exception){
                    data = JSONObject(mapOf("content" to ""))
                }

                connection.disconnect()
                val replyTo = msg.replyTo
                val message = Message.obtain(null, 0, 0, 0)
                val bundle = Bundle()
                bundle.putString("json", data.toString())
                message.data = bundle
                try {
                    replyTo.send(message)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }

            R.integer.POST_HTTP -> {
                var success = true
                var done = false
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


                data = JSONObject(mapOf("content" to connection.getResponseCode().toString()))

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



class HttpService: Service() {

    private var mServiceLooper: Looper? = null
    private var mServiceHandler: IncomingHandler? = null
    lateinit var thread: HandlerThread
    lateinit var mMessenger : Messenger

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