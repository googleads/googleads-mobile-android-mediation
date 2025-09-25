package com.google.ads.mediation.mintegral

/**
 * A Mintegral's ad slot is identified by two IDs (ad unit ID and placement ID).
 *
 * This is a data class to hold both ad unit ID and placement ID.
 */
data class MintegralSlotIdentifier(val adUnitId: String, val placementId: String)
