package com.vungle.mediation;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.vungle.publisher.AdConfig;
import com.vungle.publisher.VungleAdEventListener;
import com.vungle.publisher.VungleInitListener;
import com.vungle.publisher.VunglePub;
import com.vungle.publisher.env.WrapperFramework;
import com.vungle.publisher.inject.Injector;

import java.util.HashMap;
import java.util.Map;


class VungleManager implements VungleAdEventListener {

    private static final String TAG = VungleManager.class.getSimpleName();

    private static final String VERSION = "3.0.0";

    private static VungleManager instance;
    private VunglePub mVunglePub;
    private String currentPlayId = null;
    private boolean isInitialising = false;
    private String appId;
    private String[] placements;
    private Context ctx;
    private Handler handler = new Handler(Looper.getMainLooper());

    private Map<String, VungleListener> listeners;

    static VungleManager getInstance(String appId, String[] placements, Context ctx){
        if (instance == null)
            instance = new VungleManager(appId, placements, ctx);
        return instance;
    }

    private VungleManager(String appId, String[] placements, Context ctx){
        listeners = new HashMap<>();

        Injector injector = Injector.getInstance();
        injector.setWrapperFramework(WrapperFramework.admob);
        injector.setWrapperFrameworkVersion(VERSION.replace('.', '_'));

        this.appId = appId;
        this.placements = placements;
        this.ctx = ctx;

        mVunglePub = VunglePub.getInstance();
    }

    boolean isInitialized() {
        return mVunglePub.isInitialized();
    }

    String findPlacemnt(Bundle bundle){
        if (bundle == null)
            return placements[0];
        int ind = bundle.getInt(VungleExtrasBuilder.EXTRA_PLAY_PLACEMENT_INDEX, 0);
        if (ind < placements.length){
            return placements[ind];
        }
        return placements[0];
    }

    void init() {
        if (mVunglePub.isInitialized()) {
            for (VungleListener cb: listeners.values()) {
                if (cb.isWaitingInit()) {
                    cb.setWaitingInit(false);
                    cb.onInitialized(mVunglePub.isInitialized());
                }
            }
            return;
        }
        if (isInitialising)
            return;
        isInitialising = true;

        mVunglePub.init(ctx, appId, placements, new VungleInitListener() {
            @Override
            public void onSuccess() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        isInitialising = false;
                        mVunglePub.clearAndSetEventListeners(VungleManager.this);
                        for (VungleListener cb: listeners.values()){
                            if (cb.isWaitingInit()) {
                                cb.setWaitingInit(false);
                                cb.onInitialized(mVunglePub.isInitialized());
                            }
                        }
                    }
                });
            }

            @Override
            public void onFailure(Throwable throwable) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        isInitialising = false;
                        for (VungleListener cb: listeners.values()){
                            if (cb.isWaitingInit()) {
                                cb.setWaitingInit(false);
                                cb.onInitialized(mVunglePub.isInitialized());
                            }
                        }
                    }
                });
            }
        });
    }

    void removeListener(String id){
        if (listeners.containsKey(id)){
            listeners.remove(id);
        }
    }

    void addListener(String id, VungleListener listener){
        removeListener(id);
        listeners.put(id, listener);
    }

    void playAd(String placement, AdConfig cfg, String id){
        if (currentPlayId != null)
            return;
        currentPlayId = id;
        mVunglePub.playAd(placement, cfg);
    }

    void onPause(){
        mVunglePub.onPause();
    }

    void onResume(){
        mVunglePub.onResume();
    }

    boolean isAdPlayable(String placement) {
        return mVunglePub.isAdPlayable(placement);
    }

    void loadAd(String placement){
        if (mVunglePub.isAdPlayable(placement)){
            notifyAdIsReady(placement);
            return;
        }
        mVunglePub.loadAd(placement);
    }

    private void notifyAdIsReady(String placement){
        for(VungleListener cb : listeners.values()){
            try{
                if (cb.getWaitingForPlacement() != null && cb.getWaitingForPlacement().equals(placement)){
                    cb.onAdAvailable();
                    cb.waitForAd(null);
                }
            }catch(Exception e){
                Log.w(TAG, e);
            }
        }
    }

    @Override
    public void onAdEnd(final @NonNull String placement, final boolean wasSuccessfulView, final boolean wasCallToActionClicked) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for(Map.Entry<String, VungleListener> entry : listeners.entrySet()){
                    try{
                        if (currentPlayId == null || currentPlayId.equals(entry.getKey()))
                            entry.getValue().onAdEnd(placement, wasSuccessfulView, wasCallToActionClicked);
                    }catch(Exception e){
                        Log.w(TAG, e);
                    }
                }
                currentPlayId = null;
            }
        });
    }

    @Override
    public void onAdStart(final @NonNull String placement) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for(Map.Entry<String, VungleListener> entry : listeners.entrySet()){
                    try{
                        if (currentPlayId == null || currentPlayId.equals(entry.getKey()))
                            entry.getValue().onAdStart(placement);
                    }catch(Exception e){
                        Log.w(TAG, e);
                    }
                }
            }
        });
    }

    @Override
    public void onUnableToPlayAd(final @NonNull String placement, String reason) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for(Map.Entry<String, VungleListener> entry : listeners.entrySet()){
                    try{
                        if (currentPlayId == null || currentPlayId.equals(entry.getKey()))
                            entry.getValue().onAdFail(placement);
                    }catch(Exception e){
                        Log.w(TAG, e);
                    }
                }
                currentPlayId = null;
            }
        });
    }

    @Override
    public void onAdAvailabilityUpdate(final @NonNull String placement, boolean isAdAvailable) {
        if (!isAdAvailable)
            return;
        handler.post(new Runnable() {
            @Override
            public void run() {
                notifyAdIsReady(placement);
            }
        });
    }
}
