package com.beerup.beerapp

import android.content.res.loader.ResourcesProvider
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.Provider
import java.util.ArrayList
import javax.inject.Inject

class BeerListViewModel: ViewModel() {

    var actualStart:Int = 0
    var query: String = ""
    private val client = OkHttpClient()
    var endOfDataShowed = false

    private val _beerList = MutableLiveData<BeerList>()
    var baseUrl: String = ""
    var userLogin: String = ""

    lateinit var showList: () -> Unit
    lateinit var spinnerOn: () -> Unit
    lateinit var spinnerOff: () -> Unit
    lateinit var retryButton: () -> Unit
    lateinit var enableUI: () -> Unit
    lateinit var disableUI: () -> Unit

    fun beerList(): LiveData<BeerList> {
        return _beerList
    }


    fun downloadList() {
        _beerList.value = BeerList()
        spinnerOn()
        CoroutineScope(Dispatchers.IO).launch {
            val url =
                "${baseUrl}/beers?limit=10&queryPhrase=${query}&start=${actualStart}&login=${userLogin}"
            var serverError = false

            var json: JSONObject? = null
            try {
                var request = Request.Builder().url(url).build()
                var response = client.newCall(request).execute()
                json = JSONObject(response.body()?.string())
            } catch (e: java.lang.Exception) {
                serverError = true
            }

            if (!serverError) {
                val data = json?.getJSONArray("content")
                if (data?.length() == 0) {
                    endOfDataShowed = true
                } else {
                    for (i in 0 until data!!.length()) {
                        val beer = data.getJSONObject(i)
                        val beerObject = Beer() // I need to use retrofit, but for now this is ok xd
                        beerObject.init(beer)

                        _beerList.value?.list?.add(beerObject)
                    }
                }

                withContext(Dispatchers.Main) {
                    showList()
                    spinnerOff()
                    enableUI()
                }
            } else {
                withContext(Dispatchers.Main) {
                    spinnerOff()
                    retryButton()
                    enableUI()
                }
            }
        }
    }

    fun search(str: String) {
         endOfDataShowed = false
        _beerList.value?.list!!.clear()
        query = str
        actualStart = 0
        downloadList()
    }


    fun resetSearch() {
        endOfDataShowed = false
        _beerList.value?.list!!.clear()
        query = ""
        actualStart = 0
        downloadList()
    }

    private fun fixUrl(str: String):String {
        var output = str.replace("%","%25")
            .replace(" ","%20")
            .replace("+","%2B")
            .replace("/","%2F")
            .replace("\\","%5C")
            .replace("$","%24")
            .replace("!","%21")
            .replace("?","%3F")
            .replace("#","%23")
            .replace("&","%26")
            .replace("=","%3D")
            .replace("(","%28")
            .replace(")","%29")
            .replace("-","%2D")
            .replace("\'","dd")
            .replace("\"'","")
            .replace("@","%40")
            .replace("^","%5E")
            .replace("*","%2A")
            .replace("(","%28")
            .replace(")","%29")
            .replace("{","%7B")
            .replace("}","%7D")
            .replace("[","%5B")
            .replace("]","%5D")
            .replace("|","%7C")
            .replace("<","%3C")
            .replace(">","%3E")
            .replace(",","%2C")
            .replace(".","%2E")
            .replace(":","%3A")
            .replace("_","%5F")
            .replace(";","%3B")
            .replace("`","%60")
            .replace("~","%7E")
            .replace("[^A-Z%a-z0-9 ]","")

        return output
    }

}