package com.zenimmersive.android.apiresponsemodel


import com.google.gson.annotations.SerializedName

data class QuoteMessageResModel(
    @SerializedName("message")
    var message: String?,
    @SerializedName("result")
    var result: Qoute?,
    @SerializedName("status")
    var status: Int?
) {


    fun getQuote(lan : String?): String? {
        if(lan?.equals("fr") == true) {
            return result?.fquote
        } else {
            return result?.quote
        }
    }
}

class Qoute {
    //"result": {
    //"quote": "Peace begins where the mind becomes quiet....",
    //"fquote": "La paix commence là où l'esprit devient calme..."
    //}
    @SerializedName("quote")
    var quote: String? = null
    @SerializedName("fquote")
    var fquote: String? = null
}
