# Chartboost Adapter for Google Mobile Ads SDK for Android Changelog

## 7.5.0.0
- Verified compatibility with Chartboost SDK 7.5.0.
- Migrated the adapter to use AndroidX dependencies.
- Updated the minimum required Google Mobile Ads SDK version to 18.1.1.

## 7.3.1.1
- Updated adapter to support new open-beta Rewarded API.
- Updated the minimum required Google Mobile Ads SDK version to 17.2.0.

## 7.3.1.0
- Verified compatibility with Chartboost SDK 7.3.1.

## 7.3.0.0
- Verified compatibility with Chartboost SDK 7.3.0.

## 7.2.1.0
- Verified compatibility with Chartboost SDK 7.2.1.

## 7.2.0.1
- Updated the adapter to invoke the `onRewardedVideoComplete()` ad event.

## 7.2.0.0
- Verified compatibility with Chartboost SDK 7.2.0.

## 7.1.0.0
- Verified compatibility with Chartboost SDK 7.1.0.

## 7.0.1.0
- Verified compatibility with Chartboost SDK 7.0.1.
- Added support for new Chartboost error codes.
- Updated the Adapter project for Android Studio 3.0

## 7.0.0.0
- Updated the adapter to make it compatible with Chartboost SDK 7.0.0.

## 6.6.3.0
- Verified compatibility with Chartboost SDK 6.6.3.

## 6.6.2.0
- Verified compatibility with Chartboost SDK 6.6.2.

## 6.6.1.0
- Verified compatibility with Chartboost SDK 6.6.1.

## 6.6.0.0
- Changed the version naming system to
  [Chartboost SDK version].[adapter patch version].
- The adapters can now be added as a compile dependency by adding the following
  to the build.gradle file's dependencies tag:
  `compile 'com.google.ads.mediation:chartboost:6.6.0.0'`
- Moved to distributing the adapter as an aar instead of a jar file
  (see README for additional instructions).

## 1.1.0
- Removed Chartboost Ad Location from Chartboost extras. Ad Location is now
specified in the AdMob console when configuring Chartboost for mediation.

## 1.0.0
- Initial release. Supports reward-based video ads and interstitial ads.
