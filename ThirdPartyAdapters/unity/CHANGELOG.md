## Unity Ads Android Mediation Adapter Changelog

#### Version 4.1.0.0
- Verified compatibility with Unity Ads SDK 4.1.0.

Built and tested with:
- Google Mobile Ads SDK version 20.6.0.
- Unity Ads SDK version 4.1.0.

#### Version 4.0.1.0
- Verified compatibility with Unity Ads SDK 4.0.1.
- Updated the minimum required Google Mobile Ads SDK version to 20.6.0.

Built and tested with:
- Google Mobile Ads SDK version 20.6.0.
- Unity Ads SDK version 4.0.1.

#### Version 4.0.0.0
- Verified compatibility with Unity Ads SDK 4.0.0.
- Updated the minimum required Google Mobile Ads SDK version to 20.5.0.

Built and tested with:
- Google Mobile Ads SDK version 20.5.0.
- Unity Ads SDK version 4.0.0.

#### Version 3.7.5.0
- Verified compatibility with Unity Ads SDK 3.7.5.

Built and tested with:
- Google Mobile Ads SDK version 20.2.0.
- Unity Ads SDK version 3.7.5.

#### Version 3.7.4.0
- Verified compatibility with Unity Ads SDK 3.7.4.

Built and tested with:
- Google Mobile Ads SDK version 20.2.0.
- Unity Ads SDK version 3.7.4.

#### Version 3.7.2.0
- Verified compatibility with Unity Ads SDK 3.7.2.
- Updated the minimum required Google Mobile Ads SDK version to 20.2.0.

Built and tested with:
- Google Mobile Ads SDK version 20.2.0.
- Unity Ads SDK version 3.7.2.

#### Version 3.7.1.0
- Verified compatibility with Unity Ads SDK 3.7.1.
- Updated the minimum required Google Mobile Ads SDK version to 20.1.0.

Built and tested with:
- Google Mobile Ads SDK version 20.1.0.
- Unity Ads SDK version 3.7.1.

#### Version 3.6.2.0
- Verified compatibility with Unity Ads SDK 3.6.2.
- Fixed an issue where rewarded ads were not forwarding click callbacks.
- The UnityAds SDK has been removed from the bundled adapter build.
Publishers are now required to manually include the UnityAds SDK as an additional dependency.
- Updated the minimum required Google Mobile Ads SDK version to 19.8.0.

Built and tested with:
- Google Mobile Ads SDK version 19.8.0.
- Unity Ads SDK version 3.6.2.

#### Version 3.6.0.0
- Verified compatibility with Unity Ads SDK 3.6.0.
- Updated the minimum required Google Mobile Ads SDK version to 19.6.0.

Built and tested with:
- Google Mobile Ads SDK version 19.6.0.
- Unity Ads SDK version 3.6.0.

#### Version 3.5.1.1
- Fixed an issue where when trying to request for multiple interstitial and rewarded ads.

Built and tested with:
- Google Mobile Ads SDK version 19.5.0.
- Unity Ads SDK version 3.5.1.

#### Version 3.5.1.0
- Verified compatibility with Unity Ads SDK 3.5.1.
- Fixed an issue that causes smart banner ad requests to fail.

Built and tested with:
- Google Mobile Ads SDK version 19.5.0.
- Unity Ads SDK version 3.5.1.

#### Version 3.5.0.0
- Verified compatibility with Unity Ads SDK 3.5.0.
- Added adaptive banner support.
- Updated the minimum required Google Mobile Ads SDK version to 19.5.0.

Built and tested with:
- Google Mobile Ads SDK version 19.5.0.
- Unity Ads SDK version 3.5.0.

#### Version 3.4.8.0
- Fixed a `NullPointerException` error that occurs when a banner ad is destroyed.
- Updated the minimum required Google Mobile Ads SDK version to 19.3.0.

Built and tested with:
- Google Mobile Ads SDK version 19.3.0.
- Unity Ads SDK version 3.4.8.

#### Version 3.4.6.1
- Created an adapter build that does not include the Unity Ads SDK bundled in.
This gives publishers an option to use the Unity Ads Services when mediating on
Unity to avoid conflicting dependency issues.
  * Publishers may opt to use this by including the
  `com.google.ads.mediation:unity-adapter-only:x.y.z.p` dependency on their
  app-level `build.gradle` file.

Built and tested with:
- Google Mobile Ads SDK version 19.1.0.
- Unity Ads SDK version 3.4.6.

#### Version 3.4.6.0
- Verified compatibility with Unity Ads SDK 3.4.6.
- Adapter now forwards the `onAdOpened()` callback when a banner ad is clicked.

Built and tested with:
- Google Mobile Ads SDK version 19.1.0.
- Unity Ads SDK version 3.4.6.

#### Version 3.4.2.3
- Added descriptive error codes and reasons for adapter load/show failures.
- Updated the minimum required Google Mobile Ads SDK version to 19.1.0.

Built and tested with:
- Google Mobile Ads SDK version 19.1.0.
- Unity Ads SDK version 3.4.2.

#### Version 3.4.2.2
- Fixed a `ConcurrentModificationException` crash that occurred when Unity Ads returns an error.

Built and tested with:
- Google Mobile Ads SDK version 19.0.1.
- Unity Ads SDK version 3.4.2.

#### Version 3.4.2.1
- Improved forwarding of Unity's errors to recognize initialization and ad load failures earlier and reduce timeouts.
- Updated the minimum required Google Mobile Ads SDK version to 19.0.1.

Built and tested with:
- Google Mobile Ads SDK version 19.0.1.
- Unity Ads SDK version 3.4.2.

#### Version 3.4.2.0
- Verified compatibility with Unity Ads SDK 3.4.2.
- Updated the minimum required Google Mobile Ads SDK version to 18.3.0.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- Unity Ads SDK version 3.4.2.

#### Version 3.4.0.0
- Verified compatibility with Unity Ads SDK 3.4.0.
- Updated the minimum required Google Mobile Ads SDK version to 18.3.0.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- Unity Ads SDK version 3.4.0.

#### Version 3.3.0.0
- Verified compatibility with Unity Ads SDK 3.3.0.

Built and tested with:
- Google Mobile Ads SDK version 18.2.0.
- Unity Ads SDK version 3.3.0.

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
