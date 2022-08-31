package com.beerup.beerapp

import org.json.JSONArray
import org.json.JSONObject

class Beer {
    var name: String = ""
    var style: String = ""
    var brewery: String = ""
    var ibu: String = ""
    var abv: String = ""
    var id: String = ""
    lateinit var tags: JSONArray
    var mainPhotoUrl: String = ""
    var review: String = ""

    fun init(json: JSONObject) {
        abv = json["abv"].toString()
        brewery = json["brewery"].toString()
        ibu = json["ibu"].toString()
        name = json["name"].toString()
        id = json["beerId"].toString()
        style = json["style"].toString()
        mainPhotoUrl = json["mainPhotoUrl"].toString()
        review =  json["review"].toString()
        tags = JSONArray(json["tags"].toString())
    }

    fun toJson(): JSONObject {
        val json = JSONObject(
            mapOf(
                "name" to name,
                "beerId" to id,
                "brewery" to brewery,
                "style" to style,
                "abv" to abv,
                "ibu" to ibu,
                "mainPhotoUrl" to mainPhotoUrl,
                "review" to review,
                "tags" to tags.toString()
            )
        )

        return json
    }
}