# lib-proguard-rules.pro for publisher-side obfuscate

# region ZucksAdapter
-dontwarn com.google.android.gms.ads.mediation.ZucksAdapter

-keep class com.google.android.gms.ads.mediation.ZucksAdapter { *; }
-keep interface com.google.android.gms.ads.mediation.ZucksAdapter { *; }
-keep enum com.google.android.gms.ads.mediation.ZucksAdapter { *; }

-keep class com.google.android.gms.ads.mediation.ZucksAdapter$**.* { *; }
-keep interface com.google.android.gms.ads.mediation.ZucksAdapter$**.* { *; }
-keep enum com.google.android.gms.ads.mediation.ZucksAdapter$**.* { *; }
# endregion

# region ZucksMediationAdapter
-dontwarn com.google.ads.mediation.zucks.ZucksMediationAdapter

-keep class com.google.ads.mediation.zucks.ZucksMediationAdapter { *; }
-keep interface com.google.ads.mediation.zucks.ZucksMediationAdapter { *; }
-keep enum com.google.ads.mediation.zucks.ZucksMediationAdapter { *; }

-keep class com.google.ads.mediation.zucks.ZucksMediationAdapter$**.* { *; }
-keep interface com.google.ads.mediation.zucks.ZucksMediationAdapter$**.* { *; }
-keep enum com.google.ads.mediation.zucks.ZucksMediationAdapter$**.* { *; }
# endregion
