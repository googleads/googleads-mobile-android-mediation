# Chartboost Adapter for Google Mobile Ads SDK for Android

This is an adapter to be used in conjunction with the Google Mobile Ads
SDK in Google Play services.

## Requirements
- Android SDK 4.0 (API level 14) or later
- Google Play services 8.3 or later
- Chartboost SDK

## Instructions
- Add the compile dependency with the latest version of the Chartboost adapter
  in the **build.gradle** file
  <pre><code>dependencies {
    compile 'com.google.ads.mediation:chartboost:6.6.3.0'
  }</code></pre>
- Add the Chartboost SDK into your Android project. The
  [quick start guide](https://answers.chartboost.com/hc/en-us/articles/201219545-Android-Integration#quickstart)
  contains detailed instructions on how to import the Chartboost SDK.
- Enable the ad network in the AdMob dashboard. See the
  [mediation set up guide](https://support.google.com/admob/answer/3124703?hl=en&ref_topic=3063091)
  for details.

## Additional Code Required
- Add the following activity to your manifest:
  <pre><code>&lt;activity
    android:name="com.chartboost.sdk.CBImpressionActivity"
    android:configChanges="keyboardHidden|orientation|screenSize"
    android:excludeFromRecents="true"
    android:hardwareAccelerated="true"
    android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen" /&gt;</code></pre>

- Add the following attribute to the activity in the **AndroidManifest.xml**
  file.  
  Note: This needs to be added to any activity that will be showing Chartboost
        ads.

    `android:configChanges="keyboardHidden|orientation|screenSize"`
- Add the following code to your activity's lifecycle events.
  Note: This needs to be added to any Activity that will be showing Chartboost
        ads.

      @Override
      public void onStart() {
          super.onStart();
          Chartboost.onStart(this);
      }

      @Override
      public void onResume() {
          super.onResume();
          Chartboost.onResume(this);
      }

      @Override
      public void onPause() {
          super.onPause();
          Chartboost.onPause(this);
      }

      @Override
      public void onStop() {
          super.onStop();
          Chartboost.onStop(this);
      }

      @Override
      public void onDestroy() {
          super.onDestroy();
          Chartboost.onDestroy(this);
      }

      @Override
      public void onBackPressed() {
          // If an interstitial is on screen, close it.
          if (Chartboost.onBackPressed())
              return;
          else
              super.onBackPressed();
      }

## Using ChartboostExtrasBundleBuilder
- The `ChartboostExtrasBundleBuilder` class can be used to create a `Bundle`
  with optional information that can be passed to the adapter using `AdRequest`.
  Please find the example below showing how to use the bundle builder class.

    - Chartboost provides an ability to set the framework the app uses (for
      example Unity). To set the framework, use the
      `ChartboostExtrasBundleBuilder` class'
      `setFramework(CBFramework, versionString)` method.

    Example:
    <pre><code>Bundle bundle = new ChartboostAdapter.ChartboostExtrasBundleBuilder()
            .setFramework(CBFramework.CBFrameworkUnity, unityVersion)
            .build();
    AdRequest adRequest = new AdRequest.Builder()
            .addNetworkExtrasBundle(ChartboostAdapter.class, bundle)
            .build();</code></pre>

## Note:
- Earlier versions of the adapters can be found on
  [Bintray](https://bintray.com/google/mobile-ads-adapters-android/com.google.ads.mediation.chartboost/).
- If you prefer using a jar file, you could extract the classes.jar file from
  the aar using a standard zip extract tool.

See the [quick start guide](https://firebase.google.com/docs/admob/android/quick-start)
for the latest documentation and code samples for the Google Mobile Ads SDK.
