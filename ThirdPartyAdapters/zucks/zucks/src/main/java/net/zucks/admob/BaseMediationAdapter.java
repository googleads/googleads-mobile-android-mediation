package net.zucks.admob;

import android.content.Context;

import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.VersionInfo;
import java.util.List;

/** Base class for Zucks Mediation Adapters. Provide common logic for compatibility. */
public abstract class BaseMediationAdapter extends Adapter {

  @Override
  public void initialize(
      Context context,
      InitializationCompleteCallback initializationCompleteCallback,
      List<MediationConfiguration> list) {
    // Initialization is not needed in Zucks Ad Network SDK.
    initializationCompleteCallback.onInitializationSucceeded();
  }

  @Override
  public VersionInfo getVersionInfo() {
    return AdMobUtil.getAdapterVersionInfo();
  }

  @Override
  public VersionInfo getSDKVersionInfo() {
    return AdMobUtil.getNetworkSdkVersionInfo();
  }
}
