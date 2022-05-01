package com.example.beerapp

import android.R.attr
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.*
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI


class IncomingHandler(looper: Looper?) : Handler(looper!!) {
    private val baseUrl = "https://k4qauqp2v9.execute-api.us-east-1.amazonaws.com/prod"
    private val baseUrlRegister = "$baseUrl/users"
    private val baseUrlBeers = "$baseUrl/beers"

    override fun handleMessage(msg: Message) {
        var returnValue = 0
        var json: JSONObject? = null
        if(msg.data.containsKey("json")) {
            json = JSONObject(msg.data.getString("json") ?: "")
        }

        val url = when (msg.arg1) {
            R.integer.REGISTER_URL -> baseUrlRegister
            R.integer.LOGIN_URL -> "$baseUrlRegister/${json!!["login"]}/login"
            R.integer.BEERLIST_URL -> "$baseUrlBeers?login=${json!!["login"]}"
            R.integer.BEER_URL -> "$baseUrl/${json!!["id"]}"
            R.integer.BEERIMAGEGET_URL -> "$baseUrlBeers/${json!!["beerid"]}/image/download"
            R.integer.BEERIMAGEUPLOAD_URL -> "$baseUrlBeers/${json!!["beerid"]}/image"
            R.integer.REVIEWPOST_URL -> "$baseUrl/reviews"
            R.integer.JSON_URL -> json!!["url"].toString()
            R.integer.BEERLISTWITHPARAMS_URL -> "$baseUrlBeers?queryPhrase=${json!!["queryPhrase"]}&limit=10&start=${json!!["start"]}&login=${json!!["login"]}"
            else -> baseUrl
        }

        when(msg.what)
        {
            R.integer.GET_HTTP -> {

                val connection = URI(url).toURL().openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                connection.setRequestProperty("Content-Type", "image/bmp")
                val data = try {
                    JSONObject(connection.inputStream.bufferedReader().readText())
                } catch (e: Exception) {
                    JSONObject(mapOf("content" to ""))
                }
                Log.i("test",connection.responseCode.toString())
                connection.disconnect()
                val replyTo = msg.replyTo
                val message = Message.obtain(null, msg.arg2, 0, 0)

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


                val data = JSONObject(mapOf("content" to connection.responseCode.toString()))

                connection.disconnect()
                val replyTo = msg.replyTo
                val message = Message.obtain(null, msg.arg2, 0, 0)//w what moge dac czy sie udalo czy nie
                val bundle = Bundle()
                bundle.putString("json", data.toString())
                message.data = bundle
                try {
                    replyTo.send(message)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }

            R.integer.PUT_HTTP -> {
                val connection = URI(url).toURL().openConnection() as HttpURLConnection
                connection.requestMethod = "PUT"
                connection.doOutput = true
                var byteArray:ByteArray
                try {
                    connection.outputStream.use { os ->
                        byteArray = msg.data.get("bitmap") as ByteArray
                        os.write(byteArray, 0, byteArray.size)
                    }
                } catch (e: Exception) {
                    Log.d("err", e.toString())
                }

                val data = JSONObject(mapOf("content" to connection.responseCode.toString()))

                connection.disconnect()
                val replyTo = msg.replyTo
                val message = Message.obtain(null, msg.arg2, 0, 0)//w what moge dac czy sie udalo czy nie
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