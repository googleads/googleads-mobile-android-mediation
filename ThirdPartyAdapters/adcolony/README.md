# AdColony Adapter for Google Mobile Ads SDK for Android

This is an adapter to be used in conjunction with the Google Mobile Ads
SDK in Google Play services.

## Requirements
- Android SDK 4.0 (API level 14) or later.
- Google Mobile Ads SDK to v9.8.0 or later.
- AdColony SDK v3.0.6 or later.

## Instructions
- Add the compile dependency with the latest version of the AdColony adapter in
  the *build.gradle* file:
  <pre><code>dependencies {
    compile 'com.google.ads.mediation:adcolony:3.1.1.0'
  }
  </code></pre>
- To add AdColony to your mediation layer, you will need to create an app on the
  AdColony [dashboard.](https://clients.adcolony.com/apps) For help with setting
  up an AdColony app, follow steps 1-3 in
  [Setting up AdColony Apps](http://support.adcolony.com/customer/portal/articles/761987-setting-up-apps-zones).
- Once you have created your AdColony app, you can add the AdColony network to
  your mediation layer. Note that you will need to enter your AdColony app and
  zone IDs into the AdMob dashboard when you add the AdColony network.
- Afterwards, follow the project setup instructions as explained
  [here](https://github.com/AdColony/AdColony-Android-SDK-3/wiki/Project-Setup).
- Next, edit the mediation for your desired ad unit on the AdMob dashboard. Add
  AdColony as an ad source and set your app/zone ids. Note: If you do not
  provide both an App ID and a Zone ID on the AdMob Dashboard, AdColony cannot
  render ads. See the [mediation set up guide](https://support.google.com/admob/answer/3124703?hl=en&ref_topic=3063091)
  for details.

## Optimizations
To ensure AdColony video ads are available as often as possible, you can
initialize the AdColony SDK early on in the application lifecycle and outside of
the Google Mobile Ads SDK. To do so, insert the following code to your main
Activity’s onCreate method:
<pre><code>AdColony.configure(this,                // activity context
            “YOUR_ADCOLONY_APP_ID”,
            “ZONE_ID_1”, “ZONE_ID_2”);  // list of all your zones set up on the AdColony Dashboard
</code></pre>

## Using AdColonyBundleBuilder
The AdColonyBundleBuilder class can be used to create a Bundle with optional
information that can be passed to the adapter using AdRequest. 

You can use the AdColonyBundleBuilder to pass along the zoneID to the ad
request. If you do not provide the zoneID using the bundle builder, AdColony
will use the first zone provided in the AdMob dashboard for the specified ad
unit.
<pre><code>AdColonyBundleBuilder.setZoneId(“YOUR_ZONE_ID”);
AdRequest adRequest = new AdRequest.Builder()
    .adNetworkExtrasBundle(AdColonyAdapter.class,AdColonyBundleBuilder.build())
    .build();
interstitialAd.loadAd(adRequest);
</code></pre>

### AdColonyBundleBuilder for Rewarded Interstitial Ads
For Rewarded Interstitial Ads, you can also use the AdColonyBundleBuilder to
show pop-ups before and after the rewarded ad shows. You can also provide a
“user ID” field, to provide further analytics to the AdColony Ad Server. See the
example below:
<pre><code>AdColonyBundleBuilder.setUserId("user_id");
AdColonyBundleBuilder.setZoneId(“YOUR_ZONE_ID”);
AdColonyBundleBuilder.setShowPrePopup(true);  // set to false for no popup before ad shows
AdColonyBundleBuilder.setShowPostPopup(true); // set to false for no popups after ad shows
AdRequest request = Builder
   .addNetworkExtrasBundle(AdColonyAdapter.class, AdColonyBundleBuilder.build())
   .build();
rewardedInterstitialAd.loadAd(request);
</code></pre>

## Notes
- AdColony is only supported on API 14 (ICS) and above, and only on devices with
  a memory class (app specific memory limit) of 32MB or greater.
- If you have any questions or issues with your integration, please email us at
  [support@adcolony.com](mailto:support@adcolony.com).

See the
[quick start guide](https://firebase.google.com/docs/admob/android/quick-start)
for the latest documentation and code samples for the Google Mobile Ads SDK.
