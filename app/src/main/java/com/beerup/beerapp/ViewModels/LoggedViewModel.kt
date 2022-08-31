package com.beerup.beerapp.ViewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.beerup.beerapp.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class LoggedViewModel(app: Application)
    : AndroidViewModel(app)  {

    lateinit var errorCallback: (String) -> Unit

    lateinit var baseUrl: String
    lateinit var userLogin: String
    lateinit var sharedViewModel: SharedViewModel

    var showStats = false

    fun getStatistics(callback: (Boolean) -> Unit) {
        showStats = false
        sharedViewModel._beerList.value = BeerList()
        CoroutineScope(Dispatchers.IO).launch {
            val url = "${baseUrl}/users/${userLogin}/statistics"
            var serverError = false
            var jsonStr: String = ""
            try {
                var client = OkHttpClient()
                var request = Request.Builder().url(url).build()
                var response = client.newCall(request).execute()
                jsonStr = response.body()?.string().toString()
            } catch (e: java.lang.Exception) {
                serverError = true
            }

            withContext(Dispatchers.Main) {
                if (!serverError) {
                    if (jsonStr.isNotEmpty()) {
                        val json = JSONObject(jsonStr)
                        var content = json?.get("content") as JSONObject
                        val data = content.getJSONArray("lastThreeReviews")
                        sharedViewModel._photos.postValue(content["numberOfPhotos"].toString())
                        sharedViewModel._reviewedBeers.postValue(content["numberOfReviews"].toString())

                        for (i in 0 until data.length()) {
                            val beer = data.getJSONObject(i)
                            var beerObject = Beer()
                            beerObject.init(beer)

                            sharedViewModel._beerList.value?.list?.add(beerObject)
                        }

                        callback(true)
                    } else {
                        callback(false)
                    }
                } else {
                    callback(false)
                }
            }
        }
    }
}