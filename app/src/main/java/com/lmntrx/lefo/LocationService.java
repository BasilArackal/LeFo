package com.lmntrx.lefo;


import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;

public class LocationService extends IntentService {

    public static Boss boss = new Boss();

    //Parse ObjectID
    public static String objectId = null;

    //Location Variables
    Location old_location = null;
    Location current_location = null;
    Location gps_location = null;
    Location network_location = null;
    public final long MIN_TIME = 8000; //5000ms=5s
    public final float MIN_DISTANCE = 5;//2m

    public static boolean isSynced = false;

    //Fetch Location
    LocationManager locationManager;

    //LeFo_LocationListener is defined below
    LocationListener locationListener;

    //Context
    Context CON;

    //variable for holding Code
    public int SESSION_CODE = 1;

    //Variable for checking syncDb() status
    boolean syncStatus = false;


    public LocationService() {

        super(LocationService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.e("LOCATION SERVICE", "Service Started");

        SESSION_CODE = Integer.parseInt(intent.getStringExtra("SESSION_CODE"));


        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(5000);
                        updateCurrentLocation();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).run();

    }

    private void updateCurrentLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LeFo_LocationListener();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, locationListener);

    }


    class LeFo_LocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                current_location = location;
                //Registering new location in database
                if (old_location != current_location) {
                    updateParseDB(SESSION_CODE, location);
                    old_location = current_location;
                }
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {
            //Toast.makeText(CON, "Location was disabled. Please enable it to continue.", Toast.LENGTH_SHORT).show(); //Message when GPS is turned off

        }
    }

    private void updateParseDB(int session_code, Location location) {
        if (!isSynced) {
            syncDB(location);
        } else {
            //Object ID has to be fetched first
            //then using it the object is updated with new location
            ParseQuery<ParseObject> queryID = ParseQuery.getQuery(Boss.PARSE_CLASS);
            queryID.whereEqualTo(Boss.KEY_QRCODE, SESSION_CODE);
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
                                        Log.e("Update", e.getMessage());
                                    }
                                }
                            });
                        }
                    } else {
                        //Incase of an unknown error
                        Log.e("Update", e.getMessage());
                    }
                }
            });
        }
    }

    private void syncDB(Location location) {
        if ((!(SESSION_CODE + "").isEmpty() || SESSION_CODE != 1) && location != null) {
            ParseObject parseObject = new ParseObject(Boss.PARSE_CLASS);
            ParseGeoPoint geoPoint = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
            parseObject.put(Boss.KEY_QRCODE, SESSION_CODE);
            parseObject.put(Boss.KEY_LOCATION, geoPoint);
            parseObject.saveInBackground();
            Log.e("SYNC", "Synced");
            isSynced = true;
        } else {
            Log.e("SYNC", "Code is empty or location is empty");
        }
    }
}
