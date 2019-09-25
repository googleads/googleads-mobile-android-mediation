package com.google.ads.mediation.verizon;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.mediation.MediationNativeAdapter;
import com.google.android.gms.ads.mediation.MediationNativeListener;
import com.verizon.ads.ErrorInfo;
import com.verizon.ads.VASAds;
import com.verizon.ads.nativeplacement.NativeAd;
import com.verizon.ads.nativeplacement.NativeAdFactory;
import com.verizon.ads.nativeplacement.NativeComponentBundle;

import java.lang.ref.WeakReference;
import java.util.Map;


final class AdapterNativeListener implements NativeAd.NativeAdListener, NativeAdFactory.NativeAdFactoryListener {

	private static final String TAG = AdapterNativeListener.class.getSimpleName();

	private final WeakReference<MediationNativeAdapter> nativeAdapterWeakRef;
	private final MediationNativeListener nativeListener;
	private final Context context;

	private NativeAd nativeAd;


	AdapterNativeListener(final Context context, final MediationNativeAdapter adapter, final MediationNativeListener listener) {
		this.nativeAdapterWeakRef = new WeakReference<>(adapter);
		this.nativeListener = listener;
		this.context = context;
	}


	@Override
	public void onError(final NativeAd nativeAd, final ErrorInfo errorInfo) {

		Log.e(TAG, "Verizon Ads SDK native ad error: " + errorInfo);
	}


	@Override
	public void onClosed(final NativeAd nativeAd) {

		ThreadUtils.postOnUiThread(new Runnable() {
			@Override
			public void run() {

				MediationNativeAdapter adapter = nativeAdapterWeakRef.get();

				if ((nativeListener != null) && (adapter != null)) {
					nativeListener.onAdClosed(adapter);
				}
			}
		});
		Log.i(TAG, "Verizon Ads SDK native ad closed.");
	}


	@Override
	public void onClicked(final NativeComponentBundle nativeComponentBundle) {

		ThreadUtils.postOnUiThread(new Runnable() {
			@Override
			public void run() {

				MediationNativeAdapter adapter = nativeAdapterWeakRef.get();

				if ((nativeListener != null) && (adapter != null)) {
					nativeListener.onAdClicked(adapter);
				}
			}
		});
		Log.i(TAG, "Verizon Ads SDK native ad clicked.");
	}


	@Override
	public void onAdLeftApplication(final NativeAd nativeAd) {

		ThreadUtils.postOnUiThread(new Runnable() {
			@Override
			public void run() {

				MediationNativeAdapter adapter = nativeAdapterWeakRef.get();

				if ((nativeListener != null) && (adapter != null)) {
					nativeListener.onAdLeftApplication(adapter);
				}
			}
		});
		Log.i(TAG, "Verizon Ads SDK native ad left application.");
	}


	@Override
	public void onEvent(final NativeAd nativeAd, final String s, final String s1, final Map<String, Object> map) {

		// no op.  events not supported in adapter
	}


	@Override
	public void onLoaded(final NativeAdFactory nativeAdFactory, final NativeAd nativeAd) {

		this.nativeAd = nativeAd;

		ThreadUtils.postOnUiThread(new Runnable() {
			@Override
			public void run() {

				MediationNativeAdapter adapter = nativeAdapterWeakRef.get();

				if ((nativeListener != null) && (adapter != null)) {
					AdapterUnifiedNativeAdMapper mapper = new AdapterUnifiedNativeAdMapper(context, nativeAd);
					nativeListener.onAdLoaded(adapter, mapper);
				}
			}
		});
		Log.i(TAG, "Verizon Ads SDK native ad request succeeded.");
	}


	@Override
	public void onCacheLoaded(final NativeAdFactory nativeAdFactory, final int i, final int i1) {

		// no op.  caching not supported in adapter
	}


	@Override
	public void onCacheUpdated(final NativeAdFactory nativeAdFactory, final int i) {

		// no op.  caching not supported in adapter
	}


	@Override
	public void onError(final NativeAdFactory nativeAdFactory, final ErrorInfo errorInfo) {

		Log.i(TAG, "Verizon Ads SDK Native Ad request failed (" + errorInfo.getErrorCode() + "): " +
			errorInfo.getDescription());

		switch (errorInfo.getErrorCode()) {
			case VASAds.ERROR_AD_REQUEST_FAILED:
				ThreadUtils.postOnUiThread(new Runnable() {
					@Override
					public void run() {

						MediationNativeAdapter adapter = nativeAdapterWeakRef.get();

						if ((nativeListener != null) && (adapter != null)) {
							nativeListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_INTERNAL_ERROR);
						}
					}
				});
				break;
			case VASAds.ERROR_AD_REQUEST_TIMED_OUT:
				ThreadUtils.postOnUiThread(new Runnable() {
					@Override
					public void run() {

						MediationNativeAdapter adapter = nativeAdapterWeakRef.get();

						if ((nativeListener != null) && (adapter != null)) {
							nativeListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_NETWORK_ERROR);
						}
					}
				});
				break;
			default:
				ThreadUtils.postOnUiThread(new Runnable() {
					@Override
					public void run() {

						MediationNativeAdapter adapter = nativeAdapterWeakRef.get();

						if ((nativeListener != null) && (adapter != null)) {
							nativeListener.onAdFailedToLoad(adapter, AdRequest.ERROR_CODE_NO_FILL);
						}
					}
				});
		}
	}


	void destroy() {

		if (nativeAd != null) {
			nativeAd.destroy();
		}
	}
}
