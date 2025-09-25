## BidMachine Android Mediation Adapter Changelog

#### Next version
- Updated how the requested banner ad size is mapped to BidMachine's banner ad
  size. Now allows for requested ad sizes that are close to a size supported by
  BidMachine (instead of requiring the requested size to strictly match a
  supported size).

#### Version 3.4.0.0 (In progress)
- Added support for waterfall banner ads.
- Added support for waterfall interstitial ads.
- Added support for waterfall rewarded ads.
- Added support for waterfall native ads.
- Added watermark implementation.
- Added `MediationAdCallback.onAdOpened()` call when BidMachine's `onAdImpression()` callback is received for full-screen ads. This ensures that publishers recieve the correct callbacks when full-screen ad is displayed.

#### Version 3.3.0.0
- Initial release.

Built and tested with:
- Google Mobile Ads SDK version 24.5.0.
- BidMachine SDK version 3.3.0.