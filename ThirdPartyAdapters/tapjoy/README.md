# Tapjoy Adapter for Google Mobile Ads SDK for Android

This is an adapter to be used in conjunction with the Google Mobile Ads
SDK in Google Play services.

## Requirements
- Android SDK 2.2 or later
- Google Play services 8.3 or later
- Tapjoy SDK

## Instructions
- Add the compile dependency with the latest version of the Tapjoy adapter
  in the **build.gradle** file
  <pre><code>dependencies {
    compile 'com.google.ads.mediation:tapjoy:1.0.0'
  }</code></pre>
- Add the Tapjoy SDK into your Android project. The
  [quick start guide](http://dev.tapjoy.com/sdk-integration/android/getting-started-guide-publishers-android/)
  contains detailed instructions on how to import the Tapjoy SDK.
- Enable the Tapjoy ad network in the AdMob dashboard. See the
  [mediation set up guide](https://support.google.com/admob/answer/3124703?hl=en&ref_topic=3063091)
  for details.

## Additional Code Required
- Add the following activities to your manifest:
	<pre><code>&lt;activity
	  android:name="com.tapjoy.TJAdUnitActivity"
	  android:configChanges="orientation|keyboardHidden|screenSize"
	  android:hardwareAccelerated="true"
	  android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen" /&gt;
	&lt;activity
	  android:name="com.tapjoy.mraid.view.ActionHandler"
	  android:configChanges="orientation|keyboardHidden|screenSize" /&gt;
	&lt;activity
	  android:name="com.tapjoy.mraid.view.Browser"
	  android:configChanges="orientation|keyboardHidden|screenSize" /&gt;
	&lt;activity
	  android:name="com.tapjoy.TJContentActivity"
	  android:configChanges="orientation|keyboardHidden|screenSize"
	  android:theme="@android:style/Theme.Translucent.NoTitleBar"
	  android:hardwareAccelerated="true" /&gt;</code></pre>

- Make sure the following permissions are enabled in your **AndroidManifest.xml**
  file.  
  <pre><code>&lt;uses-permission android:name="android.permission.INTERNET"/&gt;</code></pre>

 ## Using TapjoyExtrasBundleBuilder
- The `TapjoyExtrasBundleBuilder` class can be used to create a `Bundle`
  with optional information that can be passed to the adapter using `AdRequest` or `loadAd`.
  Please find the example below showing how to use the bundle builder class.

    Example:
    <pre><code>Bundle bundle = new TapjoyAdapter.TapjoyExtrasBundleBuilder()
            .setDebug(true)
            .build();
  rewardedVideoAd.loadAd(ADMOB_VIDEO_ID, new AdRequest.Builder()
            .addNetworkExtrasBundle(TapjoyAdapter.class, extras)
            .build());</code></pre>

**Notes:** 
- Different Placements must be used to load multiple ad units simultaneously. 
  For example: when creating multiple instances of TapjoyAdapter,
  each instance will need to use a different adUnitID on init. If the same 
  adUnitID  is used, only one instance will be able to load/show, the second 
  instance will do nothing.
- The Tapjoy SDK does not pass specific reward values for rewarded
  video ads, the adapter defaults to a reward of type "" with value 0. Please
  override the reward value in the AdMob console.
  For more information on setting reward values for AdMob ad units, see the
  Rewarded Interstitial section of this article 
  [Help Center Article](https://support.google.com/admob/answer/3052638).

- If you prefer using a jar file, you could extract the classes.jar file from
  the aar using a standard zip extract tool.

See the [quick start guide](https://firebase.google.com/docs/admob/android/quick-start)
for the latest documentation and code samples for the Google Mobile Ads SDK.