package com.beerup.beerapp

import android.content.SharedPreferences
import android.os.*
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONObject
import org.json.JSONArray
import java.util.ArrayList

class MainActivity : AppCompatActivity() {
    public var enableBackPress = true
    public var beerid = ""
    public var rating = 0.0f
    public var list: JSONArray? = null
    public var mainMenu: Menu? = null
    public var sentPhotos = 0
    public var reviedBeers = 0
    public var userLogin = ""
    public var beerList = ArrayList<BeerElementView>()
    public var backButtonEnd = true
    public var bottomNavigation : BottomNavigationView? = null
    public var actualPage: FragmentVisible = FragmentVisible.HOMEPAGE
    private lateinit var sharedPref: SharedPreferences

    enum class FragmentVisible{
        ABOUT,
        BEERLIST,
        HOMEPAGE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPref = getSharedPreferences("userInfo", MODE_PRIVATE)
        if(sharedPref.contains("userLogin")){
            userLogin = sharedPref.getString("userLogin","").toString()
        }
        setContentView(R.layout.activity_main)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigation?.setOnNavigationItemSelectedListener { item ->
            when(item.itemId) {
                R.id.homepage -> {
                    if(actualPage != FragmentVisible.HOMEPAGE) {
                        var action: NavDirections? = null
                        when (actualPage) {
                            FragmentVisible.BEERLIST -> {
                                action =
                                    BeerListFragmentDirections.actionBeerListFragmentToLoggedFragment(
                                        userLogin,
                                        ""
                                    )
                            }
                            FragmentVisible.ABOUT -> {
                                action =
                                    AboutFragmentDirections.actionAboutFragmentToLoggedFragment(
                                        userLogin,
                                        ""
                                    )
                            }
                        }
                        actualPage = FragmentVisible.HOMEPAGE
                        navController.navigate(action!!)
                    }
                    true
                }
                R.id.beerlist -> {
                    if(actualPage != FragmentVisible.BEERLIST) {
                        var action: NavDirections? = null
                        when (actualPage) {
                            FragmentVisible.HOMEPAGE -> {
                                action =
                                    LoggedFragmentDirections.actionLoggedFragmentToBeerListFragment(
                                        userLogin
                                    )
                            }
                            FragmentVisible.ABOUT -> {
                                action =
                                    AboutFragmentDirections.actionAboutFragmentToBeerListFragment(
                                        userLogin
                                    )
                            }
                        }
                        actualPage = FragmentVisible.BEERLIST
                        navController.navigate(action!!)
                    }
                    true
                }
                R.id.about -> {
                    if(actualPage != FragmentVisible.ABOUT) {
                        var action: NavDirections? = null
                        when (actualPage) {
                            FragmentVisible.BEERLIST -> {
                                action =
                                    BeerListFragmentDirections.actionBeerListFragmentToAboutFragment()
                            }
                            FragmentVisible.HOMEPAGE -> {
                                action =
                                    LoggedFragmentDirections.actionLoggedFragmentToAboutFragment()
                            }
                        }
                        actualPage = FragmentVisible.ABOUT
                        navController.navigate(action!!)
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onBackPressed() {
        if(backButtonEnd) {
            finish()
        }

        if(enableBackPress) {
            super.onBackPressed()
        }
    }

    public fun addNewBeer(json: JSONObject) {
        val beerView = BeerElementView(
            getLayoutInflater(), json,
            userLogin!!, null,
            baseContext
        )

        for(i in 0 until beerList.size) {
            if(beerList[i].id == beerView.id) {
                beerList.remove(beerList[i])
                break
            }
        }

        beerList.add(0,beerView)

        if(beerList.size > 3) {
            beerList.remove(beerList.get(3))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.popup_menu, menu)
        if (!sharedPref.contains("userLogin")) {
            menu.setGroupVisible(0, false)
        }

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (sharedPref.contains("userLogin")) {
            val logout = menu.findItem(R.id.logout)
            logout.isVisible = true
        } else {
            val logout = menu.findItem(R.id.logout)
            logout.isVisible = false
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout -> {
                logout()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun logout() {
        val editor = sharedPref.edit()

        beerList.clear() // clear last reviewed beers from old account
        userLogin = ""
        if (sharedPref.contains("userLogin")) {
            editor.remove("userLogin")
            editor.apply()
        }

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val action = LoggedFragmentDirections.actionLoggedFragmentToUnloggedFragment()
        navController.navigate(action)
    }
}