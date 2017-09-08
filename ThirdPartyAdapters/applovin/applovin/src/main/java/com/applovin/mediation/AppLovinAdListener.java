package com.applovin.mediation;

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
import java.util.PriorityQueue;
import java.util.Queue;

import android.os.Handler;
import android.os.Looper;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;

/**
 * AppLovin SDK ad listener used with the AppLovin ad adapter for AdMob.
 * @version 7.3.2.0
 */

class AppLovinAdListener implements AppLovinAdClickListener, AppLovinAdRewardListener,
        AppLovinAdLoadListener, AppLovinAdDisplayListener, AppLovinAdVideoPlaybackListener {
    private static final Handler uiHandler       = new Handler( Looper.getMainLooper() );

    private boolean                          isRewarded;
    private boolean                          fullyWatched;
    private String                           adType;
    private ApplovinAdapter                  adapter;
    private MediationInterstitialListener    interstitialListener;
    private MediationRewardedVideoAdListener rewardedListener;
    private RewardItem                       reward;

    // AdMob preloads ads in bursts of 2 requests
    public static final int ADS_QUEUE_MIN_CAPACITY = 2;

    // Failsafe for when ads are loaded but discarded
    public static final Queue<AppLovinAd> ADS_QUEUE = new LinkedList<AppLovinAd>();

    AppLovinAdListener( ApplovinAdapter adapter, MediationRewardedVideoAdListener listener )
    {
        this.adapter     = adapter;
        rewardedListener = listener;
        isRewarded       = true;
        adType           = "Rewarded video";
    }

    AppLovinAdListener( ApplovinAdapter adapter, MediationInterstitialListener listener )
    {
        this.adapter         = adapter;
        interstitialListener = listener;
        isRewarded           = false;
        adType               = "Interstitial";
    }

    public void updateAdMobListener(final MediationInterstitialListener listener)
    {
        this.interstitialListener = listener;
    }

    boolean hasAdReady()
    {
        return !ADS_QUEUE.isEmpty();
    }

    AppLovinAd dequeueAd()
    {
        return ADS_QUEUE.poll();
    }

    //
    // Ad Load Listener
    //

    @Override
    public void adReceived(final AppLovinAd ad)
    {
        ApplovinAdapter.log( DEBUG, adType + " did load ad: " + ad.getAdIdNumber() );

        runOnUiThread( new AdLoadRunnable( this )
        {
            @Override
            public void run(  )
            {
                if ( isRewarded )
                {
                    rewardedListener.onAdLoaded( adapter );
                }
                else
                {
                    ADS_QUEUE.offer( ad );

                    ApplovinAdapter.adLoaded( this.listener );
                    interstitialListener.onAdLoaded( adapter );
                }
            };
        });
    }

    @Override
    public void failedToReceiveAd(final int errorCode)
    {
        ApplovinAdapter.log( DEBUG, adType + " failed to load with error: " + errorCode );

        runOnUiThread(new AdLoadRunnable( this ) {
            @Override
            public void run() {
                if ( isRewarded )
                {
                    rewardedListener.onAdFailedToLoad( adapter, ApplovinAdapter.toAdMobErrorCode( errorCode ) );
                }
                else
                {
                    ApplovinAdapter.adLoadFailed( this.listener );
                    interstitialListener.onAdFailedToLoad( adapter, ApplovinAdapter.toAdMobErrorCode( errorCode ) );
                }
            }
        });
    }

    //
    // Ad Display Listener
    //

    @Override
    public void adDisplayed(final AppLovinAd ad)
    {
        ApplovinAdapter.log( DEBUG, adType + " displayed" );

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if ( isRewarded )
                {
                    rewardedListener.onAdOpened( adapter );
                }
                else
                {
                    interstitialListener.onAdOpened( adapter );
                }
            }
        });

        fullyWatched = false;
    }

    @Override
    public void adHidden(final AppLovinAd ad)
    {
        ApplovinAdapter.log( DEBUG, adType + " dismissed" );

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            if ( isRewarded && fullyWatched && reward != null )
            {
                ApplovinAdapter.log( DEBUG, "Rewarded " + reward.getAmount() + " " + reward.getType() );
                rewardedListener.onRewarded( adapter, reward );
            }

            if ( isRewarded )
            {
                rewardedListener.onAdClosed( adapter );
                reward = null;
            }
            else
            {
                interstitialListener.onAdClosed( adapter );
            }
            }
        });
    }

    //
    // Ad Click Listener
    //

    @Override
    public void adClicked(final AppLovinAd ad)
    {
        ApplovinAdapter.log( DEBUG, adType + " clicked" );

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
            if ( isRewarded )
            {
                rewardedListener.onAdClicked( adapter );
                rewardedListener.onAdLeftApplication( adapter );
            }
            else
            {
                interstitialListener.onAdClicked( adapter );
                interstitialListener.onAdLeftApplication( adapter );
            }
            }
        });
    }

    //
    // Video Playback Listener
    //

    @Override
    public void videoPlaybackBegan(AppLovinAd ad)
    {
        ApplovinAdapter.log( DEBUG, adType + " playback began" );

        if ( isRewarded )
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    rewardedListener.onVideoStarted( adapter );
                }
            });
        }
    }

    @Override
    public void videoPlaybackEnded(AppLovinAd ad, double percentViewed, boolean fullyWatched)
    {
        ApplovinAdapter.log( DEBUG, adType + " playback ended at playback percent: " + percentViewed );
        this.fullyWatched = fullyWatched;
    }

    //
    // Reward Listener
    //

    @Override
    public void userOverQuota(final AppLovinAd appLovinAd, final Map map)
    {
        ApplovinAdapter.log( ERROR, "Rewarded video validation request for ad did exceed quota with response: " + map );
    }

    @Override
    public void validationRequestFailed(final AppLovinAd appLovinAd, final int errorCode)
    {
        ApplovinAdapter.log( ERROR, "Rewarded video validation request for ad failed with error code: " + errorCode );
    }

    @Override
    public void userRewardRejected(final AppLovinAd appLovinAd, final Map map)
    {
        ApplovinAdapter.log( ERROR, "Rewarded video validation request was rejected with response: " + map );
    }

    @Override
    public void userDeclinedToViewAd(final AppLovinAd appLovinAd)
    {
        ApplovinAdapter.log( DEBUG, "User declined to view rewarded video" );
    }

    @Override
    public void userRewardVerified(final AppLovinAd ad, final Map map)
    {
        final String currency  = (String) map.get( "currency" );
        final String amountStr = (String) map.get( "amount" );
        final int    amount    = (int) Double.parseDouble( amountStr ); // AppLovin returns amount as double

        ApplovinAdapter.log( DEBUG, "Verified " + amount + " " + currency );

        reward = new AppLovinRewardItem( amount, currency );
    }

    /**
     * Reward item wrapper class.
     */
    private static final class AppLovinRewardItem
            implements RewardItem
    {
        private final int    amount;
        private final String type;

        private AppLovinRewardItem(final int amount, final String type)
        {
            this.amount = amount;
            this.type   = type;
        }

        @Override
        public String getType()
        {
            return type;
        }

        @Override
        public int getAmount()
        {
            return amount;
        }
    }

    private abstract class AdLoadRunnable implements Runnable
    {
        AppLovinAdListener listener;
        AdLoadRunnable( AppLovinAdListener listener )
        {
            this.listener = listener;
        }
    }

    private static void runOnUiThread(final AdLoadRunnable runnable)
    {
        if ( Looper.myLooper() == Looper.getMainLooper() )
        {
            runnable.run();
        }
        else
        {
            uiHandler.post( runnable );
        }
    }

    private static void runOnUiThread(final Runnable runnable)
    {
        if ( Looper.myLooper() == Looper.getMainLooper() )
        {
            runnable.run();
        }
        else
        {
            uiHandler.post( runnable );
        }
    }
}
