========================================================
Chartboost Adapter for Google Mobile Ads SDK for Android
========================================================

This is an adapter to be used in conjunction with the Google Mobile Ads
SDK in Google Play services.

Requirements:
- Android SDK 3.2 or later
- Google Play services 8.3 or later
- Chartboost SDK

Instructions:
- Add the adapter jar into your Android project.
- Add the Chartboost SDK into your Android project (Detailed instructions on how
  to import Chartboost SDK are available at:
  https://answers.chartboost.com/hc/en-us/articles/201219545-Android-Integration
  #quickstart).
- Enable the Ad network in the Ad Network Mediation UI.

Additional Code Required:
- Add the following activity to your manifest:

`<activity
    android:name="com.chartboost.sdk.CBImpressionActivity"
    android:configChanges="keyboardHidden|orientation|screenSize"
    android:excludeFromRecents="true"
    android:hardwareAccelerated="true"
    android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen"/>`

- Add the following attribute to the activity in the 'AndroidManifest.xml' file.
  Note: This needs to be added to any activity that will be showing Chartboost
        ads.

        android:configChanges="keyboardHidden|orientation|screenSize"

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

Using ChartboostExtrasBundleBuilder:
- The ChartboostExtrasBundleBuilder class can be used to create a Bundle with
  optional information that can be passed to the adapter using AdRequest. Please
  find the example below showing how to use the bundle builder class.

    - Chartboost provides an ability to set the framework the app uses (for
      example Unity). To set the framework, use the
      ChartboostExtrasBundleBuilder class'
      setFramework(CBFramework, versionString) method.

Example:

Bundle bundle = new ChartboostAdapter.ChartboostExtrasBundleBuilder()
                    .setFramework(CBFramework.CBFrameworkUnity, unityVersion)
                    .build();
AdRequest adRequest = new AdRequest.Builder()
                    .addNetworkExtrasBundle(ChartboostAdapter.class, bundle)
                    .build();

The latest documentation and code samples for the Google Mobile Ads SDK are
available at:
https://developers.google.com/admob/android/quick-start
