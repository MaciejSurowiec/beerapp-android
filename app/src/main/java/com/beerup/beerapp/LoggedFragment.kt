package com.beerup.beerapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [LoggedFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoggedFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var userLogin: String? = null
    private var param2: String? = null
    private var spinner: ProgressBar? = null
    private var thisView: View? = null
    private var jsonStr: String = ""


    suspend fun getStatistics(){
        val url = "${getString(R.string.baseUrl)}/users/${userLogin}/statistics"
        var response: Response? = null
        var json: JSONObject? = null
        var serverError = false
        try {
            var client = OkHttpClient()
            var request = Request.Builder().url(url).build()
            var response = client.newCall(request).execute()
            jsonStr = response.body()?.string().toString()
        } catch (e: java.lang.Exception) {
            serverError = true
        }

        if(!serverError) {
            withContext(Dispatchers.Main) {
                (activity as MainActivity).mainMenu?.findItem(R.id.logout)?.isEnabled = true
                (activity as MainActivity).mainMenu?.findItem(R.id.beerlist)?.isEnabled = true
                (activity as MainActivity).mainMenu?.findItem(R.id.about)?.isEnabled = true
                spinner?.visibility = View.GONE
                if(jsonStr.isNotEmpty()) {
                    val json = JSONObject(jsonStr)
                    var content = json?.get("content") as JSONObject
                    val data = content.getJSONArray("lastThreeReviews")
                    (activity as MainActivity).sentPhotos =
                        content["numberOfPhotos"].toString().toInt()
                    (activity as MainActivity).reviedBeers =
                        content["numberOfReviews"].toString().toInt()
                    for (i in 0 until data.length()) {
                        val beer = data.getJSONObject(i)
                        val view = BeerElementView(
                            getLayoutInflater(), beer,
                            userLogin!!, spinner!!,
                            requireActivity().baseContext, this@LoggedFragment
                        )
                        (activity as MainActivity).beerList.add(view)
                    }

                    showStatistics()
                } else {
                    retryButton()
                }
            }
        } else {
            withContext(Dispatchers.Main) {
               retryButton()
            }
        }
    }

    private fun retryButton() {
        var retryButton = thisView?.findViewById<Button>(R.id.retrybutton)
        (activity as AppCompatActivity)?.getSupportActionBar()?.hide()
        retryButton?.visibility = View.VISIBLE
        spinner?.visibility = View.GONE
        thisView?.findViewById<TextView>(R.id.beertext)?.visibility = View.GONE
        thisView?.findViewById<RelativeLayout>(R.id.userinfo)?.visibility = View.GONE
        retryButton?.setOnClickListener {
            retryButton?.visibility = View.GONE
            spinner?.visibility = View.VISIBLE
            (activity as AppCompatActivity)?.getSupportActionBar()?.show()
            thisView?.findViewById<RelativeLayout>(R.id.userinfo)?.visibility = View.VISIBLE
            thisView?.findViewById<TextView>(R.id.beertext)?.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                getStatistics()
            }
        }
    }

    private fun showStatistics() {
        thisView?.findViewById<TextView>(R.id.reviewNumber)?.text = (activity as MainActivity).reviedBeers.toString()
        thisView?.findViewById<TextView>(R.id.photoNumber)?.text = (activity as MainActivity).sentPhotos.toString()

        val mainLayout = thisView?.findViewById<LinearLayout>(R.id.lastreviewed)
        mainLayout?.removeAllViews()
        for (i in 0 until (activity as MainActivity).beerList.size) {
            if((activity as MainActivity).beerList[i].notInList) {
                (activity as MainActivity).beerList[i].addToBeerList(spinner!!,this@LoggedFragment)
            }
            if(this.isVisible) {
                mainLayout?.addView((activity as MainActivity).beerList[i].mView)
                (activity as MainActivity).beerList[i].setImage()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userLogin = it.getString("userLogin")
            jsonStr = it.getString("StatJson").toString()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //(activity as AppCompatActivity)?.getSupportActionBar()?.show()
        (activity as MainActivity).backButtonEnd = true
        (activity as MainActivity)?.bottomNavigation?.visibility = View.VISIBLE
        thisView = inflater.inflate(R.layout.fragment_logged, container, false)

        return thisView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val main = activity as MainActivity
        if(main.beerid != "") {
            for (i in 0 until main.beerList.size) {
                if (main.beerList.get(i).id.equals(main.beerid)) {
                    if (main.rating != 0.0f) {
                        main.beerList.get(i).updateRate(main.rating)
                        main.rating = 0.0f
                    }

                    if (main.list != null) {
                        main.beerList.get(i).tags = main.list
                        main.list = null
                    }

                    main.beerid = ""
                    break
                }
            }
        }

        spinner = thisView?.findViewById(R.id.progressBar2)
        if(main.beerList.size == 0) {
            if(jsonStr.isNotEmpty()) {
                val json = JSONObject(jsonStr)
                var content = json?.get("content") as JSONObject
                val data = content.getJSONArray("lastThreeReviews")
                (activity as MainActivity).sentPhotos = content["numberOfPhotos"].toString().toInt()
                (activity as MainActivity).reviedBeers = content["numberOfReviews"].toString().toInt()
                for (i in 0 until data.length()) {
                    val beer = data.getJSONObject(i)
                    val view = BeerElementView(
                        getLayoutInflater(), beer,
                        userLogin!!, spinner!!,
                        requireActivity().baseContext, this@LoggedFragment
                    )
                    (activity as MainActivity).beerList.add(view)
                }
                if(this.isVisible) {
                    showStatistics()
                }
            } else {
                (activity as MainActivity).mainMenu?.findItem(R.id.logout)?.isEnabled = false
                (activity as MainActivity).mainMenu?.findItem(R.id.beerlist)?.isEnabled = false
                (activity as MainActivity).mainMenu?.findItem(R.id.about)?.isEnabled = false
                spinner?.visibility = View.VISIBLE
                CoroutineScope(Dispatchers.IO).launch {
                    getStatistics()
                }
            }
        }
        else {
            showStatistics()
        }

        val navHostFragment = activity?.getSupportFragmentManager()?.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val mainActivity = activity as MainActivity
        mainActivity.enableBackPress = false
    }

    fun removeBeerElements() {
        val mainLayout = thisView?.findViewById<LinearLayout>(R.id.lastreviewed)

        for (i in 0 until (activity as MainActivity).beerList.size) {
            mainLayout?.removeView((activity as MainActivity).beerList.get(i).mView)
        }
    }

    override fun onDestroyView() {
        removeBeerElements()

        super.onDestroyView()
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