## Unity Ads Android Mediation Adapter Changelog

#### Version 3.2.0.1
- Fixed a null pointer exception crash that occurred when calling `loadAd()`
  before calling `UnityAds.initialize()`.
- Updated the minimum required Google Mobile Ads SDK version to 18.2.0.

#### Version 3.2.0.0
- Fixed an issue that caused Banner Ad requests to fail.
- Verified compatibility with Unity Ads SDK 3.2.0.
- Migrated the adapter to AndroidX.
- Updated the minimum required Google Mobile Ads SDK version to 18.1.1.

#### Version 3.1.0.0
- Added support for flexible banner ad sizes.
- Adapter fails the ad request if the requested size isn't compatible
  with any Unity Ads banner sizes
- Verified compatibility with Unity Ads SDK 3.1.0.

#### Version 3.0.1.0
- Verified compatibility with Unity Ads SDK 3.0.1.
- Fixed a bug that caused 'NPE' while showing an interstitial ad.

#### Version 3.0.0.2
- Updated adapter to support new open-beta Rewarded API.
- Updated the minimum required Google Mobile Ads SDK version to 17.2.0.

#### Version 3.0.0.1
- Added support for Unity Ads Banner.

#### Version 3.0.0.0
- Verified compatibility with Unity Ads SDK 3.0.0.

#### Version 2.3.0.0
- Verified compatibility with Unity Ads SDK 2.3.0.

#### Version 2.2.1.1
- Updated the adapter to invoke the `onRewardedVideoComplete()` ad event.

#### Version 2.2.1.0
- Verified compatibility with Unity Ads SDK 2.2.1.

#### Version 2.2.0.0
- Verified compatibility with Unity Ads SDK 2.2.0.

#### Version 2.1.2.0
- Verified compatibility with Unity Ads SDK 2.1.2.

#### Version 2.1.1.0
- Verified compatibility with Unity Ads SDK 2.1.1.

#### Version 2.1.0.0
- Updated the adapter to make it compatible with Unity Ads SDK 2.1.0.

#### Version 2.0.8.0
- Verified compatibility with Unity Ads SDK 2.0.8.

#### Version 2.0.7.0
- Using Unity Ads's click reporting (AdMob and Unity Ads click statistics will
  match up).
- Added onAdLeftApplication callback support.

#### Version 2.0.6.0
- Verified compatibility with Unity Ads SDK 2.0.6.

#### Version 2.0.5.0
- The adapters can now be added as a compile dependency by adding the following
  to the build.gradle file's dependencies tag:
  `compile 'com.google.ads.mediation:unity:2.0.5.0'`
- Moved to distributing the adapter as an aar instead of a jar file
  (see README for additional instructions).

#### Version 2.0.4.0
- Fixed a bug that caused rewarded video ads to fail to load when an
  interstitial ad was loaded first.

#### Version 2.0.2.0
- Changed the version naming system to
  [Unity Ads SDK version].[adapter patch version].
- Updated the minimum required Unity Ads SDK to v2.0.2.
- Updated the minimum required Google Mobile Ads SDK to v9.0.0.
- Apps are no longer required to call UnityAds.changeActivity(this).

#### Version 1.0.0
- Initial release. Supports reward-based video ads and interstitial ads.
