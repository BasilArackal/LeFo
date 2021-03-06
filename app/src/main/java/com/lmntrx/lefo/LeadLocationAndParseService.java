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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;

/*
 * Created by livin on 13/1/16.
 */
public class LeadLocationAndParseService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    //Transaction Tokens
    public static final String LOST_GPS = "com.lmntrx.lefo.LOST_GPS";
    public static final String CANNOT_LOCATE = "com.lmntrx.lefo.CANNOT_LOCATE";
    public static final String NO_LOCATION_PERMISSION = "com.lmntrx.lefo.NO_LOCATION_PERMISSION";
    public static final String GOT_YA = "com.lmntrx.lefo.GOT_YA";
    public static final String SESSION_INTERRUPTED = "com.lmntrx.lefo.SESSION_INTERRUPTED";

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

    //test
    GoogleApiClient googleApiClient = null;


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
        } catch (NullPointerException e) {
            Boss.removeNotification();
            Log.e(Boss.LOG_TAG, e.getMessage() + " ");
        }

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        googleApiClient.connect();


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LeFo_LocationListener();


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            alertNoPermission();
            Log.d(Boss.LOG_TAG + "LOCATION_SERVICE", "Permission Denied");
            exit();
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, locationListener);
            current_location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (current_location != null) {
                try {
                    syncDB(current_location);
                } catch (IllegalArgumentException e) {
                    Log.e(Boss.LOG_TAG, e.getMessage());
                    exit();
                }
            } else {
                current_location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (current_location != null) {
                    try {
                        syncDB(current_location);
                    } catch (IllegalArgumentException e) {
                        Log.e(Boss.LOG_TAG, e.getMessage());
                        exit();
                    }
                } else {
                    alertCannotLocate();
                }
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }


    public void exit() {
        googleApiClient.disconnect();
        Log.d(Boss.LOG_TAG, "exit() called");
        unRegisterAllFollowers();
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

    private void unRegisterAllFollowers() {


        ParseQuery<ParseObject> query = new ParseQuery<>(Boss.PARSE_BLACKLIST_CLASS);
        query.whereEqualTo(Boss.KEY_CON_CODE, Lead.SESSION_CODE);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> results, com.parse.ParseException e) {
                if (results != null) {
                    if (e == null) {
                        for (ParseObject result : results) {

                            try {
                                result.delete();
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
            }
        });


        ParseQuery<ParseObject> followerDetailsQuery = new ParseQuery<>(Boss.PARSE_FOLLOWERS_CLASS);
        followerDetailsQuery.whereEqualTo(Boss.KEY_CON_CODE, Lead.SESSION_CODE);
        followerDetailsQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> results, com.parse.ParseException e) {
                if (results != null) {
                    if (e == null) {
                        for (ParseObject result : results) {
                            try {
                                result.delete();
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
            }
        });


    }

    private void alertNoPermission() {
        Intent noPermission = new Intent(NO_LOCATION_PERMISSION);
        LeadLocationAndParseService.this.sendBroadcast(noPermission);
        Log.e(Boss.LOG_TAG, "alertNoPermission() Called");
    }

    @SuppressLint("LongLogTag")
    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            alertNoPermission();
            Log.d(Boss.LOG_TAG, "Location Permission Denied");
            exit();
        } else
            current_location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(Boss.LOG_TAG, "Suspended GoogleAPIClient Connection");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(Boss.LOG_TAG, "GoogleAPIClient Connection Failed");
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

    private void alertSessionInterrupted() {
        Intent sessionInterrupted = new Intent(SESSION_INTERRUPTED);
        LeadLocationAndParseService.this.sendBroadcast(sessionInterrupted);
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
                        //In case of an unknown error
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
            } catch (NullPointerException e) {
                Log.e(Boss.LOG_TAG, e.getMessage() + " ");
                Boss.removeNotification();
                alertSessionInterrupted();
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
        ParseQuery<ParseObject> query = new ParseQuery<>(Boss.PARSE_CLASS);
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
        } catch (NullPointerException e) {
            Log.e(Boss.LOG_TAG, e.getMessage());
        }
    }


}
