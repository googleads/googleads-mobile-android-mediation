# MoPub Adapter for Google Mobile Ads SDK for Android

This is an adapter to be used in conjunction with the Google Mobile Ads SDK.

## Requirements
- Android SDK 4.1 (API level 16) and up.
- Google Mobile Ads SDK version 9.6.1 and up.
- MoPub SDK 4.9.0 and up.

## Instructions
- Add the compile dependency with the latest version of the MoPub adapter in the
  **build.gradle** file:
  <pre><code>dependencies {
    compile 'com.google.ads.mediation:mopub:4.12.0.0'
  }</code></pre>
- Add the MoPub SDK to your project. The
  [Getting Started](https://github.com/mopub/mopub-android-sdk/wiki/Getting-Started)
  guide contains detailed instructions on how to add MoPub SDK to your project.
- Enable the ad network in the AdMob dashboard. See the
  [mediation set up guide](https://support.google.com/admob/answer/3124703?hl=en&ref_topic=3063091)
  for details..

## Additional Code Required # TODO
- Optional, remove if no additional code is required

## Using MoPubAdapter.BundleBuilder
- The MoPubAdapter.BundleBuilder class can be used to create a bundle with
  optional information to be passed to the MoPub adapter in an AdRequest:

    <pre><code>Bundle bundle = new MoPubAdapter.BundleBuilder()
            .setPrivacyIconSizeDp(15)
            .build();
    AdRequest adRequest = new AdRequest.Builder()
            .addNetworkExtrasBundle(MoPubAdapter.class, bundle)
            .build();</code></pre>

## Notes
- MoPub and Google SDKs will be tracking impressions and clicks in their own way
  and so please expect discrepancies.
- MoPub has 5 assets including icon, title, description, main image and CTA
  text.
- Currently MoPub adapter is built to return install ads via Google mediation.
  If you are requesting content ads only, there will be no ads returned.

See the
[quick start guide](https://firebase.google.com/docs/admob/android/quick-start)
for the latest documentation and code samples for the Google Mobile Ads SDK.
