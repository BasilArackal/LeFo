package com.lmntrx.lefo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by livin on 11/1/16.
 */
public class Control {

    //Internet Connectivity Status Check Function
    public boolean isNetworkAvailable(Context con) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) con.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
