package com.example.beerapp

import android.R
import android.content.Context
import android.content.Intent
import android.os.*
import android.provider.MediaStore
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat.startActivityForResult
import org.json.JSONObject


class BeerElementView {

    lateinit var mView: View
    var mycontext: Context? = null
    var rated: Boolean = false;
    constructor(inflater: LayoutInflater,beer: JSONObject,messenger: Messenger,
                user: String, reply: Messenger, spinner: ProgressBar) {

        mView = inflater.inflate(com.example.beerapp.R.layout.beer_element, null)

        val name = mView.findViewById<TextView>(com.example.beerapp.R.id.beername)
        val brewery = mView.findViewById<TextView>(com.example.beerapp.R.id.beerbrewery)
        val style = mView.findViewById<TextView>(com.example.beerapp.R.id.beerstyle)
        var rateButton  = mView.findViewById<Button>(com.example.beerapp.R.id.rate)
        var cameraButton  = mView.findViewById<Button>(com.example.beerapp.R.id.sendphoto)
        val rating = mView.findViewById<RatingBar>(com.example.beerapp.R.id.rating)
        //val image = v.findViewById<ImageView>(R.id.beerimage)
        val abv = mView.findViewById<TextView>(com.example.beerapp.R.id.beerabv)
        val ibu = mView.findViewById<TextView>(com.example.beerapp.R.id.beeribu)
        name.text = beer["name"].toString()
        brewery.text = beer["brewery"].toString()
        style.text = beer["style"].toString()
        //image.setImageURI(Uri.parse(beer["image"].toString()))
        abv.text = beer["abv"].toString()
        ibu.text = beer["ibu"].toString()

        if(beer["review"].toString() != "null"){
            rating.rating = beer["review"].toString().toFloat()
            rated = true
        }
        if(rating.rating == 0.0f) {
            rateButton.setEnabled(false)
        }

        rating.setOnRatingBarChangeListener { ratingBar, fl, b ->
            if(rating.rating > 0.0f) {
                rateButton.setEnabled(true)
            } else {
                rateButton.setEnabled(false)
            }
        }

        rateButton.setOnClickListener{
            spinner.visibility = View.VISIBLE
            val message = Message.obtain(null, com.example.beerapp.R.integer.POST_HTTP, com.example.beerapp.R.integer.REVIEWPOST_URL, 1)
            val bundle = Bundle()
            var beerint = beer["beerId"].toString().toInt()
            if(rating.rating > 0) {
                val json = JSONObject(
                    mapOf(
                        "login" to user,
                        "beer_id" to beer["beerId"].toString(),
                        "stars" to rating.rating.toInt()
                    )
                )

                bundle.putString("json", json.toString())
                message.data = bundle
                message.replyTo = reply
                messenger!!.send(message)
            }
        }

        cameraButton.setOnClickListener{
            var message: Message? = null
            if(rated) {
                message = Message.obtain(null, com.example.beerapp.R.integer.PUT_HTTP, com.example.beerapp.R.integer.BEERIMAGEUPLOAD_URL, 2)
            } else {
                message = Message.obtain(null, com.example.beerapp.R.integer.GET_HTTP, com.example.beerapp.R.integer.BEERIMAGEUPLOAD_URL, 2)
            }
            message.replyTo = reply
            val bundle = Bundle()
            val json = JSONObject(mapOf("beerid" to beer["beerId"].toString()))
            bundle.putString("json", json.toString())
            message.data = bundle
            messenger!!.send(message)
        }
    }
}