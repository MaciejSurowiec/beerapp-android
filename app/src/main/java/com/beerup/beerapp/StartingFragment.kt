package com.beerup.beerapp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class StartingFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private suspend fun getStatistic(userLogin: String, navController: NavController) {
        val url = "${getString(R.string.baseUrl)}/users/${userLogin}/statistics"
        var jsonStr = ""
        try {
            val client = OkHttpClient()
            var request = Request.Builder().url(url).build()
            var response = client.newCall(request).execute()
        } catch (e: java.lang.Exception) {
            Log.i("start",e.message.toString())
        }

        withContext(Dispatchers.Main) {
            val action = StartingFragmentDirections.actionStartingFragmentToLoggedFragment(
                userLogin,
                jsonStr // if there was an error empty string will be also fine
            )
            navController.navigate(action)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = activity?.getSharedPreferences("userInfo", AppCompatActivity.MODE_PRIVATE)

        val navHostFragment = activity?.getSupportFragmentManager()?.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        if(sharedPref!!.contains("userLogin")) {

            var userLogin = sharedPref!!.getString("userLogin",null).toString()
            CoroutineScope(Dispatchers.IO).launch {
                getStatistic(userLogin, navController)
            }

        } else {
            val action = StartingFragmentDirections.actionStartingFragmentToUnloggedFragment()
            navController.navigate(action)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as AppCompatActivity)?.getSupportActionBar()?.hide()
        (activity as MainActivity)?.bottomNavigation?.visibility = View.GONE

        return inflater.inflate(R.layout.fragment_starting, container, false)
    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            StartingFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}