## Tapjoy Android Mediation Adapter Changelog

#### Version 12.9.1.0
- Verified compatibility with Tapjoy SDK version 12.9.1.
- Updated the minimum required Google Mobile Ads SDK version to 20.6.0.

Built and tested with:
- Google Mobile Ads SDK version 20.6.0.
- Tapjoy SDK version 12.9.1.

#### Version 12.9.0.0
- Verified compatibility with Tapjoy SDK version 12.9.0.
- Updated the minimum required Google Mobile Ads SDK version to 20.5.0.

Built and tested with:
- Google Mobile Ads SDK version 20.5.0.
- Tapjoy SDK version 12.9.0.

#### Version 12.8.1.1
- Verified compatibility with Tapjoy SDK version 12.8.1.
- Updated the minimum required Google Mobile Ads SDK version to 20.4.0.
- Fixed an issue where a crash could occur if Tapjoy's TJError.message field is null.

Built and tested with:
- Google Mobile Ads SDK version 20.4.0.
- Tapjoy SDK version 12.8.1.

#### Version 12.8.1.0
- Verified compatibility with Tapjoy SDK version 12.8.1.
- Fixed an issue where a `NullPointerException` is being logged when an error is logged by the adapter.
- Updated the minimum required Google Mobile Ads SDK version to 20.2.0.

Built and tested with:
- Google Mobile Ads SDK version 20.2.0.
- Tapjoy SDK version 12.8.1.

#### Version 12.8.0.1
- Updated the minimum required Google Mobile Ads SDK version to 20.0.0.

Built and tested with:
- Google Mobile Ads SDK version 20.0.0.
- Tapjoy SDK version 12.8.0.

#### Version 12.8.0.0
- Fixed incorrect error messages.
- Updated the minimum required Google Mobile Ads SDK version to 19.7.0.

Built and tested with:
- Google Mobile Ads SDK version 19.7.0.
- Tapjoy SDK version 12.8.0.

#### Version 12.7.1.0
- Verified compatibility with Tapjoy SDK version 12.7.1.
- Updated the minimum required Google Mobile Ads SDK version to 19.5.0.

Built and tested with:
- Google Mobile Ads SDK version 19.5.0.
- Tapjoy SDK version 12.7.1.

#### Version 12.7.0.0
- Fixed an issue where the adapter returns an incorrect version string.
- Updated the minimum required Google Mobile Ads SDK version to 19.4.0.

Built and tested with:
- Google Mobile Ads SDK version 19.4.0.
- Tapjoy SDK version 12.7.0.

#### Version 12.6.1.0
- Verified compatibility with Tapjoy SDK version 12.6.1.
- Loading multiple interstitial ads with the same Tapjoy placement name at once now results in a load error for the second request. This behavior now matches existing behavior for rewarded ads.

Built and tested with:
- Google Mobile Ads SDK version 19.2.0.
- Tapjoy SDK version 12.6.1.

#### Version 12.6.0.0
- Verified compatibility with Tapjoy SDK version 12.6.0.
- Updated the minimum required Google Mobile Ads SDK version to 19.2.0.

Built and tested with:
- Google Mobile Ads SDK version 19.2.0.
- Tapjoy SDK version 12.6.0.

#### Version 12.4.2.1
- Added descriptive error codes and reasons for adapter load/show failures.
- Updated the minimum required Google Mobile Ads SDK version to 19.0.1.

Built and tested with:
- Google Mobile Ads SDK version 19.0.1.
- Tapjoy SDK version 12.4.2.

#### Version 12.4.2.0
- Verified compatibility with Tapjoy SDK version 12.4.2.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- Tapjoy SDK version 12.4.2.

#### Version 12.4.1.0
- Verified compatibility with Tapjoy SDK version 12.4.1.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- Tapjoy SDK version 12.4.1.

#### Version 12.4.0.0
- Verified compatibility with Tapjoy SDK version 12.4.0.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- Tapjoy SDK version 12.4.0.

#### Version 12.3.4.0
- Verified compatibility with Tapjoy SDK version 12.3.4.
- Updated the minimum required Google Mobile Ads SDK version to 18.3.0.

Built and tested with:
- Google Mobile Ads SDK version 18.3.0.
- Tapjoy SDK version 12.3.4.

#### Version 12.3.3.0
- Verified compatibility with Tapjoy SDK version 12.3.3.

#### Version 12.3.2.0
- Verified compatibility with Tapjoy SDK version 12.3.2.
- Updated the minimum required Google Mobile Ads SDK version to 18.2.0.

#### Version 12.3.1.1
- Tapjoy Adapter now only requests a single ad for every placement.

#### Version 12.3.1.0
- Updated the adapter to invoke 'onClick()' ad event.
- Verified compatibility with tapjoy SDK version 12.3.1.
- Updated the minimum required Google Mobile Ads SDK version to 17.2.1.

#### Version 12.2.1.1
- Added bidding capability to the adapter for interstitial and
  rewarded ads.

#### Version 12.2.1.0
- Verified compatibility with tapjoy SDK version 12.2.1.

#### Version 12.2.0.1
- Updated adapter to support new open-beta Rewarded API.
- Updated the minimum required Google Mobile Ads SDK version to 17.2.0.

#### Version 12.2.0.0
- Verified compatibility with tapjoy SDK version 12.2.0.

#### Version 12.1.0.0
- Verified compatibility with tapjoy SDK versiobn 12.1.0.

#### Version 12.0.0.1
- Updated the adapter to invoke the `onRewardedVideoComplete()` ad event.

#### Version 12.0.0.0
- Verified compatibility with tapjoy SDK versiobn 12.0.0.

#### Version 11.12.2.0
- Verified compatibility with tapjoy SDK versiobn 11.12.2.

#### Version 11.12.1.0
- Verified compatibility with tapjoy SDK versiobn 11.12.1.

#### Version 11.12.0.0
- Verified compatibility with tapjoy SDK versiobn 11.12.0.

#### Version 11.11.1.0
- Verified compatibility with Tapjoy SDK version 11.11.1.

#### Version 11.11.0.0
- Updated the adapter to make it compatible with Tapjoy SDK version 11.11.0.

#### Version 11.10.2.0
- Updated the default reward amount to 1.
- Verified compatibility with Tapjoy SDK version 11.10.2.

#### Version 11.10.1.0
- Verified compatibility with Tapjoy SDK version 11.10.1.

#### Version 11.10.0.0
- Verified compatibility with Tapjoy SDK version 11.10.0.

#### Version 11.9.1.0
- Initial release. Supports reward-based video ads and interstitial ads.
