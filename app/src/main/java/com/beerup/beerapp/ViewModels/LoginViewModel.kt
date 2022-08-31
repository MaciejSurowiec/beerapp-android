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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import org.mindrot.jbcrypt.BCrypt


class LoginViewModel(app: Application)
    : AndroidViewModel(app) {

    lateinit var baseUrl: String
    lateinit var errorCallback: (String) -> Unit
    lateinit var passwordError: () -> Unit
    lateinit var loginError: () -> Unit

    var _isUIActive = MutableLiveData<Boolean>(true)


    fun checkPassword(userLogin: String, password: String, navigate: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val url = "${baseUrl}/users/$userLogin/login"
            var json: JSONObject? = null
            var response: Response? = null
            var serverError = false
            try {
                val client = OkHttpClient()
                var request = Request.Builder().url(url).build()
                response = client.newCall(request).execute()
                json = JSONObject(response.body()?.string())
            } catch (e: java.lang.Exception) {
                serverError = true
                _isUIActive.postValue(true)
                withContext(Dispatchers.Main) {
                    errorCallback("Błąd serwera spróbuj ponownie później")
                }
            }

            if (!serverError) {
                val code = response?.code()
                if (code == 200) {
                    val passwordDB = json?.get("content") as String
                    withContext(Dispatchers.Main) {
                        if (BCrypt.checkpw(password, passwordDB)) {
                            val sharedPref =
                                getApplication<Application>().getSharedPreferences(
                                    "userInfo",
                                    AppCompatActivity.MODE_PRIVATE
                                )

                            val editor = sharedPref?.edit()
                            editor?.putString("userLogin", userLogin)
                            editor?.apply()
                            navigate(userLogin)
                        } else {
                            _isUIActive.postValue(true)
                           passwordError()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loginError()
                        _isUIActive.postValue(true)
                    }
                }
            }
        }
    }
}