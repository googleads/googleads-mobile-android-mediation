package com.google.ads.mediation.mytarget;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

public class MyTargetTools
{
	private static final String KEY_SLOT_ID = "slotId";
	private static final String TAG = "MyTargetTools";
	static final @NonNull String PARAM_MEDIATION_KEY = "mediation";
	static final @NonNull String PARAM_MEDIATION_VALUE = "1";

	/**
	 * Checks params taken from Google. MyTarget slotId must be only positive, so if we return
	 * negative number, it was invalid request
	 *
	 * @param context          app context
	 * @param serverParameters bundle with server params, must contain myTarget slot ID
	 * @return myTarget slot ID, or negative number, if something gone wrong and we should return
	 * invalid request callback to Google SDK
	 */
	static int checkAndGetSlotId(final @Nullable Context context,
								 final @Nullable Bundle serverParameters)
	{
		int slotId = -1;

		if (context == null)
		{
			Log.w(TAG, "Failed to request ad, Context is null.");
			return slotId;
		}

		if (serverParameters == null)
		{
			Log.w(TAG, "Failed to request ad, serverParameters is null.");
		}
		else
		{
			String slotIdParam = serverParameters.getString(KEY_SLOT_ID);
			if (TextUtils.isEmpty(slotIdParam))
			{
				Log.w(TAG, "Failed to request ad, slotId is null or empty.");
			}
			else
			{
				try
				{
					slotId = Integer.parseInt(slotIdParam);
				}
				catch (NumberFormatException e)
				{
					Log.w(TAG, "Failed to request ad, unable to convert slotId " + slotIdParam
							+ " to int");
				}
			}
		}
		return slotId;
	}
}
