package com.example.beerapp

import android.os.Bundle
import android.os.Message
import android.os.RemoteException
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import org.mindrot.jbcrypt.BCrypt

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private var login: TextView? = null
private var password: TextView? = null
private var password2: TextView? = null
private var email: TextView? = null
private var spinner: ProgressBar? = null
private var button: Button? = null
private val client = OkHttpClient()
private var thisView: View? = null
private val JSON = MediaType.parse("application/json; charset=utf-8")
private val passwordPattern = Regex("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%\\\\!*()\\-|\\]\\[\"\":;?.,<>`~{}=^&+=])(?=\\S+$).{4,}$")
private var userLogin = ""
/**
 * A simple [Fragment] subclass.
 * Use the [RegisterFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RegisterFragment : Fragment() {
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

    suspend fun Register(json: JSONObject) {
        var url="${context?.getString(com.example.beerapp.R.string.baseUrl)}/users"
        var request: Request? = null
        val formBody: RequestBody = RequestBody.create(JSON, json.toString())

        request = Request.Builder().url(url).post(formBody).build()
        val response: Response = client.newCall(request).execute()
        val code = response.code()

        withContext(Dispatchers.Main) {
            if (code == 204) {
                val sharedPref = activity?.getSharedPreferences("userInfo", AppCompatActivity.MODE_PRIVATE)
                val editor = sharedPref?.edit()
                editor?.putString("userLogin", userLogin)
                editor?.apply()
                val navHostFragment = activity?.getSupportFragmentManager()?.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController

                val action = RegisterFragmentDirections.actionRegisterFragmentToLoggedFragment(userLogin,"")
                navController.navigate(action)
            } else {
                button?.isEnabled = false
                spinner?.visibility = View.VISIBLE
                Toast.makeText(activity?.baseContext, response.message(), Toast.LENGTH_LONG).show()
            }
        }
    }

    fun initButton() {
        button?.setOnClickListener {
            val emailString = email?.text.toString()
            userLogin = login?.text.toString()

            if(userLogin.contains(" ")){
                login?.error = "login nie może zawierać spacji"
                return@setOnClickListener
            }

            if (password?.text.toString().isNotEmpty()) {
                if (!passwordPattern.matches(password?.text.toString())) {
                    password?.error = "Podane hasło jest zbyt słabe"
                    return@setOnClickListener
                } else {
                    if (password?.text.toString() != password2?.text.toString()) {
                        password2?.error = "Podane hasła sa różne"
                        return@setOnClickListener
                    }
                }
            }

            if (!emailString.isEmpty()) {
                if (!Patterns.EMAIL_ADDRESS.matcher(emailString).matches()) {
                    email?.error = "To nie jest mail"
                    return@setOnClickListener
                }
            }

            if (emailString.isEmpty() ||
                password?.text.toString().isEmpty() ||
                password2?.text.toString().isEmpty() ||
                userLogin.isEmpty()
            ) {
                Toast.makeText(activity?.baseContext, "brakuje danych", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val hashed = BCrypt.hashpw(password?.text.toString(), BCrypt.gensalt())
            val json = JSONObject(
                mapOf(
                    "login" to userLogin,
                    "email" to emailString,
                    "password" to hashed
                )
            )

            button?.isEnabled = false
            spinner?.visibility = View.VISIBLE

            CoroutineScope(Dispatchers.IO).launch {
                Register(json)
            }
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        thisView = inflater.inflate(R.layout.fragment_register, container, false)

        button = thisView?.findViewById(R.id.button)
        login = thisView?.findViewById(R.id.login)
        email = thisView?.findViewById(R.id.email)
        password = thisView?.findViewById(R.id.password)
        password2 = thisView?.findViewById(R.id.password2)
        spinner = thisView?.findViewById(R.id.progressBar)
        initButton()

        return thisView
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment RegisterFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            RegisterFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}