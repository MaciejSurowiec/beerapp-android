package com.beerup.beerapp

import android.os.Bundle
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
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
private val JSON = MediaType.parse("application/json; charset=utf-8")
private var userLogin = ""

class RegisterFragment : Fragment() {
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
        var url="${context?.getString(com.beerup.beerapp.R.string.baseUrl)}/users"
        var response: Response? = null
        var serverError = false
        val formBody: RequestBody = RequestBody.create(JSON, json.toString())
        try{
            val request = Request.Builder().url(url).post(formBody).build()
            response = client.newCall(request).execute()
        } catch (e: java.lang.Exception) {
            serverError = true
            withContext(Dispatchers.Main) {
                enableUI()
                spinner?.visibility = View.GONE
                Toast.makeText(activity?.baseContext, "Błąd serwera spróbuj ponownie później", Toast.LENGTH_LONG).show()
            }
        }

        if(!serverError) {
            val code = response?.code()
            withContext(Dispatchers.Main) {
                if (code == 204) {
                    val sharedPref =
                        activity?.getSharedPreferences("userInfo", AppCompatActivity.MODE_PRIVATE)
                    val editor = sharedPref?.edit()
                    editor?.putString("userLogin", userLogin)
                    editor?.apply()
                    val navHostFragment = activity?.supportFragmentManager
                        ?.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    val navController = navHostFragment.navController

                    val action = RegisterFragmentDirections.actionRegisterFragmentToLoggedFragment(
                        userLogin,
                        ""
                    )
                    navController.navigate(action)
                } else {
                    if (code == 400) {
                        enableUI()
                        spinner?.visibility = View.GONE
                        login?.error = "podany login jest zajęty"
                    } else {
                        enableUI()
                        spinner?.visibility = View.GONE
                        Toast.makeText(
                            activity?.baseContext,
                            "Błąd serwera spróbuj ponownie później",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
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
                if (password?.text.toString().count() < 8) {
                    password?.error = "Hasło musi składać się z co najmniej 8 znaków"
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
                Toast.makeText(activity?.baseContext, "brakuje danych", Toast.LENGTH_SHORT).show()
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

            disableUI()
            spinner?.visibility = View.VISIBLE

            CoroutineScope(Dispatchers.IO).launch {
                Register(json)
            }
        }
    }

    fun disableUI(){
        button?.isEnabled = false
        login?.isEnabled = false
        password?.isEnabled = false
        email?.isEnabled = false
        password2?.isEnabled = false
    }

    fun enableUI(){
        button?.isEnabled = true
        login?.isEnabled = true
        password?.isEnabled = true
        email?.isEnabled = true
        password2?.isEnabled = true
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mainActivity = activity as MainActivity
        mainActivity.enableBackPress = true
        mainActivity.backButtonEnd = false
        button = view.findViewById(R.id.button)
        login = view.findViewById(R.id.login)
        email = view.findViewById(R.id.email)
        password = view.findViewById(R.id.password)
        password2 = view.findViewById(R.id.password2)
        spinner = view.findViewById(R.id.progressBar)

        login?.doOnTextChanged { text, start, before, count ->
            button?.isEnabled = login?.text!!.isNotEmpty() && email?.text!!.isNotEmpty() &&
                    password?.text!!.isNotEmpty() && password2?.text!!.isNotEmpty()
        }

        email?.doOnTextChanged { text, start, before, count ->
            button?.isEnabled = login?.text!!.isNotEmpty() && email?.text!!.isNotEmpty() &&
                    password?.text!!.isNotEmpty() && password2?.text!!.isNotEmpty()
        }

        password?.doOnTextChanged { text, start, before, count ->
            button?.isEnabled = login?.text!!.isNotEmpty() && email?.text!!.isNotEmpty() &&
                    password?.text!!.isNotEmpty() && password2?.text!!.isNotEmpty()
        }

        password2?.doOnTextChanged { text, start, before, count ->
            button?.isEnabled = login?.text!!.isNotEmpty() && email?.text!!.isNotEmpty() &&
                    password?.text!!.isNotEmpty() && password2?.text!!.isNotEmpty()
        }

        initButton()
    }

    companion object {

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