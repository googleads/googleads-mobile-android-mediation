/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.google.ads.mediation.yandex.base;

import android.os.Bundle;
import androidx.annotation.Nullable;

public class MediationDataParser {

    private static final String BLOCK_ID_KEY = "blockID";

    @Nullable
    public String parseBlockId(@Nullable final Bundle serverParameters) {
        return serverParameters != null ? serverParameters.getString(BLOCK_ID_KEY) : null;
    }
}
