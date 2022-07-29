package com.beerup.beerapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.NavHostFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import org.mindrot.jbcrypt.BCrypt

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
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

    suspend fun checkPassword(userLogin : String) {
        val url = "${getString(R.string.baseUrl)}/users/$userLogin/login"
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
            withContext(Dispatchers.Main) {
                enableUI()
                spinner?.visibility = View.GONE
                Toast.makeText(activity?.baseContext,
                    "Błąd serwera spróbuj ponownie później",
                    Toast.LENGTH_LONG).show()
            }
        }

        if(!serverError) {
            val code = response?.code()
            if (code == 200) {
                val passwordDB = json?.get("content") as String
                withContext(Dispatchers.Main) {
                    if (BCrypt.checkpw(password?.text.toString(), passwordDB)) {
                        val sharedPref =
                            activity?.getSharedPreferences(
                                "userInfo",
                                AppCompatActivity.MODE_PRIVATE
                            )
                        val editor = sharedPref?.edit()
                        editor?.putString("userLogin", userLogin)
                        editor?.apply()
                        val navHostFragment = activity?.supportFragmentManager
                            ?.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                        val navController = navHostFragment.navController
                        (activity as MainActivity)?.userLogin = userLogin
                        val action =
                            LoginFragmentDirections.actionLoginFragmentToLoggedFragment(
                                userLogin,
                                ""
                            )
                        navController.navigate(action)
                    } else {
                        password?.error = "Błędne hasło"
                        enableUI()
                        spinner?.visibility = View.GONE
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    login?.error = "Błędny login"
                    enableUI()
                    spinner?.visibility = View.GONE
                }
            }
        }
    }

    fun initButton() {
        button?.setOnClickListener {
            var loginText = login?.text.toString()
            if(loginText.contains(" ")){
                if(loginText.last() == ' ')
                {
                    loginText = loginText.dropLast(1)
                    if(loginText.contains(" ")){
                        login?.error = "login nie może zawierać spacji"
                        return@setOnClickListener
                    }
                } else {
                    login?.error = "login nie może zawierać spacji"
                    return@setOnClickListener
                }
            }

            disableUI()
            spinner?.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                checkPassword(loginText)
            }
        }
    }

    fun disableUI(){
        button?.isEnabled = false
        login?.isEnabled = false
        password?.isEnabled = false
    }

    fun enableUI(){
        button?.isEnabled = true
        login?.isEnabled = true
        password?.isEnabled = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        button = view.findViewById(R.id.button)
        login = view.findViewById(R.id.login)
        password = view.findViewById(R.id.password)
        spinner = view.findViewById(R.id.progressBar)
        initButton()
        val mainActivity = activity as MainActivity
        mainActivity.enableBackPress = true
        mainActivity.backButtonEnd = false

        login?.doOnTextChanged { text, start, before, count ->
            button?.isEnabled = login?.text!!.isNotEmpty() &&  password?.text!!.isNotEmpty()
        }

        password?.doOnTextChanged { text, start, before, count ->
            button?.isEnabled = login?.text!!.isNotEmpty() &&  password?.text!!.isNotEmpty()
        }
    }

    companion object {

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