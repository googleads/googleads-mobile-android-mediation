## AppLovin Android Mediation Adapter Changelog

### Version 9.13.1.0
- Verified compatibility with AppLovin SDK 9.13.1.
- Adapter now throws an error if multiple interstitial ads are requested using the same Zone ID.

Built and tested with:
- Google Mobile Ads SDK version 19.2.0.
- AppLovin SDK version 9.13.1.

#### Version 9.13.0.0
- Updated the adapter to support inline adaptive banner requests.
- Verified compatibility with AppLovin SDK 9.13.0.
- Updated the minimum required Google Mobile Ads SDK version to 19.2.0.

Built and tested with:
- Google Mobile Ads SDK version 19.2.0.
- AppLovin SDK version 9.13.0.

#### Version 9.12.8.0
- Verified compatibility with AppLovin SDK 9.12.8.

Built and tested with:
- Google Mobile Ads SDK version 19.1.0.
- AppLovin SDK version 9.12.8.

#### Version 9.12.7.0
- Verified compatibility with AppLovin SDK 9.12.7.

Built and tested with:
- Google Mobile Ads SDK version 19.1.0.
- AppLovin SDK version 9.12.7.

#### Version 9.12.6.1
- Fixed bug introduced in [9.11.4.1](https://github.com/googleads/googleads-mobile-android-mediation/blob/master/ThirdPartyAdapters/applovin/CHANGELOG.md#version-91141) where open bidding banner ads timeout.

Built and tested with:
- Google Mobile Ads SDK version 19.1.0.
- AppLovin SDK version 9.12.6.

#### Version 9.12.6.0
- Verified compatibility with AppLovin SDK 9.12.6.

Built and tested with:
- Google Mobile Ads SDK version 19.1.0.
- AppLovin SDK version 9.12.6.

#### Version 9.12.5.0
- Verified compatibility with AppLovin SDK 9.12.5.

Built and tested with:
- Google Mobile Ads SDK version 19.1.0.
- AppLovin SDK version 9.12.5.

#### Version 9.12.4.0
- Verified compatibility with AppLovin SDK 9.12.4.
- Adapter now requires an `Activity` context to initialize and load ads from AppLovin.

Built and tested with:
- Google Mobile Ads SDK version 19.1.0.
- AppLovin SDK version 9.12.4.

#### Version 9.12.3.0
- Verified compatibility with AppLovin SDK 9.12.3.

Built and tested with:
- Google Mobile Ads SDK version 19.1.0.
- AppLovin SDK version 9.12.3.

#### Version 9.12.2.0
- Verified compatibility with AppLovin SDK 9.12.2.

Built and tested with:
- Google Mobile Ads SDK version 19.1.0.
- AppLovin SDK version 9.12.2.

#### Version 9.12.1.0
- Verified compatibility with AppLovin SDK 9.12.1.
- Updated the minimum required Google Mobile Ads SDK version to 19.1.0.

Built and tested with:
- Google Mobile Ads SDK version 19.1.0.
- AppLovin SDK version 9.12.1.

#### Version 9.12.0.0
- Verified compatibility with AppLovin SDK 9.12.0.

Built and tested with:
- Google Mobile Ads SDK version 19.0.1.
- AppLovin SDK version 9.12.0.

#### Version 9.11.4.1
- Fixed an issue that may cause open bidding banner/interstitial ad requests to timeout.
- Added descriptive error codes and reasons for adapter load/show failures.
- Updated the minimum required Google Mobile Ads SDK version to 19.0.1.

Built and tested with:
- Google Mobile Ads SDK version 19.0.1.
- AppLovin SDK version 9.11.4.

#### Version 9.11.4.0
- Verified compatibility with AppLovin SDK 9.11.4.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- AppLovin SDK version 9.11.4.

#### Version 9.11.2.0
- Verified compatibility with AppLovin SDK 9.11.2.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- AppLovin SDK version 9.11.2.

#### Version 9.11.1.0
- Verified compatibility with AppLovin SDK 9.11.1.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- AppLovin SDK version 9.11.1.

#### Version 9.10.5.0
- Verified compatibility with AppLovin SDK 9.10.5.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- AppLovin SDK version 9.10.5.

#### Version 9.9.1.2
- Removed all references to AppLovin placement ID.
- Updated the minimum required Google Mobile Ads SDK version to 18.3.0.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- AppLovin SDK version 9.9.1.

#### Version 9.9.1.1
- Native ads now leverage the unified native ads mediation API.

Built and tested with:
- Google Mobile Ads SDK version 18.2.0.
- AppLovin SDK version 9.9.1.

#### Version 9.9.1.0
- Verified compatibility with AppLovin SDK 9.9.1.
- Updated the minimum required Google Mobile Ads SDK version to 18.2.0.

#### Version 9.8.0.0
- Verified compatibility with AppLovin SDK 9.8.0.
- Updated the minimum required Google Mobile Ads SDK version to 18.1.1.

#### Version 9.7.2.0
- Verified compatibility with AppLovin SDK 9.7.2.
- Updated the minimum required Google Mobile Ads SDK version to 18.1.0.

#### Version 9.4.2.0
- Added open bidding capability to the adapter for banner, interstitial and rewarded ads.
- Added support for flexible banner ad sizes.
- Verified compatibility with AppLovin SDK 9.4.2.

#### Version 9.2.1.1
- Updated adapter to support new open-beta Rewarded API.
- Updated the minimum required Google Mobile Ads SDK version to 17.2.0.

#### Version 9.2.1.0
- Verified compatibility with AppLovin SDK 9.2.1

#### Version 9.1.3.0
- Removed support for placements as they were deprecated by AppLovin SDK.
- Verified compatibility with AppLovin SDK 9.1.3

#### Version 9.1.0.0
- Verified compatibility with AppLovin SDK 9.1.0

#### Version 8.1.4.0
- Verified compatibility with AppLovin SDK 8.1.4

#### Version 8.1.3.0
- Verified compatibility with AppLovin SDK 8.1.3

#### Version 8.1.0.0
- Verified compatibility with AppLovin SDK 8.1.0

#### Version 8.0.2.1
- Updated the adapter to invoke the `onRewardedVideoComplete()` ad event.

#### Version 8.0.2.0
- Verified compatibility with AppLovin SDK 8.0.2

#### Version 8.0.1.1
- Added support for native ads.
- Set AdMob as mediation provider on the AppLovin SDK.

#### Version 8.0.1.0
- Verified compatibility with AppLovin SDK 8.0.1

#### Version 8.0.0.0
- Verified compatibility with AppLovin SDK 8.0.0

#### Version 7.8.6.0
- Verified compatibility with AppLovin SDK 7.8.6

#### Version 7.8.5.0
- Added support for zones and smart banners.
- Verified compatibility with AppLovin SDK 7.8.5.

#### Version 7.7.0.0
- Verified compatibility with AppLovin SDK 7.7.0.

#### Version 7.6.2.0
- Verified compatibility with AppLovin SDK 7.6.2.

#### Version 7.6.1.0
- Verified compatibility with AppLovin SDK 7.6.1.

#### Version 7.6.0.0
- Verified compatibility with AppLovin SDK 7.6.0.

#### Version 7.5.0.0
- Verified compatibility with AppLovin SDK 7.5.0.

#### Version 7.4.1.1
- Added support for banner ads.

#### Version 7.4.1.0
- Verified compatibility with AppLovin SDK 7.4.1.

#### Version 7.3.2.0
- Added support for interstitial ads.

#### Earlier versions
- Added support for rewarded video ads.
