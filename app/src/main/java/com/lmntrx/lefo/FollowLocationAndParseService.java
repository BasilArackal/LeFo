package com.lmntrx.lefo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/*
 * Created by livin on 15/1/16.
 */
public class FollowLocationAndParseService extends Service {

    //Location Variables
    Location old_location = null;
    Location current_location = null;
    public final long MIN_TIME = 8000; //5000ms=5s
    public final float MIN_DISTANCE = 10;//10m

    static String device_id;


    Boolean alerted = false;


    //Broadcast Tokens
    public static final String LOST_GPS = "com.lmntrx.LOST_GPS";
    public static final String CANNOT_LOCATE = "com.lmntrx.CANNOT_LOCATE";
    public static final String NO_LOCATION_PERMISSION = "com.lmntrx.NO_LOCATION_PERMISSION";


    public static boolean cancelled = false;

    //Fetch Location
    LocationManager locationManager;

    //LeFo_LocationListener is defined below
    LocationListener locationListener;

    //variable for holding Code
    public static int SESSION_CODE = 1;

    //Object_id
    public String OBJECT_ID;
    public static String FOBJECT_ID;

    int LEADER_LOCATION_UPDATE_INTERVAL = 5000;

    Timer t;


    @SuppressLint("LongLogTag")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(Boss.LOG_TAG + "LOCATION_SERVICE", "Service Started");

        device_id=Boss.getDeviceID(MapsActivity.context);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LeFo_LocationListener();

        try {
            SESSION_CODE = Integer.parseInt(intent.getStringExtra("SESSION_CODE"));
            OBJECT_ID = intent.getStringExtra("OBJECT_ID");
        } catch (NullPointerException e) {
            Log.e(Boss.LOG_TAG, e.getMessage());
            exit();
        }


        cancelled = false;

        try {
            getLeaderLocation();
            getFollowerLocation();
        } catch (NullPointerException e) {
            Log.e(Boss.LOG_TAG, e.getMessage());
            Boss.alertLostConnection(MapsActivity.context, MapsActivity.activity);
        }


        return super.onStartCommand(intent, flags, startId);
    }

    private void getLeaderLocation() {

        TimerTask feedLocation;
        final Handler handler = new Handler();
        t = new Timer();
        feedLocation = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        if (OBJECT_ID != null) {

                            ParseQuery<ParseObject> query = ParseQuery.getQuery(Boss.PARSE_CLASS);
                            query.getInBackground(OBJECT_ID, new GetCallback<ParseObject>() {
                                public void done(ParseObject object, ParseException e) {
                                    if (e == null) {
                                        if (object != null) {
                                            showLeaderLoc(object.getParseGeoPoint(Boss.KEY_LOCATION));
                                        }
                                    } else {
                                        if (!alerted) {
                                                MapsActivity.alertSessionEnd();
                                                alerted = true;
                                                e.printStackTrace();
                                                exit();
                                        }
                                    }
                                }
                            });
                        }
                        //getFollowerLoc(map);
                        Log.d(Boss.LOG_TAG + "TIMER", "Timer Running");
                    }
                });
            }
        };
        t.schedule(feedLocation, LEADER_LOCATION_UPDATE_INTERVAL, LEADER_LOCATION_UPDATE_INTERVAL); //updates every 5s


    }

    public void exit() {

        try {
            t.cancel();
        } catch (NullPointerException e) {
            Log.e(Boss.LOG_TAG, e.getMessage());
        }
        cancelled = true;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(locationListener);
        } else locationManager.removeUpdates(locationListener);

        stopSelf();

    }

    private void showLeaderLoc(ParseGeoPoint parseGeoPoint) {

        MapsActivity.showLeaderLoc(parseGeoPoint);

    }

    @SuppressLint("LongLogTag")
    private void getFollowerLocation() {

        //Applies only for API 23 and above. Cannot handle it directly from a service so i'll do it later. Peace\/
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            alertNoPermission();
            Log.d(Boss.LOG_TAG + "LOCATION_SERVICE", "Permission Denied");
            exit();
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, locationListener);
            current_location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (current_location != null) {
                showFollowerLoc(current_location);
            } else {
                current_location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (current_location != null) {
                    showFollowerLoc(current_location);
                } else {
                    alertCannotLocate();
                }
            }
        }

    }

    private void showFollowerLoc(Location current_location) {

        MapsActivity.showFollowerLoc(current_location);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void alertNoPermission() {
        Intent noPermission = new Intent(NO_LOCATION_PERMISSION);
        FollowLocationAndParseService.this.sendBroadcast(noPermission);
        Log.e(Boss.LOG_TAG, "alertNoPermssion() Called");
    }

    private void alertDisabledGps() {
        Intent disabledGps = new Intent(LOST_GPS);
        FollowLocationAndParseService.this.sendBroadcast(disabledGps);
    }

    private void alertCannotLocate() {
        Intent cannotLocate = new Intent(CANNOT_LOCATE);
        FollowLocationAndParseService.this.sendBroadcast(cannotLocate);
    }


    private class LeFo_LocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {

            if (location != null) {
                current_location = location;
                //Registering new location in database
                if (old_location != current_location) {
                    showFollowerLoc(current_location);
                    old_location = current_location;
                }
            }

        }

        @SuppressLint("LongLogTag")
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

            switch (status) {
                case LocationProvider.AVAILABLE:
                    Log.i(Boss.LOG_TAG + "LocationListener", "GPS Available");
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    Log.e(Boss.LOG_TAG + "LocationListener", "GPS Out Of Service");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.w(Boss.LOG_TAG + "LocationListener", "GPS Temporarily Unavailable");
                    break;
                default:
                    Log.e(Boss.LOG_TAG + "LocationListener", "Status changed");
            }


        }

        @Override
        public void onProviderEnabled(String provider) {

            Log.e(Boss.LOG_TAG + "Location", "Location was enabled");

        }

        @Override
        public void onProviderDisabled(String provider) {
            alertDisabledGps();
            Log.e(Boss.LOG_TAG + "Location", "Location was disabled");

        }
    }

    @Override
    public void onDestroy() {
        if (!cancelled) {
            t.cancel();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.removeUpdates(locationListener);
            } else locationManager.removeUpdates(locationListener);
        }
        super.onDestroy();
    }


    public static void updateFollowerStatus(final Boolean status) {
        Log.d(Boss.LOG_TAG, "Updating Follower Status " + status);
        ParseQuery<ParseObject> queryID = ParseQuery.getQuery(Boss.PARSE_FCLASS);
        queryID.whereEqualTo(Boss.KEY_DEVICE_ID, device_id );
        queryID.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e == null) {
                    for (ParseObject result : parseObjects) {
                        // Retrieving objectId
                        FOBJECT_ID = result.getObjectId();
                    }

                    // Retrieving data from object
                    if (FOBJECT_ID != null) {
                        ParseQuery<ParseObject> query = ParseQuery.getQuery(Boss.PARSE_FCLASS);
                        query.getInBackground(FOBJECT_ID, new GetCallback<ParseObject>() {
                            public void done(ParseObject parseUpdateObject, ParseException e) {
                                if (e == null) {
                                    parseUpdateObject.put(Boss.KEY_isActive, status);
                                    parseUpdateObject.saveInBackground();
                                } else {
                                    Log.e(Boss.LOG_TAG + "FLPS", e.getMessage());
                                }
                            }
                        });
                    } else {
                        Log.e(Boss.LOG_TAG + "FLPS", "Null ObjectID");
                    }


                } else {
                    Log.e(Boss.LOG_TAG + "FLPS", e.getMessage());
                }
            }
        });

    }


}
