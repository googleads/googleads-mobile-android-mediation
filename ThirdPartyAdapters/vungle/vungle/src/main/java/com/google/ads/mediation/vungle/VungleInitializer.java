package com.google.ads.mediation.vungle;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.vungle.warren.InitCallback;
import com.vungle.warren.Vungle;
import com.vungle.warren.network.VungleApiClient;

import java.util.ArrayList;

public class VungleInitializer implements InitCallback {

    private static VungleInitializer instance;
    private boolean mIsInitializing = false;

    private ArrayList<VungleInitializationListener> mInitListeners;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    public static VungleInitializer getInstance() {
        if (instance == null) {
            instance = new VungleInitializer();
        }
        return instance;
    }

    private VungleInitializer() {
        mInitListeners = new ArrayList<>();
    }

    boolean isInitializing() {
        return mIsInitializing;
    }

    public boolean isInitialized() {
        return Vungle.isInitialized();
    }

    public void initialize(String appId, Context context, VungleInitializationListener listener) {
        if (isInitializing()) {
            mInitListeners.add(listener);
            return;
        }

        if (isInitialized()) {
            listener.onInitializeSuccess();
            return;
        }

        mIsInitializing = true;

        VungleApiClient.addWrapperInfo(VungleApiClient.WrapperFramework.admob,
                com.vungle.warren.BuildConfig.VERSION_NAME.replace('.', '_'));
        Vungle.init(appId, context.getApplicationContext(), VungleInitializer.this);
        getInstance().mInitListeners.add(listener);
    }

    @Override
    public void onSuccess() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (VungleInitializationListener listener : mInitListeners) {
                    listener.onInitializeSuccess();
                }
                mInitListeners.clear();
            }
        });
        mIsInitializing = false;
    }

    @Override
    public void onError(final Throwable throwable) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                for (VungleInitializationListener listener : mInitListeners) {
                    listener.onInitializeError(throwable.getLocalizedMessage());
                }
                mInitListeners.clear();
            }
        });
        mIsInitializing = false;
    }

    @Override
    public void onAutoCacheAdAvailable(String placementID) {
        // Unused
    }

    public interface VungleInitializationListener {
        void onInitializeSuccess();
        void onInitializeError(String errorMessage);
    }

}
