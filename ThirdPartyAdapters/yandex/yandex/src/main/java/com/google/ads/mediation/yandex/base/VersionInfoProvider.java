/*
 * This file is a part of the Yandex Advertising Network
 *
 * Version for Android (C) 2021 YANDEX
 *
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://legal.yandex.com/partner_ch/
 */
package com.google.ads.mediation.yandex.base;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.mediation.VersionInfo;

public class VersionInfoProvider {

    private static final String VERSION_SEPARATOR_REGEX = "\\.";

    private static final int MAJOR_VERSION_PART_INDEX = 0;
    private static final int MINOR_VERSION_PART_INDEX = 1;
    private static final int PATCH_VERSION_PART_INDEX = 2;
    private static final int MICRO_VERSION_PART_INDEX = 3;

    @NonNull
    private final AdapterVersionProvider mAdapterVersionProvider;

    public VersionInfoProvider() {
        mAdapterVersionProvider = new AdapterVersionProvider();
    }

    @NonNull
    public VersionInfo getAdapterVersionInfo() {
        final String adapterVersion = mAdapterVersionProvider.getAdapterVersion();

        final String[] versionParts = adapterVersion.split(VERSION_SEPARATOR_REGEX);
        final int major = getVersionPart(versionParts, MAJOR_VERSION_PART_INDEX);
        final int minor = getVersionPart(versionParts, MINOR_VERSION_PART_INDEX);
        final int patchPart = getVersionPart(versionParts, PATCH_VERSION_PART_INDEX);
        final int microPart = getVersionPart(versionParts, MICRO_VERSION_PART_INDEX);
        final int patch = patchPart * 100 + microPart;

        return new VersionInfo(major, minor, patch);
    }

    @NonNull
    public VersionInfo getSdkVersionInfo() {
        final String sdkVersion = com.yandex.mobile.ads.common.MobileAds.getLibraryVersion();

        final String[] versionParts = sdkVersion.split(VERSION_SEPARATOR_REGEX);
        final int major = getVersionPart(versionParts, MAJOR_VERSION_PART_INDEX);
        final int minor = getVersionPart(versionParts, MINOR_VERSION_PART_INDEX);
        final int patch = getVersionPart(versionParts, PATCH_VERSION_PART_INDEX);

        return new VersionInfo(major, minor, patch);
    }

    private int getVersionPart(@NonNull final String[] versionParts,
                               final int partIndex) {
        if (versionParts.length > partIndex) {
            try {
                return Integer.parseInt(versionParts[partIndex]);
            } catch (final NumberFormatException ignored) {
            }
        }

        return 0;
    }
}
