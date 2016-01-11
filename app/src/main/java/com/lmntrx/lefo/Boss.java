package com.lmntrx.lefo;

import android.content.Context;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

//This class contains the main methods and important stuff to control the overall functionality of LeFo. DO NOT PLAY WITH IT!!

public class Boss {

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
}
