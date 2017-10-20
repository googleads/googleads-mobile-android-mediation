package com.google.ads.mediation.mytarget;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.google.android.gms.ads.mediation.MediationBannerAdapter;
import com.google.android.gms.ads.mediation.MediationBannerListener;
import com.google.android.gms.ads.mediation.MediationInterstitialAdapter;
import com.google.android.gms.ads.mediation.MediationInterstitialListener;
import com.my.target.ads.CustomParams;
import com.my.target.ads.InterstitialAd;
import com.my.target.ads.MyTargetView;
import com.my.target.ads.MyTargetView.MyTargetViewListener;

import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Mediation adapter for myTarget.
 */

public class MyTargetAdapter implements MediationBannerAdapter, MediationInterstitialAdapter
{
	private static final @NonNull String TAG = "MyTargetAdapter";

	private @Nullable MyTargetView myTargetView;
	private @Nullable InterstitialAd interstitial;

	@Override
	public void requestBannerAd(Context context,
								MediationBannerListener mediationBannerListener,
								Bundle serverParameters,
								AdSize adSize,
								MediationAdRequest mediationAdRequest,
								Bundle mediationExtras)
	{
		int slotId = MyTargetTools.checkAndGetSlotId(context, serverParameters);
		Log.d(TAG, "Requesting myTarget banner mediation, slotId: " + slotId);
		if (slotId < 0)
		{
			if (mediationBannerListener != null)
			{
				mediationBannerListener.onAdFailedToLoad(MyTargetAdapter.this,
														 AdRequest.ERROR_CODE_INVALID_REQUEST);
			}
			return;
		}

		if (adSize == null)
		{
			Log.w(TAG, "Failed to request ad, AdSize is null.");
			if (mediationBannerListener != null)
			{
				mediationBannerListener.onAdFailedToLoad(MyTargetAdapter.this,
														 AdRequest.ERROR_CODE_INVALID_REQUEST);
			}
			return;
		}

		MyTargetBannerListener bannerListener = null;
		if (mediationBannerListener != null)
		{
			bannerListener = new MyTargetBannerListener(mediationBannerListener);
		}

		if (AdSize.MEDIUM_RECTANGLE.equals(adSize))
		{
			Log.d(TAG, "Loading myTarget banner, size: 300x250");
			loadBanner(bannerListener, mediationAdRequest, slotId,
					   MyTargetView.AdSize.BANNER_300x250, context);
		}
		else if (AdSize.LEADERBOARD.equals(adSize))
		{
			Log.d(TAG, "Loading myTarget banner, size: 728x90");
			loadBanner(bannerListener, mediationAdRequest, slotId,
					   MyTargetView.AdSize.BANNER_728x90, context);
		}
		else if (AdSize.BANNER.equals(adSize))
		{
			Log.d(TAG, "Loading myTarget banner, size: 320x50");
			loadBanner(bannerListener, mediationAdRequest, slotId,
					   MyTargetView.AdSize.BANNER_320x50, context);
		}
		else
		{
			Log.w(TAG, "AdSize " + adSize.toString() + " is not currently supported");
			if (mediationBannerListener != null)
			{
				mediationBannerListener.onAdFailedToLoad(MyTargetAdapter.this,
														 AdRequest.ERROR_CODE_NO_FILL);
			}
		}

	}

	@Override
	public View getBannerView()
	{
		return myTargetView;
	}

	@Override
	public void requestInterstitialAd(Context context,
									  MediationInterstitialListener mediationInterstitialListener,
									  Bundle serverParameters,
									  MediationAdRequest mediationAdRequest,
									  Bundle mediationExtras)
	{
		int slotId = MyTargetTools.checkAndGetSlotId(context, serverParameters);
		Log.d(TAG, "Requesting myTarget interstitial mediation, slotId: " + slotId);

		if (slotId < 0)
		{
			if (mediationInterstitialListener != null)
			{
				mediationInterstitialListener.onAdFailedToLoad(MyTargetAdapter.this,
															   AdRequest.ERROR_CODE_INVALID_REQUEST);
			}
			return;
		}

		MyTargetInterstitialListener bannerListener = null;
		if (mediationInterstitialListener != null)
		{
			bannerListener = new MyTargetInterstitialListener(mediationInterstitialListener);
		}

		if (interstitial != null)
		{
			interstitial.destroy();
		}

		interstitial = new InterstitialAd(slotId, context);
		CustomParams params = interstitial.getCustomParams();
		params.setCustomParam(MyTargetTools.PARAM_MEDIATION_KEY, MyTargetTools.PARAM_MEDIATION_VALUE);
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
		interstitial.setListener(bannerListener);
		interstitial.load();
	}

	@Override
	public void showInterstitial()
	{
		if (interstitial != null)
		{
			interstitial.show();
		}
	}

	@Override
	public void onDestroy()
	{
		if (myTargetView != null)
		{
			myTargetView.destroy();
		}
		if (interstitial != null)
		{
			interstitial.destroy();
		}
	}

	@Override
	public void onPause()
	{
		if (myTargetView != null)
		{
			myTargetView.pause();
		}
	}

	@Override
	public void onResume()
	{
		if (myTargetView != null)
		{
			myTargetView.resume();
		}
	}

	/**
	 * Starts loading banner
	 *
	 * @param myTargetBannerListener listener for ad callbacks
	 * @param mediationAdRequest     Google mediation request
	 * @param slotId                 myTarget slot ID
	 * @param adSize                 myTarget banner size
	 * @param context                app context
	 */
	private void loadBanner(@Nullable MyTargetBannerListener myTargetBannerListener,
							@Nullable MediationAdRequest mediationAdRequest,
							int slotId,
							int adSize,
							@NonNull Context context)
	{
		if (myTargetView != null)
		{
			myTargetView.destroy();
		}

		myTargetView = new MyTargetView(context);

		myTargetView.init(slotId, adSize, false);

		CustomParams params = myTargetView.getCustomParams();
		if (mediationAdRequest != null)
		{
			int gender = mediationAdRequest.getGender();
			params.setGender(gender);
			Log.d(TAG, "Set gender to " + gender);

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

		params.setCustomParam(MyTargetTools.PARAM_MEDIATION_KEY, MyTargetTools.PARAM_MEDIATION_VALUE);
		myTargetView.setListener(myTargetBannerListener);
		myTargetView.load();
	}

	/**
	 * A {@link MyTargetBannerListener} used to forward myTarget banner events to Google
	 */
	private class MyTargetBannerListener implements MyTargetViewListener
	{

		@NonNull
		private final MediationBannerListener listener;

		MyTargetBannerListener(final @NonNull MediationBannerListener listener)
		{
			this.listener = listener;
		}

		@Override
		public void onLoad(final MyTargetView view)
		{
			Log.d(TAG, "Banner mediation Ad loaded");
			view.start();
			listener.onAdLoaded(MyTargetAdapter.this);
		}

		@Override
		public void onNoAd(final String s, final MyTargetView view)
		{
			Log.d(TAG, "Banner mediation Ad failed to load: " + s);
			listener.onAdFailedToLoad(MyTargetAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
		}

		@Override
		public void onClick(final MyTargetView view)
		{
			Log.d(TAG, "Banner mediation Ad clicked");
			listener.onAdClicked(MyTargetAdapter.this);
			listener.onAdOpened(MyTargetAdapter.this);
			// click redirects user to Google Play, or web browser, so we can notify
			// about left application
			listener.onAdLeftApplication(MyTargetAdapter.this);
		}
	}

	/**
	 * A {@link MyTargetInterstitialListener} used to forward myTarget interstitial events to Google
	 */
	private class MyTargetInterstitialListener implements InterstitialAd.InterstitialAdListener
	{

		private final @NonNull MediationInterstitialListener listener;

		MyTargetInterstitialListener(final @NonNull MediationInterstitialListener listener)
		{
			this.listener = listener;
		}

		@Override
		public void onLoad(final InterstitialAd ad)
		{
			Log.d(TAG, "Interstitial mediation Ad loaded");
			listener.onAdLoaded(MyTargetAdapter.this);
		}

		@Override
		public void onNoAd(final String s, final InterstitialAd ad)
		{
			Log.d(TAG, "Interstitial mediation Ad failed to load: " + s);
			listener.onAdFailedToLoad(MyTargetAdapter.this, AdRequest.ERROR_CODE_NO_FILL);
		}

		@Override
		public void onClick(final InterstitialAd ad)
		{
			Log.d(TAG, "Interstitial mediation Ad clicked");
			listener.onAdClicked(MyTargetAdapter.this);
			// click redirects user to Google Play, or web browser, so we can notify
			// about left application
			listener.onAdLeftApplication(MyTargetAdapter.this);
		}

		@Override
		public void onDismiss(final InterstitialAd ad)
		{
			Log.d(TAG, "Interstitial mediation Ad dismissed");
			listener.onAdClosed(MyTargetAdapter.this);
		}

		@Override
		public void onVideoCompleted(final InterstitialAd ad)
		{
		}

		@Override
		public void onDisplay(final InterstitialAd ad)
		{
			Log.d(TAG, "Interstitial mediation Ad displayed");
			listener.onAdOpened(MyTargetAdapter.this);
		}
	}
}
