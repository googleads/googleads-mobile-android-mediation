package jp.maio.sdk.android.mediation.admob.adapter;

import jp.maio.sdk.android.v2.Version;

public class MaioAdsManager {
    public static final String KEY_MEDIA_ID = "mediaId";
    public static final String KEY_ZONE_ID = "zoneId";

    public static Version getSdkVersion() {
        return Version.Companion.getInstance();
    }
}
