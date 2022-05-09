package com.example.beerapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*


class BeerListActivity: AppCompatActivity() {
    private lateinit var httpService: Intent
    private var mMessenger: Messenger? = null
    private val replyMessage = Messenger(IncomingHandler())
    private var isBound = false
    private var context = this
    private var url = ""
    private lateinit var spinner: ProgressBar
    private lateinit var mCurrentPhotoPath: String
    private lateinit var photoURI: Uri
    private lateinit var userLogin: String
    var actualstart: Int = 0
    private var scroll: BetterScrollView? = null
    private var query: String = ""
    private var uploadText: TextView? = null
    private var uploadSpinner: ProgressBar? = null
    private var loading: Boolean = false
    private var jsonDelayed: JSONObject? = null
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            isBound = true
            mMessenger = Messenger(service)
            viewList()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }
    }

    private fun viewList(){
        val message = Message.obtain(null, R.integer.GET_HTTP, R.integer.BEERLIST_URL, 0)
        message.replyTo = replyMessage
        val bundle = Bundle()
        val json = JSONObject(mapOf("login" to userLogin))
        bundle.putString("json", json.toString())
        message.data = bundle
        mMessenger!!.send(message)
    }

    inner class IncomingHandler : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                0 -> {
                    val reply = JSONObject(msg.data.getString("json") ?: "")
                    if (reply.has("content")) {
                        val data = reply.getJSONArray("content")
                        val mainLayout = findViewById<LinearLayout>(R.id.beerlist_layout)
                        mainLayout.removeAllViews()
                        if (loading) {
                            if (jsonDelayed != null) {
                                Log.i("test", "1")
                                mainLayout.removeAllViews()
                                val message = Message.obtain(
                                    null,
                                    R.integer.GET_HTTP,
                                    R.integer.BEERLISTWITHPARAMS_URL,
                                    0
                                )
                                message.replyTo = replyMessage
                                val bundle = Bundle()
                                bundle.putString("json", jsonDelayed.toString())
                                message.data = bundle
                                mMessenger!!.send(message)
                                jsonDelayed = null
                                loading = false
                            } else {
                                loading = false
                                spinner.visibility = View.GONE
                            }
                        } else {
                            spinner.visibility = View.GONE
                        }
                        if(data.length() != 0) {
                            for (i in 0 until data.length()) {
                                val beer = data.getJSONObject(i)
                               // val view = BeerElementView(
                                //    getLayoutInflater(), beer, //mMessenger!!,
                               //     userLogin, spinner, context, cameraFun
                               // )
                               // mainLayout.addView(view.mView)
                            }
                        } else {
                            Toast.makeText(applicationContext, "Brak wyników", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                1 -> {
                    spinner.visibility = View.GONE
                    Toast.makeText(applicationContext, "ocena wysłana", Toast.LENGTH_LONG).show()
                }

                2 -> {
                    val reply = JSONObject(msg.data.getString("json") ?: "")
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile()
                    } catch (ex: IOException) {
                    }
                    url = reply["content"].toString()
                    if (photoFile != null) {
                        photoURI =
                            FileProvider.getUriForFile(context, "com.example.beerapp", photoFile)

                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(cameraIntent, 123)
                    }
                }

                3 -> {
                    val reply = JSONObject(msg.data.getString("json") ?: "")
                    if (reply.has("content")) {
                        val data = reply.getJSONArray("content")
                        val mainLayout = findViewById<LinearLayout>(R.id.beerlist_layout)
                        spinner.visibility = View.GONE
                        if(data.length() != 0) {
                            for (i in 0 until data.length()) {
                                val beer = data.getJSONObject(i)
                               // val view = BeerElementView(
                               //     getLayoutInflater(), beer,
                               //     userLogin, spinner, context, false
                              //  )

                               // mainLayout.addView(view.mView)
                            }
                        } else {
                            Toast.makeText(applicationContext, "Koniec danych", Toast.LENGTH_LONG).show()
                        }
                    }
                    scroll?.loading = false
                }
            }
        }
    }

   suspend fun photoLoaded(){
       withContext(Main) {
           uploadText?.visibility = View.GONE
           uploadSpinner?.visibility = View.GONE
       }
   }

   suspend fun uploadPhoto(json: JSONObject, byteArray: ByteArray){
        val url = json["url"].toString()

        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "PUT"
       // connection.setRequestProperty("Content-Type", "image/jpeg")
        connection.doOutput = true
        try {
            connection.outputStream.use { os ->
                os.write(byteArray, 0, byteArray.size)
            }
        } catch (e: Exception) {
            Log.d("err", e.toString())
        }
        val bundle = Bundle()

        try {
            val output = JSONObject(mapOf("content" to connection.responseCode.toString()))
            bundle.putString("json", output.toString())
        } catch (e: Exception) {
            Log.d("err", e.toString())
        }
        connection.disconnect()
        val message = Message.obtain(null, 3, 0, 0)

        message.data = bundle
        try {
            mMessenger?.send(message)
        } catch (e: RemoteException) {
            e.printStackTrace()
        }

        photoLoaded()
    }

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )

        mCurrentPhotoPath = image.absolutePath
        return image
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123) {
            if (resultCode == RESULT_OK) {
                val bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoURI)
                val json = JSONObject(mapOf("url" to url))
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                uploadText?.visibility=View.VISIBLE
                uploadSpinner?.visibility=View.VISIBLE

                CoroutineScope(IO).launch {
                    uploadPhoto(json, stream.toByteArray())
                }
            }
        }
    }

    fun reload(){
        val message = Message.obtain(
            null,
            R.integer.GET_HTTP,
            R.integer.BEERLISTWITHPARAMS_URL,
            3 )
        message.replyTo = replyMessage
        val bundle = Bundle()
        val json = JSONObject(mapOf("queryPhrase" to query,"login" to userLogin,"start" to actualstart))
        bundle.putString("json", json.toString())
        message.data = bundle
        spinner.visibility = View.VISIBLE
        mMessenger!!.send(message)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = getSharedPreferences("userInfo", MODE_PRIVATE)
        userLogin = sharedPref.getString("userLogin", null).toString()
        setContentView(R.layout.activity_beerlist)
        spinner = findViewById(R.id.progressBar)
        spinner.visibility=View.VISIBLE
        httpService = Intent(this, HttpService::class.java)
        bindService(httpService, serviceConnection, BIND_AUTO_CREATE)
        val search = findViewById<SearchView>(R.id.searchbar)
        scroll = findViewById<BetterScrollView>(R.id.scrollable)
        //scroll?.beerList = this
        uploadText = findViewById<TextView>(R.id.uploadText)
        uploadSpinner = findViewById<ProgressBar>(R.id.uploadProgress)
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                if(newText.length >= 3) {
                    actualstart = 0
                    query = newText.replace(" ","%20")
                    val json = JSONObject(mapOf("queryPhrase" to query,"login" to userLogin,"start" to actualstart))

                    spinner.visibility = View.VISIBLE
                    if(!loading) {
                        val message = Message.obtain(null, R.integer.GET_HTTP, R.integer.BEERLISTWITHPARAMS_URL, 0)
                        message.replyTo = replyMessage
                        val bundle = Bundle()
                        bundle.putString("json", json.toString())
                        message.data = bundle
                        mMessenger!!.send(message)
                    } else {
                        jsonDelayed = json
                    }

                    findViewById<LinearLayout>(R.id.beerlist_layout).removeAllViews()
                    loading = true
                }
                return false
            }

            override fun onQueryTextSubmit(querys: String): Boolean {
                actualstart = 0
                query = querys.replace(" ","%20")
                val json = JSONObject(mapOf("queryPhrase" to query,"login" to userLogin,"start" to actualstart))
                spinner.visibility=View.VISIBLE

                if(!loading) {
                    val message = Message.obtain(null, R.integer.GET_HTTP, R.integer.BEERLISTWITHPARAMS_URL, 0)
                    message.replyTo = replyMessage
                    val bundle = Bundle()
                    bundle.putString("json", json.toString())
                    message.data = bundle
                    mMessenger!!.send(message)
                } else {
                    jsonDelayed = json
                }

                loading = true
                return false
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
        }
    }
}