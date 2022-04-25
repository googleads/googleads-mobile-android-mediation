## Facebook Android Mediation Adapter Changelog

#### Next Version
- Added warning messages for waterfall mediation deprecation. See [Meta's blog](https://fb.me/bNFn7qt6Z0sKtF) for more information.

#### 6.10.0.0
- Verified compatibility with Facebook SDK v6.10.0.

Built and tested with:
- Google Mobile Ads SDK version 20.6.0.
- Facebook SDK version 6.10.0.

#### 6.8.0.1
- Added support for forwarding click and impression callbacks in bidding ads.
- Added support for forwarding the `onAdFailedToShow()` callback when interstitial bidding ads fail to present.
- Updated the minimum required Google Mobile Ads SDK version to 20.6.0.

Built and tested with:
- Google Mobile Ads SDK version 20.6.0.
- Facebook SDK version 6.8.0.

#### 6.8.0.0
- Verified compatibility with Facebook SDK v6.8.0.
- Updated the minimum required Google Mobile Ads SDK version to 20.4.0.

Built and tested with:
- Google Mobile Ads SDK version 20.4.0.
- Facebook SDK version 6.8.0.

#### 6.7.0.0
- Verified compatibility with Facebook SDK v6.7.0.

Built and tested with:
- Google Mobile Ads SDK version 20.3.0.
- Facebook SDK version 6.7.0.

#### 6.6.0.0
- Verified compatibility with Facebook SDK v6.6.0.
- Updated the minimum required Google Mobile Ads SDK version to 20.3.0.

Built and tested with:
- Google Mobile Ads SDK version 20.3.0.
- Facebook SDK version 6.6.0.

#### 6.5.1.1
- Fixed a bug introduced in 6.5.1.0 where test ads are returned instead of live ads.
- Updated the adapter to use the new `AdError` API.

Built and tested with:
- Google Mobile Ads SDK version 20.2.0.
- Facebook SDK version 6.5.1.

#### 6.5.1.0 (Deprecated)
- An issue with version 6.5.1.0 has been detected and confirmed. It is
  recommended to upgrade to version 6.5.1.1.
- Verified compatibility with Facebook SDK v6.5.1.
- Updated the minimum required Google Mobile Ads SDK version to 20.2.0.

Built and tested with:
- Google Mobile Ads SDK version 20.2.0.
- Facebook SDK version 6.5.1.

#### 6.5.0.0
- Verified compatibility with Facebook SDK v6.5.0.
- Fixed an issue where native ads did not include Facebook's cover image.
- Updated the minimum required Google Mobile Ads SDK version to 20.1.0.

Built and tested with:
- Google Mobile Ads SDK version 20.1.0.
- Facebook SDK version 6.5.0.

#### 6.4.0.0
- Verified compatibility with Facebook SDK v6.4.0.
- Updated the minimum required Google Mobile Ads SDK version to 20.0.0.

Built and tested with:
- Google Mobile Ads SDK version 20.0.0.
- Facebook SDK version 6.4.0.

#### 6.3.0.1
- Fixed an issue where a `ClassCastException` is thrown when rendering native ads on apps that don't use `ImageView` to render image assets.

Built and tested with:
- Google Mobile Ads SDK version 19.7.0.
- Facebook SDK version 6.3.0.

#### 6.3.0.0
- Verified compatibility with Facebook SDK v6.3.0.

Built and tested with:
- Google Mobile Ads SDK version 19.7.0.
- Facebook SDK version 6.3.0.

#### 6.2.1.0
- Verified compatibility with Facebook SDK v6.2.1.
- Updated the minimum required Google Mobile Ads SDK version to 19.7.0.

Built and tested with:
- Google Mobile Ads SDK version 19.7.0.
- Facebook SDK version 6.2.1.

#### 6.2.0.1
- Removed support for the deprecated `NativeAppInstallAd` format. Apps should request for unified native ads.
- Updated the minimum required Google Mobile Ads SDK version to 19.6.0.

Built and tested with:
- Google Mobile Ads SDK version 19.6.0.
- Facebook SDK version 6.2.0.

#### 6.2.0.0
- Verified compatibility with Facebook SDK v6.2.0.
- Updated the minimum required Google Mobile Ads SDK version to 19.5.0.

Built and tested with:
- Google Mobile Ads SDK version 19.5.0.
- Facebook SDK version 6.2.0.

#### 6.1.0.0
- Verified compatibility with Facebook SDK v6.1.0.
- Updated the minimum required Google Mobile Ads SDK version to 19.4.0.

Built and tested with:
- Google Mobile Ads SDK version 19.4.0.
- Facebook SDK version 6.1.0.

#### 6.0.0.0
- Verified compatibility with Facebook SDK v6.0.0.
- Updated the minimum required Google Mobile Ads SDK version to 19.3.0.

Built and tested with:
- Google Mobile Ads SDK version 19.3.0.
- Facebook SDK version 6.0.0.

#### 5.11.0.0
- Verified compatibility with Facebook SDK v5.11.0.

Built and tested with:
- Google Mobile Ads SDK version 19.2.0.
- Facebook SDK version 5.11.0.

#### 5.10.1.0
- Verified compatibility with Facebook SDK v5.10.1.

Built and tested with:
- Google Mobile Ads SDK version 19.2.0.
- Facebook SDK version 5.10.1.

#### 5.10.0.0
- Verified compatibility with Facebook SDK v5.10.0.

Built and tested with:
- Google Mobile Ads SDK version 19.2.0.
- Facebook SDK version 5.10.0.

#### 5.9.1.0
- Verified compatibility with Facebook SDK v5.9.1.

Built and tested with:
- Google Mobile Ads SDK version 19.2.0.
- Facebook SDK version 5.9.1.

#### 5.9.0.2
- Added support for rewarded interstitial ads.
- Updated the adapter to support inline adaptive banner requests.
- Fixed an issue where bidding banner ads always render full-width.
- Updated the minimum required Google Mobile Ads SDK version to 19.2.0.

Built and tested with:
- Google Mobile Ads SDK version 19.2.0.
- Facebook SDK version 5.9.0.

#### 5.9.0.1
- Adapter now forwards an error if the FAN SDK encounters an error while presenting an interstitial/rewarded ad.

Built and tested with:
- Google Mobile Ads SDK version 19.1.0.
- Facebook SDK version 5.9.0.

#### 5.9.0.0
- Verified compatibility with Facebook SDK v5.9.0.

Built and tested with:
- Google Mobile Ads SDK version 19.1.0.
- Facebook SDK version 5.9.0.

#### 5.8.0.2
- Fixed incorrect variable reference which caused a crash in certain scenarios
when loading native ads.

Built and tested with:
- Google Mobile Ads SDK version 19.1.0.
- Facebook SDK version 5.8.0.

#### 5.8.0.1
- Added additional descriptive error codes and reasons for adapter load/show failures.
- Updated the minimum required Google Mobile Ads SDK version to 19.1.0.

Built and tested with:
- Google Mobile Ads SDK version 19.1.0.
- Facebook SDK version 5.8.0.

#### 5.8.0.0
- Verified compatibility with Facebook SDK v5.8.0.
- Updated the minimum required Google Mobile Ads SDK version to 19.0.1.

Built and tested with:
- Google Mobile Ads SDK version 19.0.1.
- Facebook SDK version 5.8.0.

#### 5.7.1.1
- Added support for Facebook Audience Network adapter errors.
- Added descriptive error codes and reasons for adapter load/show failures.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- Facebook SDK version 5.7.1.

#### 5.7.1.0
- Verified compatibility with Facebook SDK v5.7.1.
- Added support for Facebook Native Banner ads when using bidding.
- Native ads now use 'Drawable' for the icon asset.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- Facebook SDK version 5.7.1.

#### 5.7.0.0
- Verified compatibility with Facebook SDK v5.7.0.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- Facebook SDK version 5.7.0.

#### 5.6.1.0
- Verified compatibility with Facebook SDK v5.6.1.
- Updated the minimum required Google Mobile Ads SDK version to 18.3.0.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- Facebook SDK version 5.6.1.

#### 5.6.0.0
- Verified compatibility with Facebook SDK v5.6.0.
- Updated Facebook Adapter to use `AdChoicesView`.

Built and tested with:
- Google Mobile Ads SDK version 18.2.0.
- Facebook SDK version 5.6.0.

#### 5.5.0.0
- Verified compatibility with Facebook SDK v5.5.0.

#### 5.4.1.1
- Fixed an issue that causes a crash when Native Ads are removed.

#### 5.4.1.0
- Verified compatibility with Facebook SDK v5.4.1.
- Added support for Facebook Native Banner Ads for waterfall mediation.
  * Use `setNativeBanner()` from the `FacebookExtras` class to request for Native Banner Ads.
- Fixed an issue that caused Smart Banner Ad requests to fail.
- Fixed an issue where Rewarded Video Ads were not forwarding the `onAdClosed()` event in some cases where the app was backgrounded while the video was in progress.
- Migrated the adapter to `AndroidX`.
- Updated the minimum required Google Mobile Ads SDK version to 18.1.1.

#### 5.4.0.0
- Verified compatibility with Facebook SDK v5.4.0.

#### 5.3.1.2
- Fixed a bug where Facebook bidding failed to initialize due
  to "No placement IDs found".

#### 5.3.1.1
- Updated native RTB ads impression tracking.
- Updated the minimum required Google Mobile Ads SDK version to 17.2.1.

#### 5.3.1.0
- Added bidding capability to the adapter for banner, interstitial,
  rewarded and native ads.
- Verified compatibility with Facebook SDK v5.3.1.

#### 5.3.0.0
- Updated mediation service name for Google Mobile Ads.
- Added adapter version to the initialization call.
- Verified compatibility with Facebook SDK v5.3.0.

#### 5.2.0.2
- Added support for flexible banner ad sizes.

#### 5.2.0.1
- Updated adapter to support new open-beta Rewarded API.
- Updated the minimum required Google Mobile Ads SDK version to 17.2.0.

#### 5.2.0.0
- Verified compatibility with Facebook SDK v5.2.0.

#### 5.1.1.1
- Updated the adapter to populate Advertiser Name for Unified Native Ads.

#### 5.1.1.0
- Replaced AdChoices View with AdOptions View.
- Verified compatibility with Facebook SDK v5.1.1

#### 5.1.0.1
- Fixed an ANR issue caused by 'getGMSVersionCode()'.

#### 5.1.0.0
- Initialize Facebook SDK for each ad format.

#### 5.0.1.0
- Verified compatibility with Facebook SDK v5.0.1.

#### 5.0.0.1
- Updated the adapter to create the rewarded ad object at ad request time.

#### 5.0.0.0
- Verified compatibility with Facebook SDK v5.0.0.

#### 4.99.3.0
- Verified compatibility with Facebook SDK v4.99.3.

#### 4.99.1.1
- Fixed a bug where the Ad Choices icon is not shown for Unified Native Ads.
- Fixed a bug where the adapter would throw an exception when trying to download images.

#### 4.99.1.0
- Verified compatibility with Facebook SDK v4.99.1.

#### 4.28.2.1
- Updated the adapter to invoke the `onRewardedVideoComplete()` ad event.

#### 4.28.2.0
- Verified compatibility with Facebook SDK v4.28.2.

#### 4.28.1.1
- Fixed an issue where clicks are not being registered for Unified Native Ads.

#### 4.28.1.0
- Verified compatibility with Facebook SDK v4.28.1.

#### 4.28.0.0
- Verified compatibility with Facebook SDK v4.28.0.

#### 4.27.1.0
- Verified compatibility with Facebook SDK v4.27.1.

#### 4.27.0.0
- Verified compatibility with Facebook SDK v4.27.0.

#### 4.26.1.0
- Verified compatibility with Facebook SDK v4.26.1.
- Updated the Adapter project for Android Studio 3.0

#### 4.26.0.0
- Added support for rewarded video ads.
- Added support for native video ads.
- Verified compatibility with Facebook SDK v4.26.0.

#### 4.25.0.0
- Fixed an issue where incorrectly sized banners were being returned.
- Updated the adapter's view tracking for native ads to register individual
  asset views with the Facebook SDK rather than the entire ad view. This means
  that background (or "whitespace") clicks on the native ad will no longer
  result in clickthroughs.
- Verified compatibility with Facebook SDK v4.25.0.

#### 4.24.0.0
- Verified compatibility with Facebook SDK v4.24.0.

#### 4.23.0.0
- Verified compatibility with Facebook SDK v4.23.0.

#### 4.22.1.0
- Verified compatibility with Facebook SDK v4.22.1.

#### 4.22.0.0
- Updated the adapter to make it compatible with Facebook SDK v4.22.0.

#### 4.21.1.0
- Verified compatibility with Facebook SDK v4.21.1.

#### 4.21.0.0
- Verified compatibility with Facebook SDK v4.21.0.

#### 4.20.0.0
- Updated the minimum supported Android API level to 14+.
- Verified compatibility with Facebook SDK v4.20.0.

#### 4.19.0.0
- Verified compatibility with Facebook SDK v4.19.0.

#### 4.18.0.0
- Verified compatibility with Facebook SDK v4.18.0.

#### 4.17.0.0
- Added support for native ads.

#### 4.15.0.0
- Changed the version naming system to
  [FAN SDK version].[adapter patch version].
- Updated the minimum required FAN SDK to v4.15.0.
- Updated the minimum required Google Mobile Ads SDK to v9.2.0.
- Fixed a bug where Facebook's click callbacks for interstitial ads weren't
  forwarded correctly.
- The adapter now also forwards onAdLeftApplication when an ad is clicked.

#### 1.2.0
- Fixed a bug that so that AdSize.SMART_BANNER is now a valid size.

#### 1.1.0
- Added support for full width x 250 format when request is
for AdSize.MEDIUM_RECTANGLE

#### 1.0.1
- Added support for AdSize.SMART_BANNER

#### 1.0.0
- Initial release
