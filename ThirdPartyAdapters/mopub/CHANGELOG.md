# MoPub Adapter for Google Mobile Ads SDK for Android Changelog

## 5.8.0.0
- Verified compatibility with MoPub SDK 5.8.0.
- Updated the minimum required Google Mobile Ads SDK version to 18.1.1.

## 5.7.1.1
- Fixed an issue with loading ads using an `Application` Context.
- Migrated the adapter to AndroidX.
- Updated the minimum required Google Mobile Ads SDK version to 18.0.0.

## 5.7.1.0
- Verified compatibility with MoPub SDK 5.7.1.
- Fixed a bug where adapter would not invoke rewarded ad events when the MoPub SDK was initialized before sending an Ad Request.

## 5.7.0.0
- Verified compatibility with MoPub SDK 5.7.0.

## 5.4.1.2
- Added support for flexible banner ad sizes.
- Updated adapter to support new open-beta Rewarded API.
- Updated the minimum required Google Mobile Ads SDK version to 17.2.0.

## 5.4.1.1
- Added support for MoPub Rewarded Video Ads.

## 5.4.1.0
- Verified compatibility with MoPub SDK 5.4.1.

## 5.4.0.0
- Fixed a native ad crash for publishers importing MoPub's non-native modules.
- Verified compatibility with MoPub SDK 5.4.0.

## 5.3.0.2
- Remove the check that prevents ad requests for native content ad.

## 5.3.0.1
- Initialize MoPub and reattempt ad requests manually in the adapters for use cases that do not do so in the app.

## 5.3.0.0
- Verified compatibility with MoPub SDK 5.3.0.

## 5.2.0.1
- Updated the adapter to invoke 'onAdLeftApplication()' ad event.

## 5.2.0.0
- Verified compatibility with MoPub SDK 5.2.0.

## 5.1.0.0
- Verified compatibility with MoPub SDK 5.1.0.

## 5.0.0.0
- Verified compatibility with MoPub SDK 5.0.0.

## 4.20.0.0
- Verified compatibility with MoPub SDK 4.20.0.

## 4.19.0.1
- Fixed an NPE issue when a null image URL is returned by MoPub.

## 4.19.0.0
- Verified compatibility with MoPub SDK 4.19.0.

## 4.18.0.0
- Verified compatibility with MoPub SDK 4.18.0.
- Updated the Adapter project for Android Studio 3.0 and Android API 26.

## 4.17.0.0
- Using MoPub's impression and click reporting for native ads (AdMob and MoPub
  impression and click statistics will match up).
- Verified compatibility with MoPub SDK 4.17.0.

## 4.16.1.0
- Verified compatibility with MoPub SDK 4.16.1.

## 4.16.0.0
- Verified compatibility with MoPub SDK 4.16.0.

## 4.15.0.0
- Verified compatibility with MoPub SDK 4.15.0.

## 4.14.0.0
- Verified compatibility with MoPub SDK 4.14.0.
- Adapter shows a warning when the requested banner ad size does not match the
  ad size set in MoPub UI.

## 4.13.0.0
- Verified compatibility with MoPub SDK 4.13.0.

## 4.12.0.0
- Verified compatibility with MoPub SDK 4.12.0.
- Added support for MoPub native ads.
- Updated interstitial and banner ads per Google's latest mediation APIs.

## Previous
- Support for MoPub banner and interstitial ads.
