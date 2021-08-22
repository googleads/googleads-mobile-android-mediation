/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.google.ads.mediation.yandex.nativeads.view;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ViewUtils {

    private ViewUtils() { }

    @Nullable
    public static <T extends View> T castView(@Nullable final View view,
                                              @NonNull final Class<T> expectedViewClass) {
        return expectedViewClass.isInstance(view) ? expectedViewClass.cast(view) : null;
    }
}
