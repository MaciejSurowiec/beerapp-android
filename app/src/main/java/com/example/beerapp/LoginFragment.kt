package com.example.beerapp

import android.os.Bundle
import android.os.Message
import android.os.RemoteException
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.mindrot.jbcrypt.BCrypt

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private var thisView: View? = null
private var button: Button? = null
private var login: EditText? = null
private var password: EditText? = null
private var spinner: ProgressBar? = null

/**
 * A simple [Fragment] subclass.
 * Use the [LoginFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoginFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    suspend fun checkPassword() {
        val userLogin = login?.text.toString()
        val url = "${getString(R.string.baseUrl)}/users/$userLogin/login"
        var json: JSONObject? = null
        try {
            val client = OkHttpClient()
            var request = Request.Builder().url(url).build()
            var response = client.newCall(request).execute()
            json = JSONObject(response.body()?.string())
        } catch (e: java.lang.Exception) {
            json = JSONObject(mapOf("error" to "error"))
        }

        val passwordDB = json?.get("content") as String
        withContext(Dispatchers.Main) {
            if (BCrypt.checkpw(password?.text.toString(), passwordDB)) {
                val sharedPref = activity?.getSharedPreferences("userInfo", AppCompatActivity.MODE_PRIVATE)
                val editor = sharedPref?.edit()
                editor?.putString("userLogin", userLogin)
                editor?.apply()
                val navHostFragment = activity?.getSupportFragmentManager()?.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController

                val action = LoginFragmentDirections.actionLoginFragmentToLoggedFragment(userLogin,"")
                navController.navigate(action)
            } else {
                password?.error = "Błędne hasło"
                button?.isEnabled = true
                spinner?.visibility = View.GONE
            }
        }
    }


    fun initButton() {
        button?.setOnClickListener {
            if(login?.text.toString().contains(" ")){
                login?.error = "login nie może zawierać spacji"
                return@setOnClickListener
            }

            val json = JSONObject(mapOf("login" to login?.text.toString()))

            button?.isEnabled = false
            spinner?.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                checkPassword()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        thisView = inflater.inflate(R.layout.fragment_login, container, false)
        button = thisView?.findViewById(R.id.button)
        login = thisView?.findViewById(R.id.login)
        password = thisView?.findViewById(R.id.password)
        spinner = thisView?.findViewById(R.id.progressBar)
        initButton()
        // Inflate the layout for this fragment
        return thisView
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment LoginFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            LoginFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}