## InMobi Android Mediation Adapter Changelog

#### Version 9.0.8.0
- Removed support for the deprecated NativeAppInstallAd format. Apps should request unified native ads.
- Updated the minimum required Google Mobile Ads SDK version to 19.3.0.
- Verified compatibility with InMobi SDK version 9.0.8.

Built and test with:
- Google Mobile Ads SDK version 19.3.0.
- InMobi SDK version 9.0.8.

#### Version 9.0.7.1
- Fixed an issue where the adapter did not keep InMobi SDK's initialization state properly.
- Updated the minimum required Google Mobile Ads SDK version to 19.2.0.

Built and test with:
- Google Mobile Ads SDK version 19.2.0.
- InMobi SDK version 9.0.7.

#### Version 9.0.7.0
- Verified compatibility with InMobi SDK version 9.0.7.
- Updated the adapter to support inline adaptive banner requests.
- Adapter now includes proguard configuration as suggested by [InMobi's guidelines](https://support.inmobi.com/monetize/android-guidelines).

Built and test with:
- Google Mobile Ads SDK version 19.1.0.
- InMobi SDK version 9.0.7.

#### Version 9.0.6.0
- Verified compatibility with InMobi SDK version 9.0.6.
- Native ads: Fixed a bug that causes the `primaryView` of InMobi to disappear
while scrolling in native feed integration.
- Native ads: Fixed a bug that causes the `primaryView` of InMobi not being
positioned center inside the `mediaView`.

Built and test with:
- Google Mobile Ads SDK version 19.1.0.
- InMobi SDK version 9.0.6.

#### Version 9.0.5.0
- Verified compatibility with InMobi SDK version 9.0.5.

Built and test with:
- Google Mobile Ads SDK version 19.1.0.
- InMobi SDK version 9.0.5.

#### Version 9.0.4.0
- Updated the minimum required Google Mobile Ads SDK version to 19.1.0.

Built and test with:
- Google Mobile Ads SDK version 19.1.0.
- InMobi SDK version 9.0.4.

#### Version 9.0.2.0
- Verified compatibility with InMobi SDK version 9.0.2.
- Updated the minimum required Google Mobile Ads SDK version to 18.3.0.
- Removed open bidding capability for banner, interstitial, and rewarded formats.
- Fixed a bug that causes a crash when trying to render a native ad.

Built and tested with
- Google Mobile Ads SDK version 18.3.0.
- InMobi SDK version 9.0.2.

#### Version 7.3.0.1
- Native ads now leverage the unified native ads mediation API.

Built and tested with
- Google Mobile Ads SDK version 18.2.0.
- InMobi SDK version 7.3.0.

#### Version 7.3.0.0
- Verified compatibility with InMobi SDK version 7.3.0.
- Updated the minimum required Google Mobile Ads SDK version to 18.2.0.

#### Version 7.2.9.0
- Added open bidding capability to the adapter for banner, interstitial and rewarded ads.
- Verified compatibility with InMobi SDK version 7.2.9.
- Updated the minimum required Google Mobile Ads SDK version to 18.1.1.

#### Version 7.2.7.0
- Verified compatibility with InMobi SDK version 7.2.7.

#### Version 7.2.2.2
- Added support for flexible banner ad sizes.

#### Version 7.2.2.1
- Updated adapter to support new open-beta Rewarded API.
- Updated the minimum required Google Mobile Ads SDK version to 17.2.0.

#### Version 7.2.2.0
- Verified compatibility with InMobi SDK version 7.2.2.

#### Version 7.2.1.0
- Verified compatibility with InMobi SDK version 7.2.1.

#### Version 7.2.0.0
- Verified compatibility with InMobi SDK version 7.2.0.

#### Version 7.1.1.1
- Updated the adapter to invoke the `onRewardedVideoComplete` ad event.

#### Version 7.1.1.0
- Verified compatibility with InMobi SDK version 7.1.1.

#### Version 7.1.0.0
- Added InMobiConsent class which provides updateGDPRConsent() and getConsentObj() methods.
- Verified compatibility with InMobi SDK version 7.1.0.

#### Version 7.0.4.0
- Verified compatibility with InMobi SDK version 7.0.4.

#### Version 7.0.2.0
- Verified compatibility with InMobi SDK version 7.0.2.

#### Version 7.0.1.0
- Updated the adapter to make it compatible with InMobi SDK version 7.0.1.
- Added support for native video ads.
- For native ads, a media view is always returned by the adapter. The adapter
  no longer returns an image asset, instead the media view will display an image
  for static native ads.
- Updated the adapter project for Android Studio 3.0.

#### Version 6.2.4.0
- Verified compatibility with InMobi SDK version 6.2.4.

#### Version 6.2.3.0
- Changed the version naming system to
  [InMobi SDK version].[adapter patch version].

#### Earlier versions
- Adds support for banners, interstitials, rewarded video and native ad formats.
