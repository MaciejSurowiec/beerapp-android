package com.beerup.beerapp.ViewModels

import android.app.Application
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.collection.ArraySet
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.beerup.beerapp.Beer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.internal.Util
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class BeerDetailsViewModel(app: Application)
    : AndroidViewModel(app) {

    private lateinit var photoURI: Uri
    private lateinit var mCurrentPhotoPath: String
    private lateinit var uploadUrl: String
    lateinit var json: JSONObject

    var tagList = ArraySet<String>() // tags from json, or sent
    var actualTagList = ArraySet<String>() // tags changed by user

    lateinit var callbackWithSpinner: (message: String?) -> Unit
    private val client = OkHttpClient()
    private val JSON = MediaType.parse("application/json; charset=utf-8")
    private var baseUrl = ""
    lateinit var beer: Beer
    private var notReviewed = true
    lateinit var sharedViewModel: SharedViewModel
    var _isReviewButtonBlocked = MutableLiveData(true)
    var _isTagButtonBlocked = MutableLiveData(true)

    lateinit var activityResultLauncher: ActivityResultLauncher<Intent>


    fun init(jsonStr: String, url: String, viewModel: SharedViewModel) {
        baseUrl = url
        json = JSONObject(jsonStr)
        sharedViewModel = viewModel

        beer = Beer()
        beer.init(json)
        notReviewed = json["review"].toString().toInt() == 0

        for(i in 0 until beer.tags.length()) {
            tagList.add(beer.tags[i].toString())
            actualTagList.add(beer.tags[i].toString())
        }
    }


    fun sendReview(rating: Float) { // in need to update beerlist too
        val reviewJson = JSONObject(
            mapOf(
                "login" to json["user"],
                "beer_id" to json["beerId"].toString().toInt(),
                "stars" to rating.times(2).toInt()
            )
        )

        CoroutineScope(Dispatchers.IO).launch {
            var url = "${baseUrl}/reviews"
            var request: Request? = null

            if (notReviewed) { //post
                val formBody: RequestBody = RequestBody.create(JSON, reviewJson.toString())
                request = Request.Builder().url(url).post(formBody).build()
            } else { // put
                val formBody: RequestBody = FormBody.create(JSON, reviewJson!!["stars"].toString())
                url += "/${reviewJson!!["login"]}/${reviewJson!!["beer_id"]}"
                request = Request.Builder().url(url).put(formBody).build()
            }

            val response: Response = client.newCall(request).execute()

            withContext(Dispatchers.Main) {
                if (response.code() == 204) {
                    json.put("review", rating.times(2))
                    beer.review = rating.times(2).toString()
                    callbackWithSpinner("Ocena Wysłana")
                    _isReviewButtonBlocked.postValue(true)
                    sharedViewModel.updateBeerInStats(json, notReviewed)// need to update beer lilst
                } else {
                    callbackWithSpinner("Błąd wysyłania")
                }
            }
        }
    }


    fun sendTags() {
        CoroutineScope(Dispatchers.IO).launch {
            var url = "${baseUrl}/reviews/${sharedViewModel.userLogin}/${beer.id}/tags"
            tagList.clear()
            tagList.addAll(actualTagList)
            val jsonArray = JSONArray()

            actualTagList.forEach {
                jsonArray.put(it)
            }

            val formBody: RequestBody = RequestBody.create(JSON, jsonArray.toString())
            val request = Request.Builder()
                .url(url)
                .put(formBody)
                .build()

            val response: Response = client.newCall(request).execute()
            withContext(Dispatchers.Main) {
                if (response.code() == 204) {
                    _isTagButtonBlocked.postValue(true)
                    callbackWithSpinner("Tagi Wysłane")
                } else {
                    callbackWithSpinner("Błąd podczas wysyłania")
                }
            }
        }
    }


    fun modifyTag(tag: String, add: Boolean) {
        if(add) {
            actualTagList.add(tag)
        } else {
            actualTagList.remove(tag)
        }

        if(actualTagList.size == 0) {
            _isTagButtonBlocked.postValue(true)
        } else {
            _isTagButtonBlocked.postValue(tagList == actualTagList)
        }
    }


    suspend fun uploadImage(url: String, bitmap: ByteArray) {
        val formBody: RequestBody = RequestBody.create(MediaType.parse("image/*jpg"),bitmap)
        val request = Request.Builder()
            .url(url)
            .put(formBody)
            .build()

        val response: Response = client.newCall(request).execute()
        incrementPhotoNumber()
        withContext(Dispatchers.Main) {
            callbackWithSpinner("Zdjęcie wysłane")
        }
        val fdelete: File = File(photoURI.getPath())
        if (fdelete.exists()) {
            if (fdelete.delete()) {
                Log.i("photofile","file Deleted :" + photoURI.getPath())
            } else {
                Log.i("photofile","file not Deleted :" + photoURI.getPath())
            }
        }
    }


    suspend fun incrementPhotoNumber() {
        val url = "${baseUrl}/users/${json!!["user"]}/photos"
        val request = Request.Builder().url(url).post(Util.EMPTY_REQUEST).build()
        val response = client.newCall(request).execute()

        sharedViewModel._photos.postValue(sharedViewModel._photos.value + 1)
    }


    fun getImageUrl(contentResolver: ContentResolver, bundle: Bundle) {
        val stream = ByteArrayOutputStream()
        val bitmap = bundle?.get("data") as Bitmap
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        CoroutineScope(Dispatchers.IO).launch {
            var jsonI: JSONObject? = null
            var serverError = false
            try {
                val request = Request.Builder().url(uploadUrl).build()
                val response = client.newCall(request).execute()
                jsonI = JSONObject(response.body()?.string())
            } catch (e: java.lang.Exception) {
                serverError = true
            }

            if (!serverError) {
                uploadImage(jsonI?.get("content").toString(), stream.toByteArray())
            } else {
                withContext(Dispatchers.Main) {
                    callbackWithSpinner("Błąd podczas wysyłania zdjęcia")
                }
            }
        }
    }


    fun startCamera(){
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        var fileError = false
        uploadUrl = "${baseUrl}/beers/${beer.id}/image"
        var photoFile: File? = null
        try {
            photoFile = createImageFile()
        } catch (e: IOException) {
            fileError = true
        }


        if(!fileError) {
            if (photoFile != null) {
                photoURI =
                    FileProvider.getUriForFile(
                        getApplication<Application>().baseContext!!,
                        "com.beerup.beerapp",
                        photoFile
                    )

                //cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                activityResultLauncher.launch(cameraIntent)
                //startActivityForResult(cameraIntent, 123)
            }
        }
    }


    @Throws(IOException::class)
    private fun createImageFile(): File? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        val image = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )

        mCurrentPhotoPath = image.absolutePath
        return image
    }
}