package com.beerup.beerapp.ViewModels

import android.app.Application
import androidx.collection.ArraySet
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.beerup.beerapp.Beer
import com.beerup.beerapp.BeerList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject

class SharedViewModel(app: Application)
    : AndroidViewModel(app)  {

    lateinit var userLogin: String
    var _reviewedBeers = MutableLiveData("-")
    var _photos = MutableLiveData("-")
    var _tagsDownloaded = MutableLiveData(false)
    var _statsDownloaded = MutableLiveData(false)
    var _tagsError = MutableLiveData(false)

    var _beerList = MutableLiveData<BeerList>()
    lateinit var baseUrl: String
    var enableBackPress = true
    var backButtonEnd = true

    var tagList = ArraySet<String>()

    fun beerList(): LiveData<BeerList> {
        return _beerList
    }


    fun updateBeerInStats(json: JSONObject, newReview: Boolean) {
        if(newReview){
            _reviewedBeers.postValue(
                _reviewedBeers.value?.toInt()?.plus(1).toString()
            )
        }

        for (i in 0 until _beerList.value?.list!!.size) {
            if(_beerList.value?.list!![i].id == json["beerId"]) {
                _beerList.value?.list!!.removeAt(i)
                break
            }
        }

        if(_beerList.value?.list!!.size > 2) {
            _beerList.value?.list!!.removeLast()
        }

        var beer = Beer()
        beer.init(json)
        _beerList.value?.list!!.add(0, beer)
    }

    fun getStatistics() { // yeah I know it's doubled but this one is bit diffrent xd
        _beerList.value = BeerList()
        CoroutineScope(Dispatchers.IO).launch {
            val url = "${baseUrl}/users/${userLogin}/statistics"
            var response: Response? = null
            var json: JSONObject? = null
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
                        _photos.postValue(content["numberOfPhotos"].toString())
                        _reviewedBeers.postValue(content["numberOfReviews"].toString())

                        for (i in 0 until data.length()) {
                            val beer = data.getJSONObject(i)
                            var beerObject = Beer()
                            beerObject.init(beer)

                            _beerList.value?.list?.add(beerObject)
                        }

                        _statsDownloaded.postValue(true) // no matter of result i want to go further
                    }
                } else {
                    _statsDownloaded.postValue(true) // no matter of result i want to go further
                }
            }
        }
    }


    fun getTags(){
        CoroutineScope(Dispatchers.IO).launch {
            val url = "${baseUrl}/beers/tags"

            var jsonTags: JSONObject? = null
            var serverError = false
            try {
                val client = OkHttpClient()
                var request = Request.Builder().url(url).build()
                var response = client.newCall(request).execute()
                jsonTags = JSONObject(response.body()?.string())
            } catch (e: java.lang.Exception) {
                serverError = true
            }

            if (!serverError) {
                if (jsonTags!!.has("content")) {
                    val data = JSONArray(jsonTags!!["content"].toString())

                    for (i in 0 until data.length()) {
                        tagList.add(data.getString(i))
                    }

                    _tagsDownloaded.postValue(true)
                } else {
                    getTags() // same as there \/
                }
            } else {
                //getTags() // maybe shows some error but font go further cause app wont work without tags
                _tagsError.postValue(true)
            }
        }
    }

}