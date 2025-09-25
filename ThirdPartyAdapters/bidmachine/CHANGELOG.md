## BidMachine Android Mediation Adapter Changelog

#### Version 3.4.0.0
- Added support for waterfall banner ads.
- Added support for waterfall interstitial ads.
- Added support for waterfall rewarded ads.
- Added support for waterfall native ads.
- Added watermark implementation.
- Added `MediationAdCallback.onAdOpened()` call when BidMachine's `onAdImpression()` callback is received for full-screen ads. This ensures that publishers recieve the correct callbacks when full-screen ad is displayed.
-- Verified compatibility with BidMachine SDK version 3.4.0.

Built and tested with:
- Google Mobile Ads SDK version 24.6.0.
- BidMachine SDK version 3.4.0.

#### Version 3.3.0.0
- Initial release.

Built and tested with:
- Google Mobile Ads SDK version 24.5.0.
- BidMachine SDK version 3.3.0.