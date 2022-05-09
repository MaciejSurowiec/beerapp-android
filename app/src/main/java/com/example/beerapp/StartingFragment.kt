package com.example.beerapp

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class StartingFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private suspend fun getStatistic(userLogin: String, navController: NavController) {
        val url = "${getString(R.string.baseUrl)}/users/${userLogin}/statistics"
        var json: JSONObject? = null
        try {
            val client = OkHttpClient()
            var request = Request.Builder().url(url).build()
            var response = client.newCall(request).execute()
            json = JSONObject(response.body()?.string())
        } catch (e: java.lang.Exception) {
            json = JSONObject(mapOf("error" to "error"))
        }

        withContext(Dispatchers.Main) {
            val action = StartingFragmentDirections.actionStartingFragmentToLoggedFragment(
                userLogin,
                json.toString()
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
        // Inflate the layout for this fragment
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