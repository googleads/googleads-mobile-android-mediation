package com.google.ads.mediation.verizon;


import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.verizon.ads.ActivityStateManager;
import com.verizon.ads.RequestMetadata;
import com.verizon.ads.VASAds;
import com.verizon.ads.edition.StandardEdition;
import com.verizon.ads.inlineplacement.InlineAdFactory;
import com.verizon.ads.interstitialplacement.InterstitialAdFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;

public class VerizonMediationAdapter implements MediationBannerAdapter,
        MediationInterstitialAdapter {

    private static final String VERSION = "1.1.1.0";
    private static final String PLACEMENT_KEY = "placement_id";
    private static final String SITE_KEY = "site_id";
    private static final String ORANGE_PLACEMENT_KEY = "position";
    private static final String DCN_KEY = "dcn";

    protected static String TAG = VerizonMediationAdapter.class.getSimpleName();

    @SuppressWarnings("FieldCanBeLocal")
    private static String MEDIATOR_ID = "AdMobVAS-" + VERSION;

    static {
        Log.i(TAG, "Verizon Ads SDK Adapter Version: " + MEDIATOR_ID);
    }

    private LinearLayout internalView;
    private WeakReference<Context> contextWeakRef;
    private InlineAdFactory inlineAdFactory;
    private AdapterInterstitialListener adapterInterstitialListener;
    private MediationInterstitialListener mediationInterstitialListener;
    private AdapterInlineListener adapterInlineListener;
    private InterstitialAdFactory interstitialAdFactory;

    @Override
    public void requestBannerAd(final Context context, final MediationBannerListener listener,
                                final Bundle serverParameters,
                                com.google.android.gms.ads.AdSize adSize,
                                final MediationAdRequest mediationAdRequest,
                                final Bundle mediationExtras) {

        String placementId = fetchPlacementId(serverParameters);

        setContext(context);

        if (!initializeSDK(context, mediationExtras, serverParameters)) {
            Log.e(TAG, "Unable to initialize Verizon Ads SDK");

            ThreadUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {

                    listener.onAdFailedToLoad(VerizonMediationAdapter.this,
                            AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
            });

            return;
        }

        setCoppaValue(mediationAdRequest);

        adSize = normalizeSize(context, adSize);

        internalView = new LinearLayout(context);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        internalView.setLayoutParams(lp);

        try {
            VASAds.setLocationEnabled((mediationAdRequest.getLocation() != null));

            adapterInlineListener = new AdapterInlineListener(this, listener, internalView);
            inlineAdFactory = new InlineAdFactory(context, placementId,
                    Collections.singletonList(
                            new com.verizon.ads.inlineplacement.AdSize(adSize.getWidth(),
                                    adSize.getHeight())),
                    adapterInlineListener);

            inlineAdFactory.setRequestMetaData(getRequestMetadata(mediationAdRequest));

            inlineAdFactory.load(adapterInlineListener);

        } catch (Exception e) {
            Log.e(TAG, "Failed to create InlineAd instance", e);

            ThreadUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {

                    listener.onAdFailedToLoad(VerizonMediationAdapter.this,
                            AdRequest.ERROR_CODE_INVALID_REQUEST);
                }
            });
        }
    }


    @Override
    public View getBannerView() {

        return internalView;
    }


    @Override
    public void requestInterstitialAd(final Context context,
                                      final MediationInterstitialListener listener,
                                      final Bundle serverParameters,
                                      final MediationAdRequest mediationAdRequest,
                                      final Bundle mediationExtras) {

        this.mediationInterstitialListener = listener;

        String placementId = fetchPlacementId(serverParameters);

        setContext(context);

        if (!initializeSDK(context, mediationExtras, serverParameters)) {
            Log.e(TAG, "Unable to initialize Verizon Ads SDK");

            ThreadUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {

                    listener.onAdFailedToLoad(VerizonMediationAdapter.this,
                            AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
            });

            return;
        }

        setCoppaValue(mediationAdRequest);

        try {
            VASAds.setLocationEnabled((mediationAdRequest.getLocation() != null));
            adapterInterstitialListener = new AdapterInterstitialListener(this, listener);
            interstitialAdFactory = new InterstitialAdFactory(context, placementId,
                    adapterInterstitialListener);
            interstitialAdFactory.setRequestMetaData(getRequestMetadata(mediationAdRequest));
            interstitialAdFactory.load(adapterInterstitialListener);

        } catch (Exception e) {
            Log.e(TAG, "Failed to create InterstitialAd instance", e);

            ThreadUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {

                    listener.onAdFailedToLoad(VerizonMediationAdapter.this,
                            AdRequest.ERROR_CODE_INVALID_REQUEST);
                }
            });
        }
    }


    @Override
    public void showInterstitial() {

        if (adapterInterstitialListener == null) {
            if (mediationInterstitialListener != null) {
                mediationInterstitialListener.onAdFailedToLoad(VerizonMediationAdapter.this,
                        AdRequest.ERROR_CODE_INVALID_REQUEST);
            }

            return;
        }

        try {
            adapterInterstitialListener.show(getContext());
        } catch (Exception e) {
            Log.e(TAG, "Error occurred attempting to show Interstitial Ad.", e);
        }
    }


    @Override
    public void onDestroy() {

        Log.i(TAG, "Aborting.");
        if (inlineAdFactory != null) {
            inlineAdFactory.abortLoad();
        }

        if (interstitialAdFactory != null) {
            interstitialAdFactory.abortLoad();
        }


        if (adapterInterstitialListener != null) {
            adapterInterstitialListener.destroy();
        }

        if (adapterInlineListener != null) {
            adapterInlineListener.destroy();
        }

    }


    @Override
    public void onPause() {

    }


    @Override
    public void onResume() {

    }


    private RequestMetadata getRequestMetadata(final MediationAdRequest mediationAdRequest) {

        RequestMetadata.Builder requestMetadataBuilder = new RequestMetadata.Builder();

        // Keywords
        if (mediationAdRequest.getKeywords() != null) {
            requestMetadataBuilder.setKeywords(new ArrayList<>(mediationAdRequest.getKeywords()));
        }

        requestMetadataBuilder.setMediator(MEDIATOR_ID);

        return requestMetadataBuilder.build();
    }


    private void setCoppaValue(final MediationAdRequest mediationAdRequest) {
        // COPPA
        if (mediationAdRequest.taggedForChildDirectedTreatment() ==
                MediationAdRequest.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE) {
            VASAds.setCoppa(true);
        } else if (mediationAdRequest.taggedForChildDirectedTreatment() ==
                MediationAdRequest.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE) {
            VASAds.setCoppa(false);
        }
    }


    private AdSize normalizeSize(final Context context,
                                 final AdSize adSize) {

        int width = adSize.getWidth();
        if (width < 0) {
            float density = context.getResources().getDisplayMetrics().density;
            width = Math.round(adSize.getWidthInPixels(context) / density);
        }

        ArrayList<AdSize> potentials = new ArrayList<>(3);
        potentials.add(0, new AdSize(width, 50));
        potentials.add(1, new AdSize(width, 90));
        potentials.add(2, new AdSize(width, 250));
        Log.i(TAG, "Potential ad sizes: " + potentials.toString());

        return findClosestSize(context, adSize, potentials);
    }

    private boolean initializeSDK(final Context context, final Bundle mediationExtras,
                                  final Bundle serverParams) {

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            Log.e(TAG, "Verizon Ads SDK minimum supported API is 16");

            return false;
        }

        boolean success = true;

        if (!VASAds.isInitialized()) {

            if (!(context instanceof Activity)) {
                Log.e(TAG, "StandardEdition.initialize must be explicitly called when " +
                        "instantiating the AdMob " +
                        "AdView or InterstitialAd without an Activity.");

                return false;
            }

            String siteId = null;
            try {
                if (mediationExtras != null && mediationExtras.containsKey(SITE_KEY)) {
                    siteId = mediationExtras.getString(SITE_KEY);
                }
                // If we get site ID from the serverParams (not yet implemented), overwrite
                // everything!
                if (serverParams != null && serverParams.containsKey(SITE_KEY)) {
                    siteId = serverParams.getString(SITE_KEY);
                }

                // Support for legacy Nexage and MM mediation
                if (TextUtils.isEmpty(siteId)) {
                    if (mediationExtras != null && mediationExtras.containsKey(DCN_KEY)) {
                        siteId = mediationExtras.getString(DCN_KEY);
                    }
                    // If we get site ID from the serverParams (not yet implemented), overwrite
                    // everything!
                    if (serverParams != null && serverParams.containsKey(DCN_KEY)) {
                        siteId = serverParams.getString(DCN_KEY);
                    }
                }

                if (TextUtils.isEmpty(siteId)) {
                    Log.e(TAG, "Verizon Ads SDK Site ID must be set in mediation extras or "
                            + "server params");

                    return false;
                }

                Log.d(TAG, "Using site ID: " + siteId);

                success = StandardEdition.initialize(((Activity) context).getApplication(), siteId);
            } catch (Exception e) {
                Log.e(TAG, "Error occurred initializing Verizon Ads SDK.", e);

                return false;
            }
        }

        VASAds.getActivityStateManager().setState((Activity) context,
                ActivityStateManager.ActivityState.RESUMED);

        return success;
    }


    private String fetchPlacementId(final Bundle serverParams) {

        String placementId = null;
        if (serverParams == null) {
            return null;
        } else if (serverParams.containsKey(VerizonMediationAdapter.PLACEMENT_KEY)) {
            placementId = serverParams.getString(VerizonMediationAdapter.PLACEMENT_KEY);
        } else if (serverParams.containsKey(VerizonMediationAdapter.ORANGE_PLACEMENT_KEY)) {
            placementId = serverParams.getString(VerizonMediationAdapter.ORANGE_PLACEMENT_KEY);
        }

        return placementId;
    }


    private void setContext(final Context context) {

        contextWeakRef = new WeakReference<>(context);
    }


    private Context getContext() {

        if (contextWeakRef == null) {

            return null;
        }

        return contextWeakRef.get();
    }

    // Start of helper code to remove when available in SDK

    /**
     * Find the closest supported AdSize from the list of potentials to the provided size. Returns
     * null if none are within given threshold size range.
     */
    public static AdSize findClosestSize(
            Context context, AdSize original, ArrayList<AdSize> potentials) {
        if (potentials == null || original == null) {
            return null;
        }
        float density = context.getResources().getDisplayMetrics().density;
        int actualWidth = Math.round(original.getWidthInPixels(context) / density);
        int actualHeight = Math.round(original.getHeightInPixels(context) / density);
        original = new AdSize(actualWidth, actualHeight);

        AdSize largestPotential = null;
        for (AdSize potential : potentials) {
            if (isSizeInRange(original, potential)) {
                if (largestPotential == null) {
                    largestPotential = potential;
                } else {
                    largestPotential = getLargerByArea(largestPotential, potential);
                }
            }
        }
        return largestPotential;
    }

    public static boolean isSizeInRange(AdSize original, AdSize potential) {
        if (potential == null) {
            return false;
        }
        double minWidthRatio = 0.5;
        double minHeightRatio = 0.7;

        int originalWidth = original.getWidth();
        int potentialWidth = potential.getWidth();
        int originalHeight = original.getHeight();
        int potentialHeight = potential.getHeight();

        if (originalWidth * minWidthRatio > potentialWidth || originalWidth < potentialWidth) {
            return false;
        }

        if (originalHeight * minHeightRatio > potentialHeight || originalHeight < potentialHeight) {
            return false;
        }
        return true;
    }

    private static AdSize getLargerByArea(AdSize size1, AdSize size2) {
        int area1 = size1.getWidth() * size1.getHeight();
        int area2 = size2.getWidth() * size2.getHeight();
        return area1 > area2 ? size1 : size2;
    }
    // End code to remove when available in SDK
}
