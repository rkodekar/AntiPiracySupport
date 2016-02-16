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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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
	
    static EventHandler mHandler = new EventHandler();
    static final int MSG_UNINSTALL = 100;
    static final int MSG_DISABLE = 101;
    static final int MSG_FINISH = 102;

    static AntiPiracyUtils.PackageDeleteObserver mObserverDelete;
    static Method mUninstallMethod;
    static PackageManager mPm;
    
    static List<String> mInstalledList = new ArrayList<String>();

    // begin private region

    private class EventHandler extends Handler {
        public void handleMessage(Message m) {
            switch (m.what) {
                // uninstall
                case MSG_UNINSTALL:
                    try {
                        uninstallPackages();
                    } catch (IllegalAccessException WTF) {
                        Log.e(TAG, "IllegalAccessException" + WTF);
                    } catch (InvocationTargetException BBQ) {
                        Log.e(TAG, "InvocationTargetException" + BBQ);
                    }
                    break;
                    
                // disable
                case MSG_DISABLE:
                    try {
                        disablePackages() {
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
                mPm.setApplicationEnabledSetting(app, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);
            }
        }

        private synchronized void uninstallPackages() throws
                IllegalArgumentException, IllegalAccessException, InvocationTargetException {

            String[] packageNames = new String[mInstalledList.size()];
            packageNames = mInstalledList.toArray(packageNames);

            for (String app : packageNames) {
                mPm.setApplicationEnabledSetting(app, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0);

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
    
    void shutdown() {
        if (mHandler != null) {
            mHandler.sendEmptyMessage(MSG_FINISH);
            mHandler = null;
        }
    }
 
   /**
    * @param String packageName - package name to check
    * @param boolean disableNonMarket - if this is activated because of restricted content we should disable non-market app
    * installation here to increase the inconvenience for piraters.
    * @return true if the package name we're checking is installed
    */
   private static boolean isInstalled(@NonNull final String packageName, boolean disableNonMarket) {
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
    
    // end private region. begin public region.
    
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
            if (isInstalled(app, disableNonMarket)) {
                mInstalledList.add(app);
            }
        }

        mHandler.sendEmptyMessage(MSG_UNINSTALL);
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
            if (isInstalled(app, disableNonMarket)) {
                mInstalledList.add(app);
            }
        }

        mHandler.sendEmptyMessage(MSG_DISABLE);
	}
	
	/**
	 * Manually check for and uninstall and individual package
     * @param Context context
     * @param String targetPackage - check for and uninstall this package
     * @param boolean disableNonMarket - if this is because of restricted content we need to
     * disable non-market apps here to increase inconvenience
     */
	public static void uninstallTarget(Context context, @NonNull String targetPackage, boolean disableNonMarket) {
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

		if (isInstalled(targetPackage, disableNonMarket) {
        	mInstalledList.add(targetPackage);
        }

        mHandler.sendEmptyMessage(MSG_UNINSTALL);
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
        mObserverDelete = AntiPiracyUtils.getPackageDeleteObserver();

        try {
            mUninstallMethod = AntiPiracyUtils.getUninstallTypes(mPm);
        } catch (NoSuchMethodException WTF) {
            Log.e(TAG, "NoSuchMethodException" + WTF);
            // Unfortunately, we're finished without this
            shutdown();
            return;
        }

        if (isInstalled(targetPackage, disableNonMarket) {
        	mInstalledList.add(targetPackage);
        }

        mHandler.sendEmptyMessage(MSG_DISABLE);
    }
}
