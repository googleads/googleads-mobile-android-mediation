## BidMachine Android Mediation Adapter Changelog

#### Version 3.7.1.1
- Maps `AgeRestrictedTreatment` to BidMachine's COPPA API.

Built and tested with:
- Google Mobile Ads SDK version 25.4.0.
- Google Mobile Ads Next-Gen SDK version 1.2.1.
- BidMachine SDK version 3.7.1.

#### Version 3.7.0.0
- Verified compatibility with BidMachine SDK version 3.7.0.

Built and tested with:
- Google Mobile Ads SDK version 25.1.0.
- Google Mobile Ads Next-Gen SDK version 1.1.0.
- BidMachine SDK version 3.7.0.

#### Version 3.6.1.0
- Added property to build the adapter with GMA Next-Gen SDK dependency.
- Verified compatibility with BidMachine SDK version 3.6.1.

Built and tested with:
- Google Mobile Ads SDK version 25.1.0.
- BidMachine SDK version 3.6.1.

#### Version 3.5.1.2
- Updated BidMachine Adapter to use AdPlacementConfig.

Built and tested with:
- Google Mobile Ads SDK version 24.9.0.
- BidMachine SDK version 3.5.1.

#### Version 3.5.1.1
- Added support for forwarding the `tagForUnderAgeOfConsent` Google Mobile Ads SDK
parameter to the BidMachine SDK.

Built and tested with:
- Google Mobile Ads SDK version 24.9.0.
- BidMachine SDK version 3.5.1.

#### Version 3.5.1.0
- Verified compatibility with BidMachine SDK version 3.5.1.

Built and tested with:
- Google Mobile Ads SDK version 24.9.0.
- BidMachine SDK version 3.5.1.

#### Version 3.5.0.0
- Verified compatibility with BidMachine SDK version 3.5.0.

Built and tested with:
- Google Mobile Ads SDK version 24.7.0.
- BidMachine SDK version 3.5.0.

#### Version 3.4.0.1
- Removed class-level references to `Context` objects to help with memory leak issues.
- Updated adapter to support banner ad requests that are close in size to its
supported formats, instead of requiring an exact size match.

Built and tested with:
- Google Mobile Ads SDK version 24.7.0.
- BidMachine SDK version 3.4.0

#### Version 3.4.0.0
- Added waterfall support for banner, interstitial, rewarded and native ad formats.
- Adapter now forwards `onAdOpened()` with `onAdImpression() ` for full-screen ads.
- Verified compatibility with BidMachine SDK version 3.4.0.

Built and tested with:
- Google Mobile Ads SDK version 24.6.0.
- BidMachine SDK version 3.4.0.

#### Version 3.3.0.0
- Initial release.

Built and tested with:
- Google Mobile Ads SDK version 24.5.0.
- BidMachine SDK version 3.3.0.