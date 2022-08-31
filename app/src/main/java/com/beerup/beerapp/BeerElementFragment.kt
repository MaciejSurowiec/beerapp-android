package com.beerup.beerapp

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.beerup.beerapp.ViewModels.BeerElementViewModel
import com.beerup.beerapp.ViewModels.SharedViewModel
import org.json.JSONObject

class BeerElementFragment() : Fragment() {

    lateinit var viewModel: BeerElementViewModel
    private lateinit var activitySpinner: ProgressBar
    private lateinit var imageSpinner: ProgressBar
    lateinit var rating: RatingBar
    lateinit var image: ImageView
    var noData = false
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var userLogin: String? = null
        var beerStr: String? = null
        var isThisBeerList: Boolean? = null
        if (arguments != null) {
            userLogin = arguments?.getString("userLogin")
            beerStr = arguments?.getString("beer")
            isThisBeerList  = arguments?.getBoolean("isThisBeerList")

            val json = JSONObject(beerStr)
            val beer = Beer()
            beer.init(json)

            sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
            viewModel = ViewModelProvider(this).get(BeerElementViewModel::class.java)

            viewModel.isThisBeerList = isThisBeerList!!
            viewModel.userLogin = userLogin!!
            viewModel.beer = beer
        } else {
            noData = true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.beer_element, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(!noData) {
            val name = view.findViewById<TextView>(R.id.beername)
            val brewery = view.findViewById<TextView>(R.id.beerbrewery)
            val style = view.findViewById<TextView>(R.id.beerstyle)

            image = view.findViewById(R.id.beerimage)
            rating = view.findViewById(R.id.rating)
            val abv = view.findViewById<TextView>(R.id.beerabv)
            val ibu = view.findViewById<TextView>(R.id.beeribu)
            imageSpinner = view.findViewById(R.id.imagebar)
            name.text = viewModel.beer.name
            brewery.text = viewModel.beer.brewery
            style.text = viewModel.beer.style
            abv.text = "ABV: ${viewModel.beer.abv}"
            ibu.text = "IBU: ${viewModel.beer.ibu}"
            imageSpinner.visibility = View.VISIBLE
            viewModel.downloadImage()
            if (viewModel.beer.review != "null") {
                rating.rating = viewModel.beer.review.toFloat().div(2)
            } else {
                rating.rating = 0.0f
            }
            viewModel._bitmap.observe(viewLifecycleOwner) {
                if (it != null) {
                    setImage()
                }
            }

            view.setOnClickListener {
                if (viewModel._bitmap.value != null) {
                    val navHostFragment = activity?.getSupportFragmentManager()
                        ?.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                    val navController = navHostFragment.navController

                    if (viewModel.isThisBeerList) {
                        val action =
                            BeerListFragmentDirections.actionBeerListFragmentToBeerDetailsFragment(
                                viewModel.BeerJson().toString(),
                                viewModel.getPhotoBundle()
                            )
                        navController.navigate(action)
                    } else {
                        val action =
                            LoggedFragmentDirections.actionLoggedFragmentToBeerDetailsFragment(
                                viewModel.BeerJson().toString(),
                                viewModel.getPhotoBundle()
                            )
                        navController.navigate(action)
                    }
                }
            }
        } else {
            view.findViewById<LinearLayout>(com.beerup.beerapp.R.id.beercontainer)
                .visibility=View.GONE

            view.findViewById<TextView>(com.beerup.beerapp.R.id.endofdata)
                .visibility=View.VISIBLE
        }
    }

    fun setImage() {
        if(viewModel.bitmap == null) { // always null you say, so why it's crashing
            viewModel.downloadImage()
        } else {
            imageSpinner.visibility = View.GONE

            var width = image.width
            if(width == 0) {
                width = 100
            }
            image.setImageBitmap(
                Bitmap.createScaledBitmap(
                    viewModel.bitmap,
                    width,
                    viewModel.getHeight(width),
                    false
                )
            )
        }
    }

    companion object {

        fun newInstance(param1: String?, param2: String?): BeerElementFragment {
            val fragment = BeerElementFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}