package com.lmntrx.lefo;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Vibrator;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class Follow extends AppCompatActivity {

    public String SESSION_CODE = null;
    EditText codeTXT;
    Activity thisActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.RetainTheme(this);
        setContentView(R.layout.activity_follow);
        codeTXT = (EditText) findViewById(R.id.codeTxt);

        startService(new Intent(this, ConnectivityReporter.class));

        thisActivity=this;


        IntentFilter lostConnection = new IntentFilter();
        lostConnection.addAction(ConnectivityReporter.CONNECTION_STATUS_TOKEN_NEGATIVE);
        registerReceiver(lostConnectionBR, lostConnection);

        IntentFilter connectionResumed = new IntentFilter();
        connectionResumed.addAction(ConnectivityReporter.CONNECTION_STATUS_TOKEN_POSITIVE);
        registerReceiver(connectionResumedBR, connectionResumed);





    }

    public void startJourney(View view) {
        try {
            SESSION_CODE = codeTXT.getText() + "";
            verifyAndStart();
        } catch (NullPointerException e) {
            Log.e(Boss.LOG_TAG + "Follow", "NullPointerException Code Empty");
            inform("Please enter a session code");
        }
    }

    private void verifyAndStart() {
        if (!SESSION_CODE.isEmpty()) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    int status = Boss.verifySessionCode(SESSION_CODE);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (status == Boss.SESSION_APPROVED) {
                        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                        vibrator.vibrate(50);
                        startMaps(SESSION_CODE);
                    } else {
                        inform("Enter a valid LeFo Session Code");
                    }
                }
            });
            thread.run();
        } else {
            //Toast.makeText(this,"Please enter a session code",Toast.LENGTH_LONG).show();
            inform("Please enter a session code");
        }
    }

    private void startMaps(String sessionCode) {
        Intent maps = new Intent(this, MapsActivity.class);
        maps.putExtra("SESSION_CODE", sessionCode);
        startActivity(maps);
        Follow.this.finish();
    }

    public void openQRScanner(View view) {
        startActivity(new Intent(this, Scanner.class));
    }

    private void inform(String msg) {
        View view = findViewById(R.id.codeTxt).getRootView();
        Snackbar snackbar = Snackbar.make(view, msg, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    private BroadcastReceiver lostConnectionBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Boss.inform("Lost Connection", thisActivity.findViewById(R.id.codeTxt), 0);
        }
    };

    private BroadcastReceiver connectionResumedBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Boss.inform("Connected", thisActivity.findViewById(R.id.codeTxt), 1);
        }
    };


    @Override
    protected void onDestroy() {

        unregisterReceiver(lostConnectionBR);
        unregisterReceiver(connectionResumedBR);


        super.onDestroy();
    }
}
