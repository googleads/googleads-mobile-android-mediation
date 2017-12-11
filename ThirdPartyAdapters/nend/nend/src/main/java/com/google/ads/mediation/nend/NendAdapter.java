package com.google.ads.mediation.nend;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;

import net.nend.android.NendAdInformationListener;
import net.nend.android.NendAdInterstitial;
import net.nend.android.NendAdInterstitial.NendAdInterstitialClickType;
import net.nend.android.NendAdInterstitial.NendAdInterstitialShowResult;
import net.nend.android.NendAdInterstitial.NendAdInterstitialStatusCode;
import net.nend.android.NendAdInterstitial.OnCompletionListener;
import net.nend.android.NendAdInterstitialVideo;
import net.nend.android.NendAdVideo;
import net.nend.android.NendAdVideoListener;
import net.nend.android.NendAdView;
import net.nend.android.NendAdView.NendError;

@SuppressWarnings("unused")
public class NendAdapter implements MediationBannerAdapter, MediationInterstitialAdapter, NendAdInformationListener {

    private static final String LOG_TAG = "NendAdapter";
    private NendAdView mNendAdView;
    private MediationBannerListener mListener;
    private MediationInterstitialListener mListenerInterstitial;
    private NendAdInterstitialVideo mNendAdInterstitialVideo;

    public static final String KEY_USER_ID = "key_user_id";
    public static final String KEY_INTERSTITIAL_TYPE = "key_interstitial_type";
    public enum InterstitialType {
        TYPE_VIDEO,
        TYPE_NORMAL
    }
    private enum InterstitialVideoStatus {
        PLAYING,
        PLAYING_WHEN_CLICKED,
        STOPPED
    }
    private InterstitialVideoStatus mInterstitialVideoStatus = InterstitialVideoStatus.PLAYING;

    private Activity mActivity;

    private boolean mIsDetached = false;
    private boolean mIsRequireLoadAd = false;

    private boolean mIsRequestBannerAd = false;
    private boolean mIsPausingWebView = false;

    private View.OnAttachStateChangeListener mAttachStateChangeListener = new View.OnAttachStateChangeListener() {
        @Override
        public void onViewAttachedToWindow(View v) {
            mNendAdView.setListener(NendAdapter.this);
            if (mIsRequireLoadAd) {
                mNendAdView.loadAd();
                mIsRequireLoadAd = false;
            }
            mIsDetached = false;
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
            mIsDetached = true;
        }
    };

    private void requireLoadAd() {
        if (mIsDetached && !mIsRequireLoadAd) {
            mIsRequireLoadAd = true;
        }
    }

    @Override
    public void onDestroy() {
        mNendAdView = null;
        mListener = null;
        mListenerInterstitial = null;
        mActivity = null;
        if (mNendAdInterstitialVideo != null) {
            mNendAdInterstitialVideo.releaseAd();
            mNendAdInterstitialVideo = null;
        }
    }

    @Override
    public void onPause() {
        if (mNendAdView != null) {
            if (mNendAdView.getChildAt(0) instanceof WebView) {
                mIsPausingWebView = true;
            }
            mNendAdView.pause();
            requireLoadAd();
        }
    }

    @Override
    public void onResume() {
        if (mNendAdView != null) {
            if (mIsPausingWebView) {
                mNendAdView.resume();
            }
            requireLoadAd();
            mIsPausingWebView = false;
        }
    }

    @Override
    public void requestInterstitialAd(
            Context context,
            MediationInterstitialListener listener,
            Bundle serverParameters,
            MediationAdRequest mediationAdRequest,
            Bundle mediationExtras
    ) {
        mListenerInterstitial = listener;

        if (!(context instanceof Activity)) {
            Log.w(LOG_TAG, "Context not an Activity");
            adFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
        } else {
            mActivity = (Activity) context;

            String apikey = serverParameters.getString("apiKey");
            int spotId = Integer.parseInt(serverParameters.getString("spotId"));
            if (mediationExtras != null) {
                final InterstitialType type = (InterstitialType) mediationExtras.getSerializable(KEY_INTERSTITIAL_TYPE);
                if (type == InterstitialType.TYPE_VIDEO) {
                    requestNendInterstialVideo(context, apikey, spotId, mediationExtras.getString(KEY_USER_ID, ""));
                    return;
                }
            }
            requestNendInterstitial(context, apikey, spotId);
        }
    }

    private void requestNendInterstitial(Context context, String apikey, int spotId) {
        NendAdInterstitial.loadAd(context, apikey, spotId);
        NendAdInterstitial.isAutoReloadEnabled = false;
        NendAdInterstitial.setListener(new OnCompletionListener() {
            @Override
            public void onCompletion(NendAdInterstitialStatusCode status) {
                switch (status) {
                    case SUCCESS:
                        adLoaded();
                        break;
                    case FAILED_AD_DOWNLOAD:
                        adFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
                        break;
                    case FAILED_AD_INCOMPLETE:
                        adFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
                        break;
                    case FAILED_AD_REQUEST:
                        adFailedToLoad(AdRequest.ERROR_CODE_INVALID_REQUEST);
                        break;
                    case INVALID_RESPONSE_TYPE:
                        adFailedToLoad(AdRequest.ERROR_CODE_INTERNAL_ERROR);
                        break;
                }
            }
        });
    }

    private void requestNendInterstialVideo(Context context, String apikey, int spotId, String userId) {
        mNendAdInterstitialVideo = new NendAdInterstitialVideo(context, spotId, apikey);
        mNendAdInterstitialVideo.setMediationName("AdMob");
        if (!TextUtils.isEmpty(userId)) {
            mNendAdInterstitialVideo.setUserId(userId);
        }
        mNendAdInterstitialVideo.setAdListener(new NendAdVideoListener() {
            @Override
            public void onLoaded(NendAdVideo nendAdVideo) {
                adLoaded();
            }

            @Override
            public void onFailedToLoad(NendAdVideo nendAdVideo, int errorCode) {
                adFailedToLoad(ErrorUtil.convertErrorCodeFromNendVideoToAdMob(errorCode));
            }

            @Override
            public void onFailedToPlay(NendAdVideo nendAdVideo) {
                Log.w(LOG_TAG, "Interstitial video ad failed to play...");
            }

            @Override
            public void onShown(NendAdVideo nendAdVideo) {
                adOpened();
            }

            @Override
            public void onClosed(NendAdVideo nendAdVideo) {
                adClosed();
                if (mInterstitialVideoStatus == InterstitialVideoStatus.PLAYING_WHEN_CLICKED) {
                    adLeftApplication();
                }
            }

            @Override
            public void onStarted(NendAdVideo nendAdVideo) {
                mInterstitialVideoStatus = InterstitialVideoStatus.PLAYING;
            }

            @Override
            public void onStopped(NendAdVideo nendAdVideo) {
                if (mInterstitialVideoStatus != InterstitialVideoStatus.PLAYING_WHEN_CLICKED) {
                    mInterstitialVideoStatus = InterstitialVideoStatus.STOPPED;
                }
            }

            @Override
            public void onCompleted(NendAdVideo nendAdVideo) {
                mInterstitialVideoStatus = InterstitialVideoStatus.STOPPED;
            }

            @Override
            public void onAdClicked(NendAdVideo nendAdVideo) {
                //NOTE: We'll delay sending "left application" for "close ad" can be sent first.
                switch (mInterstitialVideoStatus) {
                    case PLAYING:
                    case PLAYING_WHEN_CLICKED:
                        mInterstitialVideoStatus = InterstitialVideoStatus.PLAYING_WHEN_CLICKED;
                        break;
                    default:
                        adLeftApplication();
                        break;
                }
            }

            @Override
            public void onInformationClicked(NendAdVideo nendAdVideo) {
                adLeftApplication();
            }
        });
        mNendAdInterstitialVideo.loadAd();
    }

    @Override
    public void showInterstitial() {
        if (mNendAdInterstitialVideo != null) {
            showNendAdInterstitialVideo();
        } else {
            showNendAdInterstitial();
        }
    }

    private void showNendAdInterstitial() {
        NendAdInterstitialShowResult result = NendAdInterstitial.showAd(mActivity, new NendAdInterstitial.OnClickListener() {
            @Override
            public void onClick(NendAdInterstitialClickType clickType) {
                switch (clickType) {
                    case CLOSE:
                        adClosed();
                        break;
                    case DOWNLOAD:
                        adLeftApplication();
                        adClosed();
                        break;
                    case INFORMATION:
                        adLeftApplication();
                        adClosed();
                        break;
                    default:
                        break;
                }
            }
        });

        switch (result) {
            case AD_SHOW_SUCCESS:
                adOpened();
                break;
            case AD_SHOW_ALREADY:
                break;
            case AD_FREQUENCY_NOT_RECHABLE:
                break;
            case AD_REQUEST_INCOMPLETE:
                break;
            case AD_LOAD_INCOMPLETE:
                // Request is not start yet or requesting now..
                break;
            case AD_DOWNLOAD_INCOMPLETE:
                break;
            default:
                break;
        }
    }

    private void showNendAdInterstitialVideo() {
        if (mNendAdInterstitialVideo.isLoaded()) {
            mNendAdInterstitialVideo.showAd(mActivity);
        } else {
            Log.w(LOG_TAG, "Interstitial video ad is not ready...");
        }
    }

    @Override
    public View getBannerView() {
        return mNendAdView;
    }

    @Override
    public void requestBannerAd(
            Context context,
            MediationBannerListener listener,
            Bundle serverParameters,
            AdSize adSize,
            MediationAdRequest mediationAdRequest,
            Bundle mediationExtras
    ) {
        int adSizeWidth = adSize.getWidth();
        int adSizeHeight = adSize.getHeight();

        mListener = listener;

        // Available ad sizes are listed below.
        // -> https://github.com/fan-ADN/nendSDK-Android/wiki/About-Ad-Sizes#available-ad-sizes-are-listed-below
        if ((adSizeWidth == 320 && adSizeHeight == 50) ||
                (adSizeWidth == 320 && adSizeHeight == 100) ||
                (adSizeWidth == 300 && adSizeHeight == 250) ||
                (adSizeWidth == 728 && adSizeHeight == 90)) {
            String apikey = serverParameters.getString("apiKey");
            String spotId = serverParameters.getString("spotId");
            if (apikey != null && spotId != null) {
                int intSpotId = Integer.parseInt(spotId);
                mNendAdView = new NendAdView(context, intSpotId, apikey);

                // NOTE: Use the reload function of AdMob mediation instead of NendAdView.
                //       So, reload function of NendAdView should be stopped.
                mNendAdView.pause();

                mNendAdView.setListener(this);
                mNendAdView.addOnAttachStateChangeListener(mAttachStateChangeListener);
                mNendAdView.loadAd();

                mIsRequestBannerAd = true;
            } else {
                Log.w(LOG_TAG, "apikey and spotId is must not be null");
                if (mListener != null) {
                    mListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                }
            }
        } else {
            Log.w(LOG_TAG, "Invalid Ad type");
            if (mListener != null) {
                mListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
            }
        }
    }

    /**
     * NendAdListener callbacks
     */
    @Override
    public void onReceiveAd(NendAdView adView) {
        if (mListener != null && mIsRequestBannerAd) {
            // New request or auto reload from AdMob network.
            mListener.onAdLoaded(this);
            mIsRequestBannerAd = false;
        } else {
            // This case is not need to send onAdLoaded to AdMob network.
            Log.d(LOG_TAG, "This ad is auto reloading by nend network.");
        }
    }

    @Override
    public void onFailedToReceiveAd(NendAdView adView) {
        if (!mIsRequestBannerAd) {
            // This case is not need to call listener function to AdMob network.
            return;
        }
        mIsRequestBannerAd = false;
        NendError nendError = adView.getNendError();
        if (mListener != null) {
            switch (nendError) {
                case INVALID_RESPONSE_TYPE:
                    mListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                    break;
                case FAILED_AD_DOWNLOAD:
                    mListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
                    break;
                case FAILED_AD_REQUEST:
                    mListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INVALID_REQUEST);
                    break;
                case AD_SIZE_TOO_LARGE:
                    mListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                    break;
                case AD_SIZE_DIFFERENCES:
                    mListener.onAdFailedToLoad(this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
                    break;
            }
        }
    }

    @Override
    public void onClick(NendAdView adView) {
        if (mListener != null) {
            mListener.onAdOpened(this);
            mListener.onAdLeftApplication(this);
        }
    }

    @Override
    public void onDismissScreen(NendAdView adView) {
        if (mListener != null) {
            mListener.onAdClosed(this);
        }
    }

    @Override
    public void onInformationButtonClick(NendAdView adView) {
        if (mListener != null) {
            mListener.onAdLeftApplication(this);
        }
    }

    /**
     * MediationInterstitialListener callbacks
     */
    public void adLeftApplication() {
        if (mListenerInterstitial != null) {
            mListenerInterstitial.onAdLeftApplication(this);
        }
    }

    public void adClosed() {
        if (mListenerInterstitial != null) {
            mListenerInterstitial.onAdClosed(this);
        }
    }

    public void adFailedToLoad(int errorCode) {
        if (mListenerInterstitial != null) {
            mListenerInterstitial.onAdFailedToLoad(this, errorCode);
        }
    }

    public void adLoaded() {
        if (mListenerInterstitial != null) {
            mListenerInterstitial.onAdLoaded(this);
        }
    }

    public void adOpened() {
        if (mListenerInterstitial != null) {
            mListenerInterstitial.onAdOpened(this);
        }
    }
}