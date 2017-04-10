# Unity Ads Adapter for Google Mobile Ads SDK for Android

This is an adapter to be used in conjunction with the Google Mobile Ads
SDK in Google Play services.

## Requirements
- Android SDK 4.0 (API level 14) or later.
- Google Mobile Ads SDK to v9.0.0 or later.
- Unity Ads SDK v2.0.7 or later.

## Instructions
- Add the compile dependency with the latest version of the Unity Ads adapter in
  the **build.gradle** file:
  <pre><code>dependencies {
    compile 'com.google.ads.mediation:unity:2.1.0.0'
  }</code></pre>
- Import the Unity Ads library project into your Android project. The
  [integration guide](http://unityads.unity3d.com/help/monetization/integration-guide-android)
  contains detailed instructions on how to import Unity Ads library.
- Enable the ad network in the AdMob dashboard. See the
  [mediation set up guide](https://support.google.com/admob/answer/3124703?hl=en&ref_topic=3063091)
  for details.
- Unity Ads SDK does not provide a reward value when rewarded video is
  completed, so the adapter defaults to a reward of type "" with value 1. Please
  override the reward value in the AdMob console.

## Notes
- Earlier versions of the adapters can be found on
  [Bintray](https://bintray.com/google/mobile-ads-adapters-android/com.google.ads.mediation.unity/).
- If you prefer using a jar file, you could extract the classes.jar file from
  the aar using a standard zip extract tool.

See the [quick start guide](https://firebase.google.com/docs/admob/android/quick-start)
for the latest documentation and code samples for the Google Mobile Ads SDK.
