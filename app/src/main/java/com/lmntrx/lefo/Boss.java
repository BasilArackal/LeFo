package com.lmntrx.lefo;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.GoogleMap;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;
import java.util.Random;

//This class contains the main methods and important stuff to control the overall functionality of LeFo. DO NOT PLAY WITH IT!!

public class Boss {

    public static boolean DarkTheme=false; // <------ THIS SHOULD BE MADE A SHAREDPREFERANCE

    public static final String LOG_TAG="LeFoLog ";

    public static int SESSION_APPROVED=2,VERIFIED_STATUS=1;

    public static int LEADER_MARKER=1, FOLLOWER_MARKER=2;

    static Intent locationService, followService;

    //Parse Authentication Keys
    public final String PARSE_APP_KEY="U4lYViqyMsMmvicbKzvKWLV4mkOJN6VfPbtfvHmp";
    public final String PARSE_CLIENT_KEY="PPNey0aT3L0LAuj9LuEgBgtSpn4eEALQ5WMJzAM6";

    public static boolean isParseInitialised=false;

    //Parse Class Name
    public static final String PARSE_CLASS = "LeFo_DB";
    public static final String PARSE_FCLASS = "Followers";

    //Parse Keys
    public static final String KEY_QRCODE = "QR_CODE";
    public static final String KEY_CON_CODE = "Con_Code";
    public static final String KEY_DEVICE = "deviceName";
    public static final String KEY_DEVICE_ID = "deviceID";
    public static final String KEY_isActive = "isActive";
    public static final String KEY_LOCATION = "LOCATION";

    public static String OBJECT_ID;

    static Notification notification=null;
    static NotificationManager notificationManager=null;

    public static boolean notified=false;

    public static GoogleMap MAP;


    //Internet Connectivity Status Check Function
    public boolean isNetworkAvailable(Context con) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) con.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    //Check is GPS is enabled
    public boolean isGpsEnabled(Context context){
        final LocationManager manager=(LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public void checkGooglePlayServiceStatus(Context context) {
        int statusCode= GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (statusCode!= ConnectionResult.SUCCESS){
            if (GooglePlayServicesUtil.isUserRecoverableError(statusCode)){
                Toast.makeText(context, statusCode + ": Update or install Google play services to continue", Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(context,statusCode+": Google play services not available",Toast.LENGTH_LONG).show();
            }
        }
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    public void initializeParse(Context con) {
        if (!isParseInitialised){
            Parse.initialize(con,PARSE_APP_KEY,PARSE_CLIENT_KEY);
            isParseInitialised=true;
        }
    }

    //Random Code Generator
    public static int genLeFoCode() {
        Random random = new Random();
        int min = 999, max = 99999999;
        return random.nextInt((max - min + 1) + min);
    }

    public void startSession(Context context, String session_code) {
        locationService=new Intent(context,LeadLocationAndParseService.class);
        locationService.putExtra("SESSION_CODE", session_code);
        context.startService(locationService);
    }

    public static void deleteSession(Context context) {
        try {
            context.stopService(locationService);
        }catch (NullPointerException e){
            Log.e(Boss.LOG_TAG,e.getMessage());
        }
        removeNotification();
    }

    public static void removeNotification() {
        try {
            notificationManager.cancelAll();
            Lead.canRefresh=true;
        }catch (Exception e){

            Log.e(Boss.LOG_TAG,e.getMessage());
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void notifySessionRunning(Context context){
        Lead.canRefresh=false;
        //Intent for direct action from notification
        Intent deleteIntent=new Intent(context,CloseLeFoSessionReceiver.class);
        PendingIntent deletePendingIntent=PendingIntent.getBroadcast(context, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //scaling bitmap
        Bitmap bm = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.lefo_icon),
                context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height),
                true);

        Intent contentIntent=new Intent(context,Lead.class);
        PendingIntent contentPendingIntent=PendingIntent.getBroadcast(context,0,contentIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(context.getApplicationContext());
        builder.setContentTitle("LeFo Session Running");
        builder.setContentText("Session Code:" + Lead.SESSION_CODE);
       // builder.setSubText("Tap to quit session");
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "End Session", deletePendingIntent);
        builder.setTicker("LeFo Session Initiated");
        builder.setContentIntent(contentPendingIntent);
        builder.setSmallIcon(R.drawable.ic_media_play);
        builder.setLargeIcon(bm);
        builder.setAutoCancel(false);
        builder.setOngoing(true);
        builder.setPriority(Notification.PRIORITY_HIGH);
        notification = builder.build();
        notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
        notified=true;
    }

    //EnableGPS Dialog
    public static void buildAlertMessageNoGps(final Context context) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("Your GPS seems to be disabled, please enable it to continue.")
                .setCancelable(false)
                .setTitle("Turn On Location")
                .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        context.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }


    public static void buildAlertMessageLostGps(final Context context, final Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("Your GPS seems to be disabled, please enable it to continue.")
                .setCancelable(false)
                .setTitle("Session Interrupted")
                .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        context.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("Close Session", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                        Boss.deleteSession(context);
                        activity.finish();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public static void askPermission(String name,Activity activity) {

        switch (name){
            case "LOCATION":{
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        0); break;
            }
            case "CAMERA":{
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.CAMERA},
                        0); break;
            }
        }
    }

    public static void revertFAB() {
        try {
            Lead.fab.setImageResource(R.drawable.ic_media_play);
        }catch (NullPointerException e){
         Log.e(Boss.LOG_TAG,e.getMessage());
        }
    }

    public static int verifySessionCode(final String session_code) {
        final int integer_code;
        try{
            integer_code=Integer.parseInt(session_code);
        }catch (Exception e){
            return 1;
        }
        ParseQuery<ParseObject> queryID = ParseQuery.getQuery(PARSE_CLASS);
        queryID.whereEqualTo(KEY_QRCODE, integer_code);
        queryID.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e == null) {
                    if (parseObjects.isEmpty()) {
                        VERIFIED_STATUS = 1;
                    } else {
                        for (ParseObject result : parseObjects) {
                            OBJECT_ID = result.getObjectId();
                            VERIFIED_STATUS = 2;
                        }
                    }
                } else {
                    VERIFIED_STATUS = 1;
                }
            }
        });

        return VERIFIED_STATUS;
    }

    public static void startFollowSession(String session_code, String object_id, Context context) {

        followService=new Intent(context,FollowLocationAndParseService.class);
        followService.putExtra("SESSION_CODE", session_code);
        followService.putExtra("OBJECT_ID", object_id);
        context.startService(followService);

    }

    public static void closeFollowSession(Context context) {
        try{
            context.stopService(followService);
        }catch (NullPointerException e){
            Log.e(Boss.LOG_TAG,"Service was not started");
        }
    }

    public static void registerFollower(String SESSION_CODE, Boolean isActive, Context context) {

        ParseObject parseObject = new ParseObject(PARSE_FCLASS);
        parseObject.put(KEY_CON_CODE, SESSION_CODE);
        parseObject.put(KEY_DEVICE, getDeviceName());
        parseObject.put(KEY_isActive, isActive);
        parseObject.put(KEY_DEVICE_ID,getDeviceID(context));
        parseObject.saveInBackground();

    }

    public static String getDeviceID(Context context){

        DeviceUuidFactory deviceUuidFactory=new DeviceUuidFactory(context);

        return deviceUuidFactory.getDeviceUuid().toString();

    }

    public static void alertLostConnection(final Context context, final Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("Sorry, Connection was lost.")
                .setCancelable(false)
                .setTitle("Session Interrupted")
                .setNegativeButton("Close Session", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                        if (context == Lead.CON){
                            try {

                                Boss.deleteSession(context);

                            }catch (Exception e){
                                try {
                                    removeNotification();
                                }catch (Exception e1){
                                    System.exit(0);
                                }
                            }
                            Log.e(Boss.LOG_TAG,"Here Man");

                        }else {
                            Boss.closeFollowSession(context);
                        }
                            activity.finish();
                        }
                    }

                    );
                    final AlertDialog alert = builder.create();
                    alert.show();


                }

    public static void buildAlertMessageSessionInterrupted(final Context context) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("Sorry, Connection was lost.")
                .setCancelable(false)
                .setTitle("Session Interrupted")
                .setNegativeButton("Close Session", new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                dialog.cancel();
                                Lead.currentLeadActivity.finish();
                            }
                        }

                );
        final AlertDialog alert = builder.create();
        alert.show();

    }
}

