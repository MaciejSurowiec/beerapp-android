package com.example.beerapp

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.internal.Util.EMPTY_REQUEST
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
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
    private lateinit var photoURI: Uri
    private lateinit var mCurrentPhotoPath: String
    private lateinit var uploadUrl: String
    private var uploadSpinner: ProgressBar? = null
    private var uploadText: TextView? = null

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userLogin = it.getString("userLogin")
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        (activity as AppCompatActivity)?.getSupportActionBar()?.hide()
        thisView = inflater.inflate(R.layout.fragment_beer_list, container, false)
        spinner = thisView?.findViewById(R.id.progressBar)
        spinner!!.visibility = View.VISIBLE
        scroll = thisView?.findViewById(R.id.scrollable)
        scroll!!.beerList = this
        search = thisView?.findViewById(R.id.searchbar)
        uploadSpinner = thisView?.findViewById(R.id.uploadProgress)
        uploadText = thisView?.findViewById(R.id.uploadText)
        searchEngine()
        CoroutineScope(Dispatchers.IO).launch {
            showList()
        }
        return thisView
    }

    fun reload() {
        spinner!!.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            showList()
        }
    }


    @Throws(IOException::class)
    private fun createImageFile(): File? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )

        mCurrentPhotoPath = image.absolutePath
        return image
    }

    fun startCamera(url: String){
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        uploadUrl = url
        var photoFile: File? = null
        try {
            photoFile = createImageFile()
        } catch (ex: IOException) {
        }


        if (photoFile != null) {
            photoURI =
                FileProvider.getUriForFile(activity?.baseContext!! , "com.example.beerapp", photoFile)

            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            startActivityForResult(cameraIntent, 123)
        }
    }

    suspend fun getImageUrl(url: String, bitmap: ByteArray) {
        var json: JSONObject? = null
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            json = JSONObject(response.body()?.string())
        } catch (e: java.lang.Exception) {
            json = JSONObject(mapOf("content" to "error"))
        }

        uploadImage(json?.get("content").toString(), bitmap)
    }

    suspend fun incrementPhotoNumber() {
        val url = "${activity?.getString(R.string.baseUrl)}/users/${userLogin}/photos"
        val request = Request.Builder().url(url).post(EMPTY_REQUEST).build()
        val response = client.newCall(request).execute()

    }


    suspend fun uploadImage(url: String, bitmap: ByteArray){
         val formBody: RequestBody = RequestBody.create(MediaType.parse("image/*jpg"),bitmap)
         val request = Request.Builder()
            .url(url)
            .put(formBody)
            .build()

        val response: Response = client.newCall(request).execute()

        incrementPhotoNumber()

        withContext(Dispatchers.Main) {
            uploadText?.visibility = View.GONE
            uploadSpinner?.visibility = View.GONE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                val stream = ByteArrayOutputStream()
                val bitmap = MediaStore.Images.Media.getBitmap(activity?.getContentResolver(), photoURI)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                uploadText?.visibility = View.VISIBLE
                uploadSpinner?.visibility = View.VISIBLE
                CoroutineScope(Dispatchers.IO).launch {
                    getImageUrl(uploadUrl, stream.toByteArray())
                }
            }
        }
    }

    fun searchEngine() {
        search?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                if(newText.length >= 3) {
                    actualStart = 0
                    query = newText.replace(" ","%20")

                    spinner?.visibility = View.VISIBLE
                    if(!loading) {
                        loading = true
                        CoroutineScope(Dispatchers.IO).launch {
                            withContext(Dispatchers.Main) {
                                activity?.findViewById<LinearLayout>(R.id.beerlist_layout)
                                    ?.removeAllViews()
                            }
                            showList()
                        }
                    }
                }
                return false
            }

            override fun onQueryTextSubmit(querys: String): Boolean {
                actualStart = 0
                query = querys.replace(" ","%20")
                val json = JSONObject(mapOf("queryPhrase" to query,"login" to userLogin,"start" to actualStart))
                spinner?.visibility = View.VISIBLE
                if(!loading) {
                    loading = true
                    CoroutineScope(Dispatchers.IO).launch {
                        withContext(Dispatchers.Main) {
                            activity?.findViewById<LinearLayout>(R.id.beerlist_layout)
                                ?.removeAllViews()
                        }
                        showList()
                    }
                }
                return false
            }
        })
    }

    suspend fun downloadList(): JSONObject{
        val url = "${activity?.getString(R.string.baseUrl)}/beers?limit=10&queryPhrase=${query}&start=${actualStart}&login=${userLogin}"

        var json: JSONObject? = null
        try {
            var request = Request.Builder().url(url).build()
            var response = client.newCall(request).execute()
            json = JSONObject(response.body()?.string())
        } catch (e: java.lang.Exception) {
            null
            json = JSONObject(mapOf("error" to "error"))
        }

        return json!!
    }

    suspend fun showList() {
        val json = downloadList()
        withContext(Dispatchers.Main) {
            val mainLayout = thisView?.findViewById<LinearLayout>(R.id.beerlist_layout)
            val data = json.getJSONArray("content")
            if(data.length() == 0) {
                Toast.makeText(activity?.baseContext, "Brak danych", Toast.LENGTH_LONG).show()
            }

            for (i in 0 until data.length()) {
                val beer = data.getJSONObject(i)
                val view = BeerElementView(
                    layoutInflater, beer,
                    userLogin!!, spinner!!,
                    requireActivity().baseContext, this@BeerListFragment
                )
                mainLayout?.addView(view.mView)
            }

            spinner!!.visibility = View.GONE
            if(scroll!!.loading){
                scroll!!.loading = false
            }

            if(loading){
                loading = false
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment BeerListFragment.
         */
        // TODO: Rename and change types and number of parameters
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