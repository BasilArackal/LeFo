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

/*
 * Created by livin on 9/2/16.
 */
public class LiveTrackLocationAndParseService extends Service {

    int liveTrackCode=1;

    //Location Variables
    Location old_location = null;
    Location current_location = null;
    public final long MIN_TIME = 8000; //5000ms=5s
    public final float MIN_DISTANCE = 10;//10m

    public static boolean isSynced = false;

    static int callFrom=0;

    //Fetch Location
    LocationManager locationManager;

    //LeFo_LocationListener is defined below
    LocationListener locationListener;

    //Parse ObjectID
    public static String objectId = null;


    //Transaction Tokens
    public static final String LOST_GPS = "com.lmntrx.lefo.LOST_GPS";
    public static final String CANNOT_LOCATE = "com.lmntrx.lefo.CANNOT_LOCATE";
    public static final String NO_LOCATION_PERMISSION = "com.lmntrx.lefo.NO_LOCATION_PERMISSION";
    public static final String GOT_YA = "com.lmntrx.lefo.GOT_YA";
    public static final String SESSION_INTERRUPTED = "com.lmntrx.lefo.SESSION_INTERRUPTED";


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Boss.LOG_TAG, "Started LiveTrack Service");

        try {
            liveTrackCode=intent.getIntExtra("LIVE_TRACK_CODE", 1);
            callFrom=intent.getIntExtra("CALL_FROM",0);
        }catch (NullPointerException e){
            Log.e(Boss.LOG_TAG," "+e.getMessage());
        }


        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LeFo_LocationListener();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            alertNoPermission();
            Log.d(Boss.LOG_TAG , "Permission Denied");
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, locationListener);
            current_location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (current_location != null) {
                try {
                    syncDB(current_location);
                } catch (IllegalArgumentException e) {
                    Log.e(Boss.LOG_TAG, e.getMessage());
                }
            } else {
                current_location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (current_location != null) {
                    try {
                        syncDB(current_location);
                    } catch (IllegalArgumentException e) {
                        Log.e(Boss.LOG_TAG, e.getMessage());
                    }
                } else {
                    alertCannotLocate();
                }
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void syncDB(Location location) {
        if ((!(liveTrackCode + "").isEmpty() || liveTrackCode != 1) && location != null) {
            try {
                Log.d(Boss.LOG_TAG + "SYNC", "Syncing to ParseDB");
                ParseObject parseObject = new ParseObject(Boss.PARSE_CLASS);
                ParseGeoPoint geoPoint = new ParseGeoPoint(location.getLatitude(), location.getLongitude());
                parseObject.put(Boss.KEY_QRCODE, liveTrackCode);
                parseObject.put(Boss.KEY_LOCATION, geoPoint);
                parseObject.saveInBackground();
                Log.d(Boss.LOG_TAG + "SYNC", "Synced");
                isSynced = true;
                alertGotYou();
            } catch (NullPointerException e) {
                Log.e(Boss.LOG_TAG, e.getMessage()+" ");
                //Boss.removeNotification();
                alertSessionInterrupted();
                //exit();
            }
        } else {
            Log.e(Boss.LOG_TAG + "SYNC", "Code is empty or location is empty");
        }
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

    @Override
    public void onDestroy() {

        Log.d(Boss.LOG_TAG,"Stopped LiveTrack Service");

        unRegisterAllFollowers();
        isSynced = false;
        objectId = null;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(locationListener);
        } else locationManager.removeUpdates(locationListener);
        deleteSession();
        super.onDestroy();
    }

    @SuppressLint("LongLogTag")
    private void deleteSession() {
        Log.d(Boss.LOG_TAG + "LOCATION_SERVICE", "deleteSession() called");
        ParseQuery<ParseObject> query = new ParseQuery<>(Boss.PARSE_CLASS);
        query.whereEqualTo(Boss.KEY_QRCODE, liveTrackCode);

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


    class LeFo_LocationListener implements LocationListener {


        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                current_location = location;
                //Registering new location in database
                if (old_location != current_location) {
                    updateParseDB(liveTrackCode);
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
        LiveTrackLocationAndParseService.this.sendBroadcast(disabledGps);
    }

    private void alertCannotLocate() {
        Intent cannotLocate = new Intent(CANNOT_LOCATE);
        LiveTrackLocationAndParseService.this.sendBroadcast(cannotLocate);
    }

    private void alertGotYou() {
        if (callFrom!=1){
            Intent cannotLocate = new Intent(GOT_YA);
            LiveTrackLocationAndParseService.this.sendBroadcast(cannotLocate);
        }
    }

    private void alertSessionInterrupted() {
        Intent sessionInterrupted = new Intent(SESSION_INTERRUPTED);
        LiveTrackLocationAndParseService.this.sendBroadcast(sessionInterrupted);
    }

    private void alertNoPermission() {
        Intent noPermission = new Intent(NO_LOCATION_PERMISSION);
        LiveTrackLocationAndParseService.this.sendBroadcast(noPermission);
        Log.e(Boss.LOG_TAG, "alertNoPermission() Called");
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

}
