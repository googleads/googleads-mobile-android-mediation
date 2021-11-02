Google Mobile Ads SDK - Android Mediation
=========================================
The Google Mobile Ads SDK is the latest generation in Google mobile advertising,
and features refined ad formats and streamlined APIs for access to mobile ad
networks and advertising solutions. The SDK enables mobile app developers to
maximize their monetization in native mobile apps.

This repository is broken into two sections:

## Mediation Example

A sample project demonstrating how an ad network can plug into AdMob Mediation.
The project contains a "Sample Ad Network" SDK library, as well as a sample
mediation adapter and custom event implementation for AdMob Mediation capable of
loading banners, interstitials, rewarded video, and native ads.

A test application is also included, and uses the Google Mobile Ads SDK to call
into the adapter and custom event to test their implementations. It can be used
during development to test new adapters and custom events, once ad units have
been set up.

## Mediation Adapters

Open source adapters for mediating via the Google Mobile Ads SDK. A list of
these adapters is available on our
[Mediation](https://developers.google.com/admob/android/mediation#choosing_your_mediation_networks)
page.

## Prebuilt adapters

For prebuilt versions of these adapters, see the
[Google Maven Repository](https://maven.google.com/web/index.html?#com.google.ads.mediation).

## Documentation

Check out our [developer site](https://developers.google.com/admob/android)
for documentation on using the SDK and our
[mediation developer guide](https://developers.google.com/admob/android/mediation-developer)
for information on how to create an adapter.
You can also join the developer community on
[our SDK forum](https://groups.google.com/forum/#!forum/google-admob-ads-sdk).

## Suggesting improvements

For feature requests, or to suggest other improvements, please use
[github's issue tracker](https://github.com/googleads/googleads-mobile-android-mediation/issues).

## License
[Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html)
