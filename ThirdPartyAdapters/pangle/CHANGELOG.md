## Pangle Android Mediation Adapter Changelog

#### Version 8.1.0.3.0
- Verified compatibility with Pangle SDK version 8.1.0.3.

Built and tested with:
- Google Mobile Ads SDK version 25.3.0
- Google Mobile Ads Next-Gen SDK version 1.0.1
- Pangle SDK version 8.1.0.3

#### Version 8.0.0.5.0
- Verified compatibility with Pangle SDK version 80.0.5.

Built and tested with:
- Google Mobile Ads SDK version 251.0.
- Google Mobile Ads Next-Gen SDK version 10.1.
- Pangle SDK version 80.0.5.

#### Version 8.0.0.4.0
- Verified compatibility with Pangle SDK version 80.0.4.

Built and tested with:
- Google Mobile Ads SDK version 251.0.
- Pangle SDK version 80.0.4.

#### Version 7.9.1.3.0
- Verified compatibility with Pangle SDK version 79.1.3.

Built and tested with:
- Google Mobile Ads SDK version 251.0.
- Pangle SDK version 79.1.3.

#### Version 7.9.1.2.0
- Added property to build the adapter with GMA Next-Gen SDK dependency.
- Verified compatibility with Pangle SDK version 79.1.2.

Built and tested with:
- Google Mobile Ads SDK version 251.0.
- Google Mobile Ads Next-Gen SDK version 025.0-beta01.
- Pangle SDK version 79.1.2.

#### Version 7.9.1.1.0
- Verified compatibility with Pangle SDK version 79.1.1.

Built and tested with:
- Google Mobile Ads SDK version 250.0.
- Pangle SDK version 79.1.1.

#### Version 7.9.1.0.0
- Verified compatibility with Pangle SDK version 79.1.0.

Built and tested with:
- Google Mobile Ads SDK version 250.0.
- Pangle SDK version 79.1.0.

#### Version 7.9.0.9.0
- Removed calls to `PAGConfig.setGDPRConsent`,`PAGConfig.getGDPRConsent`,
  `PAGConfig.Builder.setGDPRConsent` since those APIs have been removed from
  Pangle SDK.
- Updated collectSignals() to use
  `PAGSdk.getBiddingToken(Context context, PAGBiddingRequest biddingRequest, PAGBidCallback bidCallback)`
- Verified compatibility with Pangle SDK version 79.0.9.

Built and tested with:
- Google Mobile Ads SDK version 249.0.
- Pangle SDK version 79.0.9.

#### Version 7.8.5.9.0
- Verified compatibility with Pangle SDK version 78.5.9.

Built and tested with:
- Google Mobile Ads SDK version 249.0.
- Pangle SDK version 78.5.9.

#### Version 7.8.5.8.0
- Verified compatibility with Pangle SDK version 78.5.8.

Built and tested with:
- Google Mobile Ads SDK version 249.0.
- Pangle SDK version 78.5.8.

#### Version 7.8.5.2.0
- Verified compatibility with Pangle SDK version 78.5.2.

Built and tested with:
- Google Mobile Ads SDK version 249.0.
- Pangle SDK version 78.5.2.

#### Version 7.8.0.8.0
- Verified compatibility with Pangle SDK version 78.0.8.

Built and tested with:
- Google Mobile Ads SDK version 248.0.
- Pangle SDK version 78.0.8.

#### Version 7.8.0.7.0
- Verified compatibility with Pangle SDK version 78.0.7.

Built and tested with:
- Google Mobile Ads SDK version 248.0.
- Pangle SDK version 78.0.7.

#### Version 7.7.0.2.0
- Verified compatibility with Pangle SDK version 77.0.2.

Built and tested with:
- Google Mobile Ads SDK version 247.0.
- Pangle SDK version 77.0.2.

#### Version 7.6.0.5.0
- Verified compatibility with Pangle SDK version 76.0.5.

Built and tested with:
- Google Mobile Ads SDK version 247.0.
- Pangle SDK version 76.0.5.

#### Version 7.6.0.4.1
- Removed class-level references to Context. Can help reduce memory leak issues.

Built and tested with:
- Google Mobile Ads SDK version 246.0.
- Pangle SDK version 76.0.4.

#### Version 7.6.0.4.0
- Verified compatibility with Pangle SDK version 76.0.4.

Built and tested with:
- Google Mobile Ads SDK version 246.0.
- Pangle SDK version 76.0.4.

#### Version 7.6.0.3.0
- Verified compatibility with Pangle SDK version 76.0.3.

Built and tested with:
- Google Mobile Ads SDK version 246.0.
- Pangle SDK version 76.0.3.

#### Version 7.6.0.2.0
- Verified compatibility with Pangle SDK version 76.0.2.

Built and tested with:
- Google Mobile Ads SDK version 246.0.
- Pangle SDK version 76.0.2.

#### Version 7.5.0.4.0
- Verified compatibility with Pangle SDK version 75.0.4.

Built and tested with:
- Google Mobile Ads SDK version 245.0.
- Pangle SDK version 75.0.4.

#### Version 7.5.0.3.0
- Verified compatibility with Pangle SDK version 75.0.3.

Built and tested with:
- Google Mobile Ads SDK version 245.0.
- Pangle SDK version 75.0.3.

#### Version 7.5.0.2.0
- Verified compatibility with Pangle SDK version 75.0.2.

Built and tested with:
- Google Mobile Ads SDK version 245.0.
- Pangle SDK version 75.0.2.

#### Version 7.3.0.5.0
- Verified compatibility with Pangle SDK version 73.0.5.

Built and tested with:
- Google Mobile Ads SDK version 245.0.
- Pangle SDK version 73.0.5.

#### Version 7.3.0.4.0
- Verified compatibility with Pangle SDK version 73.0.4.

Built and tested with:
- Google Mobile Ads SDK version 244.0.
- Pangle SDK version 73.0.4.

#### Version 7.3.0.3.0
- Verified compatibility with Pangle SDK version 73.0.3.

Built and tested with:
- Google Mobile Ads SDK version 244.0.
- Pangle SDK version 73.0.3.

#### Version 7.2.0.6.0
- Verified compatibility with Pangle SDK version 72.0.6.

Built and tested with:
- Google Mobile Ads SDK version 244.0.
- Pangle SDK version 72.0.6.

#### Version 7.2.0.4.0
- Verified compatibility with Pangle SDK version 72.0.4.

Built and tested with:
- Google Mobile Ads SDK version 243.0.
- Pangle SDK version 72.0.4.

#### Version 7.2.0.3.0
- Verified compatibility with Pangle SDK version 72.0.3.

Built and tested with:
- Google Mobile Ads SDK version 243.0.
- Pangle SDK version 72.0.3.

#### Version 7.1.0.8.0
- Updated the privacy APIs from the `PangleMediationAdapter` class.
- Removed `PangleMediationAdapter.setDoNotSell()`. Use `PangleMediationAdapter.setPAConsent()`
- Added support for adaptive banner ad sizes.
- Verified compatibility with Pangle SDK version 71.0.8.

Built and tested with:
- Google Mobile Ads SDK version 242.0.
- Pangle SDK version 71.0.8.

#### Version 6.5.0.8.0
- Verified compatibility with Pangle SDK version 65.0.8.

Built and tested with:
- Google Mobile Ads SDK version 241.0.
- Pangle SDK version 65.0.8.

#### Version 6.5.0.6.0
- Verified compatibility with Pangle SDK version 65.0.6.

Built and tested with:
- Google Mobile Ads SDK version 241.0.
- Pangle SDK version 65.0.6.

#### Version 6.5.0.5.0
- Verified compatibility with Pangle SDK version 65.0.5.

Built and tested with:
- Google Mobile Ads SDK version 240.0.
- Pangle SDK version 65.0.5.

#### Version 6.5.0.4.1
- Updated the minimum required Android API level to 23.
- Updated the minimum required Google Mobile Ads SDK version to 240.0.

Built and tested with:
- Google Mobile Ads SDK version 240.0.
- Pangle SDK version 65.0.4.

#### Version 6.5.0.4.0
- Verified compatibility with Pangle SDK version 65.0.4.

Built and tested with:
- Google Mobile Ads SDK version 236.0.
- Pangle SDK version 65.0.4.

#### Version 6.5.0.3.0
- Verified compatibility with Pangle SDK version 65.0.3.

Built and tested with:
- Google Mobile Ads SDK version 236.0.
- Pangle SDK version 65.0.3.

#### Version 6.4.0.6.0
- Verified compatibility with Pangle SDK version 64.0.6.

Built and tested with:
- Google Mobile Ads SDK version 236.0.
- Pangle SDK version 64.0.6.

#### Version 6.4.0.5.0
- Verified compatibility with Pangle SDK version 64.0.5.

Built and tested with:
- Google Mobile Ads SDK version 236.0.
- Pangle SDK version 64.0.5.

#### Version 6.4.0.4.0
- Verified compatibility with Pangle SDK version 64.0.4.

Built and tested with:
- Google Mobile Ads SDK version 236.0.
- Pangle SDK version 64.0.4.

#### Version 6.4.0.3.0
- Verified compatibility with Pangle SDK version 64.0.3.

Built and tested with:
- Google Mobile Ads SDK version 236.0.
- Pangle SDK version 64.0.3.

#### Version 6.4.0.2.0
- Verified compatibility with Pangle SDK version 64.0.2.

Built and tested with:
- Google Mobile Ads SDK version 236.0.
- Pangle SDK version 64.0.2.

#### Version 6.3.0.4.0
- Verified compatibility with Pangle SDK version 63.0.4.

Built and tested with:
- Google Mobile Ads SDK version 234.0.
- Pangle SDK version 63.0.4.

#### Version 6.3.0.2.0
- Verified compatibility with Pangle SDK version 63.0.2.

Built and tested with:
- Google Mobile Ads SDK version 234.0.
- Pangle SDK version 63.0.2.

#### Version 6.2.0.7.0
- Verified compatibility with Pangle SDK version 62.0.7.

Built and tested with:
- Google Mobile Ads SDK version 233.0.
- Pangle SDK version 62.0.7.

#### Version 6.2.0.6.0
- Verified compatibility with Pangle SDK version 62.0.6.

Built and tested with:
- Google Mobile Ads SDK version 233.0.
- Pangle SDK version 62.0.6.

#### Version 6.2.0.5.0
- Verified compatibility with Pangle SDK version 62.0.5.

Built and tested with:
- Google Mobile Ads SDK version 233.0.
- Pangle SDK version 62.0.5.

#### Version 6.2.0.4.0
- Verified compatibility with Pangle SDK version 62.0.4.

Built and tested with:
- Google Mobile Ads SDK version 233.0.
- Pangle SDK version 62.0.4.

#### Version 6.1.0.9.0
- Verified compatibility with Pangle SDK version 61.0.9.

Built and tested with:
- Google Mobile Ads SDK version 232.0.
- Pangle SDK version 61.0.9.

#### Version 6.1.0.7.0
- Verified compatibility with Pangle SDK version 61.0.7.

Built and tested with:
- Google Mobile Ads SDK version 232.0.
- Pangle SDK version 61.0.7.

#### Version 6.1.0.6.0
- Verified compatibility with Pangle SDK version 61.0.6.

Built and tested with:
- Google Mobile Ads SDK version 232.0.
- Pangle SDK version 61.0.6.

#### Version 6.0.0.8.0
- Verified compatibility with Pangle SDK version 60.0.8.

Built and tested with:
- Google Mobile Ads SDK version 231.0.
- Pangle SDK version 60.0.8.

#### Version 6.0.0.7.0
- Verified compatibility with Pangle SDK version 60.0.7.

Built and tested with:
- Google Mobile Ads SDK version 231.0.
- Pangle SDK version 60.0.7.

#### Version 6.0.0.5.0
- Verified compatibility with Pangle SDK version 60.0.5.

Built and tested with:
- Google Mobile Ads SDK version 231.0.
- Pangle SDK version 60.0.5.

#### Version 6.0.0.4.0
- Verified compatibility with Pangle SDK version 60.0.4.

Built and tested with:
- Google Mobile Ads SDK version 231.0.
- Pangle SDK version 60.0.4.

#### Version 6.0.0.3.0
- Verified compatibility with Pangle SDK version 60.0.3.

Built and tested with:
- Google Mobile Ads SDK version 231.0.
- Pangle SDK version 60.0.3.

#### Version 5.9.0.6.0
- Verified compatibility with Pangle SDK version 59.0.6.

Built and tested with:
- Google Mobile Ads SDK version 231.0.
- Pangle SDK version 59.0.6.

#### Version 5.9.0.5.0
- Verified compatibility with Pangle SDK version 59.0.5.

Built and tested with:
- Google Mobile Ads SDK version 231.0.
- Pangle SDK version 59.0.5.

#### Version 5.9.0.4.0
- Verified compatibility with Pangle SDK version 59.0.4.

Built and tested with:
- Google Mobile Ads SDK version 230.0.
- Pangle SDK version 59.0.4.

#### Version 5.9.0.2.0
- Verified compatibility with Pangle SDK version 59.0.2.

Built and tested with:
- Google Mobile Ads SDK version 230.0.
- Pangle SDK version 59.0.2.

#### Version 5.8.1.0.0
- Verified compatibility with Pangle SDK version 58.1.0.

Built and tested with:
- Google Mobile Ads SDK version 230.0.
- Pangle SDK version 58.1.0.

#### Version 5.8.0.9.0
- Verified compatibility with Pangle SDK version 58.0.9.
- Updated the minimum required Google Mobile Ads SDK version to 230.0.

Built and tested with:
- Google Mobile Ads SDK version 230.0.
- Pangle SDK version 58.0.9.

#### Version 5.8.0.7.0
- Verified compatibility with Pangle SDK version 58.0.7.

Built and tested with:
- Google Mobile Ads SDK version 226.0.
- Pangle SDK version 58.0.7.

#### Version 5.8.0.6.0
- Verified compatibility with Pangle SDK version 58.0.6.

Built and tested with:
- Google Mobile Ads SDK version 226.0.
- Pangle SDK version 58.0.6.

#### Version 5.7.0.3.0
- Verified compatibility with Pangle SDK version 57.0.3.

Built and tested with:
- Google Mobile Ads SDK version 226.0.
- Pangle SDK version 57.0.3.

#### Version 5.7.0.2.0
- Verified compatibility with Pangle SDK version 57.0.2.

Built and tested with:
- Google Mobile Ads SDK version 226.0.
- Pangle SDK version 57.0.2.

#### Version 5.7.0.1.0
- Verified compatibility with Pangle SDK version 57.0.1.

Built and tested with:
- Google Mobile Ads SDK version 226.0.
- Pangle SDK version 57.0.1.

#### Version 5.6.0.3.0
- Verified compatibility with Pangle SDK version 56.0.3.
- Updated the minimum required Google Mobile Ads SDK version to 226.0.

Built and tested with:
- Google Mobile Ads SDK version 226.0.
- Pangle SDK version 56.0.3.

#### Version 5.5.0.9.0
- Updated the minimum required Google Mobile Ads SDK version to 225.0.
- Verified compatibility with Pangle SDK version 55.0.9.

Built and tested with:
- Google Mobile Ads SDK version 225.0.
- Pangle SDK version 55.0.9.

#### Version 5.5.0.8.0
- Verified compatibility with Pangle SDK version 55.0.8.

Built and tested with:
- Google Mobile Ads SDK version 223.0.
- Pangle SDK version 55.0.8.

#### Version 5.5.0.7.0
- Verified compatibility with Pangle SDK version 55.0.7.

Built and tested with:
- Google Mobile Ads SDK version 223.0.
- Pangle SDK version 55.0.7.

#### Version 5.5.0.6.0
- Bidding app open ad format isn't supported in this version as this adapter version was reverted to depend on Google Mobile Ads SDK version 223.0.
- Verified compatibility with Pangle SDK version 55.0.6.

Built and tested with:
- Google Mobile Ads SDK version 223.0.
- Pangle SDK version 55.0.6.

#### Version 5.5.0.4.0
- Added bidding support for app open ad format.
- Added watermark support for bidding ads.
- Updated the minimum required Google Mobile Ads SDK version to 224.0.
- Verified compatibility with Pangle SDK version 55.0.4.

Built and tested with:
- Google Mobile Ads SDK version 224.0.
- Pangle SDK version 55.0.4.

#### Version 5.4.1.1.0
- Fixed an issue where the adapter fails to initialize.
- Verified compatibility with Pangle SDK version 54.1.1.

Built and tested with:
- Google Mobile Ads SDK version 223.0.
- Pangle SDK version 54.1.1.

#### Version 5.4.0.9.0
- Verified compatibility with Pangle SDK version 54.0.9.
- Updated the minimum required Google Mobile Ads SDK version to 223.0.

Built and tested with:
- Google Mobile Ads SDK version 223.0.
- Pangle SDK version 54.0.9.

#### Version 5.4.0.8.0
- Verified compatibility with Pangle SDK version 54.0.8.

Built and tested with:
- Google Mobile Ads SDK version 222.0.
- Pangle SDK version 54.0.8.

#### Version 5.3.0.6.0
- Verified compatibility with Pangle SDK version 53.0.6.

Built and tested with:
- Google Mobile Ads SDK version 222.0.
- Pangle SDK version 53.0.6.

#### Version 5.3.0.5.0
- Verified compatibility with Pangle SDK version 53.0.5.

Built and tested with:
- Google Mobile Ads SDK version 222.0.
- Pangle SDK version 53.0.5.

#### Version 5.3.0.4.0
- Verified compatibility with Pangle SDK version 53.0.4.

Built and tested with:
- Google Mobile Ads SDK version 222.0.
- Pangle SDK version 53.0.4.

#### Version 5.2.0.7.0
- Verified compatibility with Pangle SDK version 52.0.7.

Built and tested with:
- Google Mobile Ads SDK version 221.0.
- Pangle SDK version 52.0.7.

#### Version 5.2.0.6.0
- Verified compatibility with Pangle SDK version 52.0.6.

Built and tested with:
- Google Mobile Ads SDK version 221.0.
- Pangle SDK version 52.0.6.

#### Version 5.2.0.5.0
- Verified compatibility with Pangle SDK version 52.0.5.

Built and tested with:
- Google Mobile Ads SDK version 221.0.
- Pangle SDK version 52.0.5.

#### Version 5.2.0.3.0
- Added waterfall support for app open, banner (includes MREC), interstitial,
  rewarded and native ad formats.
- Verified compatibility with Pangle SDK version 52.0.3.

Built and tested with:
- Google Mobile Ads SDK version 220.0.
- Pangle SDK version 52.0.3.

#### Version 5.1.0.9.0
- Verified compatibility with Pangle SDK version 51.0.9.

Built and tested with:
- Google Mobile Ads SDK version 220.0.
- Pangle SDK version 51.0.9.

#### Version 5.1.0.8.0
- Verified compatibility with Pangle SDK version 51.0.8.

Built and tested with:
- Google Mobile Ads SDK version 220.0.
- Pangle SDK version 51.0.8.

#### Version 5.1.0.6.0
- Updated adapter to use new `VersionInfo` class.
- Updated the minimum required Google Mobile Ads SDK version to 220.0.

Built and tested with:
- Google Mobile Ads SDK version 220.0.
- Pangle SDK version 51.0.6.

#### Version 5.0.1.1.0
- Verified compatibility with Pangle SDK version 50.1.1.

Built and tested with:
- Google Mobile Ads SDK version 215.0.
- Pangle SDK version 50.1.1.

#### Version 5.0.1.0.0
- Verified compatibility with Pangle SDK version 50.1.0.

Built and tested with:
- Google Mobile Ads SDK version 215.0.
- Pangle SDK version 50.1.0.

#### Version 5.0.0.9.0
- Verified compatibility with Pangle SDK version 50.0.9.

Built and tested with:
- Google Mobile Ads SDK version 215.0.
- Pangle SDK version 50.0.9.

#### Version 5.0.0.8.0
- Verified compatibility with Pangle SDK version 50.0.8.

Built and tested with:
- Google Mobile Ads SDK version 215.0.
- Pangle SDK version 50.0.8.

#### Version 5.0.0.7.0
- Verified compatibility with Pangle SDK version 50.0.7.

Built and tested with:
- Google Mobile Ads SDK version 215.0.
- Pangle SDK version 50.0.7.

#### Version 5.0.0.6.0
- Verified compatibility with Pangle SDK version 50.0.6.

Built and tested with:
- Google Mobile Ads SDK version 215.0.
- Pangle SDK version 50.0.6.

#### Version 4.9.0.9.0
- Verified compatibility with Pangle SDK version 49.0.9.
- Updated the minimum required Google Mobile Ads SDK version to 215.0.

Built and tested with:
- Google Mobile Ads SDK version 215.0.
- Pangle SDK version 49.0.9.

#### Version 4.9.0.8.0
- Verified compatibility with Pangle SDK version 49.0.8.

Built and tested with:
- Google Mobile Ads SDK version 214.0.
- Pangle SDK version 49.0.8.

#### Version 4.9.0.7.0
- Verified compatibility with Pangle SDK version 49.0.7.

Built and tested with:
- Google Mobile Ads SDK version 214.0.
- Pangle SDK version 49.0.7.

#### Version 4.9.0.6.0
- Verified compatibility with Pangle SDK version 49.0.6.

Built and tested with:
- Google Mobile Ads SDK version 214.0.
- Pangle SDK version 49.0.6.

#### Version 4.8.1.0.0
- Verified compatibility with Pangle SDK version 48.1.0.
- Updated the minimum required Google Mobile Ads SDK version to 214.0.

Built and tested with:
- Google Mobile Ads SDK version 214.0.
- Pangle SDK version 48.1.0.

#### Version 4.8.0.9.0
- Verified compatibility with Pangle SDK version 48.0.9.

Built and tested with:
- Google Mobile Ads SDK version 213.0.
- Pangle SDK version 48.0.9.

#### Version 4.8.0.8.0
- Verified compatibility with Pangle SDK version 48.0.8.

Built and tested with:
- Google Mobile Ads SDK version 213.0.
- Pangle SDK version 48.0.8.

#### Version 4.8.0.7.0
- Verified compatibility with Pangle SDK version 48.0.7.

Built and tested with:
- Google Mobile Ads SDK version 213.0.
- Pangle SDK version 48.0.7.

#### Version 4.8.0.6.0
- Verified compatibility with Pangle SDK version 48.0.6.

Built and tested with:
- Google Mobile Ads SDK version 213.0.
- Pangle SDK version 48.0.6.

#### Version 4.7.0.7.0
- Verified compatibility with Pangle SDK version 47.0.7.
- Updated the minimum required Google Mobile Ads SDK version to 213.0.

Built and tested with:
- Google Mobile Ads SDK version 213.0.
- Pangle SDK version 47.0.7.

#### Version 4.7.0.6.0
- Verified compatibility with Pangle SDK version 47.0.6.

Built and tested with:
- Google Mobile Ads SDK version 212.0.
- Pangle SDK version 47.0.6.

#### Version 4.7.0.5.0
- Verified compatibility with Pangle SDK version 47.0.5.

Built and tested with:
- Google Mobile Ads SDK version 212.0.
- Pangle SDK version 47.0.5.

#### Version 4.7.0.3.0
- Verified compatibility with Pangle SDK version 47.0.3.
- The adapter now attempts to initialize the Pangle SDK before loading ads.

Built and tested with:
- Google Mobile Ads SDK version 212.0.
- Pangle SDK version 47.0.3.

#### Version 4.6.0.9.0
- Verified compatibility with Pangle SDK version 46.0.9.
- Updated the minimum required Google Mobile Ads SDK version to 212.0.

Built and tested with:
- Google Mobile Ads SDK version 212.0.
- Pangle SDK version 46.0.9.

#### Version 4.5.0.6.1
- Added bidding support for native ad format.
- Updated the minimum required Google Mobile Ads SDK version to 211.0.

Built and tested with:
- Google Mobile Ads SDK version 211.0.
- Pangle SDK version 45.0.6.

#### Version 4.5.0.6.0
- Verified compatibility with Pangle SDK version 45.0.6.

Built and tested with:
- Google Mobile Ads SDK version 210.0.
- Pangle SDK version 45.0.6.

#### Version 4.5.0.5.0
- Verified compatibility with Pangle SDK version 45.0.5.

Built and tested with:
- Google Mobile Ads SDK version 210.0.
- Pangle SDK version 45.0.5.

#### Version 4.5.0.4.0
- Verified compatibility with Pangle SDK version 45.0.4.

Built and tested with:
- Google Mobile Ads SDK version 210.0.
- Pangle SDK version 45.0.4.

#### Version 4.5.0.3.0
- Verified compatibility with Pangle SDK version 45.0.3.
- Updated `compileSdkVersion` and `targetSdkVersion` to API 31.
- Updated the minimum required Google Mobile Ads SDK version to 210.0.
- Updated the minimum required Android API level to 19.

Built and tested with:
- Google Mobile Ads SDK version 210.0.
- Pangle SDK version 45.0.3.

#### Version 4.3.0.9.0
- Verified compatibility with Pangle SDK version 43.0.9.

Built and tested with:
- Google Mobile Ads SDK version 206.0.
- Pangle SDK version 43.0.9.

#### Version 4.3.0.8.0
- Verified compatibility with Pangle SDK version 43.0.8.

Built and tested with:
- Google Mobile Ads SDK version 206.0.
- Pangle SDK version 43.0.8.

#### Version 4.3.0.7.0
- Verified compatibility with Pangle SDK version 43.0.7.

Built and tested with:
- Google Mobile Ads SDK version 206.0.
- Pangle SDK version 43.0.7.

#### Version 4.3.0.6.0
- Verified compatibility with Pangle SDK version 43.0.6.

Built and tested with:
- Google Mobile Ads SDK version 206.0.
- Pangle SDK version 43.0.6.

#### Version 4.3.0.4.0
- Verified compatibility with Pangle SDK version 43.0.4.

Built and tested with:
- Google Mobile Ads SDK version 206.0.
- Pangle SDK version 43.0.4.

#### Version 4.2.5.3.0
- Initial release!
- Added bidding support for banner (includes MREC), interstitial and rewarded ad formats.

Built and tested with:
- Google Mobile Ads SDK version 206.0.
- Pangle SDK version 42.5.3.
