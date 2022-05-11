package com.example.beerapp

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
        try {
            var client = OkHttpClient()
            var request = Request.Builder().url(url).build()
            var response = client.newCall(request).execute()
            json = JSONObject(response.body()?.string())
        } catch (e: java.lang.Exception) {
            null
        }

        //jsonStr = json?.get("content").toString()
        var content = json?.get("content") as JSONObject

        val data = content.getJSONArray("lastThreeReviews")

        withContext(Dispatchers.Main) {
            thisView?.findViewById<TextView>(R.id.welcome)?.text = "Witaj ${userLogin}"
            thisView?.findViewById<TextView>(R.id.reviewNumber)?.text = "Liczba ocenionych piw: ${content["numberOfReviews"].toString()}"
            thisView?.findViewById<TextView>(R.id.photoNumber)?.text = "Liczba wysłanych zdjęć: ${content["numberOfPhotos"].toString()}"
            val mainLayout = thisView?.findViewById<LinearLayout>(R.id.lastreviewed)

            for (i in 0 until data.length()) {
                val beer = data.getJSONObject(i)
                val view = BeerElementView(
                    getLayoutInflater(), beer,
                    userLogin!!, spinner!!,
                    requireActivity().baseContext
                )
                mainLayout?.addView(view.mView)
            }
        }
    }

    private fun showStatistics() {
        val json = JSONObject(jsonStr)
        var content = json?.get("content") as JSONObject
        thisView?.findViewById<TextView>(R.id.welcome)?.text = "Witaj ${userLogin}"
        thisView?.findViewById<TextView>(R.id.reviewNumber)?.text = "Liczba ocenionych piw: ${content["numberOfReviews"].toString()}"
        thisView?.findViewById<TextView>(R.id.photoNumber)?.text = "Liczba wysłanych zdjęć: ${content["numberOfPhotos"].toString()}"
        val mainLayout = thisView?.findViewById<LinearLayout>(R.id.lastreviewed)

        val data = content.getJSONArray("lastThreeReviews")
        for (i in 0 until data.length()) {
            val beer = data.getJSONObject(i)
            val view = BeerElementView(
                getLayoutInflater(), beer,
                userLogin!!, spinner!!,
                requireActivity().baseContext
            )
            mainLayout?.addView(view.mView)
        }
        jsonStr = ""
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
        (activity as AppCompatActivity)?.getSupportActionBar()?.show()
        thisView = inflater.inflate(R.layout.fragment_logged, container, false)
        spinner = thisView?.findViewById(R.id.progressBar2)

        if(jsonStr.isEmpty()){
            CoroutineScope(Dispatchers.IO).launch {
                getStatistics()
            }
        }
        else {
            showStatistics()
        }
        // Inflate the layout for this fragment
        return thisView
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment LoggedFragment.
         */
        // TODO: Rename and change types and number of parameters
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