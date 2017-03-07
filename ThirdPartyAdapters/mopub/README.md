===================================================
MoPub Adapter for Google Mobile Ads SDK for Android
===================================================

This is an adapter to be used in conjunction with the Google Mobile Ads SDK in
Google Play Services.

Requirements:
- Android SDK 2.3 (API level 9) and up.
- Google Mobile Ads SDK version 9.6.1 and up.
- MoPub SDK 4.9.0 and up.

Instructions:
- Add the adapter jar into your Android project.
- Add the MoPub SDK to your project (detailed instructions on how to add MoPub
SDK are availbale at:
https://github.com/mopub/mopub-android-sdk/wiki/Getting-Started).
- Enable the ad network in the AdMob dashboard (a guide on how to set up AdMob
mediation is available at:
https://support.google.com/admob/answer/3124703?hl=en&ref_topic=3063091).

Native Ads Notes:
- MoPub has 5 assets including icon, title, description, main image and CTA
text.
- Currently MoPub adapter is built to return install ads via Google mediation.
If you are requesting content ads only, there will be no ads returned.

Impression and Click Tracking:
- MoPub and Google SDKs will be tracking impressions and clicks in their own way
and so please expect discrepancies.
