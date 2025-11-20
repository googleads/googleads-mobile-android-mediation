package com.google.ads.mediation.yandex.base

import android.content.Context
import com.yandex.mobile.ads.banner.BannerAdSize
import org.json.JSONObject

class AdMobServerExtrasParser(private val serverExtras: JSONObject) {

    fun parseAdUnitId(): String? {
        var adUnitId = serverExtras.optString(AD_UNIT_ID)
        if (adUnitId.isNullOrEmpty()) {
            adUnitId = serverExtras.optString(BLOCK_ID)
        }

        return adUnitId
    }

    fun parseShouldOpenLinksInApp(): Boolean {
        return serverExtras.optBoolean(OPEN_LINKS_IN_APP)
    }

    fun parseAdSize(context: Context): BannerAdSize? {
        val serverAdWidth = parseInteger(AD_WIDTH_KEY)
        val serverAdHeight = parseInteger(AD_HEIGHT_KEY)
        return if (serverAdWidth != null && serverAdHeight != null) {
            BannerAdSize.fixedSize(context, serverAdWidth, serverAdHeight)
        } else {
            null
        }
    }

    private fun parseInteger(key: String): Int? {
        val numberExtra = serverExtras.optString(key)
        var number: Int? = null

        try {
            if (numberExtra != null) {
                number = numberExtra.toInt()
            }
        } catch (ignored: NumberFormatException) {
        }

        return number
    }

    companion object {
        private const val BLOCK_ID = "blockID"
        private const val AD_UNIT_ID = "adUnitId"
        private const val AD_WIDTH_KEY = "adWidth"
        private const val AD_HEIGHT_KEY = "adHeight"
        private const val OPEN_LINKS_IN_APP = "openLinksInApp"
    }
}
