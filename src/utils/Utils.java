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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.antipiracy.support.utils.AntiPiracyUtils;
import org.antipiracy.support.utils.AntiPiracyUtils.PackageDeleteObserver;

import static org.antipiracy.support.utils.AntiPiracyConstants.*;

/** This service blocks the install of known piracy/malware apps. Please report new piracy
 * apps to ROM developers deploying this code.
 * @author github.com/AlmightyMegadeth00 - activethrasher00@gmail.com
 */
public class Utils {
	private static final String TAG = Utils.class.getCanonicalName();
	
	// Notify service handler
    static EventHandler mHandler = new EventHandler();
    static final int MSG_UNINSTALL = 100;
    static final int MSG_DISABLE = 101;
    static final int MSG_FINISH = 102;

    static AntiPiracyUtils.PackageDeleteObserver mObserverDelete;
    static Method mUninstallMethod;
    static PackageManager mPm;
    
    static List<String> mInstalledList = new ArrayList<String>();
    
    public static void uninstallConfiguration(Context context) {
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
            if (isInstalled(app)) {
                mInstalledList.add(app);
            }
        }

        mHandler.sendEmptyMessage(MSG_UNINSTALL);
	}
	
	public static void disableConfiguration(Context context) {
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
            if (isInstalled(app)) {
                mInstalledList.add(app);
            }
        }

        mHandler.sendEmptyMessage(MSG_DISABLE);
	}
	
	private static boolean isInstalled(final String packageName) {
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
        return true;
    }

    private class EventHandler extends Handler {
        public void handleMessage(Message m) {
            switch (m.what) {
                case MSG_UNINSTALL:
                    // uninstall
                    try {
                        uninstallPackages();
                    } catch (IllegalAccessException WTF) {
                        Log.e(TAG, "IllegalAccessException" + WTF);
                    } catch (InvocationTargetException BBQ) {
                        Log.e(TAG, "InvocationTargetException" + BBQ);
                    }
                    break;
                case MSG_DISABLE:
                    
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
}
