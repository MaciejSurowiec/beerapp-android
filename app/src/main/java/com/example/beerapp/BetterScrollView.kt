package com.example.beerapp

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.util.LongSparseArray
import android.view.MotionEvent
import android.view.translation.ViewTranslationResponse
import android.widget.ScrollView

class BetterScrollView(context: Context?,attrs: AttributeSet): ScrollView(context,attrs) {

    public var beerList: BeerListActivity? = null
    public var loading: Boolean = false
    public override fun onOverScrolled(
        scrollX: Int, scrollY: Int,
        clampedX: Boolean, clampedY: Boolean ) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
        if(!loading) {
            if (clampedY && scrollY > 0) {
                beerList!!.actualstart += 10
                loading = true
                beerList!!.reload()
            }
        }
    }

}