package com.beerup.beerapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import com.beerup.beerapp.ViewModels.LoggedViewModel
import com.beerup.beerapp.ViewModels.SharedViewModel


private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


class LoggedFragment : Fragment() {

    private var param2: String? = null
    private lateinit var spinner: ProgressBar
    private lateinit var thisView: View
    private var jsonStr: String = ""

    private lateinit var viewModel: LoggedViewModel
    private lateinit var sharedViewModel: SharedViewModel

    private lateinit var photosText: TextView
    private lateinit var reviewText: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var userLogin: String = ""
        arguments?.let {
            userLogin = it.getString("userLogin").toString()
            jsonStr = it.getString("StatJson").toString()
        }

        viewModel = ViewModelProvider(this).get(LoggedViewModel::class.java)
        viewModel.baseUrl = getString(R.string.baseUrl)
        viewModel.errorCallback = ::errorCallback
        viewModel.userLogin = userLogin

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        viewModel.sharedViewModel = sharedViewModel
        sharedViewModel.userLogin = userLogin

        if(sharedViewModel._beerList.value == null) {
            viewModel.getStatistics(::statisticsCallback)
        } else {
            viewModel.showStats = true
        }
    }


    private fun statisticsCallback(success: Boolean) {
        if(success) {
            showStatistics()
        } else {
            retryButton()
        }
    }


    private fun errorCallback(message: String) {
        Toast.makeText(activity?.baseContext,
            message,
            Toast.LENGTH_LONG
        ).show()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //(activity as AppCompatActivity)?.getSupportActionBar()?.show()
        sharedViewModel.enableBackPress = false
        sharedViewModel.backButtonEnd = true
        (activity as MainActivity).bottomNavigation?.visibility = View.VISIBLE
        thisView = inflater.inflate(R.layout.fragment_logged, container, false)

        return thisView
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.enableBackPress = false

        photosText = thisView.findViewById(R.id.photoNumber)
        reviewText = thisView.findViewById(R.id.reviewNumber)

        spinner = thisView.findViewById(R.id.progressBar2)

        sharedViewModel._photos.observe(viewLifecycleOwner) {
            photosText.text = sharedViewModel._photos.value
        }

        sharedViewModel._reviewedBeers.observe(viewLifecycleOwner) {
            reviewText.text = sharedViewModel._reviewedBeers.value
        }

        if(viewModel.showStats) {
            childFragmentManager.fragments.forEach {
                childFragmentManager.beginTransaction().remove(it).commit()
            }
            showStatistics()
        }
    }


    private fun retryButton() {
        var retryButton = thisView.findViewById<Button>(R.id.retrybutton)
        (activity as AppCompatActivity).supportActionBar?.hide()
        retryButton?.visibility = View.VISIBLE
        spinner.visibility = View.GONE
        thisView.findViewById<TextView>(R.id.beertext).visibility = View.GONE
        thisView.findViewById<RelativeLayout>(R.id.userinfo).visibility = View.GONE
        retryButton?.setOnClickListener {
            retryButton.visibility = View.GONE
            spinner.visibility = View.VISIBLE
            (activity as AppCompatActivity).supportActionBar?.show()
            thisView.findViewById<RelativeLayout>(R.id.userinfo).visibility = View.VISIBLE
            thisView.findViewById<TextView>(R.id.beertext).visibility = View.VISIBLE

            if(sharedViewModel._beerList.value == null) {
                sharedViewModel._beerList.value = BeerList()
                viewModel.getStatistics(::statisticsCallback)
            } else {
                sharedViewModel._beerList.value?.list?.clear()
                viewModel.getStatistics(::statisticsCallback)
            }
        }
    }


    private fun showStatistics() {
        for (i in 0 until sharedViewModel.beerList().value?.list!!.size) {
            if(this.isVisible) {
                val beerFragment = BeerElementFragment()

                val args = Bundle()
                args.putString("userLogin", sharedViewModel.userLogin)
                args.putString(
                    "beer",
                    sharedViewModel.beerList().value?.list!![i].toJson().toString()
                )
                args.putBoolean("isThisBeerList", false)
                beerFragment.arguments = args;
                val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
                transaction.add(R.id.lastreviewed, beerFragment).commit()
            }
        }
    }


    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            LoggedFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}