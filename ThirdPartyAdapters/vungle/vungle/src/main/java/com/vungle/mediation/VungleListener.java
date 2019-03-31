package com.vungle.mediation;

/**
 * A listener class used to send Vungle events from {@link VungleManager} to
 * {@link VungleInterstitialAdapter}.
 */
abstract class VungleListener {
    private String mWaitingForPlacement;
    private boolean mIsWaitingInit = false;

    void waitForAd(String placement) {
        this.mWaitingForPlacement = placement;
    }

    String getWaitingForPlacement() {
        return mWaitingForPlacement;
    }

    public boolean isWaitingInit() {
        return mIsWaitingInit;
    }

    void setWaitingInit(boolean isWaitingInit) {
        this.mIsWaitingInit = isWaitingInit;
    }

    void onAdEnd(String placement, boolean wasSuccessfulView, boolean wasCallToActionClicked) {}

    void onAdStart(String placement) {}

    void onAdFail(String placement) {}

    void onAdAvailable() {}

    void onAdFailedToLoad() {}
}
