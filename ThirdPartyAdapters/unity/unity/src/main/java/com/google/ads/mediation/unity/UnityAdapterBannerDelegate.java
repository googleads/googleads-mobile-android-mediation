package com.google.ads.mediation.unity;

import com.unity3d.services.banners.IUnityBannerListener;

public interface UnityAdapterBannerDelegate extends IUnityBannerListener {
    String getPlacementId();
}
