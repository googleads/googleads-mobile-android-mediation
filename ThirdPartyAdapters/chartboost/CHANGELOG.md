## Chartboost Android Mediation Adapter Changelog

#### Version 8.4.2.0
- Verified compatibility with Chartboost SDK 8.4.2.
- Updated 'compileSdkVersion' and 'targetSdkVersion' to API 31.
- Updated the minimum required Google Mobile Ads SDK version to 20.6.0.

Built and tested with:
- Google Mobile Ads SDK version 20.6.0.
- Chartboost SDK version 8.4.2.

#### Version 8.4.1.0
- Verified compatibility with Chartboost SDK 8.4.1.
- Updated the minimum required Google Mobile Ads SDK version to 20.6.0.

Built and tested with:
- Google Mobile Ads SDK version 20.6.0.
- Chartboost SDK version 8.4.1.

#### Version 8.3.1.0
- Verified compatibility with Chartboost SDK 8.3.1.
- Updated the adapter to use new `AdError` API.

Built and tested with:
- Google Mobile Ads SDK version 20.5.0.
- Chartboost SDK version 8.3.1.

#### Version 8.3.0.0
- Verified compatibility with Chartboost SDK 8.3.0.
- Updated the minimum required Google Mobile Ads SDK version to 20.5.0.

Built and tested with:
- Google Mobile Ads SDK version 20.5.0.
- Chartboost SDK version 8.3.0.

#### Version 8.2.1.0
- Verified compatibility with Chartboost SDK 8.2.1.
- Updated the minimum required Google Mobile Ads SDK version to 20.1.0.

Built and tested with:
- Google Mobile Ads SDK version 20.1.0.
- Chartboost SDK version 8.2.1.

#### Version 8.2.0.1
- Updated the minimum required Google Mobile Ads SDK version to 20.0.0.

Built and tested with:
- Google Mobile Ads SDK version 20.0.0.
- Chartboost SDK version 8.2.0.

#### Version 8.2.0.0
- Verified compatibility with Chartboost SDK 8.2.0.
- Updated the minimum required Google Mobile Ads SDK version to 19.5.0.

Built and tested with:
- Google Mobile Ads SDK version 19.5.0.
- Chartboost SDK version 8.2.0.

#### Version 8.1.0.0
- Verified compatibility with Chartboost SDK 8.1.0.

Built and tested with:
- Google Mobile Ads SDK version 19.2.0.
- Chartboost SDK version 8.1.0.

#### Version 8.0.3.2
- Added descriptive error codes and reasons for adapter load/show failures.
- Updated the minimum required Google Mobile Ads SDK version to 19.2.0.

Built and tested with:
- Google Mobile Ads SDK version 19.2.0.
- Chartboost SDK version 8.0.3.

#### Version 8.0.3.1
- Added support for Chartboost banner ads.

Built and tested with:
- Google Mobile Ads SDK version 19.1.0.
- Chartboost SDK version 8.0.3.

#### Version 8.0.3.0
- Verified compatibility with Chartboost SDK 8.0.3.
- Updated the minimum required Google Mobile Ads SDK version to 19.1.0.

Built and tested with:
- Google Mobile Ads SDK version 19.1.0.
- Chartboost SDK version 8.0.3.

#### Version 8.0.2.0
- Verified compatibility with Chartboost SDK 8.0.2.
- Updated the minimum required Google Mobile Ads SDK version to 19.0.1.

Built and tested with:
- Google Mobile Ads SDK version 19.0.1.
- Chartboost SDK version 8.0.2.

#### Version 8.0.1.0
- Verified compatibility with Chartboost SDK 8.0.1.
- Requires Chartboost SDK 8.0.1 or higher.
- Activity context is no longer required to load ads.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- Chartboost SDK version 8.0.1.

#### Version 7.5.0.1
- Fixed a bug where Chartboost adapter failed to invoke ad events.
- Updated the minimum required Google Mobile Ads SDK version to 18.3.0.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- Chartboost SDK version 7.5.0.

#### Version 7.5.0.0
- Verified compatibility with Chartboost SDK 7.5.0.
- Migrated the adapter to use AndroidX dependencies.
- Updated the minimum required Google Mobile Ads SDK version to 18.1.1.

#### Version 7.3.1.1
- Updated adapter to support new open-beta Rewarded API.
- Updated the minimum required Google Mobile Ads SDK version to 17.2.0.

#### Version 7.3.1.0
- Verified compatibility with Chartboost SDK 7.3.1.

#### Version 7.3.0.0
- Verified compatibility with Chartboost SDK 7.3.0.

#### Version 7.2.1.0
- Verified compatibility with Chartboost SDK 7.2.1.

#### Version 7.2.0.1
- Updated the adapter to invoke the `onRewardedVideoComplete()` ad event.

#### Version 7.2.0.0
- Verified compatibility with Chartboost SDK 7.2.0.

#### Version 7.1.0.0
- Verified compatibility with Chartboost SDK 7.1.0.

#### Version 7.0.1.0
- Verified compatibility with Chartboost SDK 7.0.1.
- Added support for new Chartboost error codes.
- Updated the Adapter project for Android Studio 3.0

#### Version 7.0.0.0
- Updated the adapter to make it compatible with Chartboost SDK 7.0.0.

#### Version 6.6.3.0
- Verified compatibility with Chartboost SDK 6.6.3.

#### Version 6.6.2.0
- Verified compatibility with Chartboost SDK 6.6.2.

#### Version 6.6.1.0
- Verified compatibility with Chartboost SDK 6.6.1.

#### Version 6.6.0.0
- Changed the version naming system to
  [Chartboost SDK version].[adapter patch version].
- The adapters can now be added as a compile dependency by adding the following
  to the build.gradle file's dependencies tag:
  `compile 'com.google.ads.mediation:chartboost:6.6.0.0'`
- Moved to distributing the adapter as an aar instead of a jar file
  (see README for additional instructions).

#### Version 1.1.0
- Removed Chartboost Ad Location from Chartboost extras. Ad Location is now
specified in the AdMob console when configuring Chartboost for mediation.

#### Version 1.0.0
- Initial release. Supports reward-based video ads and interstitial ads.
