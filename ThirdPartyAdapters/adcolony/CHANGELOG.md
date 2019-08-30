# AdColony Adapter for Google Mobile Ads SDK for Android Changelog

## 3.3.11.0
- Verified compatibility with AdColony SDK version 3.3.11.
- Updated the minimum required Google Mobile Ads SDK version to 18.1.1.

## 3.3.10.1
- Added open bidding capability to the adapter for interstitial
  and rewarded ads.

## 3.3.10.0
- Verified compatibility with AdColony SDK version 3.3.10.

## 3.3.9.0
- Verified compatibility with AdColony SDK version 3.3.9.
- Removed `setGdprRequired()` and `setGdprConsentString()` methods on `AdColonyBundleBuilder`.
- Added the `AdColonyMediationAdapter.getAppOptions()` method. Publishers must now pass GDPR information to AdColony's SDK through these options. See the [developer documentation](https://developers.google.com/admob/android/mediation/adcolony#eu_consent_and_gdpr) for more details.

## 3.3.8.1
- Updated adapter to support new open-beta Rewarded API.
- Updated the minimum required Google Mobile Ads SDK version to 17.2.0.

## 3.3.8.0
- Verified compatibility with AdColony SDK version 3.3.8.

## 3.3.7.0
- Verified compatibility with AdColony SDK version 3.3.7.

## 3.3.6.0
- Verified compatibility with AdColony SDK version 3.3.6.

## 3.3.5.1
- Updated the adapter to invoke the `onRewardedVideoComplete()` ad event.

## 3.3.5.0
- Verified compatibility with AdColony SDK version 3.3.5.

## 3.3.4.0
- Verified compatibility with AdColony SDK version 3.3.4.
- Updated the adapter with GDPR.

## 3.3.3.0
- Verified compatibility with AdColony SDK version 3.3.3.
- Removed the `setTestModeEnabled` method from the Bundle builder class. Publishers can now request test ads from AdColony by specifying a test device via `addTestDevice()` method from the AdRequest builder class.

## 3.3.2.0
- Verified compatibility with AdColony SDK version 3.3.2.

## 3.3.0.1
- Fixed an issue where the adapter fails to fill when the adapter is
  reinitialized.
- Fixed an issue where reward callback is sent incorrectly.

## 3.3.0.0
- Verified compatibility with AdColony SDK version 3.3.0.
- Updated the Adapter project for Android Studio 3.0.

## 3.2.1.1
- Added `setTestModeEnabled` method to the Bundle builder class. Publishers can
  use this method to mark AdColony requests as test requests.

## 3.2.1.0
- Verified compatibility with AdColony SDK version 3.2.1.

## 3.2.0.0
- Verified compatibility with AdColony SDK version 3.2.0.

## 3.1.2.0
- Fixed possible NullPointerExceptions.
- Verified compatibility with AdColony SDK version 3.1.2.

## 3.1.1.0
- Verified compatibility with AdColony SDK version 3.1.1.

## 3.1.0.0
- Verified compatibility with AdColony SDK version 3.1.0.

## 3.0.6.0
- Changed the version naming system to
  [AdColony SDK version].[adapter patch version].
- Updated the minimum required AdColony SDK to v3.0.6.

## Earlier Versions
- Supports rewarded video and interstitial ads.
