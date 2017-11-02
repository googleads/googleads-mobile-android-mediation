package com.applovin.mediation;

import android.os.Handler;
import android.os.Looper;

import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdLoadListener;
import com.applovin.sdk.AppLovinAdRewardListener;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;

/**
 * AppLovin SDK ad listener used with the AppLovin ad adapter for AdMob.
 *
 * @version 7.4.1.0
 */
class AppLovinAdListener implements AppLovinAdClickListener, AppLovinAdRewardListener,
        AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdVideoPlaybackListener {
    private static final Handler uiHandler = new Handler(Looper.getMainLooper());

    private boolean mIsRewarded;
    private boolean mFullyWatched;
    private String mAdType;
    private ApplovinAdapter mAdapter;
    private MediationInterstitialListener mInterstitialListener;
    private MediationRewardedVideoAdListener mRewardedListener;
    private RewardItem mReward;

    // AdMob preloads ads in bursts of 2 requests
    public static final int ADS_QUEUE_MIN_CAPACITY = 2;

    // Failsafe for when ads are loaded but discarded
    public static final Queue<AppLovinAd> ADS_QUEUE = new LinkedList<AppLovinAd>();

    AppLovinAdListener(ApplovinAdapter adapter, MediationRewardedVideoAdListener listener) {
        this.mAdapter = adapter;
        mRewardedListener = listener;
        mIsRewarded = true;
        mAdType = "Rewarded video";
    }

    AppLovinAdListener(ApplovinAdapter adapter, MediationInterstitialListener listener) {
        this.mAdapter = adapter;
        mInterstitialListener = listener;
        mIsRewarded = false;
        mAdType = "Interstitial";
    }

    public void updateAdMobListener(final MediationInterstitialListener listener) {
        this.mInterstitialListener = listener;
    }

    boolean hasAdReady() {
        return !ADS_QUEUE.isEmpty();
    }

    AppLovinAd dequeueAd() {
        return ADS_QUEUE.poll();
    }

    //region Ad Load Listener
    @Override
    public void adReceived(final AppLovinAd ad) {
        ApplovinAdapter.log(DEBUG, mAdType + " did load ad: " + ad.getAdIdNumber());

        runOnUiThread(new AdLoadRunnable(this) {
            @Override
            public void run() {
                if (mIsRewarded) {
                    mRewardedListener.onAdLoaded(mAdapter);
                } else {
                    ADS_QUEUE.offer(ad);

                    ApplovinAdapter.adLoaded(this.listener);
                    mInterstitialListener.onAdLoaded(mAdapter);
                }
            }

            ;
        });
    }

    @Override
    public void failedToReceiveAd(final int errorCode) {
        ApplovinAdapter.log(DEBUG, mAdType + " failed to load with error: " + errorCode);

        runOnUiThread(new AdLoadRunnable(this) {
            @Override
            public void run() {
                if (mIsRewarded) {
                    mRewardedListener.onAdFailedToLoad(mAdapter,
                            ApplovinAdapter.toAdMobErrorCode(errorCode));
                } else {
                    ApplovinAdapter.adLoadFailed(this.listener);
                    mInterstitialListener.onAdFailedToLoad(mAdapter,
                            ApplovinAdapter.toAdMobErrorCode(errorCode));
                }
            }
        });
    }
    //endregion

    //region Ad Display Listener
    @Override
    public void adDisplayed(final AppLovinAd ad) {
        ApplovinAdapter.log(DEBUG, mAdType + " displayed");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mIsRewarded) {
                    mRewardedListener.onAdOpened(mAdapter);
                } else {
                    mInterstitialListener.onAdOpened(mAdapter);
                }
            }
        });

        mFullyWatched = false;
    }

    @Override
    public void adHidden(final AppLovinAd ad) {
        ApplovinAdapter.log(DEBUG, mAdType + " dismissed");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mIsRewarded && mFullyWatched && mReward != null) {
                    ApplovinAdapter.log(DEBUG, "Rewarded "
                            + mReward.getAmount() + " " + mReward.getType());
                    mRewardedListener.onRewarded(mAdapter, mReward);
                }

                if (mIsRewarded) {
                    mRewardedListener.onAdClosed(mAdapter);
                    mReward = null;
                } else {
                    mInterstitialListener.onAdClosed(mAdapter);
                }
            }
        });
    }
    //endregion

    //region Ad Click Listener
    @Override
    public void adClicked(final AppLovinAd ad) {
        ApplovinAdapter.log(DEBUG, mAdType + " clicked");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mIsRewarded) {
                    mRewardedListener.onAdClicked(mAdapter);
                    mRewardedListener.onAdLeftApplication(mAdapter);
                } else {
                    mInterstitialListener.onAdClicked(mAdapter);
                    mInterstitialListener.onAdLeftApplication(mAdapter);
                }
            }
        });
    }
    //endregion

    //region Video Playback Listener
    @Override
    public void videoPlaybackBegan(AppLovinAd ad) {
        ApplovinAdapter.log(DEBUG, mAdType + " playback began");

        if (mIsRewarded) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mRewardedListener.onVideoStarted(mAdapter);
                }
            });
        }
    }

    @Override
    public void videoPlaybackEnded(AppLovinAd ad, double percentViewed, boolean fullyWatched) {
        ApplovinAdapter.log(DEBUG,
                mAdType + " playback ended at playback percent: " + percentViewed);
        this.mFullyWatched = fullyWatched;
    }
    //endregion

    //region Reward Listener
    @Override
    public void userOverQuota(final AppLovinAd appLovinAd, final Map map) {
        ApplovinAdapter.log(ERROR,
                "Rewarded video validation request for ad did exceed quota with response: " + map);
    }

    @Override
    public void validationRequestFailed(final AppLovinAd appLovinAd, final int errorCode) {
        ApplovinAdapter.log(ERROR,
                "Rewarded video validation request for ad failed with error code: " + errorCode);
    }

    @Override
    public void userRewardRejected(final AppLovinAd appLovinAd, final Map map) {
        ApplovinAdapter.log(ERROR,
                "Rewarded video validation request was rejected with response: " + map);
    }

    @Override
    public void userDeclinedToViewAd(final AppLovinAd appLovinAd) {
        ApplovinAdapter.log(DEBUG, "User declined to view rewarded video");
    }

    @Override
    public void userRewardVerified(final AppLovinAd ad, final Map map) {
        final String currency = (String) map.get("currency");
        final String amountStr = (String) map.get("amount");
        final int amount = (int) Double.parseDouble(amountStr); // AppLovin returns amount as double

        ApplovinAdapter.log(DEBUG, "Verified " + amount + " " + currency);

        mReward = new AppLovinRewardItem(amount, currency);
    }
    //endregion

    /**
     * Reward item wrapper class.
     */
    private static final class AppLovinRewardItem
            implements RewardItem {
        private final int mAmount;
        private final String mType;

        private AppLovinRewardItem(final int amount, final String type) {
            this.mAmount = amount;
            this.mType = type;
        }

        @Override
        public String getType() {
            return mType;
        }

        @Override
        public int getAmount() {
            return mAmount;
        }
    }

    /**
     * A {@link Runnable} to send success and failure callbacks on main thread.
     */
    private abstract class AdLoadRunnable implements Runnable {
        AppLovinAdListener listener;

        AdLoadRunnable(AppLovinAdListener listener) {
            this.listener = listener;
        }
    }

    private static void runOnUiThread(final AdLoadRunnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            uiHandler.post(runnable);
        }
    }

    private static void runOnUiThread(final Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            uiHandler.post(runnable);
        }
    }
}
