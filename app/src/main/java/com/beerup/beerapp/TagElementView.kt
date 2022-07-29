package com.beerup.beerapp

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView


class TagElementView {

    public lateinit var mView: View
    public var added: Boolean = false
    public var tagName = ""

    fun getTagText(tag: String): String {
        when(tag){
            "WHEAT" -> return "Pszeniczne"
            "BITTER" -> return "Gorzkie"
            "LIGHT" -> return "Jasne"
            "DARK" -> return "Ciemne"
            "HOPPY" -> return "Chmielowe"
            "CITRUS" -> return "Cytrusowe"
            "MALT" -> return "Słodowe"
            "FRUITY" -> return "Owocowe"
            "SOUR" -> return "Kwaśne"
            "CARMEL" -> return "Karmelowe"
            "SWEET" -> return "Słodkie"
            "CHOCOLATE" -> return "Czekoladowe"
            "COFFEE" -> return "Kawowe"
            "MILK" -> return "Mleczne"
            "HERBAL" -> return "Ziołowe"
            "BANANA" -> return "Bananowe"
            "HONEY" -> return "Miodowe"
            "FLOWER" -> return "Kwiatowe"
        }

        return "???"
    }


    constructor(tag: String, inflater: LayoutInflater, beerDetailsFragment: BeerDetailsFragment, add: Boolean){
        mView = inflater.inflate(com.beerup.beerapp.R.layout.tag_layout, null)

        val tagText = mView.findViewById<TextView>(com.beerup.beerapp.R.id.tagtext)
        val tagLayout = mView.findViewById<LinearLayout>(com.beerup.beerapp.R.id.taglayout)
        val paddingBottom: Int = tagLayout.paddingBottom
        val paddingLeft: Int = tagLayout.paddingLeft
        val paddingRight: Int = tagLayout.paddingRight
        val paddingTop: Int = tagLayout.paddingTop
        tagName = tag

        if(add) {
            added = true
            tagLayout.background = beerDetailsFragment.resources.getDrawable(com.beerup.beerapp.R.drawable.tagitemadded)
            tagLayout.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)

            if(beerDetailsFragment.resources.getConfiguration().uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                tagText.setTextColor(beerDetailsFragment.resources.getColor(com.beerup.beerapp.R.color.white))
            } else {
                tagText.setTextColor(beerDetailsFragment.resources.getColor(com.beerup.beerapp.R.color.black))
            }
        } else {
            if (beerDetailsFragment.resources.getConfiguration().uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            ) {
                tagLayout.background =
                    beerDetailsFragment.resources.getDrawable(com.beerup.beerapp.R.drawable.tagitemnotaddednight)
                tagLayout.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
            }
        }

        tagLayout.setOnClickListener{
            if(added){
                added = false
                beerDetailsFragment.removeTag(this)
                if(beerDetailsFragment.resources.getConfiguration().uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                    tagLayout.background = beerDetailsFragment.resources.getDrawable(com.beerup.beerapp.R.drawable.tagitemnotaddednight)
                } else {
                    tagLayout.background = beerDetailsFragment.resources.getDrawable(com.beerup.beerapp.R.drawable.tagitemnotadded)
                }
                tagLayout.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
                tagText.setTextColor(beerDetailsFragment.resources.getColor(com.beerup.beerapp.R.color.lightgray))
            } else{
                added = true
                beerDetailsFragment.addTag(this)
                tagLayout.background = beerDetailsFragment.resources.getDrawable(com.beerup.beerapp.R.drawable.tagitemadded)
                tagLayout.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)

                if(beerDetailsFragment.resources.getConfiguration().uiMode and
                    Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                    tagText.setTextColor(beerDetailsFragment.resources.getColor(com.beerup.beerapp.R.color.white))
                } else {
                    tagText.setTextColor(beerDetailsFragment.resources.getColor(com.beerup.beerapp.R.color.black))
                }
            }
        }

        tagText.text = getTagText(tagName)
    }
}