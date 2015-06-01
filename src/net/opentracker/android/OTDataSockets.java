/*
 *  Copyright (C) 2011 Opentracker.net 
 * 
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 * 
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 * 
 *  The full license is located at the root of this distribution
 *  in the LICENSE file.
 *
 *  Please report bugs to support@opentracker.net
 *
 */
package net.opentracker.android;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.view.WindowManager;
import android.webkit.WebView;

/**
 * 
 * This class is provides methods for collecting information about android
 * devices for Opentracker's logging/ analytics engines.
 * 
 * @author $Author: eddie $ (latest svn author)
 * @version $Id: OTDataSockets.java 14172 2012-03-08 15:33:14Z eddie $
 */
public class OTDataSockets {

    @SuppressWarnings("rawtypes")
	private static HashMap cacheType = new HashMap();

    // ten seconds
    private static final long NETWORK_CACHE_MS = 10000l;// = 1 / 6 * 1000l * 60l;
    public static final String NO_NETWORK = "no network";
    private static final String TAG = OTDataSockets.class.getName();

    // private static OTLocationService locationService = null;

    public static final String WIFI = "WIFI";

    /**
     * Gets the pretty string for this application's version name as defined in
     * the xml manifest file.
     * 
     * @param appContext
     *            The context used to examine packages
     * @return The application's version as a pretty string
     */
    public static String getAppVersion(final Context appContext) {
        long t0 = System.currentTimeMillis();
        LogWrapper.v(TAG, "getAppVersion()");
        PackageManager pm = appContext.getPackageManager();
        try {
            t0 = System.currentTimeMillis() - t0;
            LogWrapper.v(TAG, t0 + "[ms]");
            return pm.getPackageInfo(appContext.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            t0 = System.currentTimeMillis() - t0;
            LogWrapper.v(TAG, t0 + "[ms]");
            return "unknown";
        }
    }

    public static String getCoordinateAccuracy(final Context appContext) {
        long t0 = System.currentTimeMillis();
        LogWrapper.v(TAG, "getLastCoordinates()");

        LocationManager locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);

        // try to get the latest
        Location tmpLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (tmpLocation == null) {
        	 LogWrapper.v(TAG, t0 + "[ms]");
             return null;
        }

        String value = "" + tmpLocation.getAccuracy();
        LogWrapper.v(TAG, "getCoordinateAccuracy: " + value + "; on " + new Date(tmpLocation.getTime()));
        t0 = System.currentTimeMillis() - t0;
        LogWrapper.v(TAG, t0 + "[ms]");

        // dont return default 0,0 values sometimes seen
        if (tmpLocation.getLatitude() != 0f && tmpLocation.getLongitude() != 0) {
            return value;
        } else {
            return null;
        }
    }

    public static String getCoordinateLatitude(final Context appContext) {
        long t0 = System.currentTimeMillis();
        LogWrapper.v(TAG, "getLastCoordinates()");

        LocationManager locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);

        // try to get the latest
        Location tmpLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (tmpLocation == null) {
        	LogWrapper.v(TAG, t0 + "[ms]");
            return null;
        }

        String value = "" + tmpLocation.getLatitude();

         LogWrapper.v(TAG, "tmpLocation.getAccuracy: " + tmpLocation.getAccuracy());
         LogWrapper.v(TAG, "getLastLatitude: " + value + "; on " + new Date(tmpLocation.getTime()));
         t0 = System.currentTimeMillis() - t0;
         LogWrapper.v(TAG, t0 + "[ms]");

        // dont return default 0,0 values sometimes seen
        if (tmpLocation.getLatitude() != 0f && tmpLocation.getLongitude() != 0) {
            return value;
        } else {
            return null;
        }
    }

    public static String getCoordinateLongitude(final Context appContext) {
        long t0 = System.currentTimeMillis();
        LogWrapper.v(TAG, "getLastCoordinates()");

        LocationManager locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);

        // try to get the latest
        Location tmpLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (tmpLocation == null) {
            LogWrapper.v(TAG, t0 + "[ms]");
            return null;
        }
        String value = "" + tmpLocation.getLongitude();

         LogWrapper.v(TAG, "tmpLocation.getAccuracy: " + tmpLocation.getAccuracy());
         LogWrapper.v(TAG, "getLastLongitude: " + value + "; on " + new Date(tmpLocation.getTime()));
         t0 = System.currentTimeMillis() - t0;
         LogWrapper.v(TAG, t0 + "[ms]");

        // dont return default 0,0 values sometimes seen
        if (tmpLocation.getLatitude() != 0f && tmpLocation.getLongitude() != 0) {
            return value;
        } else {
            return null;
        }
    }

    public static String getCoordinateTime(final Context appContext) {
        long t0 = System.currentTimeMillis();
        LogWrapper.v(TAG, "getLastCoordinates()");

        LocationManager locationManager =
                (LocationManager) appContext
                        .getSystemService(Context.LOCATION_SERVICE);

        // try to get the latest
        Location tmpLocation =locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (tmpLocation != null) {

            String value = "" + tmpLocation.getTime();
            if (tmpLocation.getLatitude() != 0f
                    && tmpLocation.getLongitude() != 0) {
                return value;
            } else {
                return null;
            }
        } else {
            LogWrapper.v(TAG, t0 + "[ms]");
            return null;
        }
    }

    public static String getDevice() {

        long t0 = System.currentTimeMillis();
        LogWrapper.v(TAG, "getDevice()");

        String model = android.os.Build.MODEL;

        t0 = System.currentTimeMillis() - t0;
        LogWrapper.v(TAG, t0 + "[ms]");
        return model;
    }

    public static String getIpAddress() {

        long t0 = System.currentTimeMillis();
        LogWrapper.v(TAG, "getIpAddress()");
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        LogWrapper.v(TAG, t0 + "[ms]");
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
        }
        return null;
    }

    /**
     * http://stackoverflow.com/questions/4212320/get-the-current-language-in-
     * device
     */
    public static String getLocale(final Context appContext) {
        return appContext.getResources().getConfiguration().locale.toString();
    }

    /**
     * http://stackoverflow.com/questions/3838602/how-to-find-out-carriers-name-
     * in-android
     */
    public static String getCarrier(final Context appContext) {
        TelephonyManager manager = (TelephonyManager) appContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (manager != null) {
            return manager.getNetworkOperatorName();
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static String getNetworkType(final Context appContext) {
        long t0 = System.currentTimeMillis();
        LogWrapper.v(TAG, "getNetworkType()");
        if (cacheType.get("last modified time") != null && (System.currentTimeMillis() - (Long) cacheType.get("last modified time") < NETWORK_CACHE_MS)) {
            LogWrapper.v(TAG, (String) cacheType.get("networkType"));
            LogWrapper.v(TAG, "cache is " + (System.currentTimeMillis() - (Long) cacheType.get("last modified time")) + " [ms] old");

            t0 = System.currentTimeMillis() - t0;
            LogWrapper.v(TAG, t0 + "[ms]");
            return (String) cacheType.get("networkType");
        }
        try {
            LogWrapper.v(TAG, "Getting network type.");

            ConnectivityManager connectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo != null) {
	            String networkInfoTypeName = activeNetworkInfo.getTypeName();
	            
	            cacheType.put("networkType", networkInfoTypeName);
	            cacheType.put("last modified time", System.currentTimeMillis());
	            LogWrapper.v(TAG, networkInfoTypeName);
	
	            t0 = System.currentTimeMillis() - t0;
	            LogWrapper.v(TAG, t0 + "[ms]");
	            return networkInfoTypeName;
            }
        } catch (SecurityException ise) {
            LogWrapper.w(TAG, ise);
        }
        cacheType.put("networkType", NO_NETWORK);
        cacheType.put("last modified time", System.currentTimeMillis());
        LogWrapper.v(TAG, NO_NETWORK);

        t0 = System.currentTimeMillis() - t0;
        LogWrapper.v(TAG, t0 + "[ms]");
        return NO_NETWORK;
    }

    public static String getPlatform() {
        return "Android";
    }

    public static String getPlatformVersion() {
        long t0 = System.currentTimeMillis();
        String release = android.os.Build.VERSION.RELEASE;
        t0 = System.currentTimeMillis() - t0;
        LogWrapper.v(TAG, t0 + "[ms]");
        return release;
    }

    @SuppressWarnings("deprecation")
	public static String getScreenHeight(final Context appContext) {
        long t0 = System.currentTimeMillis();
        LogWrapper.v(TAG, "getScreenHeight()");
        // http://stackoverflow.com/questions/1016896/android-how-to-get-screen-dimensions
        WindowManager windowManager = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
        // int width = windowManager.getDefaultDisplay().getWidth();
        int height = windowManager.getDefaultDisplay().getHeight();

        t0 = System.currentTimeMillis() - t0;
        LogWrapper.v(TAG, t0 + "[ms]");

        return "" + height;
    }

    @SuppressWarnings("deprecation")
	public static String getScreenWidth(final Context appContext) {
        long t0 = System.currentTimeMillis();
        LogWrapper.v(TAG, "getScreenWidth()");
        // http://stackoverflow.com/questions/1016896/android-how-to-get-screen-dimensions
        WindowManager windowManager = (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
        int width = windowManager.getDefaultDisplay().getWidth();
        // int height = windowManager.getDefaultDisplay().getHeight();

        t0 = System.currentTimeMillis() - t0;
        LogWrapper.v(TAG, t0 + "[ms]");

        return "" + width;
    }

    /**
     * TODO: android does not have a way to detect the color depth.
     */
    public static String getScreenColors(final Context appContext) {
        return null;
    }

    public static String getUserAgent(final Context appContext) {
        long t0 = System.currentTimeMillis();
        LogWrapper.v(TAG, "getUserAgent()");
        // getting the user agent
        // http://developer.android.com/reference/android/webkit/WebSettings.html#getUserAgentString()
        // http://stackoverflow.com/questions/5638749/get-user-agent-in-my-app-which-doesnt-contain-a-webview
        WebView view = new WebView(appContext);
        t0 = System.currentTimeMillis() - t0;
        LogWrapper.v(TAG, t0 + "[ms]");

        return view.getSettings().getUserAgentString();
    }

    public static String getWifiInfo(final Context appContext) {
        long t0 = System.currentTimeMillis();
        LogWrapper.v(TAG, "getWifiInfo()");

        try {
            WifiManager wifiManager = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();

            LogWrapper.v("getWifiInfo(): ", wifiInfo.toString());
            return wifiInfo.toString();
        } catch (SecurityException ise) {
            LogWrapper.w(TAG, ise);
        }

        t0 = System.currentTimeMillis() - t0;
        LogWrapper.v(TAG, t0 + "[ms]");

        return "unknown";
    }

}