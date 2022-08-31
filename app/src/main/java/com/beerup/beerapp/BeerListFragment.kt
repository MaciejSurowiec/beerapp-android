package com.beerup.beerapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ViewModelProvider
import com.beerup.beerapp.ViewModels.SharedViewModel
import kotlinx.coroutines.*


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


class BeerListFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var spinner: ProgressBar
    private lateinit var search: SearchView
    lateinit var scroll: BetterScrollView
    lateinit var retryButton: Button

    private lateinit var viewModel: BeerListViewModel
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(BeerListViewModel::class.java)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        viewModel.spinnerOff = ::spinnerOff
        viewModel.spinnerOn = ::spinnerOn

        viewModel.baseUrl = activity?.getString(R.string.baseUrl).toString()
        viewModel.userLogin = sharedViewModel.userLogin
        viewModel.showList = ::showList
        viewModel.retryButton  = ::retryButton
        viewModel.enableUI = ::enableUI
        viewModel.disableUI = ::disableUI
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        (activity as AppCompatActivity).supportActionBar?.hide()
        (activity as MainActivity).bottomNavigation?.visibility = View.VISIBLE
        sharedViewModel.enableBackPress = false
        sharedViewModel.backButtonEnd = true
        return inflater.inflate(R.layout.fragment_beer_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedViewModel.backButtonEnd = false
        retryButton = view.findViewById(R.id.downloadlist)
        spinner = view.findViewById(R.id.progressBar)
        scroll = view.findViewById(R.id.scrollable)
        scroll.viewModel = viewModel
        search = view.findViewById(R.id.searchbar)

        disableUI()
        search.setOnQueryTextListener(
            DebouncingQueryTextListener(
                this@BeerListFragment.lifecycle
            ) { newText ->
                newText?.let {
                   // if (!viewModel.loading) { // without this bool, resetsearch is activated on createview xd
                    if (it.isEmpty()) {
                        if(viewModel.query.isNotEmpty()) {
                            clearBeerList()
                            viewModel.resetSearch()
                        }
                    } else {
                        clearBeerList()
                        viewModel.search(it)
                    }
                }
            }
        )

        if(childFragmentManager.fragments.size == 0) {
            viewModel.downloadList()
        } else {
            enableUI()
            clearBeerList()
            showList()
        }

        retryButton.setOnClickListener {
            spinner.visibility = View.VISIBLE
            retryButton.visibility = View.GONE
            search.visibility = View.VISIBLE
            viewModel.downloadList()
            disableUI()
        }
    }
    private fun enableSearchView(view: View, enabled: Boolean) {
        // kurwa ten android to jest miejscami jakas komedia xd
        view.isEnabled = enabled
        if (view is ViewGroup) {
            val viewGroup = view
            for (i in 0 until viewGroup.childCount) {
                val child = viewGroup.getChildAt(i)
                enableSearchView(child, enabled)
            }
        }
    }

    fun retryButton() {
        search.visibility = View.GONE
        retryButton.visibility = View.VISIBLE
    }


    fun clearBeerList() {
        childFragmentManager.fragments.forEach {
            childFragmentManager.beginTransaction().remove(it).commit()
        }
    }

    fun spinnerOn() {
        spinner.visibility = View.VISIBLE
    }

    fun spinnerOff() {
        spinner.visibility = View.GONE
    }

    fun disableUI() {
        enableSearchView(search, false)
    }

    fun enableUI() {
        enableSearchView(search, true)
    }

    fun showList() {
        val start = 0
        retryButton.visibility = View.GONE
        for (i in start until viewModel.beerList().value?.list?.size!!) {
            if(this.isVisible) {
                val beerFragment = BeerElementFragment()

                val args = Bundle()
                args.putString("userLogin", sharedViewModel.userLogin)
                args.putString(
                    "beer",
                    viewModel.beerList().value?.list!![i].toJson().toString()
                )
                args.putBoolean("isThisBeerList", true)
                beerFragment.arguments = args;
                val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
                transaction.add(R.id.beerlist_layout, beerFragment).commit()
            }
        }

        if(viewModel.endOfDataShowed) {
            if(this.isVisible) {
                val beerFragment = BeerElementFragment()
                //empty arguments cause this is just text that there is no more data
                val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
                transaction.add(R.id.beerlist_layout, beerFragment).commit()
            }
        }

        spinner.visibility = View.GONE
    }

    internal class DebouncingQueryTextListener(
        lifecycle: Lifecycle,
        private val onDebouncingQueryTextChange: (String?) -> Unit
    ) : SearchView.OnQueryTextListener, LifecycleObserver {
        var debouncePeriod: Long = 500

        private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)

        private var searchJob: Job? = null

        init {
            lifecycle.addObserver(this)
        }

        override fun onQueryTextSubmit(query: String?): Boolean {
            return false
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            searchJob?.cancel()
            searchJob = coroutineScope.launch {
                newText?.let {
                    delay(debouncePeriod)
                    onDebouncingQueryTextChange(newText)
                }
            }
            return false
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        private fun destroy() {
            searchJob?.cancel()
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            BeerListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}