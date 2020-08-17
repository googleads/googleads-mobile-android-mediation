## Vungle Android Mediation Adapter Changelog

#### Version 6.7.1.0
- Verified compatibility with Vungle SDK 6.7.1.
- Various Bug Fixes
- Adapter has been tested only for up to API version 29.
- Vungle 6.7.1 SDK has support for Android 11, and has resolved some issues in order to be used with API version 30

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
