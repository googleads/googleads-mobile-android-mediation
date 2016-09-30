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
- Add the adapter jar into your Android project.
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

The latest documentation and code samples for the Google Mobile Ads SDK are
available at: https://developers.google.com/admob/android/quick-start
