package com.beerup.beerapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream


class BeerElementView {
    public lateinit var mView: View
    public var id = ""
    private var context: Context? = null
    private var activitySpinner: ProgressBar? = null
    private var imageSpinner: ProgressBar? = null
    private var bitmap: Bitmap? = null
    private var myBitmap: Bitmap? = null
    private var imageSet = false
    public var rating:RatingBar? = null
    public var mRate = 0.0f
    public var tags: JSONArray? = null
    public var justIndicator = false
    private var noPhoto = false
    private var photoUrl = ""
    private var userLogin = ""
    private var beerJson: JSONObject? = null
    public var notInList = true

    public fun updateRate(rate: Float){
        mRate = rate
        rating?.rating = mRate
        beerJson?.put("review", mRate.times(2))
        rating?.setIsIndicator(false)
        val id = beerJson!!["beerId"].toString().toInt()
        rating?.id =  id * id * id * id + 2137 + (mRate * 4).toInt()
        rating?.invalidate()
        rating?.setIsIndicator(true)
    }

    suspend fun downloadImage(url: String) {
        noPhoto = false
        try {
            var client = OkHttpClient()
            var request = Request.Builder().url(url).build()
            var response = client.newCall(request).execute()
            myBitmap = BitmapFactory.decodeStream(response.body()?.byteStream())
        } catch (e: java.lang.Exception) {
            noPhoto = true
            Log.i("image",url)
            withContext(Dispatchers.Main) {
                mView.findViewById<TextView>(com.beerup.beerapp.R.id.nophoto).visibility =
                    View.VISIBLE
            }
        }

        if(!noPhoto) {
            if (!imageSet) {
                withContext(Dispatchers.Main) {
                    setImage()
                }
            }
        }
    }

    public fun setImage() {
        if(noPhoto) {
            CoroutineScope(Dispatchers.IO).launch {
                downloadImage(photoUrl)
            }
        } else {
            if (!justIndicator) {
                if (myBitmap != null) {
                    if (!imageSet) {
                        imageSpinner?.visibility = View.GONE
                        imageSet = true
                        val image = mView.findViewById<ImageView>(R.id.beerimage)
                        var width = image.width
                        if (width == 0) {
                            width = 100
                        }

                        var fl = (myBitmap?.height!!.toFloat() / myBitmap?.width!!.toFloat())
                        var height = (fl * image.width.toFloat())
                        bitmap = myBitmap
                        if (height.toInt() == 0) {
                            height = 100.0f
                        }
                        image.setImageBitmap(
                            Bitmap.createScaledBitmap(
                                myBitmap!!,
                                width,
                                height.toInt(),
                                false
                            )
                        )
                    }
                }
            }
        }
    }

    public fun addToBeerList(spinner: ProgressBar, beerList: Fragment) {
        activitySpinner = spinner
        initButton(beerList)
    }

    fun initButton(beerList: Fragment?){
        if (beerList != null) {
            notInList = false
            mView.setOnClickListener {
                val json = JSONObject(
                    mapOf(
                        "user" to userLogin,
                        "name" to beerJson!!["name"],
                        "beerId" to beerJson!!["beerId"],
                        "brewery" to beerJson!!["brewery"],
                        "style" to beerJson!!["style"],
                        "abv" to beerJson!!["abv"],
                        "ibu" to beerJson!!["ibu"],
                        "mainPhotoUrl" to beerJson!!["mainPhotoUrl"],
                        "review" to mRate.times(2),
                        "tags" to tags.toString()
                    )
                )
                val navHostFragment = beerList.activity?.getSupportFragmentManager()
                    ?.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController
                var bundle = Bundle()
                val stream = ByteArrayOutputStream()
                bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                bundle?.putByteArray("image", stream.toByteArray())

                if(beerList is BeerListFragment) {
                    val action =
                        BeerListFragmentDirections.actionBeerListFragmentToBeerDetailsFragment(
                            json?.toString(),
                            bundle!!
                        )
                    navController.navigate(action)
                } else {
                    if(beerList is LoggedFragment) {
                        val action = LoggedFragmentDirections.actionLoggedFragmentToBeerDetailsFragment(
                            json?.toString(),
                            bundle!!
                        )
                        navController.navigate(action)
                    }
                }
            }
        }
    }



    constructor(
        inflater: LayoutInflater, beer: JSONObject, user: String,
        spinner: ProgressBar?, appContext: Context, beerList: Fragment? = null, noData: Boolean = false) {
        mView = inflater.inflate(com.beerup.beerapp.R.layout.beer_element, null)
        context = appContext
        if(noData){
            mView.findViewById<LinearLayout>(com.beerup.beerapp.R.id.beercontainer)
                .visibility=View.GONE

            mView.findViewById<TextView>(com.beerup.beerapp.R.id.endofdata)
                .visibility=View.VISIBLE
            justIndicator = true
        } else {
            val name = mView.findViewById<TextView>(com.beerup.beerapp.R.id.beername)
            val brewery = mView.findViewById<TextView>(com.beerup.beerapp.R.id.beerbrewery)
            val style = mView.findViewById<TextView>(com.beerup.beerapp.R.id.beerstyle)
            rating = mView.findViewById(com.beerup.beerapp.R.id.rating)
            if(spinner != null) {
                activitySpinner = spinner
            }
            val abv = mView.findViewById<TextView>(com.beerup.beerapp.R.id.beerabv)
            val ibu = mView.findViewById<TextView>(com.beerup.beerapp.R.id.beeribu)
            imageSpinner = mView.findViewById(com.beerup.beerapp.R.id.imagebar)
            id = beer["beerId"].toString()
            userLogin = user
            name.text = beer["name"].toString()
            brewery.text = beer["brewery"].toString()
            style.text = beer["style"].toString()
            abv.text = "ABV: " + beer["abv"].toString()
            ibu.text = "IBU: " + beer["ibu"].toString()
            tags = JSONArray(beer["tags"].toString())
            beerJson = beer
            photoUrl = beer["mainPhotoUrl"].toString()
            CoroutineScope(Dispatchers.IO).launch {
                downloadImage(beer["mainPhotoUrl"].toString())
            }

            if (beer["review"].toString() != "null") {
                mRate = beerJson!!["review"].toString().toFloat().div(2)
                rating?.rating = mRate
            }

            rating?.id = beerJson!!["beerId"].toString().toInt() + 21372137 + (mRate * 2).toInt()
            initButton(beerList)
        }
    }
}