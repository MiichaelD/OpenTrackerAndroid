package net.opentracker.android.test;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

//TODO I only have the doubt of for how long are we receiving location updates? 
//while we are subscribed for updates, are we forcing gps/network to find a location? how do we stop it? 

// read: http://blog.doityourselfandroid.com/2010/12/25/understanding-locationlistener-android/
// on conserving battery, and timing constraints.
// read: http://stackoverflow.com/questions/2021176/how-can-i-check-the-current-status-of-the-gps-receiver
// on the status icon
// read: http://stackoverflow.com/questions/3145089/what-is-the-simplest-and-most-robust-way-to-get-the-users-current-location-in-a/3145655#3145655
// and used this code a main body of location services
// confirmed that is is best code by looking at
// http://stackoverflow.com/questions/1513485/how-do-i-get-the-current-gps-location-programmatically-in-android
// which has less control
public class OTLocationService {
	
	/*
     * A facility for threads to schedule tasks for future execution in a
     * background thread. Tasks may be scheduled for one-time execution, or for
     * repeated execution at regular intervals.
     */
    private static Timer gpsTimer, networkTimer;

    private static boolean isGpsEnabled = false, isNetworkEnabled = false;
    private static boolean isMoved = true;

    private static long lastRun = -1;
    

    /*
     * Every 5 mins our GPS kicks in and tries to retrieve the location.
     * It does so by providing the location listener with multiple location
     * updates that we can use to track the users location, before going idle.
     * 
     * When the GPS is trying to pinpoint your location, the GPS icon on the
     * phone will blink, you know that it�s consuming your battery power.
     * 
     * Notice how during these windows where its pinpointing a location, and
     * location updates are sent to the listeners, different accuracies are
     * provided.
     */
    private long minTimeMs = 5* 60 * 1000; // 5 mins in ms
    private static long minTimeBwLocUpdatesMs = 0  * 60 * 1000; // minutes x seconds * 1000ms
    private static long delayBeforeStartingTasksMs = 15;
    private static long minDistance = 0; //in meters
    
    private static LocationManager locationManager;

    private static final String TAG = OTLocationService.class.getName();

    public abstract class LocationResult {
    	/** Get the GPS position if any */
        public abstract Location getGPSLocation();

        /** Get the Network position if any */
        public abstract Location getNetworkLocation();

        /** Cache the GPS position gotten by the LocationManager for future querying */
        public abstract void setGPSLocation(Location location);

        /** Cache the Network position gotten by the LocationManager for future querying */
        public abstract void setNetworkLocation(Location location);

    }
    
    public final LocationResult locationResult = new LocationResult() {

        private Location gpsLocation;
        private Location networkLocation;

        @Override
        public Location getGPSLocation() {
            if (gpsLocation != null)
                return gpsLocation;

            if (locationManager == null)
                return null;

            // try to get the latest
            Location tmpLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            return tmpLocation; //this may be null

        }

        @Override
        public Location getNetworkLocation() {
            if (networkLocation != null)
                return networkLocation;

            if (locationManager == null)
                return null;

            // try to get the latest
            Location tmpLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            return tmpLocation;
        }

        public void setGPSLocation(Location location) {
            this.gpsLocation = location;
        }

        public void setNetworkLocation(Location location) {
            this.networkLocation = location;
        }
    };
    
    private class UpdateLocationTask extends TimerTask {
    	private UpdateLocationTask(final String provider){
    		this.provider = provider;
    	}
    	
    	private final String provider;
    	
        @Override
        public void run() {
        	//request location updates each time the minTime (ms) has passed or the location has changed for minDistance (m)
        	// we don't have minimum time nor distance filter, get all the updates!
            locationManager.removeUpdates(locationListenerGps);
            LocationListener locListener = provider == LocationManager.GPS_PROVIDER ? locationListenerGps : locationListenerNetwork;
            locationManager.requestLocationUpdates(provider, minTimeBwLocUpdatesMs, minDistance, locListener);
            Log.v(TAG, "done with "+provider+" update request...");
        }
    }

    private LocationListener locationListenerGps = new LocationListener() {

        public void onLocationChanged(Location location) {
            Log.i(TAG, "locationListenerGps: onLocationChanged called for gps " + location);

            if (location == null || (location.getLatitude() == 0.0 && location.getLongitude() == 0.0)) {
                // Hack to filter out 0.0,0.0 locations
                return;
            }
            location.setTime(System.currentTimeMillis());
            locationResult.setGPSLocation(location);
            gpsTimer.cancel();
            // locationManager.removeUpdates(this);
        }

        public void onProviderDisabled(String provider) {
            Log.v(TAG, "locationListenerGps: onProviderDisabled called");
        }

        public void onProviderEnabled(String provider) {
            Log.v(TAG, "locationListenerGps: onProviderEnabled called");
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.v(TAG, "locationListenerGps: onStatusChanged called");
        }
    };

    private LocationListener locationListenerNetwork = new LocationListener() {

        public void onLocationChanged(Location location) {
            Log.i(TAG, "locationListenerNetwork: onLocationChanged called for network " + location);

            if (location == null || (location.getLatitude() == 0.0 && location.getLongitude() == 0.0)) {
                // Hack to filter out 0.0,0.0 locations
                return;
            }
            location.setTime(System.currentTimeMillis());
            locationResult.setNetworkLocation(location);
            // locationManager.removeUpdates(this);
            networkTimer.cancel();
        }

        public void onProviderDisabled(String provider) {
            Log.v(TAG, "locationListenerNetwork: onProviderDisabled called");
        }

        public void onProviderEnabled(String provider) {
            Log.v(TAG, "locationListenerNetwork: onProviderEnabled called");
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.v(TAG, "locationListenerNetwork: onStatusChanged called");
        }

    };

    public OTLocationService(Context appContext) {
        Log.v(TAG, "constructor called");
        runLocationService(appContext);
    }

    /*
     * When called the GPS will try to pinpoint the location. The GPS icon on
     * the phone will blink and the GPS system will consume battery power.
     * 
     * The GPS location will be provided by satellites and will take some time,
     * the time needed is a function of the hardware and the system's position
     * on our planet. The variable delayGpsMs is the number of milliseconds we
     * wait before updating the last know location with GPS data. This should be
     * enough time for the GPS system to do its work.
     * 
     * This class has a safety valve defaulted to a minTimeMs of five minutes.
     * Ie you can not run two GPS pinpointing services separated by less that
     * minTimeMs milliseconds;
     */
    public void runLocationService(Context context) {

        Log.v(TAG, "runLocationService called, appContext: " + context);

        // Use LocationResult as a callback class to pass location value from
        // getLocation to user code.

        Log.v(TAG, "retreaving last run: "
                + lastRun
                + ", "
                + (lastRun != -1 ? Math.round((System.currentTimeMillis() - lastRun) / (1000 * 60))
                				 : "unknown") + " [min]");

        if (System.currentTimeMillis() - lastRun < minTimeMs) {
            return;
        }
        if (!isMoved)
            return;

        lastRun = System.currentTimeMillis();

        if (locationManager == null) {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }

        Log.v(TAG, "retreaving providers...");

        // exceptions will be thrown if provider is not permitted.
        try {
            isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            Log.e(TAG,"GPS-based location service not enabled");
        }
        try {
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            Log.e(TAG,"Network-based location service not enabled");
        }

        // don't start listeners if no provider is enabled
        if (!isGpsEnabled && !isNetworkEnabled)
            return;

        Log.d(TAG, "scheduling pinpointing gps: " + isGpsEnabled  + ", network: " + isNetworkEnabled);

        if (isNetworkEnabled) {
            Log.v(TAG, "added network update location request...");
            networkTimer = new Timer();
            UpdateLocationTask locationTask = new UpdateLocationTask(LocationManager.GPS_PROVIDER);
            networkTimer.schedule(locationTask, delayBeforeStartingTasksMs);
        }
        if (isGpsEnabled) {
            Log.v(TAG, "added gps update location request...");
            gpsTimer = new Timer();
            UpdateLocationTask locationTask = new UpdateLocationTask(LocationManager.NETWORK_PROVIDER);
            gpsTimer.schedule(locationTask, delayBeforeStartingTasksMs);
        }
    }

    /*
     * GPS systems take some time to pinpoint location data. The variable
     * delayGpsMs is the number of milliseconds we wait before updating the last
     * known location with GPS data. This should be enough time for the GPS
     * system to do its work.
     */
    public void setDelayGps(long minTimeMs) {
        // this.minTimeMs = minTimeMs;
    }

    /*
     * Sets the safety valve minTimeMs in milliseconds, ie to conserve battery
     * power you can not run two GPS pinpointing services separated by less that
     * minTimeMs milliseconds.
     */
    public void setMinTimeMs(long minTimeMs) {
         this.minTimeMs = minTimeMs;
    }
}