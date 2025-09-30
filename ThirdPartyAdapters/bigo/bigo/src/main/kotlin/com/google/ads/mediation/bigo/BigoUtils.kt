package com.google.ads.mediation.bigo

import com.google.android.gms.ads.AdError

object BigoUtils {
  fun getGmaAdError(code: Int, message: String, domain: String) = AdError(code, message, domain)
}
