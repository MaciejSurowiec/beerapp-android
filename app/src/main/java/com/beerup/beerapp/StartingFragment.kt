package com.beerup.beerapp

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.beerup.beerapp.ViewModels.SharedViewModel
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

    private lateinit var sharedViewModel: SharedViewModel
    lateinit var retryButton: Button
    lateinit var image: ImageView
    lateinit var text: TextView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = activity?.getSharedPreferences("userInfo", AppCompatActivity.MODE_PRIVATE)

        val navHostFragment = activity?.supportFragmentManager?.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        if(sharedPref!!.contains("userLogin")) {
            val userLogin = sharedPref!!.getString("userLogin", null).toString()
            sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
            sharedViewModel.userLogin = userLogin
            sharedViewModel.getStatistics()
            sharedViewModel.getTags()
        } else {
            val action = StartingFragmentDirections.actionStartingFragmentToUnloggedFragment()
            navController.navigate(action)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as AppCompatActivity).supportActionBar?.hide()
        (activity as MainActivity).bottomNavigation?.visibility = View.GONE

        return inflater.inflate(R.layout.fragment_starting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        retryButton = view.findViewById(R.id.retrytagsbutton)
        text = view.findViewById(R.id.startingtext)
        image = view.findViewById(R.id.startingimage)

        retryButton.setOnClickListener{
            sharedViewModel._statsDownloaded.postValue(false)
            sharedViewModel._tagsDownloaded.postValue(false)
            sharedViewModel._tagsError.postValue(false)

            sharedViewModel.getStatistics()
            sharedViewModel.getTags()
        }

        sharedViewModel._statsDownloaded.observe(viewLifecycleOwner) {
            if(it and sharedViewModel._tagsDownloaded.value!!) {
                val navHostFragment =
                    activity?.supportFragmentManager?.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController
                val action =
                    StartingFragmentDirections.actionStartingFragmentToLoggedFragment(sharedViewModel.userLogin)
                navController.navigate(action)
            }
        }

        sharedViewModel._tagsDownloaded.observe(viewLifecycleOwner) {
            if(it and sharedViewModel._statsDownloaded.value!!) {
                val navHostFragment =
                    activity?.supportFragmentManager?.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController
                val action =
                    StartingFragmentDirections.actionStartingFragmentToLoggedFragment(sharedViewModel.userLogin)
                navController.navigate(action)
            }
        }

        sharedViewModel._tagsError.observe(viewLifecycleOwner) {
            showRetryButton(it)
        }
    }

    fun showRetryButton(show: Boolean) {
        if(show) {
            retryButton.visibility = View.VISIBLE
            text.visibility = View.GONE
            image.visibility = View.GONE
        } else {
            retryButton.visibility = View.GONE
            text.visibility = View.VISIBLE
            image.visibility = View.VISIBLE
        }
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