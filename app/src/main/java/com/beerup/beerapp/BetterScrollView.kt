package com.beerup.beerapp

import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView

class BetterScrollView(context: Context?,attrs: AttributeSet): ScrollView(context,attrs) {

    lateinit  var viewModel: BeerListViewModel
    var loading: Boolean = false
    public override fun onOverScrolled(
        scrollX: Int, scrollY: Int,
        clampedX: Boolean, clampedY: Boolean ) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
        if(!loading && !viewModel.endOfDataShowed) {
            if (clampedY && scrollY > 0) {
                viewModel.actualStart += 10
                loading = true
                viewModel.downloadList()
            }
        }
    }

}