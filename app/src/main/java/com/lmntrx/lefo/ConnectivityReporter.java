package com.lmntrx.lefo;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

/*
 * Created by livin on 1/2/16.
 */
public class ConnectivityReporter extends IntentService {

    Boolean connected = false;

    public static Boolean SURRENDER=false;

    public static String CONNECTION_STATUS_TOKEN_NEGATIVE="com.lmntrx.lefo.CONNECTION_STATUS_TOKEN_NEGATIVE";
    public static String CONNECTION_STATUS_TOKEN_POSITIVE="com.lmntrx.lefo.CONNECTION_STATUS_TOKEN_POSITIVE";


    public ConnectivityReporter() {
        super(ConnectivityReporter.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        SURRENDER=false;

        Log.d(Boss.LOG_TAG, "Started Connectivity Reporter Service");

        while (!SURRENDER) {
            if (Boss.isNetworkAvailable(getApplicationContext())) {
                if (!connected) {
                    connected = true;
                    broadcastStatus();
                }
            } else {
                if (connected) {
                    connected = false;
                    broadcastStatus();

                }
            }
        }

    }

    private void broadcastStatus() {

        if (!connected){
            Intent intent=new Intent(CONNECTION_STATUS_TOKEN_NEGATIVE);
            ConnectivityReporter.this.sendBroadcast(intent);
        }else {
            Intent intent=new Intent(CONNECTION_STATUS_TOKEN_POSITIVE);
            ConnectivityReporter.this.sendBroadcast(intent);
        }

    }
}
