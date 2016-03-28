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
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;
import java.util.Random;

//This class contains the main methods and important stuff to control the overall functionality of LeFo. DO NOT PLAY WITH IT!!

public class Boss {

    public static final int SNACKBAR_INDEFINITE_LAUNCH = 0;
    public static final int SNACKBAR_INDEFINITE_CLOSE = 1;

    //Kicked Follower Token
    public static final String KICKED_FOLLOWER = "com.lmntrx.lefo.KICKED_FOLLOWER";
    public static boolean DarkTheme = false;

    public static final String LOG_TAG = "LeFoLog ";

    public static int LEADER_MARKER = 1, FOLLOWER_MARKER = 2;

    static Intent locationService, followService, liveTrackService;

    public static Snackbar snackbar;

    //Parse Authentication Keys
    public final String PARSE_APP_KEY = "U4lYViqyMsMmvicbKzvKWLV4mkOJN6VfPbtfvHmp";
    public final String PARSE_CLIENT_KEY = "PPNey0aT3L0LAuj9LuEgBgtSpn4eEALQ5WMJzAM6";
    public final String HEROKU_SERVER_URL = "https://lefo.herokuapp.com/parse/";

    public static boolean isParseInitialised = false;

    //Parse Class Name
    public static final String PARSE_CLASS = "LeFo_DB";
    public static final String PARSE_FOLLOWERS_CLASS = "Followers";
    public static final String PARSE_BLACKLIST_CLASS = "BlackList";

    //Parse Keys
    public static final String KEY_QRCODE = "QR_CODE";
    public static final String KEY_CON_CODE = "Con_Code";
    public static final String KEY_DEVICE = "deviceName";
    public static final String KEY_DEVICE_ID = "deviceID";
    public static final String KEY_isActive = "isActive";
    public static final String KEY_LOCATION = "LOCATION";

    public static String OBJECT_ID;

    static Notification notification = null;
    static NotificationManager notificationManager = null;

    public static boolean notified = false;

    public Boss() {
        snackbar = Snackbar.make(Home.HOME_ACTIVITY.findViewById(R.id.leadBTN), "", Snackbar.LENGTH_SHORT);
    }


    //Internet Connectivity Status Check Function
    public static boolean isNetworkAvailable(Context con) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) con.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    //Check is GPS is enabled
    public boolean isGpsEnabled(Context context) {
        final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        String user = Build.USER;
        if (model.startsWith(manufacturer)) {
            return capitalize(user + model);
        } else {
            return capitalize(user + " " + manufacturer + " " + model);
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
        if (!isParseInitialised) {
            Parse.initialize(new Parse.Configuration.Builder(con)
                            .applicationId(PARSE_APP_KEY)
                            .clientKey(PARSE_CLIENT_KEY)
                            .server(HEROKU_SERVER_URL)
                            .build()
            );
            isParseInitialised = true;
        }
    }

    //Random Code Generator
    public static int genLeFoCode() {
        Random random = new Random();
        int min = 999, max = 99999999;
        return random.nextInt((max - min + 1) + min);
    }

    public void startSession(Context context, String session_code) {
        locationService = new Intent(context, LeadLocationAndParseService.class);
        locationService.putExtra("SESSION_CODE", session_code);
        context.startService(locationService);
    }

    public static void deleteSession(Context context) {
        try {
            context.stopService(locationService);
        } catch (NullPointerException e) {
            Log.e(Boss.LOG_TAG, e.getMessage() + " ");
        }
        removeNotification();
    }

    public static void removeNotification() {
        try {
            notificationManager.cancelAll();
            Lead.canRefresh = true;
        } catch (Exception e) {

            Log.e(Boss.LOG_TAG, e.getMessage() + " ");
        }
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void notifySessionRunning(Context context) {
        Lead.canRefresh = false;
        //Intent for direct action from notification
        Intent deleteIntent = new Intent(context, CloseLeFoSessionReceiver.class);
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //scaling bitmap
        Bitmap bm = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.lefo_icon),
                context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height),
                true);

        Intent contentIntent = new Intent(context, Lead.class);
        PendingIntent contentPendingIntent = PendingIntent.getBroadcast(context, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        Notification.Builder builder = new Notification.Builder(context.getApplicationContext());
        builder.setContentTitle("LeFo Session Running");
        builder.setContentText("Session Code:" + Lead.SESSION_CODE);
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
        notified = true;
    }

    //EnableGPS Dialog
    public static void buildAlertMessageNoGps(final Context context) {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(context, android.R.style.Theme_Material_Light_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(context);
        }
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
        AlertDialog alert = builder.create();
        alert.show();
    }


    public static void buildAlertMessageLostGps(final Context context, final Activity activity) {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(context, android.R.style.Theme_Material_Light_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(context);
        }
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
        AlertDialog alert = builder.create();
        alert.show();
    }

    public static void askPermission(String name, Activity activity) {

        switch (name) {
            case "LOCATION": {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        0);
                break;
            }
            case "CAMERA": {
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.CAMERA},
                        0);
                break;
            }
        }
    }

    public static void revertFAB() {
        try {
            Lead.fab.setImageResource(R.drawable.ic_media_play);
        } catch (NullPointerException e) {
            Log.e(Boss.LOG_TAG, e.getMessage());
        }
    }

    public static void startFollowSession(String session_code, String object_id, Context context) {

        followService = new Intent(context, FollowLocationAndParseService.class);
        followService.putExtra("SESSION_CODE", session_code);
        followService.putExtra("LEADER_OBJECT_ID", object_id);
        context.startService(followService);

    }

    public static void closeFollowSession(Context context) {
        try {
            context.stopService(followService);
        } catch (NullPointerException e) {
            Log.e(Boss.LOG_TAG, "Service was not started");
        }
    }

    public static void registerFollower(String SESSION_CODE, Boolean isActive, Context context) {

        ParseObject parseObject = new ParseObject(PARSE_FOLLOWERS_CLASS);
        parseObject.put(KEY_CON_CODE, SESSION_CODE);
        parseObject.put(KEY_DEVICE, getDeviceName());
        parseObject.put(KEY_isActive, isActive);
        parseObject.put(KEY_DEVICE_ID, getDeviceID(context));
        parseObject.saveInBackground();

    }

    public static String getDeviceID(Context context) {

        DeviceUuidFactory deviceUuidFactory = new DeviceUuidFactory(context);

        return deviceUuidFactory.getDeviceUuid().toString();

    }

    public static void alertLostConnection(final Context context, final Activity activity) {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(context, android.R.style.Theme_Material_Light_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(context);
        }
        builder.setMessage("Sorry, Connection was lost.")
                .setCancelable(false)
                .setTitle("Session Interrupted")
                .setNegativeButton("Close Session", new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                dialog.cancel();
                                if (context == Lead.CON) {
                                    try {

                                        Boss.deleteSession(context);

                                    } catch (Exception e) {
                                        try {
                                            removeNotification();
                                        } catch (Exception e1) {
                                            System.exit(0);
                                        }
                                    }

                                } else {
                                    Boss.closeFollowSession(context);
                                }
                                activity.finish();
                            }
                        }

                );
        AlertDialog alert = builder.create();
        alert.show();


    }

    public static void buildAlertMessageSessionInterrupted(final Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(context, android.R.style.Theme_Material_Light_Dialog_Alert);
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
        } else {

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

    public static void inform(String msg, View rootView, int durationChoice) {
        switch (durationChoice) {
            case SNACKBAR_INDEFINITE_LAUNCH:
                snackbar = Snackbar.make(rootView, msg, Snackbar.LENGTH_INDEFINITE);
                snackbar.show();
                break;
            case SNACKBAR_INDEFINITE_CLOSE:
                try {
                    if (snackbar.isShown())
                        snackbar.dismiss();
                } catch (Exception e) {
                    Log.e(Boss.LOG_TAG, e.getMessage());
                }
                break;
            default:
                snackbar = Snackbar.make(rootView, msg, Snackbar.LENGTH_LONG);
                snackbar.show();
                break;
        }

    }

    public static void kickUser(String selectedDeviceID, String selectedDeviceName, String sessionCode, Activity activity) {

        registerInBlackList(sessionCode, selectedDeviceID, selectedDeviceName);

        deleteFollowerWithDevice(selectedDeviceID, activity);


    }

    private static void deleteFollowerWithDevice(String selectedDeviceID, final Activity activity) {

        ParseQuery<ParseObject> query = new ParseQuery<>(Boss.PARSE_FOLLOWERS_CLASS);
        query.whereEqualTo(Boss.KEY_DEVICE_ID, selectedDeviceID);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> results, com.parse.ParseException e) {
                if (results != null) {
                    if (e == null) {
                        for (ParseObject result : results) {
                            if (result.getString(Boss.KEY_CON_CODE).equals(Lead.SESSION_CODE)) {
                                try {
                                    result.delete();
                                    alertKickedUser(activity);
                                    Followers.progressBar.setVisibility(View.INVISIBLE);
                                } catch (ParseException e1) {
                                    e1.printStackTrace();
                                }
                                result.saveInBackground();
                            }

                        }
                    } else {
                        e.printStackTrace();
                        System.out.print(e.getMessage());
                    }
                }
            }
        });

    }

    private static void alertKickedUser(Activity activity) {

        Intent kickedUser = new Intent(KICKED_FOLLOWER);
        activity.sendBroadcast(kickedUser);

    }

    private static void registerInBlackList(String sessionCode, String selectedDeviceID, String selectedDeviceName) {

        ParseObject parseObject = new ParseObject(PARSE_BLACKLIST_CLASS);
        parseObject.put(KEY_CON_CODE, sessionCode);
        parseObject.put(KEY_DEVICE, selectedDeviceName);
        parseObject.put(KEY_DEVICE_ID, selectedDeviceID);
        parseObject.saveInBackground();


    }

    public void startLiveTrackSession(Context context, int liveTrackCode, int i) {

        liveTrackService = new Intent(context, LiveTrackLocationAndParseService.class);
        liveTrackService.putExtra("LIVE_TRACK_CODE", liveTrackCode);
        liveTrackService.putExtra("CALL_FROM", i);
        context.startService(liveTrackService);

    }

    public void stopLiveTrackSession(Context context) {

        try {
            context.stopService(liveTrackService);
        } catch (Exception e) {
            Log.e(Boss.LOG_TAG, " " + e.getMessage());
        }

    }
}

