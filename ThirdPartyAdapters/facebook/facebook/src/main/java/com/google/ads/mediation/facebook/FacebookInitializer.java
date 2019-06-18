package com.google.ads.mediation.facebook;

import android.content.Context;

import com.facebook.ads.AudienceNetworkAds;

import java.util.ArrayList;


class FacebookInitializer implements AudienceNetworkAds.InitListener {

    private static FacebookInitializer instance;
    private boolean mIsInitializing = false;
    private boolean mIsInitialized = false;

    private ArrayList<Listener> mListeners;

    static FacebookInitializer getInstance() {
        if (instance == null) {
            instance = new FacebookInitializer();
        }
        return instance;
    }

    private FacebookInitializer() {
        mListeners = new ArrayList<>();
    }

    void initialize(Context context, String placementId, Listener listener) {
        ArrayList<String> placements = new ArrayList<>();
        placements.add(placementId);

        getInstance().initialize(context, placements, listener);
    }

    void initialize(Context context, ArrayList<String> placements, Listener listener) {
        if (mIsInitializing) {
            mListeners.add(listener);
            return;
        }

        if (mIsInitialized) {
            listener.onInitializeSuccess();
            return;
        }

        mIsInitializing = true;

        getInstance().mListeners.add(listener);
        AudienceNetworkAds.buildInitSettings(context)
                .withMediationService("GOOGLE:"+ BuildConfig.VERSION_NAME)
                .withPlacementIds(placements)
                .withInitListener(FacebookInitializer.this)
                .initialize();
    }

    @Override
    public void onInitialized(AudienceNetworkAds.InitResult initResult) {
        mIsInitializing = false;
        mIsInitialized = initResult.isSuccess();

        for (Listener listener : mListeners) {
            if (initResult.isSuccess()) {
                listener.onInitializeSuccess();
            } else {
                listener.onInitializeError(initResult.getMessage());
            }
        }
        mListeners.clear();
    }

    interface Listener {
        void onInitializeSuccess();
        void onInitializeError(String message);
    }

}
