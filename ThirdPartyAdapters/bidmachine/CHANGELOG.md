## BidMachine Android Mediation Adapter Changelog

#### Next Version
- Removed class-level references to `Context` objects to help with memory leak issues.
- Updated how the requested banner ad size is mapped to BidMachine's banner ad
  size. Now allows for requested ad sizes that are close to a size supported by
  BidMachine (instead of requiring the requested size to strictly match a
  supported size).

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