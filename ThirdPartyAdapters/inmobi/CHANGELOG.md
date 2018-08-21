# InMobi Adapter for Google Mobile Ads SDK for Android Changelog

## 7.1.1.1
- Updated the adapter to invoke the `onRewardedVideoComplete` ad event.

## 7.1.1.0
- Verified compatibility with InMobi SDK version 7.1.1.

## 7.1.0.0
- Added InMobiConsent class which provides updateGDPRConsent() and getConsentObj() methods.
- Verified compatibility with InMobi SDK version 7.1.0.

## 7.0.4.0
- Verified compatibility with InMobi SDK version 7.0.4.

## 7.0.2.0
- Verified compatibility with InMobi SDK version 7.0.2.

## 7.0.1.0
- Updated the adapter to make it compatible with InMobi SDK version 7.0.1.
- Added support for native video ads.
- For native ads, a media view is always returned by the adapter. The adapter
  no longer returns an image asset, instead the media view will display an image
  for static native ads.
- Updated the adapter project for Android Studio 3.0.

## 6.2.4.0
- Verified compatibility with InMobi SDK version 6.2.4.

## 6.2.3.0
- Changed the version naming system to
  [InMobi SDK version].[adapter patch version].

## Earlier versions
- Adds support for banners, interstitials, rewarded video and native ad formats.
