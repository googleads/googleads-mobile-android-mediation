package com.google.ads.mediation.mytarget;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdAdapter;
import com.google.android.gms.ads.reward.mediation.MediationRewardedVideoAdListener;
import com.my.target.ads.CustomParams;
import com.my.target.ads.InterstitialAd;

import java.util.Date;
import java.util.GregorianCalendar;

public class MyTargetRewardedAdapter implements MediationRewardedVideoAdAdapter
{
	private static final String TAG = "MyTargetRewardedAdapter";
	@Nullable
	private InterstitialAd interstitial;
	private boolean initialized;
	@Nullable
	private MediationRewardedVideoAdListener listener;

	@Override
	public void initialize(Context context,
						   MediationAdRequest mediationAdRequest,
						   String serverJSON,
						   MediationRewardedVideoAdListener mediationRewardedVideoAdListener,
						   Bundle serverParameters,
						   Bundle mediationExtras)
	{
		listener = mediationRewardedVideoAdListener;
		int slotId = MyTargetTools.checkAndGetSlotId(context, serverParameters);
		Log.d(TAG, "Requesting rewarded mediation, slotId: " + slotId);

		if (slotId < 0)
		{
			if (mediationRewardedVideoAdListener != null)
			{
				mediationRewardedVideoAdListener.onInitializationFailed(
						MyTargetRewardedAdapter.this,
						AdRequest.ERROR_CODE_INVALID_REQUEST);
			}
			return;
		}

		if (interstitial != null)
		{
			initialized = false;
			interstitial.destroy();
		}

		interstitial = new InterstitialAd(slotId, context);
		CustomParams params = interstitial.getCustomParams();
		params.setCustomParam(MyTargetTools.PARAM_MEDIATION_KEY,
													  MyTargetTools.PARAM_MEDIATION_VALUE);

		if (mediationAdRequest != null)
		{
			int gender = mediationAdRequest.getGender();
			Log.d(TAG, "Set gender to " + gender);
			params.setGender(gender);
			Date date = mediationAdRequest.getBirthday();
			if (date != null && date.getTime() != -1)
			{
				GregorianCalendar calendar = new GregorianCalendar();
				GregorianCalendar calendarNow = new GregorianCalendar();

				calendar.setTimeInMillis(date.getTime());
				int a = calendarNow.get(GregorianCalendar.YEAR) -
						calendar.get(GregorianCalendar.YEAR);
				if (a >= 0)
				{
					Log.d(TAG, "Set age to " + a);
					params.setAge(a);
				}
			}
		}

		MyTargetInterstitialListener interstitialAdListener = null;
		if (mediationRewardedVideoAdListener != null)
		{
			interstitialAdListener = new MyTargetInterstitialListener(mediationRewardedVideoAdListener);
		}

		interstitial.setListener(interstitialAdListener);
		initialized = true;
		if (mediationRewardedVideoAdListener != null)
		{
			mediationRewardedVideoAdListener.onInitializationSucceeded(MyTargetRewardedAdapter.this);
		}
		Log.d(TAG, "Ad initialized ");
	}

	@Override
	public void loadAd(@Nullable MediationAdRequest request, @Nullable Bundle bundle,
					   @Nullable Bundle bundle1)
	{
		Log.d(TAG, "Load Ad");
		if (interstitial != null)
		{
			interstitial.load();
		}
		else
		{
			Log.d(TAG, "Ad failed to load: interstitial is null");
			if (listener != null)
			{
				listener.onAdFailedToLoad(MyTargetRewardedAdapter.this, AdRequest.ERROR_CODE_INTERNAL_ERROR);
			}
		}
	}

	@Override
	public void showVideo()
	{
		Log.d(TAG, "Show video");
		if (interstitial != null)
		{
			interstitial.show();
		}
	}

	@Override
	public boolean isInitialized()
	{
		return initialized;
	}

	@Override
	public void onDestroy()
	{
		if (interstitial != null)
		{
			interstitial.setListener(null);
		}
	}

	@Override
	public void onPause()
	{
	}

	@Override
	public void onResume()
	{
	}

	/**
	 * MyTarget doesn't provide reward for this moment, so we use this
	 */
	private static class MyTargetReward implements RewardItem
	{
		@Override
		public String getType()
		{
			return "";
		}

		@Override
		public int getAmount()
		{
			return 1;
		}
	}

	/**
	 * A {@link MyTargetInterstitialListener} used to forward myTarget banner events to Google
	 */
	private class MyTargetInterstitialListener implements InterstitialAd.InterstitialAdListener
	{

		private final @NonNull MediationRewardedVideoAdListener listener;

		MyTargetInterstitialListener(final @NonNull MediationRewardedVideoAdListener listener)
		{
			this.listener = listener;
		}

		@Override
		public void onLoad(final InterstitialAd ad)
		{
			Log.d(TAG, "Ad loaded");
			listener.onAdLoaded(MyTargetRewardedAdapter.this);
		}

		@Override
		public void onNoAd(final String s, final InterstitialAd ad)
		{
			Log.d(TAG, "Failed to load: " + s);
			listener.onAdFailedToLoad(MyTargetRewardedAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
		}

		@Override
		public void onClick(final InterstitialAd ad)
		{
			Log.d(TAG, "Ad clicked");
			listener.onAdClicked(MyTargetRewardedAdapter.this);
			// click redirects user to Google Play, or web browser, so we can notify
			// about left application
			listener.onAdLeftApplication(MyTargetRewardedAdapter.this);
		}

		@Override
		public void onDismiss(final InterstitialAd ad)
		{
			Log.d(TAG, "Ad dismissed");
			listener.onAdClosed(MyTargetRewardedAdapter.this);
		}

		@Override
		public void onVideoCompleted(final InterstitialAd ad)
		{
			Log.d(TAG, "Video completed");
			listener.onRewarded(MyTargetRewardedAdapter.this, new MyTargetReward());
		}

		@Override
		public void onDisplay(final InterstitialAd ad)
		{
			Log.d(TAG, "Ad displayed");
			listener.onAdOpened(MyTargetRewardedAdapter.this);
			// myTarget has no callback for starting video, but rewarded video always
			// has autoplay param and starts immediately, so we can notify Google about this
			listener.onVideoStarted(MyTargetRewardedAdapter.this);
		}
	}
}
