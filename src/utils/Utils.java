/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.antipiracy.support.utils;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.antipiracy.support.utils.AntiPiracyUtils;
import org.antipiracy.support.utils.AntiPiracyUtils.PackageDeleteObserver;

import static org.antipiracy.support.utils.AntiPiracyConstants.*;

/**
 * This service blocks the install of known piracy/malware apps. Please report new piracy
 * apps to ROM developers deploying this code.
 * @author github.com/AlmightyMegadeth00 - activethrasher00@gmail.com
 * Note: the current source can be found in github.com/ContentGuard
 */
public class Utils {
    private static final String TAG = Utils.class.getCanonicalName();

    private static EventHandler mHandler;
    static final int MSG_UNINSTALL = 100;
    static final int MSG_UNINSTALL_DISABLE = 101;
    static final int MSG_DISABLE = 102;
    static final int MSG_FINISH = 103;

    static AntiPiracyUtils.PackageDeleteObserver mObserverDelete;
    static Method mUninstallMethod;
    static PackageManager mPm;

    static List<String> mInstalledList = new ArrayList<String>();

    private static Utils mUtils;
    public static Utils getInstance() {
        if (mUtils == null) mUtils = new Utils();
        return mUtils;
    }

    private Utils() {
    }

   /**
    * @param String packageName - package name to check
    * @param boolean disableNonMarket - if this is activated because of restricted content we should disable non-market app
    * installation here to increase the inconvenience for piraters.
    * @return true if the package name we're checking is installed
    */
   private static boolean isInstalled(Context ctx, @NonNull String packageName, boolean disableNonMarket) {
        String mVersion;
        try {
            mVersion = mPm.getPackageInfo(packageName, 0).versionName;
            if (mVersion.equals(null)) {
                return false;
            }
        } catch (NameNotFoundException e) {
            if (DEBUG) Log.e(TAG, "Package " + packageName + " NameNotFoundException" + e);
            return false;
        }

        // Change the system setting to disable non-market app installs.  This isn't
        // a measure of security as much as it is to increase the inconvenience factor
        if (disableNonMarket) {
            Settings.Global.putInt(ctx.getContentResolver(), Settings.Global.INSTALL_NON_MARKET_APPS, 0);
        }

        return true;
    }

    /**
     * Check for and uninstall blacklisted packages
     * @param Context context
     * @param boolean disableNonMarket - if this is because of restricted content we need to
     * disable non-market apps here to increase inconvenience
     */
    public static void uninstallConfiguration(Context context, boolean disableNonMarket) {
        mInstalledList.clear();
        mPm = context.getPackageManager();
        mObserverDelete = AntiPiracyUtils.getPackageDeleteObserver();

        try {
            mUninstallMethod = AntiPiracyUtils.getUninstallTypes(mPm);
        } catch (NoSuchMethodException WTF) {
            Log.e(TAG, "NoSuchMethodException" + WTF);
            // Unfortunately, we're finished without this
            shutdown();
            return;
        }

        String[] packageNames = PACKAGES;
        for (String app : packageNames) {
            if (isInstalled(context, app, disableNonMarket)) {
                mInstalledList.add(app);
            }
        }

        mHandler = new EventHandler(context);
        mHandler.sendEmptyMessage(MSG_UNINSTALL_DISABLE);
    }

    /**
     * Check for and disable blacklisted packages
     * @param Context context
     * @param boolean disableNonMarket - if this is because of restricted content we need to
     * disable non-market apps here to increase inconvenience
     */
    public static void disableConfiguration(Context context, boolean disableNonMarket) {
        mInstalledList.clear();
        mPm = context.getPackageManager();
        mObserverDelete = AntiPiracyUtils.getPackageDeleteObserver();

        try {
            mUninstallMethod = AntiPiracyUtils.getUninstallTypes(mPm);
        } catch (NoSuchMethodException WTF) {
            Log.e(TAG, "NoSuchMethodException" + WTF);
            // Unfortunately, we're finished without this
            shutdown();
            return;
        }

        String[] packageNames = PACKAGES;
        for (String app : packageNames) {
            if (isInstalled(context, app, disableNonMarket)) {
                mInstalledList.add(app);
            }
        }

        mHandler = new EventHandler(context);
        mHandler.sendEmptyMessage(MSG_DISABLE);
    }

    /**
     * Manually check for and uninstall and individual package
     * @param Context context
     * @param String targetPackage - check for and uninstall this package
     * @param boolean disableNonMarket - if this is because of restricted content we need to
     * disable non-market apps here to increase inconvenience
     * @param boolean disableBeforeUninstall - disable the app before uninstalling
     */
    public static void uninstallTarget(Context context, @NonNull String targetPackage, boolean disableNonMarket, boolean disableBeforeUninstall) {
        mInstalledList.clear();
        mPm = context.getPackageManager();
        mObserverDelete = AntiPiracyUtils.getPackageDeleteObserver();

        try {
            mUninstallMethod = AntiPiracyUtils.getUninstallTypes(mPm);
        } catch (NoSuchMethodException WTF) {
            Log.e(TAG, "NoSuchMethodException" + WTF);
            // Unfortunately, we're finished without this
            shutdown();
            return;
        }

        if (isInstalled(context, targetPackage, disableNonMarket)) {
            mInstalledList.add(targetPackage);
        }

        mHandler = new EventHandler(context);
        mHandler.sendEmptyMessage(disableBeforeUninstall ? MSG_UNINSTALL_DISABLE : MSG_UNINSTALL);
    }

    /**
     * Manually check for and uninstall a list of packages
     * @param Context context
     * @param List<String> targetPackages - check for and uninstall this package
     * @param boolean disableNonMarket - if this is because of restricted content we need to
     * disable non-market apps here to increase inconvenience
     * @param boolean disableBeforeUninstall - disable the app before uninstalling
     */
    public static void uninstallTarget(Context context, @NonNull Set<String> targetPackages, boolean disableNonMarket, boolean disableBeforeUninstall) {
        mInstalledList.clear();
        mPm = context.getPackageManager();
        mObserverDelete = AntiPiracyUtils.getPackageDeleteObserver();

        try {
            mUninstallMethod = AntiPiracyUtils.getUninstallTypes(mPm);
        } catch (NoSuchMethodException WTF) {
            Log.e(TAG, "NoSuchMethodException" + WTF);
            // Unfortunately, we're finished without this
            shutdown();
            return;
        }

        for (String app : targetPackages) {
            if (isInstalled(context, app, disableNonMarket)) {
                mInstalledList.add(app);
            }
        }

        mHandler = new EventHandler(context);
        mHandler.sendEmptyMessage(disableBeforeUninstall ? MSG_UNINSTALL_DISABLE : MSG_UNINSTALL);
    }

    /**
     * @param Context context
     * @param String targetPackage - check for and disable this package
     * @param boolean disableNonMarket - if this is because of restricted content we need to
     * disable non-market apps here to increase inconvenience
     */
    public static void disableTarget(Context context, @NonNull String targetPackage, boolean disableNonMarket) {
        mInstalledList.clear();
        mPm = context.getPackageManager();

        if (isInstalled(context, targetPackage, disableNonMarket)) {
            mInstalledList.add(targetPackage);
        } else {
            Log.e(TAG, "error disabling " + targetPackage + ". package not installed");
        }

        mHandler = new EventHandler(context);
        mHandler.sendEmptyMessage(MSG_DISABLE);
    }

    /**
     * @param Context context
     * @param List<String> targetPackages - check for and disable this list of packages
     * @param boolean disableNonMarket - if this is because of restricted content we need to
     * disable non-market apps here to increase inconvenience
     */
    public static void disableTarget(Context context, @NonNull Set<String> targetPackages, boolean disableNonMarket) {
        mInstalledList.clear();
        mPm = context.getPackageManager();

        for (String app : targetPackages) {
            if (isInstalled(context, app, disableNonMarket)) {
                mInstalledList.add(app);
            } else {
                Log.e(TAG, "error disabling " + app + ". package not installed");
            }
        }

        mHandler = new EventHandler(context);
        mHandler.sendEmptyMessage(MSG_DISABLE);
    }

    public static synchronized void forceStopPackage(Context context, String pkgName) {
        ActivityManager am = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        am.forceStopPackage(pkgName);
    }

    public static void shutdown() {
        if (mHandler != null) {
            mHandler.sendEmptyMessage(MSG_FINISH);
            mHandler = null;
        }
    }

    static class EventHandler extends Handler {
		private WeakReference<Context> cRef;

		public EventHandler(Context context) {
			cRef = new WeakReference<Context>(context);
		}
			
        public void handleMessage(Message m) {
            switch (m.what) {
                // uninstall
                case MSG_UNINSTALL:
                    try {
                        uninstallPackages(false);
                    } catch (IllegalAccessException WTF) {
                        Log.e(TAG, "IllegalAccessException" + WTF);
                    } catch (InvocationTargetException BBQ) {
                        Log.e(TAG, "InvocationTargetException" + BBQ);
                    }
                    break;

               case MSG_UNINSTALL_DISABLE:
                    try {
                        uninstallPackages(true);
                    } catch (IllegalAccessException WTF) {
                        Log.e(TAG, "IllegalAccessException" + WTF);
                    } catch (InvocationTargetException BBQ) {
                        Log.e(TAG, "InvocationTargetException" + BBQ);
                    }
                    break;

                // disable
                case MSG_DISABLE:
                    try {
                        disablePackages();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                // finish
                case MSG_FINISH:
                    this.removeMessages(0);
                    break;

                default:
                    break;
            }
        }

        private synchronized void disablePackages() {
            String[] packageNames = new String[mInstalledList.size()];
            packageNames = mInstalledList.toArray(packageNames);

            for (String app : packageNames) {
                Log.e(TAG, "disabling package " + app);
                mPm.setApplicationEnabledSetting(app, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
            }
        }

        private synchronized void uninstallPackages(boolean disableFirst) throws
                IllegalArgumentException, IllegalAccessException, InvocationTargetException {

            String[] packageNames = new String[mInstalledList.size()];
            packageNames = mInstalledList.toArray(packageNames);

            for (String app : packageNames) {
                if (disableFirst) {
					mPm.setApplicationEnabledSetting(app, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
				} else {
					forceStopPackage(cRef.get(), app);
				}

                mUninstallMethod.invoke(mPm, new Object[] {
                    app, mObserverDelete, 0
                });

                // Take a pause before attempting the next package.
                try {
                   Thread.sleep(500);
                } catch (InterruptedException WTF) {
                    Log.e(TAG, "InterruptedException" + WTF);
                }
            }

            // we're finished
            shutdown();
        }
    }
}
