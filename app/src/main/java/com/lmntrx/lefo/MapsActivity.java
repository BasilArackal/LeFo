package com.lmntrx.lefo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.support.design.widget.Snackbar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.ParseGeoPoint;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static GoogleMap mMap;

    String SESSION_CODE;
    String OBJECT_ID;

    static MarkerOptions leaderMarkerOptions;
    static MarkerOptions followerMarkerOptions;
    static Marker leaderMarker;
    static Marker followerMarker;

    Boolean updated = false;

    static Context context;
    static Activity activity;

    static Boolean moved = false;

    static Boolean isActive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        context = this;
        activity = this;
        moved = false;
        isActive = true;
        updated = false;


        SESSION_CODE = getIntent().getStringExtra("SESSION_CODE");
        OBJECT_ID = Boss.OBJECT_ID;


        //Intent Filters
        IntentFilter gpsDisabled = new IntentFilter();
        gpsDisabled.addAction(FollowLocationAndParseService.LOST_GPS);
        registerReceiver(gpsDisabledBR, gpsDisabled);

        IntentFilter cannotLocate = new IntentFilter();
        cannotLocate.addAction(FollowLocationAndParseService.CANNOT_LOCATE);
        registerReceiver(cannotLocateBR, cannotLocate);

        IntentFilter noLocationPermission = new IntentFilter();
        noLocationPermission.addAction(FollowLocationAndParseService.NO_LOCATION_PERMISSION);
        registerReceiver(noLocationPermissionBR, noLocationPermission);

        IntentFilter lostConnection = new IntentFilter();
        lostConnection.addAction(ConnectivityReporter.CONNECTION_STATUS_TOKEN_NEGATIVE);
        registerReceiver(lostConnectionBR, lostConnection);

        IntentFilter connectionResumed = new IntentFilter();
        connectionResumed.addAction(ConnectivityReporter.CONNECTION_STATUS_TOKEN_POSITIVE);
        registerReceiver(connectionResumedBR, connectionResumed);

        IntentFilter kicked = new IntentFilter();
        kicked.addAction(FollowLocationAndParseService.KICKED_TOKEN);
        registerReceiver(kickedBR, kicked);

        try {
            Boss.registerFollower(SESSION_CODE, isActive, context);
        } catch (NullPointerException e) {
            Log.e(Boss.LOG_TAG, e.getMessage());
        }

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
        Bitmap leaderIcon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.bluemarker),
                context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height),
                true);

        //scaling bitmap
        Bitmap followerIcon = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.orangemarker),
                context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height),
                true);

        mMap.getUiSettings().setMapToolbarEnabled(false);
        /*mMap.setIndoorEnabled(true);
        mMap.setTrafficEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        mMap.setBuildingsEnabled(true);*/


        leaderMarkerOptions = new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(leaderIcon))
                .title("Leader")
                .flat(true)
                .anchor(0.5f, 0)
                .rotation(0);

        followerMarkerOptions = new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(followerIcon))
                .title("You")
                .flat(true)
                .anchor(0.5f, 0)
                .rotation(0);

        Boss.startFollowSession(SESSION_CODE, OBJECT_ID, this);


        LatLng sydney = new LatLng(-34, 151);
        leaderMarkerOptions.position(sydney);
        leaderMarker = mMap.addMarker(leaderMarkerOptions);
        followerMarkerOptions.position(sydney);
        followerMarker = mMap.addMarker(followerMarkerOptions);

    }

    public static void showLeaderLoc(ParseGeoPoint leaderLoc) {
        LatLng leaderLocation = new LatLng(leaderLoc.getLatitude(), leaderLoc.getLongitude());
        Log.e(Boss.LOG_TAG, "showing leader loc");
        setMarker(mMap, leaderLocation, Boss.LEADER_MARKER);
    }

    public static void showFollowerLoc(Location followerLoc) {
        LatLng followerLocation = new LatLng(followerLoc.getLatitude(), followerLoc.getLongitude());
        Log.e(Boss.LOG_TAG, "showing follower loc");
        setMarker(mMap, followerLocation, Boss.FOLLOWER_MARKER);
    }

    private static void setMarker(GoogleMap mMap, LatLng location, int i) {

        switch (i) {

            case 1:
                leaderMarker.setPosition(location);
                if (!moved) {
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.builder().target(location).zoom(16.5f).build()));
                    moved = true;
                }
                break;

            case 2:
                followerMarker.setPosition(location);
                break;


            default:

        }

    }

    @Override
    protected void onPause() {


        isActive = false;
        if (!updated) {
            FollowLocationAndParseService.updateFollowerStatus(false);
            updated = true;
        }


        super.onPause();
    }

    public static void alertSessionEnd() {
        new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle("Session Ended")
                .setMessage("This LeFo Session was closed by Leader")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        activity.finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    protected void onDestroy() {

        ConnectivityReporter.SURRENDER = true;

        isActive = false;
        if (!updated) {
            FollowLocationAndParseService.updateFollowerStatus(false);
            updated = true;
        }


        Boss.closeFollowSession(this);
        unregisterReceiver(cannotLocateBR);
        unregisterReceiver(gpsDisabledBR);
        unregisterReceiver(noLocationPermissionBR);
        unregisterReceiver(lostConnectionBR);
        unregisterReceiver(connectionResumedBR);
        unregisterReceiver(kickedBR);
        super.onDestroy();
    }

    private BroadcastReceiver gpsDisabledBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Boss.buildAlertMessageLostGps(context, activity);
        }
    };

    private BroadcastReceiver cannotLocateBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            inform("Could not locate you, try moving your device");
        }
    };

    private BroadcastReceiver kickedBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showIsKickedAlertDialog();
        }
    };

    private void showIsKickedAlertDialog() {

        new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle("Kicked")
                .setMessage("Sorry, You were kicked by the leader")
                .setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        MapsActivity.activity.finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();

    }

    private BroadcastReceiver noLocationPermissionBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            inform("Sorry, Session Failed. Please grant permission to access location and try again.");
            Boss.askPermission("LOCATION", activity);
        }
    };

    private void inform(String msg) {
        View view = findViewById(R.id.map).getRootView();
        Snackbar snackbar = Snackbar.make(view, msg, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    @Override
    protected void onResume() {


        isActive = true;
        updated = false;
        FollowLocationAndParseService.updateFollowerStatus(true);

        context = this;
        activity = this;

        super.onResume();
    }

    public void alertDisconnectSession() {
        new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle("Disconnect")
                .setMessage("Do you want to disconnect from this session?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        FollowLocationAndParseService.updateFollowerStatus(false);
                        updated = true;
                        MapsActivity.activity.finish();
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

    @Override
    public void onBackPressed() {
        alertDisconnectSession();
    }


    private BroadcastReceiver lostConnectionBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Boss.inform("Lost Connection", activity.findViewById(R.id.map), Boss.SNACKBAR_INDEFINITE_LAUNCH);
        }
    };

    private BroadcastReceiver connectionResumedBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Boss.inform("Connected", activity.findViewById(R.id.map), Boss.SNACKBAR_INDEFINITE_CLOSE);
        }
    };

}
