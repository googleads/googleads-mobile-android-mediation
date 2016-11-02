=======================================================
Unity Ads Adapter for Google Mobile Ads SDK for Android
=======================================================

This is an adapter to be used in conjunction with the Google Mobile Ads
SDK in Google Play services.

Requirements:
- Android SDK 3.2 or later
- Google Mobile Ads SDK to v9.0.0 or later
- Unity Ads SDK v2.0.2.

Instructions:
- Add the compile dependency with the latest version of the Unity Ads adapter in
  the build.gradle file:
  dependencies {
    compile 'com.google.ads.mediation:unity:2.0.5.0'
  }
- Import the Unity Ads library project into your Android project (Detailed
  instructions on how to import Unity Ads library are available at:
  http://unityads.unity3d.com/help/monetization/integration-guide-android).
- Enable the Ad network in the Ad Network Mediation UI.
- Unity Ads SDK does not provide a reward value when rewarded video is
  completed, so the adapter defaults to a reward of type "" with value 1. Please
  override the reward value in the AdMob console.

Note:
- The onAdLeftApplication event is unsupported for ads mediated from Unity Ads
  because the Unity Ads SDK does not provide an equivalent ad event that can be
  forwarded by the Google Mobile Ads SDK.
- Earlier versions of the adapters can be found at:
  https://bintray.com/google/mobile-ads-adapters-android/com.google.ads.mediation.unity/
- If you prefer using a jar file, you could extract the classes.jar file from
  the aar using a standard zip extract tool.

The latest documentation and code samples for the Google Mobile Ads SDK are
available at: https://firebase.google.com/docs/admob/android/quick-start
