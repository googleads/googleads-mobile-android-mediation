package com.google.ads.mediation.yandex.base

import android.os.Bundle
import org.json.JSONException
import org.json.JSONObject

class AdMobServerExtrasParserProvider {

    @Throws(JSONException::class)
    fun getServerExtrasParser(serverParameter: String?): AdMobServerExtrasParser {
        if (serverParameter == null) {
            throw JSONException("Server parameter must be not null")
        }
        val serverExtras = JSONObject(serverParameter)
        return AdMobServerExtrasParser(serverExtras)
    }

    @Throws(JSONException::class)
    fun getServerExtrasParser(
            serverParameters: Bundle,
            parametersKey: String?
    ): AdMobServerExtrasParser {
        val serverParameter = serverParameters.getString(parametersKey)
        return getServerExtrasParser(serverParameter)
    }
}
