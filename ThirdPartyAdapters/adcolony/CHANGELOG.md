## AdColony Android Mediation Adapter Changelog

#### Version 4.7.1.0
- Verified compatibility with AdColony SDK version 4.7.1.

Built and tested with:
- Google Mobile Ads SDK version 20.6.0.
- AdColony SDK version 4.7.1.

#### Version 4.7.0.0
- Verified compatibility with AdColony SDK version 4.7.0.
- Updated the minimum required Google Mobile Ads SDK version to 20.6.0.

Built and tested with:
- Google Mobile Ads SDK version 20.6.0.
- AdColony SDK version 4.7.0.

#### Version 4.6.5.0
- Verified compatibility with AdColony SDK version 4.6.5.

Built and tested with:
- Google Mobile Ads SDK version 20.4.0.
- AdColony SDK version 4.6.5.

#### Version 4.6.4.0
- Verified compatibility with AdColony SDK version 4.6.4.

Built and tested with:
- Google Mobile Ads SDK version 20.4.0.
- AdColony SDK version 4.6.4.

#### Version 4.6.3.0
- Verified compatibility with AdColony SDK version 4.6.3.
- Updated the minimum required Google Mobile Ads SDK version to 20.4.0.

Built and tested with:
- Google Mobile Ads SDK version 20.4.0.
- AdColony SDK version 4.6.3.

#### Version 4.6.2.0
- Verified compatibility with AdColony SDK version 4.6.2.

Built and tested with:
- Google Mobile Ads SDK version 20.3.0.
- AdColony SDK version 4.6.2.

#### Version 4.6.0.0
- Verified compatibility with AdColony SDK version 4.6.0.
- Updated the minimum required Google Mobile Ads SDK version to 20.3.0.

Built and tested with:
- Google Mobile Ads SDK version 20.3.0.
- AdColony SDK version 4.6.0.

#### Version 4.5.0.0
- Added support for banners advanced bidding.
- Verified compatibility with AdColony SDK version 4.5.0.
- Updated the minimum required Google Mobile Ads SDK version to 20.0.0.

Built and tested with:
- Google Mobile Ads SDK version 20.0.0.
- AdColony SDK version 4.5.0.

#### Version 4.4.1.0
- Verified compatibility with AdColony SDK version 4.4.1.

Built and tested with:
- Google Mobile Ads SDK version 19.7.0.
- AdColony SDK version 4.4.1.

#### Version 4.4.0.0
- Verified compatibility with AdColony SDK version 4.4.0.
- Added support of AdColony's `collectSignals()` method for bidding.
- Updated the minimum required Google Mobile Ads SDK version to 19.7.0.

Built and tested with:
- Google Mobile Ads SDK version 19.7.0.
- AdColony SDK version 4.4.0.

#### Version 4.3.0.0
- Verified compatibility with AdColony SDK version 4.3.0.
- Updated the minimum required Google Mobile Ads SDK version to 19.5.0.

Built and tested with:
- Google Mobile Ads SDK version 19.5.0.
- AdColony SDK version 4.3.0.

#### Version 4.2.4.0
- Fixed a bug where `onUserEarnedReward()` was not forwarded for rewarded ads.
- Added additional error codes for AdColony SDK initialization errors.
- Updated the minimum required Google Mobile Ads SDK version to 19.4.0.

Built and tested with:
- Google Mobile Ads SDK version 19.4.0.
- AdColony SDK version 4.2.4.

#### Version 4.2.0.0
- Verified compatibility with AdColony SDK version 4.2.0.
- Updated the adapter to support inline adaptive banner requests.
- Updated the minimum required Google Mobile Ads SDK version to 19.2.0.

Built and tested with:
- Google Mobile Ads SDK version 19.2.0.
- AdColony SDK version 4.2.0.

#### Version 4.1.4.1
- Added descriptive error codes and reasons for adapter load/show failures.
- Updated the minimum required Google Mobile Ads SDK version to 19.0.1.

Built and tested with:
- Google Mobile Ads SDK version 19.0.1.
- AdColony SDK version 4.1.4.

#### Version 4.1.4.0
- Verified compatibility with AdColony SDK version 4.1.4.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- AdColony SDK version 4.1.4.

#### Version 4.1.3.1
- Fixed an issue where the `onRewardedAdLoaded()` callback is not being forwarded by the adapter.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- AdColony SDK version 4.1.3.

#### Version 4.1.3.0
- Verified compatibility with AdColony SDK version 4.1.3.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- AdColony SDK version 4.1.3.

#### Version 4.1.2.0
- Verified compatibility with AdColony SDK version 4.1.2.
- Updated the minimum required Google Mobile Ads SDK version to 18.3.0.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- AdColony SDK version 4.1.2.

#### Version 4.1.0.0
- Verified compatibility with AdColony SDK version 4.1.0.
- Added support for banner ads.
- Updated the minimum required Google Mobile Ads SDK version to 18.2.0.

Built and tested with:
- Google Mobile Ads SDK version 18.2.0.
- AdColony SDK version 4.1.0.

#### Version 3.3.11.0
- Verified compatibility with AdColony SDK version 3.3.11.
- Updated the minimum required Google Mobile Ads SDK version to 18.1.1.

#### Version 3.3.10.1
- Added bidding capability to the adapter for interstitial
  and rewarded ads.

#### Version 3.3.10.0
- Verified compatibility with AdColony SDK version 3.3.10.

#### Version 3.3.9.0
- Verified compatibility with AdColony SDK version 3.3.9.
- Removed `setGdprRequired()` and `setGdprConsentString()` methods on `AdColonyBundleBuilder`.
- Added the `AdColonyMediationAdapter.getAppOptions()` method. Publishers must now pass GDPR information to AdColony's SDK through these options. See the [developer documentation](https://developers.google.com/admob/android/mediation/adcolony#eu_consent_and_gdpr) for more details.

#### Version 3.3.8.1
- Updated adapter to support new open-beta Rewarded API.
- Updated the minimum required Google Mobile Ads SDK version to 17.2.0.

#### Version 3.3.8.0
- Verified compatibility with AdColony SDK version 3.3.8.

#### Version 3.3.7.0
- Verified compatibility with AdColony SDK version 3.3.7.

#### Version 3.3.6.0
- Verified compatibility with AdColony SDK version 3.3.6.

#### Version 3.3.5.1
- Updated the adapter to invoke the `onRewardedVideoComplete()` ad event.

#### Version 3.3.5.0
- Verified compatibility with AdColony SDK version 3.3.5.

#### Version 3.3.4.0
- Verified compatibility with AdColony SDK version 3.3.4.
- Updated the adapter with GDPR.

#### Version 3.3.3.0
- Verified compatibility with AdColony SDK version 3.3.3.
- Removed the `setTestModeEnabled` method from the Bundle builder class. Publishers can now request test ads from AdColony by specifying a test device via `addTestDevice()` method from the AdRequest builder class.

#### Version 3.3.2.0
- Verified compatibility with AdColony SDK version 3.3.2.

#### Version 3.3.0.1
- Fixed an issue where the adapter fails to fill when the adapter is
  reinitialized.
- Fixed an issue where reward callback is sent incorrectly.

#### Version 3.3.0.0
- Verified compatibility with AdColony SDK version 3.3.0.
- Updated the Adapter project for Android Studio 3.0.

#### Version 3.2.1.1
- Added `setTestModeEnabled` method to the Bundle builder class. Publishers can
  use this method to mark AdColony requests as test requests.

#### Version 3.2.1.0
- Verified compatibility with AdColony SDK version 3.2.1.

#### Version 3.2.0.0
- Verified compatibility with AdColony SDK version 3.2.0.

#### Version 3.1.2.0
- Fixed possible NullPointerExceptions.
- Verified compatibility with AdColony SDK version 3.1.2.

#### Version 3.1.1.0
- Verified compatibility with AdColony SDK version 3.1.1.

#### Version 3.1.0.0
- Verified compatibility with AdColony SDK version 3.1.0.

#### Version 3.0.6.0
- Changed the version naming system to
  [AdColony SDK version].[adapter patch version].
- Updated the minimum required AdColony SDK to v3.0.6.

#### Earlier Versions
- Supports rewarded video and interstitial ads.
