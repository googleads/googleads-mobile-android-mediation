# Vungle Adapter for Google Mobile Ads SDK for Android Changelog

## 6.3.24.1
- Updated adapter to support new open-beta Rewarded API.
- Updated the minimum required Google Mobile Ads SDK version to 17.2.0.

## 6.3.24.0
- Verified compatibility with Vungle SDK 6.3.24.

## 6.3.17.0
- Verified compatibility with Vungle SDK 6.3.17.

## 6.3.12.0
- Verified compatibility with Vungle SDK 6.3.12.

## 6.2.5.1
- Updated the adapter to invoke the `onRewardedVideoComplete()` ad event.

## 6.2.5.0
- Verified compatibility with Vungle SDK 6.2.5.

## 5.3.2.1
- Updated adapter to correctly report clicks to the Google Mobile Ads SDK.

## 5.3.2.0
- Verified compatibility with Vungle SDK 5.3.2.
- Updated the Adpater project for Android Studio 3.0.
- Added the following methods to Bundle builder class.
   - `setOrdinalViewCount` : This field is used to pass the mediation ordinal,
   whenever Publisher receives the ordinal data reports from Vungle.
   - `setFlexViewCloseTimeInSec` : This option is used to make flex view ads
   dismiss on their own after the specified number of seconds.

## 5.3.0.0
- Verified compatibility with Vungle SDK 5.3.0.

## 5.1.0.0
- Updated the adapter to make it compatible with Vungle SDK 5.1.0.
- Changed the version naming system to
  [Vungle SDK version].[adapter patch version].

## Earlier versions
- Added support for interstitial and rewarded video ad formats.
