package com.beerup.beerapp.ViewModels

import android.app.Application
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.NavHostFragment
import com.beerup.beerapp.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject

class RegisterViewModel(app: Application)
    : AndroidViewModel(app) {

    lateinit var baseUrl: String
    private val client = OkHttpClient()
    private val JSON = MediaType.parse("application/json; charset=utf-8")
    lateinit var errorCallback: (String) -> Unit

    var _isUIActive = MutableLiveData(false) // I saw that mutable is used with _ so I will keep it


    fun Register(json: JSONObject, navigate: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            var url = "${baseUrl}/users"
            var response: Response? = null
            var serverError = false
            val formBody: RequestBody = RequestBody.create(JSON, json.toString())
            try {
                val request = Request.Builder().url(url).post(formBody).build()
                response = client.newCall(request).execute()
            } catch (e: java.lang.Exception) {
                serverError = true
                withContext(Dispatchers.Main) {
                    _isUIActive.postValue(true)
                    errorCallback("Błąd serwera spróbuj ponownie później")
                }
            }

            if (!serverError) {
                val code = response?.code()
                withContext(Dispatchers.Main) {
                    if (code == 204) {
                        val sharedPref =
                            getApplication<Application>().getSharedPreferences(
                                "userInfo",
                                AppCompatActivity.MODE_PRIVATE
                            )
                        val editor = sharedPref?.edit()
                        editor?.putString("userLogin", json!!["login"].toString())
                        editor?.apply()
                        navigate(json!!["login"].toString())
                    } else {
                        if (code == 400) {
                            //login?.error = "podany login jest zajęty"
                            _isUIActive.postValue(true)
                            errorCallback("Błąd serwera spróbuj ponownie później")
                        } else {
                            _isUIActive.postValue(true)
                            errorCallback("Błąd serwera spróbuj ponownie później")
                        }
                    }
                }
            }
        }
    }
}