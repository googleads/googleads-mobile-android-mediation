## MoPub Android Mediation Adapter Changelog

#### Next Version
- Updated the adapter to use new `AdError` API.
- Updated the minimum required Google Mobile Ads SDK version to 20.5.0.

#### Version 5.18.0.0
- Verified compatibility with MoPub SDK 5.18.0.
- Updated the minimum required Google Mobile Ads SDK version to 20.2.0.
- Updated the adapter to forward the `onAdOpened()` callback when the banner ad is tapped.

Built and tested with:
- Google Mobile Ads SDK version 20.2.0.
- MoPub SDK version 5.18.0.

#### Version 5.17.0.0
- Verified compatibility with MoPub SDK 5.17.0.
- Updated error codes to capture additional MoPub errors.
- Updated the minimum required Google Mobile Ads SDK version to 20.1.0.

Built and tested with:
- Google Mobile Ads SDK version 20.1.0.
- MoPub SDK version 5.17.0.

#### Version 5.16.4.0
- Verified compatibility with MoPub SDK 5.16.4.
- Updated the minimum required Google Mobile Ads SDK version to 20.0.0.

Built and tested with:
- Google Mobile Ads SDK version 20.0.0.
- MoPub SDK version 5.16.4.

#### Version 5.16.3.0
- Verified compatibility with MoPub SDK 5.16.3.

Built and tested with:
- Google Mobile Ads SDK version 19.8.0.
- MoPub SDK version 5.16.3.

#### Version 5.16.2.0
- Verified compatibility with MoPub SDK 5.16.2.
- Updated the minimum required Google Mobile Ads SDK version to 19.8.0.

Built and tested with:
- Google Mobile Ads SDK version 19.8.0.
- MoPub SDK version 5.16.2.

#### Version 5.15.0.1
- Removed support for the deprecated `NativeAppInstallAd` format. Apps should request for unified native ads.
- Updated the minimum required Google Mobile Ads SDK version to 19.6.0.

Built and tested with:
- Google Mobile Ads SDK version 19.6.0.
- MoPub SDK version 5.15.0.

#### Version 5.15.0.0
- Verified compatibility with MoPub SDK 5.15.0.
- Updated the minimum required Google Mobile Ads SDK version to 19.5.0.

Built and tested with:
- Google Mobile Ads SDK version 19.5.0.
- MoPub SDK version 5.15.0.

#### Version 5.14.0.0
- Verified compatibility with MoPub SDK 5.14.0.
- Updated the minimum required Google Mobile Ads SDK version to 19.4.0.

Built and tested with:
- Google Mobile Ads SDK version 19.4.0.
- MoPub SDK version 5.14.0.

#### Version 5.13.1.0
- Verified compatibility with MoPub SDK 5.13.1.
- Updated the minimum required Google Mobile Ads SDK version to 19.2.0.
- Added a new extra to the `MoPubAdapter.BundleBuilder` class:
  * `setCustomRewardData()` - custom reward data for MoPub rewarded ads.

Built and tested with:
- Google Mobile Ads SDK version 19.2.0.
- MoPub SDK version 5.13.1.

#### Version 5.13.0.0
- Verified compatibility with MoPub SDK 5.13.0.

Built and tested with:
- Google Mobile Ads SDK version 19.1.0.
- MoPub SDK version 5.13.0.

#### Version 5.12.0.1
- Updated the minimum required Google Mobile Ads SDK version to 19.1.0.
- Further added descriptive error codes and reasons for adapter load failures.

Built and tested with:
- Google Mobile Ads SDK version 19.1.0.
- MoPub SDK version 5.12.0.

#### Version 5.12.0.0
- Verified compatibility with MoPub SDK 5.12.0.
- Updated the minimum required Google Mobile Ads SDK version to 19.0.1.
- Adjusted support for flexible banner ad sizes.
- Added new extras to the `MoPubAdapter.BundleBuilder` class:
  * `setMinimumBannerWidth()` - minimum ad width allowed for MoPub banner ads.
  * `setMinimumBannerHeight()` - minimum ad height allowed for MoPub banner ads.

Built and tested with:
- Google Mobile Ads SDK version 19.0.1.
- MoPub SDK version 5.12.0.

#### Version 5.11.1.1
- Added descriptive error codes and reasons for adapter load/show failures.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- MoPub SDK version 5.11.1.

#### Version 5.11.1.0
- Verified compatibility with MoPub SDK 5.11.1.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- MoPub SDK version 5.11.1.

#### Version 5.11.0.0
- Updated minimum Android SDK version to API 19.
- Verified compatibility with MoPub SDK 5.11.0.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- MoPub SDK version 5.11.0.

#### Version 5.10.0.0
- Verified compatibility with MoPub SDK 5.10.0.
- Updated the minimum required Google Mobile Ads SDK version to 18.3.0.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- MoPub SDK version 5.10.0.

#### Version 5.9.1.1
- Native ads now leverage the unified native ads mediation API.

Built and tested with:
- Google Mobile Ads SDK version 18.2.0.
- MoPub SDK version 5.9.1.

#### Version 5.9.1.0
- Verified compatibility with MoPub SDK 5.9.1.
- Updated the minimum required Google Mobile Ads SDK version to 18.2.0.

#### Version 5.8.0.0
- Verified compatibility with MoPub SDK 5.8.0.
- Updated the minimum required Google Mobile Ads SDK version to 18.1.1.

#### Version 5.7.1.1
- Fixed an issue with loading ads using an `Application` Context.
- Migrated the adapter to AndroidX.
- Updated the minimum required Google Mobile Ads SDK version to 18.0.0.

#### Version 5.7.1.0
- Verified compatibility with MoPub SDK 5.7.1.
- Fixed a bug where adapter would not invoke rewarded ad events when the MoPub SDK was initialized before sending an Ad Request.

#### Version 5.7.0.0
- Verified compatibility with MoPub SDK 5.7.0.

#### Version 5.4.1.2
- Added support for flexible banner ad sizes.
- Updated adapter to support new open-beta Rewarded API.
- Updated the minimum required Google Mobile Ads SDK version to 17.2.0.

#### Version 5.4.1.1
- Added support for MoPub Rewarded Video Ads.

#### Version 5.4.1.0
- Verified compatibility with MoPub SDK 5.4.1.

#### Version 5.4.0.0
- Fixed a native ad crash for publishers importing MoPub's non-native modules.
- Verified compatibility with MoPub SDK 5.4.0.

#### Version 5.3.0.2
- Remove the check that prevents ad requests for native content ad.

#### Version 5.3.0.1
- Initialize MoPub and reattempt ad requests manually in the adapters for use cases that do not do so in the app.

#### Version 5.3.0.0
- Verified compatibility with MoPub SDK 5.3.0.

#### Version 5.2.0.1
- Updated the adapter to invoke 'onAdLeftApplication()' ad event.

#### Version 5.2.0.0
- Verified compatibility with MoPub SDK 5.2.0.

#### Version 5.1.0.0
- Verified compatibility with MoPub SDK 5.1.0.

#### Version 5.0.0.0
- Verified compatibility with MoPub SDK 5.0.0.

#### Version 4.20.0.0
- Verified compatibility with MoPub SDK 4.20.0.

#### Version 4.19.0.1
- Fixed an NPE issue when a null image URL is returned by MoPub.

#### Version 4.19.0.0
- Verified compatibility with MoPub SDK 4.19.0.

#### Version 4.18.0.0
- Verified compatibility with MoPub SDK 4.18.0.
- Updated the Adapter project for Android Studio 3.0 and Android API 26.

#### Version 4.17.0.0
- Using MoPub's impression and click reporting for native ads (AdMob and MoPub
  impression and click statistics will match up).
- Verified compatibility with MoPub SDK 4.17.0.

#### Version 4.16.1.0
- Verified compatibility with MoPub SDK 4.16.1.

#### Version 4.16.0.0
- Verified compatibility with MoPub SDK 4.16.0.

#### Version 4.15.0.0
- Verified compatibility with MoPub SDK 4.15.0.

#### Version 4.14.0.0
- Verified compatibility with MoPub SDK 4.14.0.
- Adapter shows a warning when the requested banner ad size does not match the
  ad size set in MoPub UI.

#### Version 4.13.0.0
- Verified compatibility with MoPub SDK 4.13.0.

#### Version 4.12.0.0
- Verified compatibility with MoPub SDK 4.12.0.
- Added support for MoPub native ads.
- Updated interstitial and banner ads per Google's latest mediation APIs.

#### Previous
- Support for MoPub banner and interstitial ads.
