package com.beerup.beerapp.ViewModels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.beerup.beerapp.Beer
import com.beerup.beerapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class BeerElementViewModel(app: Application)
    : AndroidViewModel(app)  {

    lateinit var userLogin: String
    lateinit var beer: Beer
    lateinit var bitmap: Bitmap
    var isThisBeerList: Boolean = false
    var _bitmap = MutableLiveData<Bitmap>()

    fun BeerJson(): JSONObject {
        var review = 0
        if(beer.review != "null") { // ehh xd
            review = beer.review.toInt()
        }

        val json = JSONObject(
            mapOf(
                "user" to userLogin,
                "name" to beer.name,
                "beerId" to beer.id,
                "brewery" to beer.brewery,
                "style" to beer.style,
                "abv" to beer.abv,
                "ibu" to beer.ibu,
                "mainPhotoUrl" to beer.mainPhotoUrl,
                "review" to review,
                "tags" to beer.tags.toString()
            )
        )

        return json
    }


    fun downloadImage() {
        CoroutineScope(Dispatchers.IO).launch {
            var noPhoto = false
            try {
                var client = OkHttpClient()
                var request = Request.Builder().url(beer.mainPhotoUrl).build()
                var response = client.newCall(request).execute()
                bitmap = BitmapFactory.decodeStream(response.body()?.byteStream())
            } catch (e: java.lang.Exception) {
                noPhoto = true
                Log.i("image", beer.mainPhotoUrl)
            }

            if (!noPhoto) {
                _bitmap.postValue(bitmap)
            }
        }
    }


    fun getPhotoBundle(): Bundle {
        val bundle = Bundle()
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        bundle.putByteArray("image", stream.toByteArray())

        return bundle
    }

    fun getHeight(width: Int): Int {
        var fl = (bitmap.height.toFloat() / bitmap.width.toFloat())
        var height = (fl * width.toFloat())
        if (height.toInt() == 0) {
            height = 100.0f
        }

        return height.toInt()
    }

}