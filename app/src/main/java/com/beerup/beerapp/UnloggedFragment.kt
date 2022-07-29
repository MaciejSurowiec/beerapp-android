package com.beerup.beerapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class UnloggedFragment : Fragment() {
    // TODO: Rename and change types of parameter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =  inflater.inflate(R.layout.fragment_unlogged, container, false)
        (activity as AppCompatActivity)?.getSupportActionBar()?.hide()
        (activity as MainActivity).backButtonEnd = true
        (activity as MainActivity)?.bottomNavigation?.visibility = View.GONE
        val registerButton = view?.findViewById<Button>(R.id.register)
        val loginButton = view?.findViewById<Button>(R.id.login)
        val aboutButton = view?.findViewById<Button>(R.id.aboutbutton)
        val navHostFragment = activity?.getSupportFragmentManager()?.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        registerButton?.setOnClickListener {
            val action = UnloggedFragmentDirections.actionUnloggedFragmentToRegisterFragment()
            navController.navigate(action)
        }

        loginButton?.setOnClickListener {
            val action = UnloggedFragmentDirections.actionUnloggedFragmentToLoginFragment()
            navController.navigate(action)
        }

        aboutButton?.setOnClickListener {
            val action = UnloggedFragmentDirections.actionUnloggedFragmentToAboutFragment()
            navController.navigate(action)
        }

        val mainActivity = activity as MainActivity
        mainActivity.enableBackPress = false
        // Inflate the layout for this fragment
        return view
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            UnloggedFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}