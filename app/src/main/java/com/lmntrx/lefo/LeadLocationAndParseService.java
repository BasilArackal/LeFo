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

/**
 * Created by livin on 13/1/16.
 */
public class LeadLocationAndParseService extends Service {


    //Transaction Tokens
    public static final String LOST_GPS = "com.lmntrx.LOST_GPS";
    public static final String CANNOT_LOCATE = "com.lmntrx.CANNOT_LOCATE";
    public static final String NO_LOCATION_PERMISSION = "com.lmntrx.NO_LOCATION_PERMISSION";
    public static final String GOT_YA = "com.lmntrx.GOT_YA";
    public static final String SESSION_INTERRUPTED = "com.lmntrx.SESSION_INTERRUPTED";

    //Parse ObjectID
    public static String objectId = null;

    public static boolean stop = false;

    //Location Variables
    Location old_location = null;
    Location current_location = null;
    public final long MIN_TIME = 8000; //5000ms=5s
    public final float MIN_DISTANCE = 10;//10m

    public static boolean isSynced = false;

    //Fetch Location
    LocationManager locationManager;

    //LeFo_LocationListener is defined below
    LocationListener locationListener;

    //variable for holding Code
    public int SESSION_CODE = 1;

    boolean exitCalled = false;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("LongLogTag")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        isSynced = false;
        stop = false;

        Log.d(Boss.LOG_TAG + "LOCATION_SERVICE", "Service Started");


        try {
            SESSION_CODE = Integer.parseInt(intent.getStringExtra("SESSION_CODE"));
        }catch (NullPointerException e){
            Boss.removeNotification();
            Log.e(Boss.LOG_TAG,e.getMessage());
        }


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LeFo_LocationListener();


        //Applies only for API 23 and above. Cannot handle it directly from a service so i'll do it later. Peace\/
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            alertNoPermission();
            Log.d(Boss.LOG_TAG + "LOCATION_SERVICE", "Permission Denied");
            exit();
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, locationListener);current_location=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (current_location != null){
                try{
                    syncDB(current_location);
                }catch (IllegalArgumentException e){
                    Log.e(Boss.LOG_TAG, e.getMessage());
                    exit();
                }
            }else {
                current_location=locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (current_location != null){
                    try{
                        syncDB(current_location);
                    }catch (IllegalArgumentException e){
                        Log.e(Boss.LOG_TAG,e.getMessage());
                        exit();
                    }
                }else {
                    alertCannotLocate();
                }
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }


    public void exit() {
        Log.d(Boss.LOG_TAG, "exit() called");
        stop = false;
        isSynced = false;
        objectId = null;
        Boss.removeNotification();
        Lead.isSessionOn = false;
        Boss.revertFAB();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.removeUpdates(locationListener);
            } else locationManager.removeUpdates(locationListener);
        deleteSession();
        exitCalled = true;
        LeadLocationAndParseService.this.stopSelf();
    }

    private void alertNoPermission() {
        Intent noPermission = new Intent(NO_LOCATION_PERMISSION);
        LeadLocationAndParseService.this.sendBroadcast(noPermission);
        Log.e(Boss.LOG_TAG, "alertNoPermssion() Called");
    }


    class LeFo_LocationListener implements LocationListener {


        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                current_location = location;
                //Registering new location in database
                if (old_location != current_location) {
                    updateParseDB(SESSION_CODE);
                    old_location = current_location;
                }
            }
        }

        @SuppressLint("LongLogTag")
        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
            switch (i) {
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
        public void onProviderEnabled(String s) {
            Log.e(Boss.LOG_TAG + "Location", "Location was enabled");
        }

        @Override
        public void onProviderDisabled(String s) {
            alertDisabledGps();
            Log.e(Boss.LOG_TAG + "Location", "Location was disabled");
        }
    }

    private void alertDisabledGps() {
        Intent disabledGps = new Intent(LOST_GPS);
        LeadLocationAndParseService.this.sendBroadcast(disabledGps);
    }

    private void alertCannotLocate() {
        Intent cannotLocate = new Intent(CANNOT_LOCATE);
        LeadLocationAndParseService.this.sendBroadcast(cannotLocate);
    }

    private void alertGotYou() {
        Intent cannotLocate = new Intent(GOT_YA);
        LeadLocationAndParseService.this.sendBroadcast(cannotLocate);
        Boss.notifySessionRunning(Lead.CON);
    }

    private void alertSessionInterupted() {
        Intent sessionInterupted = new Intent(SESSION_INTERRUPTED);
        LeadLocationAndParseService.this.sendBroadcast(sessionInterupted);
    }

    private void updateParseDB(int session_code) {

        if (!isSynced) {
            syncDB(current_location);
        } else {
            //Object ID has to be fetched first
            //then using it the object is updated with new location
            Log.d(Boss.LOG_TAG + "Update", "Updating ParseDB");
            ParseQuery<ParseObject> queryID = ParseQuery.getQuery(Boss.PARSE_CLASS);
            queryID.whereEqualTo(Boss.KEY_QRCODE, session_code);
            queryID.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> parseObjects, ParseException e) {
                    if (e == null) {
                        for (ParseObject result : parseObjects) {
                            // Retrieving objectId
                            objectId = result.getObjectId();
                        }
                        // Retrieving data from object
                        if (objectId != null) {
                            ParseQuery<ParseObject> query = ParseQuery.getQuery(Boss.PARSE_CLASS);
                            query.getInBackground(objectId, new GetCallback<ParseObject>() {
                                public void done(ParseObject parseUpdateObject, ParseException e) {
                                    if (e == null) {
                                        ParseGeoPoint newGeoPoint = new ParseGeoPoint(current_location.getLatitude(), current_location.getLongitude());
                                        parseUpdateObject.put(Boss.KEY_LOCATION, newGeoPoint);
                                        parseUpdateObject.saveInBackground();
                                    } else {
                                        Log.e(Boss.LOG_TAG + "Update", e.getMessage());
                                    }
                                }
                            });
                        }
                    } else {
                        //Incase of an unknown error
                        Log.e(Boss.LOG_TAG + "Update", e.getMessage());
                    }
                }
            });
        }
    }

    @SuppressLint("LongLogTag")
    private void syncDB(Location location) {
        if ((!(SESSION_CODE + "").isEmpty() || SESSION_CODE != 1) && location != null) {
            try {
                Log.d(Boss.LOG_TAG + "SYNC", "Syncing to ParseDB");
                ParseObject parseObject = new ParseObject(Boss.PARSE_CLASS);
                ParseGeoPoint geoPoint = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
                parseObject.put(Boss.KEY_QRCODE, SESSION_CODE);
                parseObject.put(Boss.KEY_LOCATION, geoPoint);
                parseObject.saveInBackground();
                Log.d(Boss.LOG_TAG + "SYNC", "Synced");
                isSynced = true;
                alertGotYou();
            }catch (NullPointerException e){
                Log.e(Boss.LOG_TAG,e.getMessage());
                Boss.removeNotification();
                alertSessionInterupted();
                exit();
            }
        } else {
            Log.e(Boss.LOG_TAG + "SYNC", "Code is empty or location is empty");
        }
    }

    @SuppressLint("LongLogTag")
    @Override
    public void onDestroy() {
        if (!exitCalled)
            exit();
        Log.d(Boss.LOG_TAG + "LOCATION_SERVICE", "Service Destroyed");
        super.onDestroy();
    }

    @SuppressLint("LongLogTag")
    private void deleteSession() {
        Log.d(Boss.LOG_TAG + "LOCATION_SERVICE", "deleteSession() called");
        ParseQuery query = new ParseQuery(Boss.PARSE_CLASS);
        query.whereEqualTo(Boss.KEY_QRCODE, SESSION_CODE);

        try {
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> results, com.parse.ParseException e) {
                    if (e == null) {
                        for (ParseObject result : results) {
                            try {
                                result.delete();
                                Log.i(Boss.LOG_TAG + "Deleted Session", result.get(Boss.KEY_QRCODE) + "");
                            } catch (ParseException e1) {
                                e1.printStackTrace();
                            }
                            result.saveInBackground();
                        }
                    } else {
                        e.printStackTrace();
                        System.out.print(e.getMessage());
                    }
                }
            });
        }catch (NullPointerException e){
            Log.e(Boss.LOG_TAG,e.getMessage());
        }
    }


}
