package com.applovin.mediation;

import com.applovin.sdk.AppLovinAd;
import com.applovin.sdk.AppLovinAdClickListener;
import com.applovin.sdk.AppLovinAdDisplayListener;
import com.applovin.sdk.AppLovinAdRewardListener;
import com.applovin.sdk.AppLovinAdVideoPlaybackListener;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;

import java.util.Map;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;

/**
 * Created by Thomas So on 1/26/18.
 */

class AppLovinIncentivizedAdListener
        implements AppLovinAdRewardListener, AppLovinAdDisplayListener, AppLovinAdClickListener, AppLovinAdVideoPlaybackListener
{
    private final ApplovinAdapter                  mAdapter;
    private final MediationRewardedVideoAdListener mMediationRewardedVideoAdListener;

    private boolean            mFullyWatched;
    private AppLovinRewardItem mRewardItem;

    AppLovinIncentivizedAdListener(ApplovinAdapter adapter, MediationRewardedVideoAdListener mediationRewardedVideoAdListener)
    {
        mAdapter = adapter;
        mMediationRewardedVideoAdListener = mediationRewardedVideoAdListener;
    }

    //
    // Ad Display Listener
    //

    @Override
    public void adDisplayed(AppLovinAd ad)
    {
        ApplovinAdapter.log( DEBUG, "Rewarded video displayed" );
        mMediationRewardedVideoAdListener.onAdOpened( mAdapter );
    }

    @Override
    public void adHidden(AppLovinAd ad)
    {
        ApplovinAdapter.log( DEBUG, "Rewarded video dismissed" );

        if ( mFullyWatched && mRewardItem != null )
        {
            mMediationRewardedVideoAdListener.onRewarded( mAdapter, mRewardItem );
        }

        mMediationRewardedVideoAdListener.onAdClosed( mAdapter );

        // Clear states in the case this listener gets re-used in the future
        mFullyWatched = false;
        mRewardItem = null;
    }

    //
    // Ad Click Listener
    //

    @Override
    public void adClicked(AppLovinAd ad)
    {
        ApplovinAdapter.log( DEBUG, "Rewarded video clicked" );

        mMediationRewardedVideoAdListener.onAdClicked( mAdapter );
        mMediationRewardedVideoAdListener.onAdLeftApplication( mAdapter );
    }

    //
    // Video Playback Listener
    //

    @Override
    public void videoPlaybackBegan(AppLovinAd ad)
    {
        ApplovinAdapter.log( DEBUG, "Rewarded video playback began" );
        mMediationRewardedVideoAdListener.onVideoStarted( mAdapter );
    }

    @Override
    public void videoPlaybackEnded(AppLovinAd ad, double percentViewed, boolean fullyWatched)
    {
        ApplovinAdapter.log( DEBUG, "Rewarded video playback ended at playback percent: " + percentViewed + "%" );
        mFullyWatched = fullyWatched;
    }

    //
    // Reward Listener
    //

    @Override
    public void userOverQuota(AppLovinAd ad, Map response)
    {
        ApplovinAdapter.log( ERROR, "Rewarded video validation request for ad did exceed quota with response: " + response );
    }

    @Override
    public void validationRequestFailed(AppLovinAd ad, int code)
    {
        ApplovinAdapter.log( ERROR, "Rewarded video validation request for ad failed with error code: " + code );
    }

    @Override
    public void userRewardRejected(AppLovinAd ad, Map response)
    {
        ApplovinAdapter.log( ERROR, "Rewarded video validation request was rejected with response: " + response );
    }

    @Override
    public void userDeclinedToViewAd(AppLovinAd ad)
    {
        ApplovinAdapter.log( DEBUG, "User declined to view rewarded video" );
    }

    @Override
    public void userRewardVerified(AppLovinAd ad, Map response)
    {
        final String currency = (String) response.get( "currency" );
        final String amountStr = (String) response.get( "amount" );
        final int amount = (int) Double.parseDouble( amountStr ); // AppLovin returns amount as double

        ApplovinAdapter.log( DEBUG, "Rewarded " + amount + " " + currency );

        mRewardItem = new AppLovinRewardItem( amount, currency );
    }

    /**
     * Reward item wrapper class.
     */
    private static final class AppLovinRewardItem
            implements RewardItem
    {
        private final int    mAmount;
        private final String mType;

        private AppLovinRewardItem(int amount, final String type)
        {
            mAmount = amount;
            mType = type;
        }

        @Override
        public String getType()
        {
            return mType;
        }

        @Override
        public int getAmount()
        {
            return mAmount;
        }
    }
}
