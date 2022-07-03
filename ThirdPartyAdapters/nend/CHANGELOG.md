## nend Android Mediation Adapter Changelog

#### Version 8.1.0.0
- Verified compatibility with nend SDK 8.1.0.
- Fixed a `NullPointerException` crash when nend returns a `null` ad image or logo URL.
- Updated the minimum required Google Mobile Ads SDK version to 20.5.0.

Built and tested with
- Google Mobile Ads SDK version 20.5.0.
- Nend SDK version 8.1.0.

#### Version 8.0.1.0
- Verified compatibility with nend SDK 8.0.1.
- Fixed a bug where some `AdError` objects were returned using the incorrect domain.
- Updated the minimum required Google Mobile Ads SDK version to 20.4.0.

Built and tested with
- Google Mobile Ads SDK version 20.4.0.
- Nend SDK version 8.0.1.

#### Version 7.1.0.0
- Verified compatibility with nend SDK 7.1.0.
- Updated error codes to capture the nend `UNSUPPORTED_DEVICE` error.
- Updated the minimum required Google Mobile Ads SDK version to 20.2.0.

Built and tested with
- Google Mobile Ads SDK version 20.2.0.
- Nend SDK version 7.1.0.

#### Version 7.0.3.0
- Verified compatibility with nend SDK 7.0.3.
- Added descriptive error codes and reasons for adapter load/show failures.
- Updated the minimum required Google Mobile Ads SDK version to 20.0.0.

Built and tested with
- Google Mobile Ads SDK version 20.0.0.
- Nend SDK version 7.0.3.

#### Version 7.0.0.0
- Verified compatibility with nend SDK 7.0.0.
- Updated the minimum required Google Mobile Ads SDK version to 19.4.0.

Built and tested with
- Google Mobile Ads SDK version 19.4.0.
- Nend SDK version 7.0.0.

#### Version 6.0.1.0
- Verified compatibility with nend SDK 6.0.1.
- Updated the minimum required Google Mobile Ads SDK version to 19.3.0.

Built and tested with
- Google Mobile Ads SDK version 19.3.0.
- Nend SDK version 6.0.1.

#### Version 6.0.0.0
- Verified compatibility with nend SDK 6.0.0.
- Updated minimum Android SDK version to API 19.

Built and tested with
- Google Mobile Ads SDK version 19.2.0.
- Nend SDK version 6.0.0.

#### Version 5.4.2.1
- Updated the adapter to support inline adaptive banner requests.
- Fixed a rare race condition crash that may happen when smart banner ads are destroyed.
- Updated the minimum required Google Mobile Ads SDK version to 19.2.0.

Built and tested with
- Google Mobile Ads SDK version 19.2.0.
- Nend SDK version 5.4.2.

#### Version 5.4.2.0
- Added support for native ads.
- Verified compatibility with nend SDK 5.4.2.
- Updated the minimum required Google Mobile Ads SDK version to 19.0.0.

Built and tested with
- Google Mobile Ads SDK version 19.0.0.
- Nend SDK version 5.4.2.

#### Version 5.3.0.0
- Verified compatibility with nend SDK 5.3.0.
- Updated the minimum required Google Mobile Ads SDK version to 18.2.0.

Built and tested with
- Google Mobile Ads SDK version 18.2.0.
- Nend SDK version 5.3.0.

#### Version 5.2.0.0
- Verified compatibility with nend SDK 5.2.0.
- Updated the minimum required Google Mobile Ads SDK version to 18.1.1.

#### Version 5.1.0.4
- Fixed an issue where Banner and Interstitial ads were not forwarding the `onAdClicked()` event.
- Fixed an issue where a `NullPointerException` was thrown when a nend Banner ad was destroyed.
- Migrated the adapter to AndroidX.
- Updated the minimum required Google Mobile Ads SDK version to 18.0.0.

#### Version 5.1.0.3
- Added implementation to display a part of nend banner in SmartBanner. Appropriate size for SmartBanner
  - Portrait and Landscape
    - Phones: 320×50
    - Tablets: 728×90 or 320×50

#### Version 5.1.0.2
- Added support for flexible banner ad sizes.

#### Version 5.1.0.1
- Updated adapter to support new open-beta Rewarded API.
- Updated the minimum required Google Mobile Ads SDK version to 17.2.0.

#### Version 5.1.0.0
- Verified compatibility with nend SDK 5.1.0.

#### Version 5.0.2.1
- Removed function that forward user features because following methods are deprecated on `AdRequest.Builder`.
  - `setGender()`
  - `setBirthday()`
  - `setIsDesignedForFamiles()`

#### Version 5.0.2.0
- Verified compatibility with nend SDK 5.0.2.

#### Version 5.0.1.0
- Verified compatibility with nend SDK 5.0.1.

#### Version 5.0.0.0
- Verified compatibility with nend SDK 5.0.0.

#### Version 4.0.5.0
- Verified compatibility with nend SDK 4.0.5.

#### Version 4.0.4.1
- Supported onRewardedVideoCompleted() method of RewardedVideoAdListener.

#### Version 4.0.4.0
- Verified compatibility with nend SDK 4.0.4.

#### Version 4.0.2.1
- Added the ability to create a `Bundle` of mediation extras using the
  `NendExtrasBundleBuilder` class.

#### Version 4.0.2.0
- First release in Google Mobile Ads Mediation open source project.
- Added support for banner, interstitial, and rewarded video ads.
