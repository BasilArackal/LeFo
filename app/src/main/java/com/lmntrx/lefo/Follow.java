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
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;

public class Follow extends AppCompatActivity {

    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 2100;

    public String SESSION_CODE = null;
    EditText codeTXT;
    Activity thisActivity;

    String device_id = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.RetainTheme(this);
        setContentView(R.layout.activity_follow);
        codeTXT = (EditText) findViewById(R.id.codeTxt);

        device_id = Boss.getDeviceID(this);

        startService(new Intent(this, ConnectivityReporter.class));

        thisActivity = this;


        IntentFilter lostConnection = new IntentFilter();
        lostConnection.addAction(ConnectivityReporter.CONNECTION_STATUS_TOKEN_NEGATIVE);
        registerReceiver(lostConnectionBR, lostConnection);

        IntentFilter connectionResumed = new IntentFilter();
        connectionResumed.addAction(ConnectivityReporter.CONNECTION_STATUS_TOKEN_POSITIVE);
        registerReceiver(connectionResumedBR, connectionResumed);


    }

    public void startJourney(View view) {
        if (startRegistrationService()) {
            try {
                SESSION_CODE = codeTXT.getText() + "";
                verifyAndStart();
            } catch (NullPointerException e) {
                Log.e(Boss.LOG_TAG + "Follow", "NullPointerException Code Empty");
                inform("Please enter a session code");
            }
        }

    }

    private void verifyAndStart() {
        if (!SESSION_CODE.isEmpty()) {

            ParseQuery<ParseObject> query = ParseQuery.getQuery(Boss.PARSE_BLACKLIST_CLASS);
            query.whereEqualTo(Boss.KEY_DEVICE_ID, device_id);
            query.whereEqualTo(Boss.KEY_CON_CODE, SESSION_CODE);
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> list, ParseException e) {
                    if (list.isEmpty()) {

                        final int integer_code;
                        try {
                            integer_code = Integer.parseInt(SESSION_CODE);
                        } catch (Exception e1) {

                            inform("Please Enter a valid session code");

                            return;
                        }
                        ParseQuery<ParseObject> queryID = ParseQuery.getQuery(Boss.PARSE_CLASS);
                        queryID.whereEqualTo(Boss.KEY_QRCODE, integer_code);
                        queryID.findInBackground(new FindCallback<ParseObject>() {
                            @Override
                            public void done(List<ParseObject> parseObjects, ParseException e) {
                                if (e == null) {
                                    if (parseObjects.isEmpty()) {

                                        inform("Please Enter a valid session code");

                                        return;
                                    } else {
                                        for (ParseObject result : parseObjects) {
                                            Boss.OBJECT_ID = result.getObjectId();
                                            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                                            vibrator.vibrate(50);
                                            startMaps(SESSION_CODE);
                                        }
                                    }
                                } else {
                                    inform("Please Enter a valid session code");
                                }
                            }
                        });


                    } else {
                        inform("Sorry, You were kicked from this session");
                    }
                }
            });

        } else {
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


        if (startRegistrationService()) {
            startActivity(new Intent(this, Scanner.class));
        }

    }

    private void inform(String msg) {
        View view = findViewById(R.id.codeTxt);
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

        ConnectivityReporter.SURRENDER=true;


        super.onDestroy();
    }


    private Boolean startRegistrationService() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int code = api.isGooglePlayServicesAvailable(this);
        if (code == ConnectionResult.SUCCESS) {
            onActivityResult(REQUEST_GOOGLE_PLAY_SERVICES, Activity.RESULT_OK, null);
            return true;
        } else if (api.isUserResolvableError(code)) {
            api.showErrorDialogFragment(this, code, REQUEST_GOOGLE_PLAY_SERVICES);
            return false;
        } else {
            String str = GoogleApiAvailability.getInstance().getErrorString(code);
            Toast.makeText(this, str, Toast.LENGTH_LONG).show();
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != Activity.RESULT_OK) {
                    ConnectivityReporter.SURRENDER=true;
                    thisActivity.finish();
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }


}
