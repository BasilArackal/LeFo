package com.lmntrx.lefo;

import android.annotation.TargetApi;
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
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.parse.Parse;

import java.util.Random;

//This class contains the main methods and important stuff to control the overall functionality of LeFo. DO NOT PLAY WITH IT!!

public class Boss {

    public static final String LOG_TAG="LeFoLog ";

    static Intent locationService;

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
    public static final String KEY_isActive = "isActive";
    public static final String KEY_LOCATION = "LOCATION";

    Notification notification=null;
    static NotificationManager notificationManager=null;


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

    public String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        String no=Build.ID;
        if (model.startsWith(manufacturer)) {
            return capitalize(model) + " " + no;
        } else {
            return capitalize(manufacturer) + " " + model+ " " + no;
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
        //locationService=new Intent(context,LeadLocationAndParseService.class);
        locationService=new Intent(context,LeadLocationAndParseService.class);
        locationService.putExtra("SESSION_CODE",session_code);
        context.startService(locationService);
        notifySessionRunning(context);
    }

    public static void deleteSession() {
        //LeadLocationAndParseService.stop=true;
        LeadLocationAndParseService.stop=true;
        removeNotification();
    }

    public static void removeNotification() {
        notificationManager.cancelAll();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void notifySessionRunning(Context context){

        //Intent for direct action from notification
        Intent deleteIntent=new Intent(context,CloseLeFoSessionReceiver.class);
        PendingIntent deletePendingIntent=PendingIntent.getBroadcast(context,0,deleteIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        //scaling bitmap
        Bitmap bm = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.lefo_icon),
                context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height),
                true);
        Notification.Builder builder = new Notification.Builder(context.getApplicationContext());
        builder.setContentTitle("LeFo Session Running");
        builder.setContentText("Session Code:" + Lead.SESSION_CODE);
       // builder.setSubText("Tap to quit session");
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel,"End Session",deletePendingIntent);
        builder.setTicker("LeFo Session Running");
        builder.setSmallIcon(R.drawable.ic_media_play);
        builder.setLargeIcon(bm);
        builder.setAutoCancel(false);
        builder.setOngoing(true);
        builder.setPriority(Notification.PRIORITY_HIGH);
        notification = builder.build();
        notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notification);
    }

    //EnableGPS Dialog
    public static void buildAlertMessageNoGps(final Context context) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("Your GPS seems to be disabled, please enable it to continue.")
                .setCancelable(false)
                .setTitle("Session Interrupted")
                .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        context.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }


    public static void buildAlertMessageLostGps(final Context context) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("Your GPS seems to be disabled, please enable it to continue.")
                .setCancelable(false)
                .setTitle("Location Turned Off")
                .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        context.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("Close Session", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                        Boss.deleteSession();
                        Lead.currentLeadActivity.finish();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public static void quitLocationService(Context context){
        context.stopService(locationService);
    }

}
