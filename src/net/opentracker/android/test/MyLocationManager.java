package net.opentracker.android.test;

import net.opentracker.android.LogWrapper;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;

public class MyLocationManager {

    private static final String TAG = MyLocationManager.class.getSimpleName();

    private Context mContext = null;

    private LocationManager mLocationManager = null;

    public MyLocationManager(Context context) {
        mContext = context;
        if (mContext != null) {
            mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        }
    }

    LocationListener[] mLocationListeners =  new LocationListener[] {
                    new LocationListener(LocationManager.GPS_PROVIDER),
                    new LocationListener(LocationManager.NETWORK_PROVIDER) };

    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;

        boolean mValid = false;

        String mProvider;

        public LocationListener(String provider) {
            mProvider = provider;
            mLastLocation = new Location(mProvider);
        }

        public void onLocationChanged(Location newLocation) {
        	// Hack to filter out 0.0,0.0 locations
            if (newLocation.getLatitude() == 0.0 && newLocation.getLongitude() == 0.0) {
                return;
            }
            if (newLocation != null) {
                if(newLocation.getTime() == 0)
                	newLocation.setTime(System.currentTimeMillis());
                LogWrapper.i(TAG, "onLocationChanged in loc mgnr");
            }
            mLastLocation.set(newLocation);
            mValid = true;
        }

        public void onProviderEnabled(String provider) {
        	mValid = true;
        }

        public void onProviderDisabled(String provider) {
            mValid = false;
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (status == LocationProvider.OUT_OF_SERVICE) {
                mValid = false;
            }
        }

        public Location current() {
            return mValid ? mLastLocation : null;
        }
    };
    
    public void getLastKnownLocation(){
    	
    }

    /**Request location updates each time the minTime (ms) has passed or the location has changed for minDistance (m)
     * We don't have minimum time nor distance filter, get all the updates! */
    public void startLocationReceiving() {
        if (this.mLocationManager != null) {
            try {
            	if (mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            		mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0F, this.mLocationListeners[1]);
            	}
            } catch (java.lang.SecurityException ex) {
            	LogWrapper.e(TAG, "SecurityException " + ex.getMessage());
            } catch (IllegalArgumentException ex) {
                // Log.e(TAG, "provider does not exist " + ex.getMessage());
            }
            try {
            	if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0F, this.mLocationListeners[1]);
            	}
            } catch (java.lang.SecurityException ex) {
            	LogWrapper.e(TAG, "SecurityException " + ex.getMessage());
            } catch (IllegalArgumentException ex) {
                // Log.e(TAG, "provider does not exist " + ex.getMessage());
            }
        }
    }

    public void stopLocationReceiving() {
        if (this.mLocationManager != null) {
            for (int i = 0; i < this.mLocationListeners.length; i++) {
                try {
                    this.mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    // ok
                }
            }
        }
    }

    public Location getCurrentLocation() {
        Location l = null;

        // go in best to worst order
        for (int i = 0; i < this.mLocationListeners.length; i++) {
            l = this.mLocationListeners[i].current();
            if (l != null)
                break;
        }

        return l;
    }
}