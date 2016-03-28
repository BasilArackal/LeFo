package com.lmntrx.lefo;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LiveTrackMapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static GoogleMap mMap;

    private static int SESSION_CODE;

    private static String LEADER_OBJECT_ID;
    private static String FOLLOWER_OBJECT_ID;

    static Boolean isActive = true;

    static Boolean moved = false;

    static MarkerOptions leaderMarkerOptions;
    static Marker leaderMarker;

    static String device_id;


    Boolean updated = false;


    Boolean alerted = false;



    int LEADER_LOCATION_UPDATE_INTERVAL = 5000;

    Timer leaderLocationUpdateTimer;

    static Boolean kickAcknowledgement = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_track_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        moved = false;
        isActive = true;
        updated=false;

        SESSION_CODE=getIntent().getIntExtra("SESSION_CODE",-1);
        LEADER_OBJECT_ID =getIntent().getStringExtra("OBJECT_ID");

        device_id=Boss.getDeviceID(this);

        try {
            Boss.registerFollower(SESSION_CODE+"", isActive, LiveTrackMapsActivity.this);
        } catch (NullPointerException e) {
            Log.e(Boss.LOG_TAG, e.getMessage());
        }

        IntentFilter lostConnection = new IntentFilter();
        lostConnection.addAction(ConnectivityReporter.CONNECTION_STATUS_TOKEN_NEGATIVE);
        registerReceiver(lostConnectionBR, lostConnection);

        IntentFilter connectionResumed = new IntentFilter();
        connectionResumed.addAction(ConnectivityReporter.CONNECTION_STATUS_TOKEN_POSITIVE);
        registerReceiver(connectionResumedBR, connectionResumed);

        getLeaderLocation();



    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //scaling bitmap
        Bitmap leaderIcon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(LiveTrackMapsActivity.this.getResources(), R.drawable.bluemarker),
                LiveTrackMapsActivity.this.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                LiveTrackMapsActivity.this.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height),
                true);


        mMap.getUiSettings().setMapToolbarEnabled(false);

        leaderMarkerOptions = new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(leaderIcon))
                .title("Leader")
                .flat(true)
                .anchor(0.5f, 0)
                .rotation(0);

        LatLng sydney = new LatLng(-34, 151);
        leaderMarkerOptions.position(sydney);
        leaderMarker = mMap.addMarker(leaderMarkerOptions);
    }

    private void getLeaderLocation() {

        TimerTask feedLocation;
        final Handler handler = new Handler();
        leaderLocationUpdateTimer = new Timer();
        feedLocation = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {

                        if (!kickAcknowledgement)
                            checkIsKicked();

                        if (LEADER_OBJECT_ID != null) {

                            ParseQuery<ParseObject> query = ParseQuery.getQuery(Boss.PARSE_CLASS);
                            query.getInBackground(LEADER_OBJECT_ID, new GetCallback<ParseObject>() {
                                public void done(ParseObject object, ParseException e) {
                                    if (e == null) {
                                        if (object != null) {
                                            showLeaderLoc(object.getParseGeoPoint(Boss.KEY_LOCATION));
                                        }
                                    } else {
                                        if (!alerted) {
                                            alertSessionEnd();
                                            alerted = true;
                                            e.printStackTrace();
                                            //exit();
                                        }
                                    }
                                }
                            });
                        }
                        Log.d(Boss.LOG_TAG + "TIMER", "Timer Running");
                    }
                });
            }
        };
        leaderLocationUpdateTimer.schedule(feedLocation, LEADER_LOCATION_UPDATE_INTERVAL, LEADER_LOCATION_UPDATE_INTERVAL); //updates every 5s


    }

    private void checkIsKicked() {

        ParseQuery<ParseObject> query = ParseQuery.getQuery(Boss.PARSE_BLACKLIST_CLASS);
        query.whereEqualTo(Boss.KEY_DEVICE_ID, device_id);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                for (ParseObject object : list) {
                    if (object.getString(Boss.KEY_CON_CODE).equals(SESSION_CODE + "")) {
                        alertKicked();
                        kickAcknowledgement = true;
                    }
                }
            }
        });

    }

    private void alertKicked() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(LiveTrackMapsActivity.this, android.R.style.Theme_Material_Light_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(LiveTrackMapsActivity.this);
        }
        builder.setCancelable(false)
                .setTitle("Kicked")
                .setMessage("Sorry, You were kicked")
                .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                       LiveTrackMapsActivity.this.finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

    }

    public static void showLeaderLoc(ParseGeoPoint leaderLoc) {
        LatLng leaderLocation = new LatLng(leaderLoc.getLatitude(), leaderLoc.getLongitude());
        Log.d(Boss.LOG_TAG, "showing leader loc");
        setMarker(mMap, leaderLocation);
    }

    private static void setMarker(GoogleMap mMap, LatLng location) {


                leaderMarker.setPosition(location);
                if (!moved) {
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.builder().target(location).zoom(16.5f).build()));
                    moved = true;
                }

    }

    @Override
    protected void onResume() {

        isActive=true;

        updateFollowerStatus(true);

        super.onResume();
    }

    public static void updateFollowerStatus(final Boolean status) {
        Log.d(Boss.LOG_TAG, "Updating Follower Status " + status);
        ParseQuery<ParseObject> queryID = ParseQuery.getQuery(Boss.PARSE_FOLLOWERS_CLASS);
        queryID.whereEqualTo(Boss.KEY_DEVICE_ID, device_id);
        queryID.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e == null) {
                    for (ParseObject result : parseObjects) {
                        // Retrieving objectId
                        FOLLOWER_OBJECT_ID = result.getObjectId();
                    }

                    // Retrieving data from object
                    if (FOLLOWER_OBJECT_ID != null) {
                        ParseQuery<ParseObject> query = ParseQuery.getQuery(Boss.PARSE_FOLLOWERS_CLASS);
                        query.getInBackground(FOLLOWER_OBJECT_ID, new GetCallback<ParseObject>() {
                            public void done(ParseObject parseUpdateObject, ParseException e) {
                                if (e == null) {
                                    parseUpdateObject.put(Boss.KEY_isActive, status);
                                    parseUpdateObject.saveInBackground();
                                } else {
                                    Log.e(Boss.LOG_TAG, e.getMessage());
                                }
                            }
                        });
                    } else {
                        Log.e(Boss.LOG_TAG, "Null ObjectID");
                    }


                } else {
                    Log.e(Boss.LOG_TAG, e.getMessage());
                }
            }
        });

    }

    @Override
    protected void onPause() {
        isActive=false;
        updateFollowerStatus(false);
        super.onPause();
    }

    @Override
    protected void onDestroy() {


        ConnectivityReporter.SURRENDER = true;

        isActive=false;
        updateFollowerStatus(false);

        kickAcknowledgement=false;

        leaderLocationUpdateTimer.cancel();


        unregisterReceiver(lostConnectionBR);
        unregisterReceiver(connectionResumedBR);

        super.onDestroy();
    }

    public void alertDisconnectSession() {

        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(LiveTrackMapsActivity.this, android.R.style.Theme_Material_Light_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(LiveTrackMapsActivity.this);
        }
        builder.setCancelable(false)
                .setTitle("Disconnect")
                .setMessage("Do you want to disconnect from this session?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        updateFollowerStatus(false);
                        updated = true;
                        LiveTrackMapsActivity.this.finish();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    void alertSessionEnd() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(LiveTrackMapsActivity.this, android.R.style.Theme_Material_Light_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(LiveTrackMapsActivity.this);
        }
        builder.setCancelable(false)
                .setTitle("Session Ended")
                .setMessage("This LeFo Session was closed")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        isActive=false;
                        updateFollowerStatus(false);
                        leaderLocationUpdateTimer.cancel();
                        LiveTrackMapsActivity.this.finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }


    @Override
    public void onBackPressed() {
        alertDisconnectSession();
    }

    private BroadcastReceiver lostConnectionBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Boss.inform("Lost Connection", LiveTrackMapsActivity.this.findViewById(R.id.map), Boss.SNACKBAR_INDEFINITE_LAUNCH);
        }
    };

    private BroadcastReceiver connectionResumedBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Boss.inform("Connected", LiveTrackMapsActivity.this.findViewById(R.id.map), Boss.SNACKBAR_INDEFINITE_CLOSE);
        }
    };

}
