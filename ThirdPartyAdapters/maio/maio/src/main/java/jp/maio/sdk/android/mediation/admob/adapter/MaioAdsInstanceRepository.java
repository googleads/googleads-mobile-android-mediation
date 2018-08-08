package jp.maio.sdk.android.mediation.admob.adapter;

import java.util.HashMap;
import jp.maio.sdk.android.MaioAdsInstance;

/**
 * Created by ade on 2018/08/06.
 */

public class MaioAdsInstanceRepository {

    private static final HashMap<String, MaioAdsInstance> mMaio = new HashMap<>();

    static void setMaioAdsInstance(String mediaId, MaioAdsInstance maio)
    {
        mMaio.put(mediaId, maio);
    }

    static boolean isInitialized(String mediaId)
    {
        return mMaio.containsKey(mediaId) && mMaio.get(mediaId) != null;
    }

    static MaioAdsInstance getMaioAdsInstance(String mediaId)
    {
        return mMaio.get(mediaId);
    }
}

