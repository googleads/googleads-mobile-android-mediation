# Chartboost Adapter for Google Mobile Ads SDK for Android Changelog

## 6.6.1.0
- Verified compatibility with Chartboost SDK 6.6.1.

## 6.6.0.0
- Changed the version naming system to
  [Chartboost SDK version].[adapter patch version].
- The adapters can now be added as a compile dependency by adding the following
  to the build.gradle file's dependencies tag:  
  `compile 'com.google.ads.mediation:chartboost:6.6.0.0'`
- Moved to distributing the adapter as an aar instead of a jar file
  (see README for additional instructions).

## 1.1.0
- Removed Chartboost Ad Location from Chartboost extras. Ad Location is now
specified in the AdMob console when configuring Chartboost for mediation.

## 1.0.0
- Initial release. Supports reward-based video ads and interstitial ads.
