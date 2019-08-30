# Facebook Adapter for Google Mobile Ads SDK for Android Changelog

## 5.5.0.0
- Verified compatibility with Facebook SDK v5.5.0.

## 5.4.1.1
- Fixed an issue that causes a crash when Native Ads are removed.

## 5.4.1.0
- Verified compatibility with Facebook SDK v5.4.1.
- Added support for Facebook Native Banner Ads.
  * Use `setNativeBanner()` from the `FacebookExtras` class to request for Native Banner Ads.
- Fixed an issue that caused Smart Banner Ad requests to fail.
- Fixed an issue where Rewarded Video Ads were not forwarding the `onAdClosed()` event in some cases where the app was backgrounded while the video was in progress.
- Migrated the adapter to `AndroidX`.
- Updated the minimum required Google Mobile Ads SDK version to 18.1.1.

## 5.4.0.0
- Verified compatibility with Facebook SDK v5.4.0.

## 5.3.1.2
- Fixed a bug where Facebook open bidding failed to initialize due
  to "No placement IDs found".

## 5.3.1.1
- Updated native RTB ads impression tracking.
- Updated the minimum required Google Mobile Ads SDK version to 17.2.1.

## 5.3.1.0
- Added open bidding capability to the adapter for banner, interstitial,
  rewarded and native ads.
- Verified compatibility with Facebook SDK v5.3.1.

## 5.3.0.0
- Updated mediation service name for Google Mobile Ads.
- Added adapter version to the initialization call.
- Verified compatibility with Facebook SDK v5.3.0.

## 5.2.0.2
- Added support for flexible banner ad sizes.

## 5.2.0.1
- Updated adapter to support new open-beta Rewarded API.
- Updated the minimum required Google Mobile Ads SDK version to 17.2.0.

## 5.2.0.0
- Verified compatibility with Facebook SDK v5.2.0.

## 5.1.1.1
- Updated the adapter to populate Advertiser Name for Unified Native Ads.

## 5.1.1.0
- Replaced AdChoices View with AdOptions View.
- Verified compatibility with Facebook SDK v5.1.1

## 5.1.0.1
- Fixed an ANR issue caused by 'getGMSVersionCode()'.

## 5.1.0.0
- Initialize Facebook SDK for each ad format.

## 5.0.1.0
- Verified compatibility with Facebook SDK v5.0.1.

## 5.0.0.1
- Updated the adapter to create the rewarded ad object at ad request time.

## 5.0.0.0
- Verified compatibility with Facebook SDK v5.0.0.

## 4.99.3.0
- Verified compatibility with Facebook SDK v4.99.3.

## 4.99.1.1
- Fixed a bug where the Ad Choices icon is not shown for Unified Native Ads.
- Fixed a bug where the adapter would throw an exception when trying to download images.

## 4.99.1.0
- Verified compatibility with Facebook SDK v4.99.1.

## 4.28.2.1
- Updated the adapter to invoke the `onRewardedVideoComplete()` ad event.

## 4.28.2.0
- Verified compatibility with Facebook SDK v4.28.2.

## 4.28.1.1
- Fixed an issue where clicks are not being registered for Unified Native Ads.

## 4.28.1.0
- Verified compatibility with Facebook SDK v4.28.1.

## 4.28.0.0
- Verified compatibility with Facebook SDK v4.28.0.

## 4.27.1.0
- Verified compatibility with Facebook SDK v4.27.1.

## 4.27.0.0
- Verified compatibility with Facebook SDK v4.27.0.

## 4.26.1.0
- Verified compatibility with Facebook SDK v4.26.1.
- Updated the Adapter project for Android Studio 3.0

## 4.26.0.0
- Added support for rewarded video ads.
- Added support for native video ads.
- Verified compatibility with Facebook SDK v4.26.0.

## 4.25.0.0
- Fixed an issue where incorrectly sized banners were being returned.
- Updated the adapter's view tracking for native ads to register individual
  asset views with the Facebook SDK rather than the entire ad view. This means
  that background (or "whitespace") clicks on the native ad will no longer
  result in clickthroughs.
- Verified compatibility with Facebook SDK v4.25.0.

## 4.24.0.0
- Verified compatibility with Facebook SDK v4.24.0.

## 4.23.0.0
- Verified compatibility with Facebook SDK v4.23.0.

## 4.22.1.0
- Verified compatibility with Facebook SDK v4.22.1.

## 4.22.0.0
- Updated the adapter to make it compatible with Facebook SDK v4.22.0.

## 4.21.1.0
- Verified compatibility with Facebook SDK v4.21.1.

## 4.21.0.0
- Verified compatibility with Facebook SDK v4.21.0.

## 4.20.0.0
- Updated the minimum supported Android API level to 14+.
- Verified compatibility with Facebook SDK v4.20.0.

## 4.19.0.0
- Verified compatibility with Facebook SDK v4.19.0.

## 4.18.0.0
- Verified compatibility with Facebook SDK v4.18.0.

## 4.17.0.0
- Added support for native ads.

## 4.15.0.0
- Changed the version naming system to
  [FAN SDK version].[adapter patch version].
- Updated the minimum required FAN SDK to v4.15.0.
- Updated the minimum required Google Mobile Ads SDK to v9.2.0.
- Fixed a bug where Facebook's click callbacks for interstitial ads weren't
  forwarded correctly.
- The adapter now also forwards onAdLeftApplication when an ad is clicked.

## 1.2.0
- Fixed a bug that so that AdSize.SMART_BANNER is now a valid size.

## 1.1.0
- Added support for full width x 250 format when request is
for AdSize.MEDIUM_RECTANGLE

## 1.0.1
- Added support for AdSize.SMART_BANNER

## 1.0.0
- Initial release
