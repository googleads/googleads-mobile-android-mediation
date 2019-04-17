package com.google.ads.mediation.dap;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ChangedPackages;
import android.content.pm.FeatureInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.SharedLibraryInfo;
import android.content.pm.VersionedPackage;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.duapps.ad.base.DuAdNetwork;
import com.duapps.ad.video.DuVideoAdSDK;

import org.json.JSONException;
import org.json.JSONStringer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class DuAdMediation {
    private static final String TAG = DuAdMediation.class.getSimpleName();

    /**
     * The following keys should be configured in the AdMob account Mediation settings.
     */
    public static final String KEY_DAP_PID = "placementId";
    public static final String KEY_APP_ID = "appId";
    public static final String KEY_APP_LICENSE = "app_license";

    private static boolean DEBUG = false;

    public static void setDebug(boolean debug) {
        DEBUG = debug;
    }

    public static void debugLog(String tag, String msg) {
        if (DEBUG) {
            String name = Thread.currentThread().getName();
            Log.d(TAG, "[" + tag + "]: " + "<" + name + "> " + msg);
        }
    }


    public static final String KEY_ALL_PLACEMENT_ID = "ALL_PID";
    public static final String KEY_ALL_VIDEO_PLACEMENT_ID = "ALL_V_PID";

    private static String sdkAppID = "";

    private static HashSet<Integer> initializedNonVideoPlacements = new HashSet<>();
    private static HashSet<Integer> initializedVideoPlacements = new HashSet<>();
    private static Handler handler;

    public static void runOnUIThread(Runnable runnable) {
        try {
            if (handler == null) {
                handler = new Handler(Looper.getMainLooper());
            }
            handler.post(runnable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeAllCallbacks() {
        if (handler != null) {
            try {
                handler.removeCallbacksAndMessages(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void configureSDKForNonVideo(Context context,
                                               Bundle mediationExtras,
                                               String appID,
                                               int placementID) {
        if (TextUtils.isEmpty(sdkAppID)) {
            sdkAppID = appID;
        } else if (!sdkAppID.equals(appID)){
            debugLog(TAG, "Already configured with App ID: '" + sdkAppID + "'. "
                    + "Ignoring attempt to configure using App ID: " + appID);
            return;
        }

        if (mediationExtras != null) {
            ArrayList<Integer> nativePlacements =
                    mediationExtras.getIntegerArrayList(KEY_ALL_PLACEMENT_ID);
            if (nativePlacements != null) {
                initializedNonVideoPlacements.addAll(nativePlacements);
            }
        }

        if (!initializedNonVideoPlacements.contains(placementID)) {
            initializedNonVideoPlacements.add(placementID);
            context = context.getApplicationContext();

            String initJsonConfig = buildJsonFromPlacements(
                    initializedNonVideoPlacements, initializedVideoPlacements);
            debugLog(TAG, "Configuring with placements: " + initJsonConfig);
            context = setAppIdInMeta(context, sdkAppID);
            DuAdNetwork.init(context, initJsonConfig);
            DuVideoAdSDK.init(context, initJsonConfig);
        }
    }

    public static void configureSDKForVideo(Context context,
                                            Bundle mediationExtras,
                                            String appID,
                                            int placementID) {
        if (TextUtils.isEmpty(sdkAppID)) {
            sdkAppID = appID;
        } else if (!sdkAppID.equals(appID)){
            debugLog(TAG, "Already configured with App ID: '" + sdkAppID + "'. "
                    + "Ignoring attempt to configure using App ID: " + appID);
            return;
        }

        if (mediationExtras != null) {
            ArrayList<Integer> videoPlacements =
                    mediationExtras.getIntegerArrayList(KEY_ALL_VIDEO_PLACEMENT_ID);
            if (videoPlacements != null) {
                initializedVideoPlacements.addAll(videoPlacements);
            }
        }

        if (!initializedVideoPlacements.contains(placementID)) {
            initializedVideoPlacements.add(placementID);
            context = context.getApplicationContext();

            String initJsonConfig = buildJsonFromPlacements(
                    initializedNonVideoPlacements, initializedVideoPlacements);
            debugLog(TAG, "Configuring with placements: " + initJsonConfig);
            context = setAppIdInMeta(context, appID);
            DuAdNetwork.init(context, initJsonConfig);
            DuVideoAdSDK.init(context, initJsonConfig);
        }
    }

    public static void initializeSDK(Context context,
                                     String appID,
                                     HashSet<Integer> nativePlacementIDs,
                                     HashSet<Integer> videoPlacementIDs) {
        if (TextUtils.isEmpty(sdkAppID)) {
            sdkAppID = appID;
        } else if (!sdkAppID.equals(appID)){
            debugLog(TAG, "Already configured with App ID: '" + sdkAppID + "'. "
                    + "Ignoring attempt to configure using App ID: " + appID);
            return;
        }

        if (!(initializedNonVideoPlacements.containsAll(nativePlacementIDs)
                && initializedVideoPlacements.containsAll(videoPlacementIDs))) {
            initializedNonVideoPlacements.addAll(nativePlacementIDs);
            initializedVideoPlacements.addAll(videoPlacementIDs);
            context = context.getApplicationContext();

            String initJsonConfig = buildJsonFromPlacements(
                    initializedNonVideoPlacements, initializedVideoPlacements);
            debugLog(TAG, "Configuring with placements: " + initJsonConfig);
            context = setAppIdInMeta(context, sdkAppID);
            DuAdNetwork.init(context, initJsonConfig);
            DuVideoAdSDK.init(context, initJsonConfig);
        }
    }

    private static Context setAppIdInMeta(Context context, String appId) {
        boolean appIdNotFound = true;

        try {
            ApplicationInfo applicationInfo = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = applicationInfo.metaData;
            if (metaData != null) {
                String appLicense = metaData.getString(KEY_APP_LICENSE);
                if (!TextUtils.isEmpty(appLicense)) {
                    DuAdMediation.debugLog(TAG, "appId from meta is " + appLicense);
                    appIdNotFound = false;
                } else {
                    if (!TextUtils.isEmpty(appId)) {
                        appIdNotFound = false;
                        DuAdMediation.debugLog(TAG, "appId from AdMob server is " + appId);
                        context = new AppIdContext(context, appId);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (appIdNotFound) {
            Log.e(TAG, "Missing or Invalid App ID.");
        }
        return context;
    }

    private static String buildJsonFromPlacements(@NonNull Collection<Integer> nativePlacements,
                                                  @NonNull Collection<Integer> videoPlacements) {
        try {
            JSONStringer array = new JSONStringer().object();

            if (nativePlacements.size() > 0) {
                array.key("native").array();
                for (Integer pid : nativePlacements) {
                    array.object().key("pid").value(pid).endObject();
                }
                array.endArray();
            }

            if (videoPlacements.size() > 0) {
                array.key("video").array();
                for (Integer pid : videoPlacements) {
                    array.object().key("pid").value(pid).endObject();
                }
                array.endArray();
            }

            array.endObject();
            return array.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean checkClassExist(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static int getValidPid(Bundle bundle) {
        if (bundle == null) {
            return -1;
        }
        String pidStr = bundle.getString(DuAdMediation.KEY_DAP_PID);
        if (TextUtils.isEmpty(pidStr)) {
            return -1;
        }
        int pid = -1;
        try {
            pid = Integer.parseInt(pidStr);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return -1;
        }
        if (pid < 0) {
            return -1;
        }
        return pid;
    }

    private static class AppIdPackageManager extends PackageManager {
        private final String appId;
        private final PackageManager innerPackageManager;
        private final Context context;

        private AppIdPackageManager(Context context, PackageManager packageManager, String appId) {
            this.innerPackageManager = packageManager;
            this.context = context;
            this.appId = appId;
        }

        public PackageInfo getPackageInfo(String s, int i) throws NameNotFoundException {
            return this.innerPackageManager.getPackageInfo(s, i);
        }

        public String[] currentToCanonicalPackageNames(String[] strings) {
            return this.innerPackageManager.currentToCanonicalPackageNames(strings);
        }

        public String[] canonicalToCurrentPackageNames(String[] strings) {
            return this.innerPackageManager.canonicalToCurrentPackageNames(strings);
        }

        public Intent getLaunchIntentForPackage(String s) {
            return this.innerPackageManager.getLaunchIntentForPackage(s);
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public Intent getLeanbackLaunchIntentForPackage(String s) {
            return this.innerPackageManager.getLeanbackLaunchIntentForPackage(s);
        }

        public int[] getPackageGids(String s) throws NameNotFoundException {
            return this.innerPackageManager.getPackageGids(s);
        }

        @TargetApi(Build.VERSION_CODES.N)
        public int[] getPackageGids(String packageName, int flags) throws NameNotFoundException {
            return this.innerPackageManager.getPackageGids(packageName, flags);
        }

        @TargetApi(Build.VERSION_CODES.N)
        public int getPackageUid(String packageName, int flags) throws NameNotFoundException {
            return this.innerPackageManager.getPackageUid(packageName, flags);
        }

        public PermissionInfo getPermissionInfo(String s, int i) throws NameNotFoundException {
            return this.innerPackageManager.getPermissionInfo(s, i);
        }

        public List<PermissionInfo> queryPermissionsByGroup(String s, int i) throws NameNotFoundException {
            return this.innerPackageManager.queryPermissionsByGroup(s, i);
        }

        public PermissionGroupInfo getPermissionGroupInfo(String s, int i) throws NameNotFoundException {
            return this.innerPackageManager.getPermissionGroupInfo(s, i);
        }

        public List<PermissionGroupInfo> getAllPermissionGroups(int i) {
            return null;
        }

        public ApplicationInfo getApplicationInfo(String s, int i) throws NameNotFoundException {
            if (s.equals(this.context.getPackageName())) {
                ApplicationInfo applicationInfo = this.innerPackageManager.getApplicationInfo(s, i);
                applicationInfo.metaData.putString(KEY_APP_LICENSE, appId);
                return applicationInfo;
            } else {
                return this.innerPackageManager.getApplicationInfo(s, i);
            }
        }

        public ActivityInfo getActivityInfo(ComponentName componentName, int i) throws NameNotFoundException {
            return this.innerPackageManager.getActivityInfo(componentName, i);
        }

        public ActivityInfo getReceiverInfo(ComponentName componentName, int i) throws NameNotFoundException {
            return this.innerPackageManager.getReceiverInfo(componentName, i);
        }

        public ServiceInfo getServiceInfo(ComponentName componentName, int i) throws NameNotFoundException {
            return this.innerPackageManager.getServiceInfo(componentName, i);
        }

        public ProviderInfo getProviderInfo(ComponentName componentName, int i) throws NameNotFoundException {
            return this.innerPackageManager.getProviderInfo(componentName, i);
        }

        public List<PackageInfo> getInstalledPackages(int i) {
            return this.innerPackageManager.getInstalledPackages(i);
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        public List<PackageInfo> getPackagesHoldingPermissions(String[] strings, int i) {
            return this.innerPackageManager.getPackagesHoldingPermissions(strings, i);
        }

        public int checkPermission(String s, String s1) {
            return this.innerPackageManager.checkPermission(s, s1);
        }

        public boolean isPermissionRevokedByPolicy(@NonNull String permName, @NonNull String pkgName) {
            return false;
        }

        public boolean addPermission(PermissionInfo permissionInfo) {
            return this.innerPackageManager.addPermission(permissionInfo);
        }

        public boolean addPermissionAsync(PermissionInfo permissionInfo) {
            return this.innerPackageManager.addPermissionAsync(permissionInfo);
        }

        public void removePermission(String s) {
            this.innerPackageManager.removePermission(s);
        }

        public int checkSignatures(String s, String s1) {
            return this.innerPackageManager.checkSignatures(s, s1);
        }

        public int checkSignatures(int i, int i1) {
            return this.innerPackageManager.checkSignatures(i, i1);
        }

        public String[] getPackagesForUid(int i) {
            return this.innerPackageManager.getPackagesForUid(i);
        }

        public String getNameForUid(int i) {
            return this.innerPackageManager.getNameForUid(i);
        }

        public List<ApplicationInfo> getInstalledApplications(int i) {
            return this.innerPackageManager.getInstalledApplications(i);
        }

        public String[] getSystemSharedLibraryNames() {
            return this.innerPackageManager.getSystemSharedLibraryNames();
        }

        public FeatureInfo[] getSystemAvailableFeatures() {
            return this.innerPackageManager.getSystemAvailableFeatures();
        }

        public boolean hasSystemFeature(String s) {
            return this.innerPackageManager.hasSystemFeature(s);
        }

        public boolean hasSystemFeature(String name, int version) {
            return false;
        }

        public ResolveInfo resolveActivity(Intent intent, int i) {
            return this.innerPackageManager.resolveActivity(intent, i);
        }

        public List<ResolveInfo> queryIntentActivities(Intent intent, int i) {
            return this.innerPackageManager.queryIntentActivities(intent, i);
        }

        public List<ResolveInfo> queryIntentActivityOptions(ComponentName componentName,
                                                            Intent[] intents, Intent intent, int i) {
            return this.innerPackageManager.queryIntentActivityOptions(componentName, intents, intent, i);
        }

        public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int i) {
            return this.innerPackageManager.queryBroadcastReceivers(intent, i);
        }

        public ResolveInfo resolveService(Intent intent, int i) {
            return this.innerPackageManager.resolveService(intent, i);
        }

        public List<ResolveInfo> queryIntentServices(Intent intent, int i) {
            return this.innerPackageManager.queryIntentServices(intent, i);
        }

        @TargetApi(Build.VERSION_CODES.KITKAT)
        public List<ResolveInfo> queryIntentContentProviders(Intent intent, int i) {
            return this.innerPackageManager.queryIntentContentProviders(intent, i);
        }

        public ProviderInfo resolveContentProvider(String s, int i) {
            return this.innerPackageManager.resolveContentProvider(s, i);
        }

        public List<ProviderInfo> queryContentProviders(String s, int i, int i1) {
            return this.innerPackageManager.queryContentProviders(s, i, i1);
        }

        public InstrumentationInfo getInstrumentationInfo(
                ComponentName componentName, int i) throws NameNotFoundException {
            return this.innerPackageManager.getInstrumentationInfo(componentName, i);
        }

        public List<InstrumentationInfo> queryInstrumentation(String s, int i) {
            return this.innerPackageManager.queryInstrumentation(s, i);
        }

        public Drawable getDrawable(String s, int i, ApplicationInfo applicationInfo) {
            return this.innerPackageManager.getDrawable(s, i, applicationInfo);
        }

        public Drawable getActivityIcon(ComponentName componentName) throws NameNotFoundException {
            return this.innerPackageManager.getActivityIcon(componentName);
        }

        public Drawable getActivityIcon(Intent intent) throws NameNotFoundException {
            return this.innerPackageManager.getActivityIcon(intent);
        }

        @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
        public Drawable getActivityBanner(ComponentName componentName) throws NameNotFoundException {
            return this.innerPackageManager.getActivityBanner(componentName);
        }

        @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
        public Drawable getActivityBanner(Intent intent) throws NameNotFoundException {
            return this.innerPackageManager.getActivityBanner(intent);
        }

        public Drawable getDefaultActivityIcon() {
            return this.innerPackageManager.getDefaultActivityIcon();
        }

        public Drawable getApplicationIcon(ApplicationInfo applicationInfo) {
            return this.innerPackageManager.getApplicationIcon(applicationInfo);
        }

        public Drawable getApplicationIcon(String s) throws NameNotFoundException {
            return this.innerPackageManager.getApplicationIcon(s);
        }

        @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
        public Drawable getApplicationBanner(ApplicationInfo applicationInfo) {
            return this.innerPackageManager.getApplicationBanner(applicationInfo);
        }

        @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
        public Drawable getApplicationBanner(String s) throws NameNotFoundException {
            return this.innerPackageManager.getApplicationBanner(s);
        }

        public Drawable getActivityLogo(ComponentName componentName) throws NameNotFoundException {
            return this.innerPackageManager.getActivityLogo(componentName);
        }

        public Drawable getActivityLogo(Intent intent) throws NameNotFoundException {
            return this.innerPackageManager.getActivityLogo(intent);
        }

        public Drawable getApplicationLogo(ApplicationInfo applicationInfo) {
            return this.innerPackageManager.getApplicationLogo(applicationInfo);
        }

        public Drawable getApplicationLogo(String s) throws NameNotFoundException {
            return this.innerPackageManager.getApplicationLogo(s);
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public Drawable getUserBadgedIcon(Drawable drawable, UserHandle userHandle) {
            return this.innerPackageManager.getUserBadgedIcon(drawable, userHandle);
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public Drawable getUserBadgedDrawableForDensity(Drawable drawable, UserHandle userHandle, Rect rect, int i) {
            return this.innerPackageManager.getUserBadgedDrawableForDensity(drawable, userHandle, rect, i);
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public CharSequence getUserBadgedLabel(CharSequence charSequence, UserHandle userHandle) {
            return this.innerPackageManager.getUserBadgedLabel(charSequence, userHandle);
        }

        public CharSequence getText(String s, int i, ApplicationInfo applicationInfo) {
            return this.innerPackageManager.getText(s, i, applicationInfo);
        }

        public XmlResourceParser getXml(String s, int i, ApplicationInfo applicationInfo) {
            return this.innerPackageManager.getXml(s, i, applicationInfo);
        }

        public CharSequence getApplicationLabel(ApplicationInfo applicationInfo) {
            return this.innerPackageManager.getApplicationLabel(applicationInfo);
        }

        public Resources getResourcesForActivity(ComponentName componentName) throws NameNotFoundException {
            return this.innerPackageManager.getResourcesForActivity(componentName);
        }

        public Resources getResourcesForApplication(ApplicationInfo applicationInfo) throws NameNotFoundException {
            return this.innerPackageManager.getResourcesForApplication(applicationInfo);
        }

        public Resources getResourcesForApplication(String s) throws NameNotFoundException {
            return this.innerPackageManager.getResourcesForApplication(s);
        }

        public void verifyPendingInstall(int i, int i1) {
            this.innerPackageManager.verifyPendingInstall(i, i1);
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        public void extendVerificationTimeout(int i, int i1, long l) {
            this.innerPackageManager.extendVerificationTimeout(i, i1, l);
        }

        public void setInstallerPackageName(String s, String s1) {
            this.innerPackageManager.setInstallerPackageName(s, s1);
        }

        public String getInstallerPackageName(String s) {
            return this.innerPackageManager.getInstallerPackageName(s);
        }

        public void addPackageToPreferred(String s) {
            this.innerPackageManager.addPackageToPreferred(s);
        }

        public void removePackageFromPreferred(String s) {
            this.innerPackageManager.removePackageFromPreferred(s);
        }

        public List<PackageInfo> getPreferredPackages(int i) {
            return this.innerPackageManager.getPreferredPackages(i);
        }

        public void addPreferredActivity(IntentFilter intentFilter, int i,
                                         ComponentName[] componentNames, ComponentName componentName) {
            this.innerPackageManager.addPreferredActivity(intentFilter, i, componentNames, componentName);
        }

        public void clearPackagePreferredActivities(String s) {
            this.innerPackageManager.clearPackagePreferredActivities(s);
        }

        public int getPreferredActivities(List<IntentFilter> list, List<ComponentName> list1, String s) {
            return this.innerPackageManager.getPreferredActivities(list, list1, s);
        }

        public void setComponentEnabledSetting(ComponentName componentName, int i, int i1) {
            this.innerPackageManager.setComponentEnabledSetting(componentName, i, i1);
        }

        public int getComponentEnabledSetting(ComponentName componentName) {
            return this.innerPackageManager.getComponentEnabledSetting(componentName);
        }

        public void setApplicationEnabledSetting(String s, int i, int i1) {
            this.innerPackageManager.setApplicationEnabledSetting(s, i, i1);
        }

        public int getApplicationEnabledSetting(String s) {
            return this.innerPackageManager.getApplicationEnabledSetting(s);
        }

        public boolean isSafeMode() {
            return this.innerPackageManager.isSafeMode();
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public PackageInstaller getPackageInstaller() {
            return this.innerPackageManager.getPackageInstaller();
        }

        @TargetApi(Build.VERSION_CODES.O)
        public List<SharedLibraryInfo> getSharedLibraries(int flags) {
            return this.innerPackageManager.getSharedLibraries(flags);
        }

        @TargetApi(Build.VERSION_CODES.O)
        public boolean canRequestPackageInstalls() {
            return this.innerPackageManager.canRequestPackageInstalls();
        }

        @TargetApi(Build.VERSION_CODES.O)
        public void setApplicationCategoryHint(@NonNull String packageName, int categoryHint) {
            this.innerPackageManager.setApplicationCategoryHint(packageName, categoryHint);
        }

        @TargetApi(Build.VERSION_CODES.O)
        public ChangedPackages getChangedPackages(int sequenceNumber) {
            return this.innerPackageManager.getChangedPackages(sequenceNumber);
        }

        @TargetApi(Build.VERSION_CODES.O)
        public void updateInstantAppCookie(@Nullable byte[] cookie) {
            this.innerPackageManager.updateInstantAppCookie(cookie);
        }

        @TargetApi(Build.VERSION_CODES.O)
        public void clearInstantAppCookie() {
            this.innerPackageManager.clearInstantAppCookie();
        }

        @TargetApi(Build.VERSION_CODES.O)
        public byte[] getInstantAppCookie() {
            return this.innerPackageManager.getInstantAppCookie();
        }

        @TargetApi(Build.VERSION_CODES.O)
        public int getInstantAppCookieMaxBytes() {
            return this.innerPackageManager.getInstantAppCookieMaxBytes();
        }

        @TargetApi(Build.VERSION_CODES.O)
        public boolean isInstantApp() {
            return this.innerPackageManager.isInstantApp();
        }

        @TargetApi(Build.VERSION_CODES.O)
        public boolean isInstantApp(String packageName) {
            return this.innerPackageManager.isInstantApp(packageName);
        }

        @TargetApi(Build.VERSION_CODES.O)
        public PackageInfo getPackageInfo(VersionedPackage versionedPackage, int flags) {
            try {
                return this.innerPackageManager.getPackageInfo(versionedPackage, flags);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private static class AppIdContext extends ContextWrapper {
        private final String appId;
        private final Context base;

        public AppIdContext(Context base, String appId) {
            super(base);
            this.base = base;
            this.appId = appId;
        }

        public Context getApplicationContext() {
            return this;
        }

        public PackageManager getPackageManager() {
            return new AppIdPackageManager(this, base.getPackageManager(), appId);
        }
    }
}
