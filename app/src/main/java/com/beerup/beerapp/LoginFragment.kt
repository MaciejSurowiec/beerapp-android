package com.beerup.beerapp

import android.app.Application
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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.beerup.beerapp.ViewModels.LoginViewModel
import com.beerup.beerapp.ViewModels.SharedViewModel
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


class LoginFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var viewModel: LoginViewModel

    private lateinit var button: Button
    private lateinit var login: EditText
    private lateinit var password: EditText
    private lateinit var spinner: ProgressBar

    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)
        viewModel.baseUrl = getString(R.string.baseUrl)
        viewModel.errorCallback = ::errorCallback
        viewModel.passwordError = ::passwordError
        viewModel.loginError = ::loginError
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


    private fun navigateToLoggedFragment(userLogin: String) {  // you need to navigate from fragment level cause this is "ui logic" xd
        val navHostFragment = activity?.supportFragmentManager
            ?.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val action =
            LoginFragmentDirections.actionLoginFragmentToLoggedFragment(userLogin)
        navController.navigate(action)
    }


    private fun initButton() {
        button.setOnClickListener {
            var loginText = login.text.toString()
            if(loginText.contains(" ")) {
                if(loginText.last() == ' ') {
                    loginText = loginText.dropLast(1)
                    if(loginText.contains(" ")) {
                        login.error = "login nie może zawierać spacji"
                        return@setOnClickListener
                    }
                } else {
                    login.error = "login nie może zawierać spacji"
                    return@setOnClickListener
                }
            }

            disableUI()
            spinner.visibility = View.VISIBLE

            viewModel.checkPassword(
                loginText,
                password.text.toString(),
                ::navigateToLoggedFragment
            )
        }
    }


    fun passwordError() {
        password.error = "Błędne hasło"
    }

    fun loginError() {
        login.error = "Błędny login"
    }

    private fun errorCallback(message: String) {
        Toast.makeText(activity?.baseContext,
            message,
            Toast.LENGTH_LONG
        ).show()
    }


    private fun disableUI(){
        button.isEnabled = false
        login.isEnabled = false
        password.isEnabled = false
    }


    private fun enableUI(){
        button.isEnabled = true
        login.isEnabled = true
        password.isEnabled = true
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
        sharedViewModel.enableBackPress = true
        sharedViewModel.backButtonEnd = false

        login.doOnTextChanged { text, start, before, count ->
            button.isEnabled = login.text.isNotEmpty() &&  password.text.isNotEmpty()
        }

        password.doOnTextChanged { text, start, before, count ->
            button.isEnabled = login.text.isNotEmpty() &&  password.text.isNotEmpty()
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