package com.example.beerapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
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
    private var query: String = ""

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
/*
    var resultLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val message = Message.obtain(null, R.integer.PUT_HTTP, R.integer.JSON_URL, 3)
            message.replyTo = replyMessage
            val bitmap = result.data?.extras?.get("data") as Bitmap

            val bundle = Bundle()
            val json = JSONObject(mapOf("url" to url))
            bundle.putString("json", json.toString())
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            bundle.putByteArray("bitmap",stream.toByteArray())
            message.data = bundle
            mMessenger!!.send(message)
        }
    }
*/
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
            when(msg.what) {
                 0 -> {
                     val reply = JSONObject(msg.data.getString("json") ?: "")
                     if (reply.has("content")) {
                         val data = reply.getJSONArray("content")
                         val inflater = getLayoutInflater()
                         val mainLayout = findViewById<LinearLayout>(R.id.beerlist_layout)

                         for (i in 0 until data.length()) {
                             val beer = data.getJSONObject(i)
                             val view =
                                 BeerElementView(inflater, beer, mMessenger!!,
                                     userLogin, replyMessage, spinner)

                             mainLayout.addView(view.mView)
                         }
                     }

                     spinner.visibility=View.GONE
                 }
                1 -> {
                    spinner.visibility = View.GONE
                    Toast.makeText(applicationContext, "ocena wysłana", Toast.LENGTH_LONG).show()
                }

                2->{
                    val reply = JSONObject(msg.data.getString("json") ?: "")
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile()
                    } catch (ex: IOException) {
                    }
                    url = reply["content"].toString()
                    if (photoFile != null) {
                        photoURI = FileProvider.getUriForFile(context, "com.example.beerapp", photoFile)

                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                        startActivityForResult(cameraIntent,123)
                    }
                }

                3 -> {
                    Toast.makeText(applicationContext, "zdjęcie wysłane", Toast.LENGTH_LONG).show()
                }
            }
        }
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
                val message = Message.obtain(null, R.integer.PUT_HTTP, R.integer.JSON_URL, 3)
                message.replyTo = replyMessage
                //val bitmap = data?.extras?.get("data") as Bitmap
                val bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoURI)
                val bundle = Bundle()
                val json = JSONObject(mapOf("url" to url))
                bundle.putString("json", json.toString())
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                bundle.putByteArray("bitmap",stream.toByteArray())
                message.data = bundle
                mMessenger!!.send(message)
            }
        }
    }

    fun reload(){
        val message = Message.obtain(
            null,
            R.integer.GET_HTTP,
            R.integer.BEERLISTWITHPARAMS_URL,
            0 )
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
        val scroll = findViewById<BetterScrollView>(R.id.scrollable)
        scroll.beerList = this
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                if(newText.length > 3) {
                    actualstart = 0
                    val mainLayout = findViewById<LinearLayout>(R.id.beerlist_layout)
                    val message = Message.obtain(
                        null,
                        R.integer.GET_HTTP,
                        R.integer.BEERLISTWITHPARAMS_URL,
                        0
                    )
                    message.replyTo = replyMessage
                    val bundle = Bundle()
                    query = newText
                    val json = JSONObject(mapOf("queryPhrase" to query,"login" to userLogin,"start" to actualstart))

                    bundle.putString("json", json.toString())
                    message.data = bundle
                    spinner.visibility = View.VISIBLE
                    mainLayout.removeAllViews()
                    mMessenger!!.send(message)
                }
                return false
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                actualstart = 0
                val mainLayout = findViewById<LinearLayout>(R.id.beerlist_layout)
                val message = Message.obtain(null, R.integer.GET_HTTP, R.integer.BEERLISTWITHPARAMS_URL, 0)
                message.replyTo = replyMessage
                val bundle = Bundle()
                val json = JSONObject(mapOf("queryPhrase" to query,"login" to userLogin,"start" to actualstart))
                bundle.putString("json", json.toString())
                message.data = bundle
                spinner.visibility=View.VISIBLE
                mainLayout.removeAllViews()
                mMessenger!!.send(message)
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