package com.beerup.beerapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.beerup.beerapp.ViewModels.BeerDetailsViewModel
import com.beerup.beerapp.ViewModels.SharedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream


class BeerDetailsFragment : Fragment() {
    private var bundle: Bundle? = null

    private lateinit var thisView: View
    private lateinit var rateButton: Button
    private lateinit var cameraButton: Button

    //private var beerView: View
    private lateinit var tagButton: Button
    private lateinit var spinner: ProgressBar
    private lateinit var rateBar: RatingBar
    private lateinit var uploadText: TextView
    private lateinit var sharedViewModel: SharedViewModel

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    private lateinit var viewModel: BeerDetailsViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var jsonStr: String? = null
        if (arguments != null) {
            jsonStr = arguments?.getString("json")
            bundle = arguments?.getBundle("image")
        }

        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { it ->
                if (it.resultCode == AppCompatActivity.RESULT_OK) {
                    //val stream = ByteArrayOutputStream()
                    val bundle = it.data?.extras
                    //val bitmap = bundle?.get("data") as Bitmap

                    //bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    //uploadText.visibility = View.VISIBLE
                    spinner.visibility = View.VISIBLE
                    disableUI()
                    activity?.contentResolver?.let { viewModel.getImageUrl(it, bundle!!) }
                }
            }

        viewModel = ViewModelProvider(this).get(BeerDetailsViewModel::class.java)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        viewModel.callbackWithSpinner = ::callbackWithSpinner
        viewModel.activityResultLauncher = activityResultLauncher
        viewModel.init(
            jsonStr!!,
            activity?.getString(R.string.baseUrl).toString(),
            sharedViewModel
        )

        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val navHostFragment = activity?.supportFragmentManager?.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                val navController = navHostFragment.navController

                navController.popBackStack()
            }
        })
    }


    fun callbackWithSpinner(message: String? = null) {
        enableUI()
        if(message != null) {
            Toast.makeText(
                activity?.baseContext, // probalby this.activity should be fine, but how knows cause emulator is shit
                message,
                Toast.LENGTH_LONG
            ).show()
        }
        spinner.visibility = View.GONE
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as AppCompatActivity).supportActionBar?.hide()
        sharedViewModel.enableBackPress = true
        sharedViewModel.backButtonEnd = false
        (activity as MainActivity).bottomNavigation?.visibility = View.GONE
        thisView = inflater.inflate(R.layout.fragment_beer_details, container, false)

        return thisView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val name = thisView.findViewById<TextView>(R.id.beername)
        val style = thisView.findViewById<TextView>(R.id.beerstyle)
        val brewery = thisView.findViewById<TextView>(R.id.beerbrewery)
        val ibu = thisView.findViewById<TextView>(R.id.beeribu)
        val abv = thisView.findViewById<TextView>(R.id.beerabv)

        spinner = thisView.findViewById(R.id.spinner)
        rateButton = thisView.findViewById(R.id.rate)
        cameraButton = thisView.findViewById(R.id.sendphoto)
        val photoView = thisView.findViewById<ImageView>(R.id.beerimage)
        rateBar = thisView.findViewById(R.id.rating)
        tagButton = thisView.findViewById(R.id.tagbutton)
        var bitmap: Bitmap? = null

        val arrayInputStream = ByteArrayInputStream(bundle?.get("image") as ByteArray)
        bitmap = BitmapFactory.decodeStream(arrayInputStream)
        name.text = viewModel.beer.name
        rateBar.rating = viewModel.beer.review.toFloat().div(2)
        style.text = viewModel.beer.style
        brewery.text = viewModel.beer.brewery
        ibu.text = "IBU: ${viewModel.beer.ibu}"
        abv.text = "ABV: ${viewModel.beer.abv}"
        photoView.setImageBitmap(bitmap)

        initButtons()
        viewTags()


        viewModel._isTagButtonBlocked.observe(viewLifecycleOwner){
            tagButton.isEnabled = !it
        }

        viewModel._isReviewButtonBlocked.observe(viewLifecycleOwner) {
            rateButton.isEnabled = !it
        }
    }


    fun disableUI() {
        sharedViewModel.enableBackPress = false
        rateBar.isEnabled = false

        rateButton.isEnabled = false

        tagButton.isEnabled = false
        cameraButton.isEnabled = false
    }

    fun enableUI() {
        sharedViewModel.enableBackPress = true
        rateBar.isEnabled = true
        cameraButton.isEnabled = true
    }

    fun initButtons() {
        rateBar.setOnRatingBarChangeListener { ratingBar, fl, b ->
            rateButton.isEnabled =
                rateBar.rating > 0.0f &&
                        rateBar.rating != viewModel.beer.review.toFloat().div(2)
        }

        rateButton.setOnClickListener {
            spinner.visibility = View.VISIBLE
            rateButton.isEnabled = false

            CoroutineScope(Dispatchers.IO).launch {
                viewModel.sendReview(rateBar.rating)
            }
        }

        cameraButton.setOnClickListener{
            viewModel.startCamera()
        }

        tagButton.setOnClickListener {
            spinner.visibility = View.VISIBLE
            viewModel.sendTags()
        }
    }


    fun viewTags() {
        sharedViewModel.tagList.forEach{

            val tagFragment = TagFragment(viewModel::modifyTag)

            val args = Bundle()
            args.putString("tagName", it)
            args.putBoolean("added", viewModel.tagList.contains(it))// empty for now
            tagFragment.arguments = args;
            val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
            transaction.add(R.id.taglist_layout, tagFragment).commit()
        }
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