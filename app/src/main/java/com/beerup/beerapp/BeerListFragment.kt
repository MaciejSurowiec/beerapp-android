package com.beerup.beerapp

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.util.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BeerListFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BeerListFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var userLogin: String? = null
    private var thisView: View? = null
    private var spinner: ProgressBar? = null
    private var search: SearchView? = null
    private var loading: Boolean = false
    public var actualStart:Int = 0
    private var query: String = ""
    var scroll: BetterScrollView? = null
    private val client = OkHttpClient()
    var showed: Boolean = false
    public var beerList = ArrayList<BeerElementView>()
    var dontResetsearch = true
    public var endOfDataShowed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userLogin = it.getString("userLogin")
            param2 = it.getString(ARG_PARAM2)
        }

        CoroutineScope(Dispatchers.IO).launch {
            downloadList()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as AppCompatActivity)?.getSupportActionBar()?.hide()
        (activity as MainActivity)?.bottomNavigation?.visibility = View.VISIBLE
        thisView = inflater.inflate(R.layout.fragment_beer_list, container, false)
        val mainActivity = activity as MainActivity
        mainActivity.enableBackPress = true
        return thisView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val main = activity as MainActivity
        main.backButtonEnd = false
        if(main.beerid != "") {
            for (i in 0 until beerList.size) {
                if(beerList.get(i).id.equals(main.beerid)) {
                    if(main.rating != 0.0f) {
                        beerList.get(i).updateRate(main.rating)
                        main.rating = 0.0f
                    }

                    if(main.list != null) {
                        beerList.get(i).tags = main.list
                        main.list = null
                    }

                    main.beerid = ""
                    break
                }
            }
        }

        spinner = thisView?.findViewById(R.id.progressBar)
        spinner!!.visibility = View.VISIBLE
        scroll = thisView?.findViewById(R.id.scrollable)
        scroll!!.beerList = this
        search = thisView?.findViewById(R.id.searchbar)
        dontResetsearch = true
        if(beerList.size > 0) {
            showList()
        }
        searchEngine()
    }

    fun reload() {
        spinner!!.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            downloadList()
            withContext(Dispatchers.Main) {
                showList(actualStart)
            }
        }
    }

    private fun fixUrl(str: String):String {
        var output = str.replace("%","%25")
            .replace(" ","%20")
            .replace("+","%2B")
            .replace("/","%2F")
            .replace("\\","%5C")
            .replace("$","%24")
            .replace("!","%21")
            .replace("?","%3F")
            .replace("#","%23")
            .replace("&","%26")
            .replace("=","%3D")
            .replace("(","%28")
            .replace(")","%29")
            .replace("-","%2D")
            .replace("\'","dd")
            .replace("\"'","")
            .replace("@","%40")
            .replace("^","%5E")
            .replace("*","%2A")
            .replace("(","%28")
            .replace(")","%29")
            .replace("{","%7B")
            .replace("}","%7D")
            .replace("[","%5B")
            .replace("]","%5D")
            .replace("|","%7C")
            .replace("<","%3C")
            .replace(">","%3E")
            .replace(",","%2C")
            .replace(".","%2E")
            .replace(":","%3A")
            .replace("_","%5F")
            .replace(";","%3B")
            .replace("`","%60")
            .replace("~","%7E")
            .replace("[^A-Z%a-z0-9 ]","")

        return output
    }


    fun searchEngine() {
        search?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                if(!dontResetsearch) {
                    endOfDataShowed = false
                    actualStart = 0
                    beerList.clear()
                    query = fixUrl(newText)
                    spinner?.visibility = View.VISIBLE
                    if(!loading) {
                        loading = true
                        CoroutineScope(Dispatchers.IO).launch {
                            withContext(Dispatchers.Main) {
                                removeBeerElements()
                                activity?.findViewById<LinearLayout>(R.id.beerlist_layout)?.removeAllViews()
                                beerList.clear()
                            }
                            downloadList()
                            withContext(Dispatchers.Main) {
                                showList()
                            }
                        }
                    }
                } else {
                    dontResetsearch = false
                }

                return false
            }

            override fun onQueryTextSubmit(querys: String): Boolean {
                actualStart = 0
                query = fixUrl(querys)
                spinner?.visibility = View.VISIBLE
                if(!loading) {
                    loading = true
                    endOfDataShowed = false
                    CoroutineScope(Dispatchers.IO).launch {
                        withContext(Dispatchers.Main) {
                            removeBeerElements()
                            activity?.findViewById<LinearLayout>(R.id.beerlist_layout)?.removeAllViews()
                            beerList.clear()
                        }
                        downloadList()
                        withContext(Dispatchers.Main) {
                            showList()
                        }
                    }
                }
                return false
            }
        })
    }

    suspend fun downloadList(){
        val url = "${activity?.getString(R.string.baseUrl)}/beers?limit=10&queryPhrase=${query}&start=${actualStart}&login=${userLogin}"
        var serverError = false

        var json: JSONObject? = null
        try {
            var request = Request.Builder().url(url).build()
            var response = client.newCall(request).execute()
            json = JSONObject(response.body()?.string())
        } catch (e: java.lang.Exception) {
            serverError = true
        }

        if(!serverError) {
            val data = json?.getJSONArray("content")
            if(this.isVisible) {
                withContext(Dispatchers.Main) {
                    search?.isEnabled = true
                }
            }
            if (data?.length() == 0) {
                if (!endOfDataShowed && beerList.size == 0) {
                    if(this.isVisible) {
                        withContext(Dispatchers.Main) {
                            val view = BeerElementView(
                                layoutInflater, JSONObject(),
                                userLogin!!, spinner!!, requireActivity().baseContext,
                                this@BeerListFragment, true
                            )
                            beerList?.add(view)
                        }
                    }
                }
                endOfDataShowed = true
            } else {
                for (i in 0 until data!!.length()) {
                    if(this.isVisible) {
                        val beer = data?.getJSONObject(i)
                        val view = BeerElementView(
                            layoutInflater, beer,
                            userLogin!!, spinner!!,
                            requireActivity().baseContext, this@BeerListFragment
                        )
                        beerList?.add(view)
                    }
                }
            }
            if (!showed) {
                withContext(Dispatchers.Main) {
                    showList()
                }
            }
        } else {
            if(this.isVisible) {
                withContext(Dispatchers.Main) {
                    var button = thisView?.findViewById<Button>(R.id.downloadlist)
                    button?.visibility = View.VISIBLE
                    search?.visibility = View.GONE
                    spinner?.visibility = View.GONE
                    button?.setOnClickListener {
                        CoroutineScope(Dispatchers.IO).launch {
                            downloadList()
                        }
                        button?.visibility = View.GONE
                        search?.visibility = View.VISIBLE
                        search?.isEnabled = false
                        spinner?.visibility = View.VISIBLE
                        showed = false
                    }
                }
            }
        }
    }

    fun removeBeerElements() {
        val mainLayout = thisView?.findViewById<LinearLayout>(R.id.beerlist_layout)

        for (i in 0 until beerList.size) {
            mainLayout?.removeView(beerList.get(i).mView)
        }
    }


    override fun onDestroyView() {
        removeBeerElements()

        super.onDestroyView()
    }

    fun showList(start: Int = 0) {
        val mainLayout = thisView?.findViewById<LinearLayout>(R.id.beerlist_layout)

        for (i in start until beerList.size) {
            if(this.isVisible) {
                mainLayout?.addView(beerList.get(i).mView)
                beerList.get(i).setImage()
            }
        }
        if(this.isVisible) {
            showed = true

            spinner!!.visibility = View.GONE
            if (scroll!!.loading) {
                scroll!!.loading = false
            }

            if (loading) {
                loading = false
            }
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            BeerListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}