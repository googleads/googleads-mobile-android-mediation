## Vungle Android Mediation Adapter Changelog

#### Version 6.10.5.0
- Verified compatibility with Vungle SDK 6.10.5.
- Updated the minimum required Google Mobile Ads SDK version to 20.6.0.

Built and tested with:
- Google Mobile Ads SDK version 20.6.0.
- Vungle SDK version 6.10.5.

#### Version 6.10.4.0
- Verified compatibility with Vungle SDK 6.10.4.

Built and tested with:
- Google Mobile Ads SDK version 20.5.0.
- Vungle SDK version 6.10.4.

#### Version 6.10.3.0
- Verified compatibility with Vungle SDK 6.10.3.

Built and tested with:
- Google Mobile Ads SDK version 20.5.0.
- Vungle SDK version 6.10.3.

#### Version 6.10.2.1
- Verified compatibility with Vungle SDK 6.10.2.
- Added bidding support for interstitial and rewarded ad formats.
- Updated the minimum required Google Mobile Ads SDK version to 20.5.0.

Built and tested with:
- Google Mobile Ads SDK version 20.5.0.
- Vungle SDK version 6.10.2.

#### Version 6.10.2.0
- Verified compatibility with Vungle SDK 6.10.2.
- Fixed an adapter issue by replacing parameter `serverParameters`, with `mediationExtras` to obtain Vungle network-specific parameters, when requesting Banner and Interstitial ads.

Built and tested with:
- Google Mobile Ads SDK version 20.3.0.
- Vungle SDK version 6.10.2.

#### Version 6.10.1.0
- Verified compatibility with Vungle SDK 6.10.1.
- Updated the minimum required Google Mobile Ads SDK version to 20.3.0.
- MREC Ads are now supported with Vungle's banner API.
- Updated standardized error codes and messages.

Built and tested with:
- Google Mobile Ads SDK version 20.3.0.
- Vungle SDK version 6.10.1.

#### Version 6.9.1.1
- Updated the minimum required Google Mobile Ads SDK version to 20.0.0.

Built and tested with:
- Google Mobile Ads SDK version 20.0.0.
- Vungle SDK version 6.9.1.

#### Version 6.9.1.0
- Verified compatibility with Vungle SDK 6.9.1.
- Added support for OMSDK.
- Various bug fixes.
- Rewarded Ad Support for Vungle onAdViewed callback.
- Updated the minimum required Google Mobile Ads SDK version to 19.7.0.

Built and tested with:
- Google Mobile Ads SDK version 19.7.0.
- Vungle SDK version 6.9.1.

#### Version 6.8.1.1
- Updated the minimum required Google Mobile Ads SDK version to 19.6.0.

Built and tested with:
- Google Mobile Ads SDK version 19.6.0.
- Vungle SDK version 6.8.1.

#### Version 6.8.1.0
- Verified compatibility with Vungle SDK 6.8.1.
- Updated the minimum required Google Mobile Ads SDK version to 19.5.0.

Built and tested with:
- Google Mobile Ads SDK version 19.5.0.
- Vungle SDK version 6.8.1.

#### Version 6.8.0.0
- Verified compatibility with Vungle SDK 6.8.0.
- Updated the adapter to not forward `onAdClosed()` when banner ads are refreshed or destroyed.
- Remove `FlexFeed` and `FlexView` (deprecated in Vungle 6.8.0).
- Updated the minimum required Google Mobile Ads SDK version to 19.4.0.

Built and tested with:
- Google Mobile Ads SDK version 19.4.0.
- Vungle SDK version 6.8.0.

#### Version 6.7.1.0
- Verified compatibility with Vungle SDK 6.7.1.
- Fixed a bug where ads wouldn't load if an ad was loaded using an application context.
- Updated `targetSdkVersion` to API 29.
- Updated the minimum required Google Mobile Ads SDK version to 19.3.0.

Built and tested with:
- Google Mobile Ads SDK version 19.3.0.
- Vungle SDK version 6.7.1.

#### Version 6.7.0.0
- Verified compatibility with Vungle SDK 6.7.0.
- Updated the adapter to support inline adaptive banner requests.
- Interstitial and rewarded ads are now unmuted by default.
- Interstitial ads now forward the `onAdLeftApplication()` callback when clicked.
- Updated the minimum required Google Mobile Ads SDK version to 19.2.0.

Built and tested with:
- Google Mobile Ads SDK version 19.2.0.
- Vungle SDK version 6.7.0.

#### Version 6.5.3.0
- Verified compatibility with Vungle SDK 6.5.3.
- Add support for the newly-introduced Vungle's Banner format.
- Updated the minimum required Google Mobile Ads SDK version to 19.0.1.

Built and tested with:
- Google Mobile Ads SDK version 19.0.1.
- Vungle SDK version 6.5.3.

#### Version 6.4.11.1
- Fixed an issue where banner ads failed to refresh.

#### Version 6.4.11.0
- Verified compatibility with Vungle SDK 6.4.11.
- Added support for banner ads.
- Migrated the adapter to AndroidX.
- Updated the minimum required Google Mobile Ads SDK version to 18.2.0.

#### Version 6.3.24.1
- Updated adapter to support new open-beta Rewarded API.
- Updated the minimum required Google Mobile Ads SDK version to 17.2.0.

#### Version 6.3.24.0
- Verified compatibility with Vungle SDK 6.3.24.

#### Version 6.3.17.0
- Verified compatibility with Vungle SDK 6.3.17.

#### Version 6.3.12.0
- Verified compatibility with Vungle SDK 6.3.12.

#### Version 6.2.5.1
- Updated the adapter to invoke the `onRewardedVideoComplete()` ad event.

#### Version 6.2.5.0
- Verified compatibility with Vungle SDK 6.2.5.

#### Version 5.3.2.1
- Updated adapter to correctly report clicks to the Google Mobile Ads SDK.

#### Version 5.3.2.0
- Verified compatibility with Vungle SDK 5.3.2.
- Updated the Adpater project for Android Studio 3.0.
- Added the following methods to Bundle builder class.
    - `setOrdinalViewCount` : This field is used to pass the mediation ordinal,
      whenever Publisher receives the ordinal data reports from Vungle.
    - `setFlexViewCloseTimeInSec` : This option is used to make flex view ads
      dismiss on their own after the specified number of seconds.

#### Version 5.3.0.0
- Verified compatibility with Vungle SDK 5.3.0.

#### Version 5.1.0.0
- Updated the adapter to make it compatible with Vungle SDK 5.1.0.
- Changed the version naming system to
  [Vungle SDK version].[adapter patch version].

#### Earlier versions
- Added support for interstitial and rewarded video ad formats.
