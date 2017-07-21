package com.vungle.mediation;

abstract class VungleListener {
    private String waitingForPlacement;
    private boolean isWaitingInit = false;
    void waitForAd(String placement){
        this.waitingForPlacement = placement;
    }
    String getWaitingForPlacement(){
        return waitingForPlacement;
    }

    public boolean isWaitingInit() {
        return isWaitingInit;
    }


    void setWaitingInit(boolean isWaitingInit){
        this.isWaitingInit = isWaitingInit;
    }

    void onAdEnd(String placement, boolean wasSuccessfulView, boolean wasCallToActionClicked){

    }

    void onAdStart(String placement){

    }

    void onAdFail(String placement){

    }

    void onAdAvailable(){}

    void onInitialized(boolean isSuccess) {

    }
}
