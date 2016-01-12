package com.lmntrx.lefo;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;
import java.util.Random;

//This class contains the main methods and important stuff to control the overall functionality of LeFo. DO NOT PLAY WITH IT!!

public class Boss {

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
    public int genLeFoCode() {
        Random random = new Random();
        int min = 999, max = 99999999;
        return random.nextInt((max - min + 1) + min);
    }

    public void startSession(Context con, String session_code) {
        Intent locationService=new Intent(con,LocationService.class);
        locationService.putExtra("SESSION_CODE",session_code);
        con.startService(locationService);
    }

    public void deleteSession() {
        LocationService.stop=true;
    }



}
