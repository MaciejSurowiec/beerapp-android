package com.beerup.beerapp.ViewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class TagViewModel(app: Application)
    : AndroidViewModel(app) {

    lateinit var tagName: String
    var add: Boolean = false
    var added: Boolean = false


    fun getTagText(): String {
        when(tagName){
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


}