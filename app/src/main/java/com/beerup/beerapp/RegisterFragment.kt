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
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.beerup.beerapp.ViewModels.LoginViewModel
import com.beerup.beerapp.ViewModels.RegisterViewModel
import com.beerup.beerapp.ViewModels.SharedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import org.mindrot.jbcrypt.BCrypt

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


class RegisterFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var login: TextView
    private lateinit var password: TextView
    private lateinit var password2: TextView
    private lateinit var email: TextView
    private lateinit var spinner: ProgressBar
    private lateinit var button: Button

    private lateinit var viewModel: RegisterViewModel
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        viewModel = ViewModelProvider(this).get(RegisterViewModel::class.java)
        viewModel.baseUrl = getString(R.string.baseUrl)
        viewModel.errorCallback = ::errorCallback
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        viewModel._isUIActive.observe(this) {
            if (it) {
                enableUI()
                spinner.visibility = View.GONE
            } else {
                disableUI()
                spinner.visibility = View.VISIBLE
            }
        }
    }


    private fun errorCallback(message: String) {
        Toast.makeText(activity?.baseContext,
            message,
            Toast.LENGTH_LONG
        ).show()
    }


    private fun navigateToLoggedFragment(userLogin: String) {  // you need to navigate from fragment level cause this is "ui logic" xd
        val navHostFragment = activity?.supportFragmentManager
            ?.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val action =
            RegisterFragmentDirections.actionRegisterFragmentToLoggedFragment(userLogin)
        navController.navigate(action)
    }


    private fun initButton() {
        button.setOnClickListener {
            val emailString = email.text.toString()
            val userLogin = login.text.toString()

            if(userLogin.contains(" ")){
                login.error = "login nie może zawierać spacji"
                return@setOnClickListener
            }

            if (password.text.toString().isNotEmpty()) {
                if (password.text.toString().count() < 8) {
                    password.error = "Hasło musi składać się z co najmniej 8 znaków"
                    return@setOnClickListener
                } else {
                    if (password.text.toString() != password2.text.toString()) {
                        password2.error = "Podane hasła sa różne"
                        return@setOnClickListener
                    }
                }
            }

            if (emailString.isNotEmpty()) {
                if (!Patterns.EMAIL_ADDRESS.matcher(emailString).matches()) {
                    email.error = "To nie jest mail"
                    return@setOnClickListener
                }
            }

            if (emailString.isEmpty() ||
                password.text.toString().isEmpty() ||
                password2.text.toString().isEmpty() ||
                userLogin.isEmpty()
            ) {
                Toast.makeText(activity?.baseContext, "brakuje danych", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val hashed = BCrypt.hashpw(password.text.toString(), BCrypt.gensalt())
            val json = JSONObject(
                mapOf(
                    "login" to userLogin,
                    "email" to emailString,
                    "password" to hashed
                )
            )

            viewModel._isUIActive.postValue(false)

            viewModel.Register(json,::navigateToLoggedFragment)
        }
    }


    fun disableUI(){
        button.isEnabled = false
        login.isEnabled = false
        password.isEnabled = false
        email.isEnabled = false
        password2.isEnabled = false
    }


    fun enableUI(){
        button.isEnabled = true
        login.isEnabled = true
        password.isEnabled = true
        email.isEnabled = true
        password2.isEnabled = true
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedViewModel.enableBackPress = true
        sharedViewModel.backButtonEnd = false
        button = view.findViewById(R.id.button)
        login = view.findViewById(R.id.login)
        email = view.findViewById(R.id.email)
        password = view.findViewById(R.id.password)
        password2 = view.findViewById(R.id.password2)
        spinner = view.findViewById(R.id.progressBar)

        login.doOnTextChanged { text, start, before, count ->
            button.isEnabled = login.text.isNotEmpty() && email.text.isNotEmpty() &&
                    password.text.isNotEmpty() && password2.text.isNotEmpty()
        }

        email.doOnTextChanged { text, start, before, count ->
            button.isEnabled = login.text.isNotEmpty() && email.text.isNotEmpty() &&
                    password.text.isNotEmpty() && password2.text.isNotEmpty()
        }

        password.doOnTextChanged { text, start, before, count ->
            button.isEnabled = login.text.isNotEmpty() && email.text.isNotEmpty() &&
                    password.text.isNotEmpty() && password2.text.isNotEmpty()
        }

        password2.doOnTextChanged { text, start, before, count ->
            button.isEnabled = login.text.isNotEmpty() && email.text.isNotEmpty() &&
                    password.text.isNotEmpty() && password2.text.isNotEmpty()
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