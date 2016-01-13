package com.lmntrx.lefo;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Service;
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

import java.security.BasicPermission;
import java.util.List;

public class LeadLocationAndParseService extends IntentService {


    //GPS LOST Token
    public static final String LOST_GPS = "com.lmntrx.Lead";

    //Parse ObjectID
    public static String objectId = null;

    public static boolean stop = false;

    //Location Variables
    Location old_location = null;
    Location current_location = null;
    public final long MIN_TIME = 3000; //5000ms=5s
    public final float MIN_DISTANCE = 3;//10m

    public static boolean isSynced = false;

    //Fetch Location
    LocationManager locationManager;

    //LeFo_LocationListener is defined below
    LocationListener locationListener;

    //variable for holding Code
    public int SESSION_CODE = 1;


    public LeadLocationAndParseService() {

        super(LeadLocationAndParseService.class.getName());
    }

    @SuppressLint("LongLogTag")
    @Override
    protected void onHandleIntent(Intent intent) {

        Log.d(Boss.LOG_TAG + "LOCATION_SERVICE", "Service Started");

        SESSION_CODE = Integer.parseInt(intent.getStringExtra("SESSION_CODE"));

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LeFo_LocationListener();

        final Context context=this;


        new Thread(new Runnable() {
            @Override
            public void run() {
                while (!isSynced) {
                    try {
                        if (!stop)
                            updateCurrentLocation();
                        else {
                            exit();
                            break;
                        }
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).run();


            new Thread(new Runnable() {
                @Override
                public void run() {
                    //Applies only for API 23 and above. Cannot handle it directly from a service so i'll do it later. Peace\/
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                         return;
                    }else{
                         locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, locationListener);
                    }
                }
            }).run();

    }

    public void exit() {
        stop=false;
        isSynced=false;
        objectId=null;
        LeadLocationAndParseService.this.stopSelf();
    }

    private void updateCurrentLocation() {
        Log.d(Boss.LOG_TAG + "Update", "Update Current Location Called");
        if (!isSynced) {

            //Applies only for API 23 and above. Cannot handle it directly from a service so i'll do it later. Peace\/
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.

                //return;
            }else {
                if ((current_location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)) == null) {
                    if ((current_location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)) == null) {
                        Log.e(Boss.LOG_TAG + "Location", "Cannot Locate You");
                    } else
                        syncDB(current_location);
                } else
                    syncDB(current_location);
            }
        }

    }


    class LeFo_LocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                current_location = location;
                //Registering new location in database
                if (old_location != current_location) {
                    updateParseDB(SESSION_CODE, location);
                    Log.e(Boss.LOG_TAG + "Chakkara", "Update called manushya!!");
                    old_location = current_location;
                }
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
            Log.e(Boss.LOG_TAG + "Chakkara", "Status changed manushya!!");

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {
            alertDisabledGps();
            Log.e(Boss.LOG_TAG + "Location", "Location was disabled. Please enable it to continue.");
        }
    }

    private void alertDisabledGps() {
        Intent brIntent = new Intent(LOST_GPS);
        LeadLocationAndParseService.this.sendBroadcast(brIntent);
    }

    private void updateParseDB(int session_code, Location location) {

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

    private void syncDB(Location location) {
        if ((!(SESSION_CODE + "").isEmpty() || SESSION_CODE != 1) && location != null) {
            Log.d(Boss.LOG_TAG + "SYNC", "Syncing to ParseDB");
            ParseObject parseObject = new ParseObject(Boss.PARSE_CLASS);
            ParseGeoPoint geoPoint = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
            parseObject.put(Boss.KEY_QRCODE, SESSION_CODE);
            parseObject.put(Boss.KEY_LOCATION, geoPoint);
            parseObject.saveInBackground();
            Log.d(Boss.LOG_TAG + "SYNC", "Synced");
            isSynced = true;
        } else {
            Log.e(Boss.LOG_TAG + "SYNC", "Code is empty or location is empty");
        }
    }

    @SuppressLint("LongLogTag")
    @Override
    public void onDestroy() {
        deleteSession();
        super.onDestroy();
        Log.d(Boss.LOG_TAG + "LOCATION_SERVICE", "Exited");
    }

    private void deleteSession() {
        ParseQuery query = new ParseQuery(Boss.PARSE_CLASS);
        query.whereEqualTo(Boss.KEY_QRCODE, SESSION_CODE);
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
    }
}
