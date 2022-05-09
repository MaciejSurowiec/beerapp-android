package com.example.beerapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject


class BeerElementView {
    public lateinit var mView: View
    private var rated: Boolean = false
    private val JSON = MediaType.parse("application/json; charset=utf-8")
    private val client = OkHttpClient()
    private var context: Context? = null
    private var activitySpinner: ProgressBar? = null

    private suspend fun sendReview(json: JSONObject,put: Boolean){
        val formBody: RequestBody = RequestBody.create(JSON, json.toString())

        var url="${context?.getString(com.example.beerapp.R.string.baseUrl)}/reviews"
        var request: Request? = null

        if(put) {
            url += "/${json!!["login"]}/${json!!["beer_id"]}"
            request = Request.Builder()
                .url(url)
                .put(formBody)
                .build()
        } else {
            request = Request.Builder()
                .url(url)
                .post(formBody)
                .build()
        }

        val response: Response = client.newCall(request).execute()
        withContext(Dispatchers.Main) {
            activitySpinner!!.visibility = View.GONE
        }
    }



    suspend fun downloadImage(url: String) {
        var myBitmap: Bitmap? = null

        try {
            var client = OkHttpClient()
            var request = Request.Builder().url(url).build()
            var response = client.newCall(request).execute()
            myBitmap = BitmapFactory.decodeStream(response.body()?.byteStream())
        } catch (e: java.lang.Exception) {
            null
        }

        if(myBitmap != null){
            withContext(Dispatchers.Main) {
                val image = mView.findViewById<ImageView>(R.id.beerimage)
                var width  = image.width
                if(width == 0) {
                    width = 100
                }

                var fl = (myBitmap.height.toFloat() / myBitmap.width.toFloat())
                var height = (fl * image.width.toFloat())

                if(height.toInt() == 0){
                    height = 100.0f
                }
                image.setImageBitmap(Bitmap.createScaledBitmap(myBitmap, width, height.toInt(), false))
            }
        }
    }


    constructor(
        inflater: LayoutInflater, beer: JSONObject,//messenger: Messenger,
        user: String, spinner: ProgressBar, appContext: Context, beerList: BeerListFragment? = null) {

        mView = inflater.inflate(com.example.beerapp.R.layout.beer_element, null)
        context = appContext
        val name = mView.findViewById<TextView>(com.example.beerapp.R.id.beername)
        val brewery = mView.findViewById<TextView>(com.example.beerapp.R.id.beerbrewery)
        val style = mView.findViewById<TextView>(com.example.beerapp.R.id.beerstyle)
        var rateButton  = mView.findViewById<Button>(com.example.beerapp.R.id.rate)
        var cameraButton  = mView.findViewById<Button>(com.example.beerapp.R.id.sendphoto)
        val rating = mView.findViewById<RatingBar>(com.example.beerapp.R.id.rating)
        activitySpinner = spinner
        val abv = mView.findViewById<TextView>(com.example.beerapp.R.id.beerabv)
        val ibu = mView.findViewById<TextView>(com.example.beerapp.R.id.beeribu)
        name.text = beer["name"].toString()
        brewery.text = beer["brewery"].toString()
        style.text = beer["style"].toString()
        abv.text = beer["abv"].toString()
        ibu.text = beer["ibu"].toString()

        CoroutineScope(Dispatchers.IO).launch {
            downloadImage(beer["mainPhotoUrl"].toString())
        }

        if(beer["review"].toString() != "null"){
            rating.rating = beer["review"].toString().toFloat()
            rated = true
        }
        if(rating.rating == 0.0f) {
            rateButton.setEnabled(false)
        }

        if(beerList == null){
            rateButton.visibility = View.GONE
            cameraButton.visibility = View.GONE
        } else {
            rating.setOnRatingBarChangeListener { ratingBar, fl, b ->
                if(rating.rating > 0.0f) {
                    rateButton.setEnabled(true)
                } else {
                    rateButton.setEnabled(false)
                }
            }

            rateButton.setOnClickListener{
                spinner.visibility = View.VISIBLE
                val beerint = beer["beerId"].toString().toInt()

                if(rating.rating > 0) {
                    val json = JSONObject(
                        mapOf( "login" to user,
                            "beer_id" to beer["beerId"].toString(),
                            "stars" to rating.rating.toInt()))

                    if(rated) {//put
                        CoroutineScope(Dispatchers.IO).launch {
                            sendReview(json,true)
                        }
                    } else {//post
                        CoroutineScope(Dispatchers.IO).launch {
                            sendReview(json,false)
                        }
                    }
                }
            }

            cameraButton.setOnClickListener{
                if(beerList!= null) {
                    val url = "${context?.getString(com.example.beerapp.R.string.baseUrl)}/beers/${beer["beerId"].toString()}/image"
                    beerList.startCamera(url)
                }


                /*
                val message = Message.obtain(null, com.example.beerapp.R.integer.GET_HTTP, com.example.beerapp.R.integer.BEERIMAGEUPLOAD_URL, 2)
                message.replyTo = reply
                val bundle = Bundle()
                val json = JSONObject(mapOf("beerid" to beer["beerId"].toString()))
                bundle.putString("json", json.toString())
                message.data = bundle
                messenger!!.send(message)
                 */
            }
        }
    }
}