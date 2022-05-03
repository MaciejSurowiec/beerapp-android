package com.example.beerapp

import android.app.IntentService
import android.content.Intent
import android.os.Bundle
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI


class PhotoService: IntentService("PhotoUploader") {
    override fun onHandleIntent(intent: Intent?) {
        var json: JSONObject? = null
        var data = intent?.extras?.getBundle("data")
        if(data!!.containsKey("json")) {
            json = JSONObject(data.getString("json") ?: "")
        }
        val url = json!!["url"].toString()

        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "PUT"
        connection.doOutput = true
        var byteArray:ByteArray
        try {
            connection.outputStream.use { os ->
                byteArray = data.get("bitmap") as ByteArray
                os.write(byteArray, 0, byteArray.size)
            }
        } catch (e: Exception) {
            Log.d("err", e.toString())
        }

        val output = JSONObject(mapOf("content" to connection.responseCode.toString()))

        connection.disconnect()
        var replyTo = data.getParcelable<Messenger>("replyTo")
        val message = Message.obtain(null, 3, 0, 0)
        val bundle = Bundle()
        bundle.putString("json", data.toString())
        message.data = bundle
        try {
            replyTo?.send(message)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }
}