## BidMachine Android Mediation Adapter Changelog

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