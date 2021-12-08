package com.google.ads.mediation.yahoo;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.MediationUtils;
import com.google.android.gms.ads.mediation.MediationAdConfiguration;
import com.google.android.gms.ads.mediation.MediationAdRequest;
import com.yahoo.ads.BuildConfig;
import com.yahoo.ads.DataPrivacy;
import com.yahoo.ads.RequestMetadata;
import com.yahoo.ads.YASAds;
import java.util.ArrayList;

class YahooAdapterUtils {

  private static final String PLACEMENT_KEY = "placement_id";
  private static final String ORANGE_PLACEMENT_KEY = "position";
  private static final String DCN_KEY = "dcn";
  private static final String MEDIATOR_ID = "AdMobVAS-" + BuildConfig.VERSION_NAME;

  public static final String SITE_KEY = "site_id";

  /**
   * Gets the ad request metadata.
   */
  public static RequestMetadata getRequestMetadata(final MediationAdRequest mediationAdRequest) {
    RequestMetadata.Builder requestMetadataBuilder = new RequestMetadata.Builder();
    if (mediationAdRequest.getKeywords() != null) {
      requestMetadataBuilder
          .putExtra("keywords", new ArrayList<>(mediationAdRequest.getKeywords()));
    }
    requestMetadataBuilder.setMediator(MEDIATOR_ID);

    return requestMetadataBuilder.build();
  }

  /**
   * Gets the ad request metadata.
   */
  public static RequestMetadata getRequestMetaData(MediationAdConfiguration adConfiguration) {
    RequestMetadata.Builder requestMetaDataBuilder = new RequestMetadata.Builder();
    requestMetaDataBuilder.setMediator(MEDIATOR_ID);
    return requestMetaDataBuilder.build();
  }

  /**
   * Passes user requested COPPA settings from mediation ad request to Yahoo SDK.
   */
  public static void setCoppaValue(final MediationAdRequest mediationAdRequest) {
    if (mediationAdRequest.taggedForChildDirectedTreatment() ==
        MediationAdRequest.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE) {
      YASAds.setDataPrivacy(new DataPrivacy.Builder(YASAds.getDataPrivacy()).setCoppaApplies(true).build());
    } else if (mediationAdRequest.taggedForChildDirectedTreatment() ==
        MediationAdRequest.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE) {
      YASAds.setDataPrivacy(new DataPrivacy.Builder(YASAds.getDataPrivacy()).setCoppaApplies(false).build());
    }
  }

  /**
   * Passes user requested COPPA settings from mediation ad configuration to Yahoo SDK.
   */
  public static void setCoppaValue(final MediationAdConfiguration mediationAdConfiguration) {
    if (mediationAdConfiguration.taggedForChildDirectedTreatment() ==
        MediationAdRequest.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE) {
      YASAds.setDataPrivacy(new DataPrivacy.Builder(YASAds.getDataPrivacy()).setCoppaApplies(true).build());
    } else if (mediationAdConfiguration.taggedForChildDirectedTreatment() ==
        MediationAdRequest.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE) {
      YASAds.setDataPrivacy(new DataPrivacy.Builder(YASAds.getDataPrivacy()).setCoppaApplies(false).build());
    }
  }

  /**
   * Gets the Yahoo site ID.
   */
  public static String getSiteId(final Bundle serverParams, final Bundle mediationExtras) {
    String siteId = null;
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
    return siteId;
  }

  /**
   * Gets the Yahoo site ID.
   */
  public static String getSiteId(final Bundle serverParams,
      final MediationAdConfiguration mediationAdConfiguration) {
    String siteId = null;
    if (mediationAdConfiguration != null &&
        mediationAdConfiguration.getMediationExtras().containsKey(SITE_KEY)) {
      siteId = mediationAdConfiguration.getMediationExtras().getString(SITE_KEY);
    }
    // If we get site ID from the serverParams (not yet implemented), overwrite
    // everything!
    if (serverParams != null && serverParams.containsKey(SITE_KEY)) {
      siteId = serverParams.getString(SITE_KEY);
    }

    // Support for legacy Nexage and MM mediation
    if (TextUtils.isEmpty(siteId)) {
      if (mediationAdConfiguration != null &&
          mediationAdConfiguration.getMediationExtras().containsKey(DCN_KEY)) {
        siteId = mediationAdConfiguration.getMediationExtras().getString(DCN_KEY);
      }
      // If we get site ID from the serverParams (not yet implemented), overwrite
      // everything!
      if (serverParams != null && serverParams.containsKey(DCN_KEY)) {
        siteId = serverParams.getString(DCN_KEY);
      }
    }
    return siteId;
  }

  /**
   * Gets the Yahoo placement ID.
   */
  public static String getPlacementId(final Bundle serverParams) {
    String placementId = null;
    if (serverParams == null) {
      return null;
    } else if (serverParams.containsKey(PLACEMENT_KEY)) {
      placementId = serverParams.getString(PLACEMENT_KEY);
    } else if (serverParams.containsKey(ORANGE_PLACEMENT_KEY)) {
      placementId = serverParams.getString(ORANGE_PLACEMENT_KEY);
    }
    return placementId;
  }

  /**
   * Returns a Yahoo supported ad size from a Google ad size.
   */
  @Nullable
  static AdSize normalizeSize(@NonNull Context context, @NonNull AdSize adSize) {
    ArrayList<AdSize> potentials = new ArrayList<>();
    potentials.add(AdSize.BANNER);
    potentials.add(AdSize.LEADERBOARD);
    potentials.add(AdSize.MEDIUM_RECTANGLE);
    return MediationUtils.findClosestSize(context, adSize, potentials);
  }

}
