# Unity Ads Adapter for Google Mobile Ads SDK for Android Changelog

## 2.0.5.0
- The adapters can now be added as a compile dependency by adding the following
  to the build.gradle file's dependencies tag:  
  `compile 'com.google.ads.mediation:unity:2.0.5.0'`
- Moved to distributing the adapter as an aar instead of a jar file
  (see README for additional instructions).

## 2.0.4.0
- Fixed a bug that caused rewarded video ads to fail to load when an
  interstitial ad was loaded first.

## 2.0.2.0
- Changed the version naming system to
  [Unity Ads SDK version].[adapter patch version].
- Updated the minimum required Unity Ads SDK to v2.0.2.
- Updated the minimum required Google Mobile Ads SDK to v9.0.0.
- Apps are no longer required to call UnityAds.changeActivity(this).

## 1.0.0
- Initial release. Supports reward-based video ads and interstitial ads.
