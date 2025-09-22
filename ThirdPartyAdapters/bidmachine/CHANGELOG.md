## BidMachine Android Mediation Adapter Changelog

#### Next Version
- Adds support for waterfall interstitial ads.
- Adds support for waterfall rewarded ads.

#### Version 3.4.0.0 (In progress)
- Added `MediationAdCallback.onAdOpened()` call when BidMachine's `onAdImpression()` callback is received for full-screen ads. This ensures that publishers recieve the correct callbacks when full-screen ad is displayed.

#### Version 3.3.0.0
- Initial release.

Built and tested with:
- Google Mobile Ads SDK version 24.5.0.
- BidMachine SDK version 3.3.0.