# Nend Adapter for Google Mobile Ads SDK for Android Changelog

## 5.1.0.4
- Fixed an issue where Banner and Interstitial ads were not forwarding the `onAdClicked()` event.
- Fixed an issue where a `NullPointerException` was thrown when a nend Banner ad was destroyed.
- Migrated the adapter to AndroidX.
- Updated the minimum required Google Mobile Ads SDK version to 18.0.0.

## 5.1.0.3
- Added implementation to display a part of nend banner in SmartBanner. Appropriate size for SmartBanner
  - Portrait and Landscape
    - Phones: 320×50
    - Tablets: 728×90 or 320×50

## 5.1.0.2
- Added support for flexible banner ad sizes.

## 5.1.0.1
- Updated adapter to support new open-beta Rewarded API.
- Updated the minimum required Google Mobile Ads SDK version to 17.2.0.

## 5.1.0.0
- Verified compatibility with nend SDK 5.1.0.

## 5.0.2.1
- Removed function that forward user features because following methods are deprecated on `AdRequest.Builder`.
  - `setGender()`
  - `setBirthday()`
  - `setIsDesignedForFamiles()`

## 5.0.2.0
- Verified compatibility with nend SDK 5.0.2.

## 5.0.1.0
- Verified compatibility with nend SDK 5.0.1.

## 5.0.0.0
- Verified compatibility with nend SDK 5.0.0.

## 4.0.5.0
- Verified compatibility with nend SDK 4.0.5.

## 4.0.4.1
- Supported onRewardedVideoCompleted() method of RewardedVideoAdListener.

## 4.0.4.0
- Verified compatibility with nend SDK 4.0.4.

## 4.0.2.1
- Added the ability to create a `Bundle` of mediation extras using the
  `NendExtrasBundleBuilder` class.

## 4.0.2.0
- First release in Google Mobile Ads Mediation open source project.
- Added support for banner, interstitial, and rewarded video ads.
