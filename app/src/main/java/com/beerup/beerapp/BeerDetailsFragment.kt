package com.beerup.beerapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.ArraySet
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.google.android.flexbox.FlexboxLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.internal.Util
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class BeerDetailsFragment : Fragment() {
    private var jsonStr: String? = null
    private val mParam2: String? = null
    private var thisView: View? = null
    private var bundle: Bundle? = null
    var rateButton: Button? = null
    var cameraButton: Button? = null
    var spinner: ProgressBar? = null
    var json: JSONObject? = null
    var rateBar: RatingBar? = null
    private val client = OkHttpClient()
    private val JSON = MediaType.parse("application/json; charset=utf-8")
    private lateinit var photoURI: Uri
    private lateinit var mCurrentPhotoPath: String
    private lateinit var uploadUrl: String
    private var uploadText: TextView? = null
    private var addedData: JSONArray? = null
    private var tagList = ArraySet<TagElementView>()
    private var allTagList = ArraySet<TagElementView>()
    private var actualTagList = ArraySet<TagElementView>()
    private var tagButton: Button? = null
    private var stars : Float = 0.0f
    private var beerView: View? = null
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            jsonStr = arguments?.getString("json")
            bundle = arguments?.getBundle("image")
        }

        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult(),
                ActivityResultCallback<ActivityResult>() { it ->
                    if (it.resultCode == AppCompatActivity.RESULT_OK) {
                        val stream = ByteArrayOutputStream()
                        val bundle = it.data?.extras
                        val bitmap = bundle?.get("data") as Bitmap

                        //val bitmap = MediaStore.Images.Media.getBitmap(activity?.contentResolver, photoURI)
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                        uploadText?.visibility = View.VISIBLE
                        spinner?.visibility = View.VISIBLE
                        disableUI()
                        CoroutineScope(Dispatchers.IO).launch {
                            getImageUrl(uploadUrl, stream.toByteArray())
                        }
                    }
                })

        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val navHostFragment = activity?.getSupportFragmentManager()?.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController

                navController.popBackStack()
            }
        })
    }

    private suspend fun sendReview(reviewJson: JSONObject, put: Boolean) {
        var url="${context?.getString(com.beerup.beerapp.R.string.baseUrl)}/reviews"
        var request: Request? = null

        if(put) {
            val formBody: RequestBody = FormBody.create(JSON, reviewJson!!["stars"].toString())
            url += "/${reviewJson!!["login"]}/${reviewJson!!["beer_id"]}"
            request = Request.Builder().url(url).put(formBody).build()
        } else {
            val formBody: RequestBody = RequestBody.create(JSON, reviewJson.toString())
            request = Request.Builder().url(url).post(formBody).build()
        }
        val main = activity as MainActivity
        main.beerid = reviewJson!!["beer_id"].toString()
        main.rating = stars
        json?.put("review", stars.times(2))
        (activity as MainActivity).addNewBeer(json!!)
        val response: Response = client.newCall(request).execute()
        if(this.isVisible) {
            withContext(Dispatchers.Main) {
                if (!put) {
                    (activity as MainActivity).reviedBeers += 1
                }

                spinner!!.visibility = View.GONE
                val rating = beerView?.findViewById<RatingBar>(com.beerup.beerapp.R.id.rating)
                rating?.rating = stars
                Toast.makeText(activity?.baseContext, "Ocena wysłana", Toast.LENGTH_SHORT).show()
            }
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
        var fileError = false
        uploadUrl = url
        var photoFile: File? = null
        try {
            photoFile = createImageFile()
        } catch (e: IOException) {
            fileError = true
            Toast.makeText(activity?.baseContext, "Błąd: " + e.message , Toast.LENGTH_SHORT).show()
        }

        if(!fileError) {
            if (photoFile != null) {
                photoURI =
                    FileProvider.getUriForFile(
                        activity?.baseContext!!,
                        "com.beerup.beerapp",
                        photoFile
                    )

               // cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

                activityResultLauncher.launch(cameraIntent)
                //startActivityForResult(cameraIntent, 123)
            }
        }
    }

    suspend fun getImageUrl(url: String, bitmap: ByteArray) {
        var jsonI: JSONObject? = null
        var serverError = false
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            jsonI = JSONObject(response.body()?.string())
        } catch (e: java.lang.Exception) {
            serverError = true
            Toast.makeText(activity?.baseContext, "Błąd: " + e.message , Toast.LENGTH_SHORT).show()
        }

        if(!serverError) {
            uploadImage(jsonI?.get("content").toString(), bitmap)
        } else {
            enableUI()
        }
    }

    suspend fun incrementPhotoNumber() {
        val url = "${activity?.getString(R.string.baseUrl)}/users/${json!!["user"]}/photos"
        val request = Request.Builder().url(url).post(Util.EMPTY_REQUEST).build()
        val response = client.newCall(request).execute()

        (activity as MainActivity).sentPhotos += 1
    }

    suspend fun uploadImage(url: String, bitmap: ByteArray) {
        val formBody: RequestBody = RequestBody.create(MediaType.parse("image/*jpg"),bitmap)
        val request = Request.Builder()
            .url(url)
            .put(formBody)
            .build()

        val response: Response = client.newCall(request).execute()
        incrementPhotoNumber()

        val fdelete: File = File(photoURI.getPath())
        if (fdelete.exists()) {
            if (fdelete.delete()) {
                Log.i("photofile","file Deleted :" + photoURI.getPath())
            } else {
                Log.i("photofile","file not Deleted :" + photoURI.getPath())
            }
        }

        withContext(Dispatchers.Main) {
            uploadText?.visibility = View.GONE
            spinner?.visibility = View.GONE
            enableUI()
            Toast.makeText(activity?.baseContext, "Zdjęcie wysłane", Toast.LENGTH_SHORT).show()
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
                spinner?.visibility = View.VISIBLE
                disableUI()
                CoroutineScope(Dispatchers.IO).launch {
                    getImageUrl(uploadUrl, stream.toByteArray())
                }
            }
        }
    }

    fun disableUI() {
        if(this.isVisible) {
            val mainActivity = activity as MainActivity
            mainActivity.enableBackPress = false
            rateBar?.isEnabled = false

            rateButton?.isEnabled = false

            tagButton?.isEnabled = false
            cameraButton?.isEnabled = false

            allTagList.forEach {
                it.mView
                    .findViewById<LinearLayout>(com.beerup.beerapp.R.id.taglayout)
                    .isEnabled = false
            }
        }
    }

    fun enableUI() {
        if(this.isVisible) {
            val mainActivity = activity as MainActivity
            mainActivity.enableBackPress = true
            rateBar?.isEnabled = true
            cameraButton?.isEnabled = true

            rateButton?.isEnabled =
                rateBar?.rating!! > 0.0f && rateBar?.rating != stars

            if(tagList.size == 0) {
                tagButton?.isEnabled = false
            } else {
                tagButton?.isEnabled = tagList != actualTagList
            }

            allTagList.forEach {
                it.mView
                    .findViewById<LinearLayout>(com.beerup.beerapp.R.id.taglayout)
                    .isEnabled = true
            }
        }
    }

    fun initButtons() {
        rateBar?.setOnRatingBarChangeListener { ratingBar, fl, b ->
            rateButton?.isEnabled =
                rateBar?.rating!! > 0.0f && rateBar?.rating != stars
        }

        rateButton?.setOnClickListener {
            spinner?.visibility = View.VISIBLE
            val beerint = json!!["beerId"].toString().toInt()
            val realRating = rateBar?.rating as Float
            val reviewJson = JSONObject(
                mapOf(
                    "login" to json!!["user"],
                    "beer_id" to beerint,
                    "stars" to realRating.times(2).toInt()
                )
            )
            stars = rateBar?.rating as Float
            rateButton?.isEnabled = false
            if (json!!["review"].toString().toFloat() > 0.0f) {//put
                CoroutineScope(Dispatchers.IO).launch {
                    sendReview(reviewJson, true)
                }
            } else {//post
                CoroutineScope(Dispatchers.IO).launch {
                    sendReview(reviewJson, false)
                }
            }
        }

        cameraButton?.setOnClickListener {

            val url =
                "${context?.getString(com.beerup.beerapp.R.string.baseUrl)}/beers/${json!!["beerId"]}/image"
            startCamera(url)
        }

        tagButton?.setOnClickListener {
            spinner?.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                sendTags()
            }
        }
    }

    fun viewTag(tag: String, added: Boolean) {
        var tagListLayout: FlexboxLayout? = null
        if(this.isVisible) {
            tagListLayout = thisView?.findViewById(R.id.taglist_layout)
            val tagElement = TagElementView(tag, layoutInflater, this, added)

            if (added) {
                actualTagList.add(tagElement)
                tagList.add(tagElement)
            }

            allTagList.add(tagElement)
            if(this.isVisible) {
                tagListLayout?.addView(tagElement.mView)
            }
        }
    }

    public fun addTag(tagElement: TagElementView) {
        tagList.add(tagElement)

        if(tagList.size == 0) {
            tagButton?.isEnabled = false
        } else {
            tagButton?.isEnabled = tagList != actualTagList
        }
    }

    public fun removeTag(tagElement: TagElementView){
        tagList.remove(tagElement)

        if(tagList.size == 0) {
            tagButton?.isEnabled = false
        } else {
            tagButton?.isEnabled = tagList != actualTagList
        }
    }

    private suspend fun sendTags() {
        var url = "${activity?.getString(R.string.baseUrl)}/reviews/${json!!["user"]}/${json!!["beerId"]}/tags"
        actualTagList.clear()
        actualTagList.addAll(tagList)
        val jsonArray = JSONArray()

        tagList.forEach {
            jsonArray.put(it.tagName.toString())
        }

        val formBody: RequestBody = RequestBody.create(JSON,jsonArray.toString())
        val request = Request.Builder()
            .url(url)
            .put(formBody)
            .build()

        val response: Response = client.newCall(request).execute()

        if(response.code() == 204){
            val main = activity as MainActivity
            main.beerid = json!!["beerId"].toString()
            main.list = jsonArray
            withContext(Dispatchers.Main) {
                tagButton?.isEnabled = false
                spinner?.visibility = View.GONE
                Toast.makeText(activity?.baseContext, "Tagi wysłane", Toast.LENGTH_SHORT).show()
            }
        }
    }

    suspend fun getTags(){
        val url = "${context?.getString(com.beerup.beerapp.R.string.baseUrl)}/beers/tags"

        var jsonTags: JSONObject? = null
        var serverError = false
        try {
            val client = OkHttpClient()
            var request = Request.Builder().url(url).build()
            var response = client.newCall(request).execute()
            jsonTags = JSONObject(response.body()?.string())
        } catch (e: java.lang.Exception) {
            serverError = true
            Toast.makeText(activity?.baseContext, "Błąd: " + e.message , Toast.LENGTH_SHORT).show()
        }

        if(!serverError) {
            if (jsonTags!!.has("content")) {
                val data = JSONArray(jsonTags!!["content"].toString())

                for (i in 0 until data.length()) {
                    val tag = data.getString(i)
                    if(this.isVisible) {
                        withContext(Dispatchers.Main) {
                            viewTag(tag, addedData.toString().contains(tag))
                        }
                    } else{
                        break
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as AppCompatActivity)?.getSupportActionBar()?.hide()
        (activity as MainActivity).enableBackPress = true
        (activity as MainActivity).backButtonEnd = false
        (activity as MainActivity)?.bottomNavigation?.visibility = View.GONE
        thisView = inflater.inflate(R.layout.fragment_beer_details, container, false)

        return thisView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val main = activity as MainActivity
        main.beerid = ""
        main.rating = 0.0f

        json = JSONObject(jsonStr)
        val name = thisView?.findViewById<TextView>(R.id.beername)
        val style = thisView?.findViewById<TextView>(R.id.beerstyle)
        val brewery = thisView?.findViewById<TextView>(R.id.beerbrewery)
        val ibu = thisView?.findViewById<TextView>(R.id.beeribu)
        val abv = thisView?.findViewById<TextView>(R.id.beerabv)
        spinner = thisView?.findViewById<ProgressBar>(R.id.spinner)
        rateButton = thisView?.findViewById<Button>(com.beerup.beerapp.R.id.rate)
        cameraButton = thisView?.findViewById<Button>(com.beerup.beerapp.R.id.sendphoto)
        val photoView = thisView?.findViewById<ImageView>(R.id.beerimage)
        rateBar = thisView?.findViewById<RatingBar>(com.beerup.beerapp.R.id.rating)
        tagButton = thisView?.findViewById(R.id.tagbutton)
        var bitmap: Bitmap? = null

        val arrayInputStream = ByteArrayInputStream(bundle?.get("image") as ByteArray)
        bitmap = BitmapFactory.decodeStream(arrayInputStream)
        name?.text = json!!["name"].toString()
        rateBar?.rating = json!!["review"].toString().toFloat().div(2)
        style?.text = json!!["style"].toString()
        brewery?.text = json!!["brewery"].toString()
        ibu?.text = "IBU: " + json!!["ibu"].toString()
        abv?.text = "ABV: " + json!!["abv"].toString()
        photoView?.setImageBitmap(bitmap)
        addedData = JSONArray(json!!["tags"].toString())

        stars = rateBar?.rating as Float
        CoroutineScope(Dispatchers.IO).launch {
            getTags()
        }
        if(stars == 0.0f) {
            rateButton?.isEnabled = false
        }

        if(json!!["review"].toString().toFloat() > 0.0f) {
            if (rateBar?.rating == json!!["review"].toString().toFloat().div(2)) {
                rateButton?.isEnabled = false
            }
        }

        initButtons()
    }

    companion object {

        fun newInstance(param1: String?, param2: String?): BeerDetailsFragment {
            val fragment = BeerDetailsFragment()
            val args = Bundle()
            args.putString("json", param1)
            fragment.arguments = args
            return fragment
        }
    }
}