package com.lmntrx.lefo;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PersistableBundle;
import android.os.Vibrator;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;

public class LiveTrack extends AppCompatActivity {

    int LIVE_TRACK_CODE;

    Switch liveTrackSwitch;

    EditText liveTrackCodeEntry;

    Boss boss;

    String device_id;

    Boolean isSessionOn, alerted;

    Bundle savedInstanceState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.savedInstanceState=savedInstanceState;

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        Utils.RetainTheme(this);

        setContentView(R.layout.activity_livetrack);

        alerted = false;

        liveTrackSwitch = (Switch) findViewById(R.id.liveTrackSwitch);
        //liveTrackSwitch.setChecked(false);
        final TextView liveTrackCodeLabel = (TextView) findViewById(R.id.liveTrackCodeLabel);
        final TextView liveTrackCodeTXT = (TextView) findViewById(R.id.TextView_LiveTrackCode);
        final TextView textView3 = (TextView) findViewById(R.id.liveTrackTextView1);
        final TextView textView4 = (TextView) findViewById(R.id.liveTrackSwitchMsg);

        LIVE_TRACK_CODE = Boss.genLeFoCode();

        isSessionOn=false;

        boss = new Boss();

        device_id = Boss.getDeviceID(this);

        Button tab1 = (Button) findViewById(R.id.tab1);

        liveTrackCodeEntry = (EditText) findViewById(R.id.liveTrackCodeEntry);

        //Intent Filters
        IntentFilter gpsDisabled = new IntentFilter();
        gpsDisabled.addAction(LeadLocationAndParseService.LOST_GPS);
        registerReceiver(gpsDisabledBR, gpsDisabled);

        IntentFilter cannotLocate = new IntentFilter();
        cannotLocate.addAction(LeadLocationAndParseService.CANNOT_LOCATE);
        registerReceiver(cannotLocateBR, cannotLocate);

        IntentFilter noLocationPermission = new IntentFilter();
        noLocationPermission.addAction(LeadLocationAndParseService.NO_LOCATION_PERMISSION);
        registerReceiver(noLocationPermissionBR, noLocationPermission);

        IntentFilter gotYa = new IntentFilter();
        gotYa.addAction(LeadLocationAndParseService.GOT_YA);
        registerReceiver(gotYaBR, gotYa);

        IntentFilter sessionInterrupted = new IntentFilter();
        sessionInterrupted.addAction(LeadLocationAndParseService.SESSION_INTERRUPTED);
        registerReceiver(sessionInterruptedBR, sessionInterrupted);

        View.OnClickListener home = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isSessionOn) {
                    Intent intent = new Intent(getApplicationContext(), Home.class);
                    startActivity(intent);
                    LiveTrack.this.finish();
                } else {
                    inform("Please, Close Live Tracking Session First");
                }

            }
        };
        tab1.setOnClickListener(home);


        liveTrackSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @SuppressLint("SetTextI18n")
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && !isSessionOn) {
                    // The toggle is enabled

                    //  liveTrackSwitch.toggle();
                    liveTrackCodeLabel.setVisibility(View.VISIBLE);
                    liveTrackCodeTXT.setVisibility(View.VISIBLE);
                    liveTrackCodeTXT.setText(LIVE_TRACK_CODE + "");
                    textView3.setText(R.string.live_track_enabled_warning);
                    textView4.setText(R.string.live_track_enabled_text);
                    try {
                        if (!isSessionOn) {
                            boss.startLiveTrackSession(LiveTrack.this, LIVE_TRACK_CODE);
                            isSessionOn = true;
                        }
                    } catch (Exception n) {
                        Log.e(Boss.LOG_TAG + "LiveTrack", "Unable to start session " + n.getMessage());
                    }
                } else if (!isChecked && isSessionOn){
                    // The toggle is disabled
                    //  liveTrackSwitch.toggle();
                    liveTrackCodeLabel.setVisibility(View.GONE);
                    liveTrackCodeTXT.setVisibility(View.GONE);
                    if (isSessionOn) {
                        boss.stopLiveTrackSession(LiveTrack.this);
                        isSessionOn = false;
                    }
                    textView3.setText(R.string.live_track_disabled_warning_text);
                    textView4.setText(R.string.live_track_disabled_text);
                }
            }
        });

    }

    @Override
    protected void onStart() {
        /*if (savedInstanceState != null) {
            isSessionOn = savedInstanceState.getBoolean("SESSION_STATUS");
            LIVE_TRACK_CODE = savedInstanceState.getInt("LIVE_TRACK_CODE");
            liveTrackSwitch.setChecked(isSessionOn);
        } else {
            isSessionOn = false;

        }*/
        super.onStart();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {

       /* outState.putBoolean("SESSION_STATUS", isSessionOn);
        outState.putInt("LIVE_TRACK_CODE", LIVE_TRACK_CODE);*/

        super.onSaveInstanceState(outState, outPersistentState);
    }

    private BroadcastReceiver gpsDisabledBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Snackbar.make(LiveTrack.this.findViewById(R.id.liveTrackSwitch), "Your GPS seems to be disabled, please enable it to continue.", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            if (liveTrackSwitch.isChecked())
                liveTrackSwitch.toggle();
        }
    };

    private BroadcastReceiver sessionInterruptedBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Snackbar.make(LiveTrack.this.findViewById(R.id.liveTrackSwitch), "Sorry, Connection was lost. Session Closed", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            if (liveTrackSwitch.isChecked())
                liveTrackSwitch.toggle();

        }
    };

    private BroadcastReceiver cannotLocateBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Snackbar.make(LiveTrack.this.findViewById(R.id.liveTrackSwitch), "Couldn't locate you, try moving your device", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

        }
    };

    private BroadcastReceiver noLocationPermissionBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(Boss.LOG_TAG, "Received BR");
            Snackbar.make(LiveTrack.this.findViewById(R.id.liveTrackSwitch), "Sorry, Session Failed. Please grant permission to access location and try again.", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            Boss.askPermission("LOCATION", LiveTrack.this);
            if (liveTrackSwitch.isChecked()) {
                liveTrackSwitch.toggle();
            }
        }
    };

    private BroadcastReceiver gotYaBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(Boss.LOG_TAG, "Received BR");
            Snackbar.make(LiveTrack.this.findViewById(R.id.liveTrackSwitch), "Started LeFo Session. Go back to exit. Safe Journey!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    };

    public void startTracking(View view) {

        int enteredCode;

        try {
            enteredCode = Integer.parseInt(liveTrackCodeEntry.getText().toString());
            verifyAndStart(enteredCode);
        } catch (NumberFormatException e) {
            Log.e(Boss.LOG_TAG, e.getMessage() + " ");
            inform("Please Enter a valid session code");
        }


    }


    private void verifyAndStart(final int SESSION_CODE) {
        if (SESSION_CODE != -1) {

            ParseQuery<ParseObject> query = ParseQuery.getQuery(Boss.PARSE_BLACKLIST_CLASS);
            query.whereEqualTo(Boss.KEY_DEVICE_ID, device_id);
            query.whereEqualTo(Boss.KEY_CON_CODE, SESSION_CODE);
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> list, ParseException e) {
                    if (list.isEmpty()) {
                        ParseQuery<ParseObject> queryID = ParseQuery.getQuery(Boss.PARSE_CLASS);
                        queryID.whereEqualTo(Boss.KEY_QRCODE, SESSION_CODE);
                        queryID.findInBackground(new FindCallback<ParseObject>() {
                            @Override
                            public void done(List<ParseObject> parseObjects, ParseException e) {
                                if (e == null) {
                                    if (parseObjects.isEmpty()) {

                                        inform("Please Enter a valid session code");

                                    } else {
                                        for (ParseObject result : parseObjects) {
                                            Boss.OBJECT_ID = result.getObjectId();
                                            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                                            vibrator.vibrate(50);
                                            startMaps(SESSION_CODE, result.getObjectId());
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

    private void inform(String msg) {
        View view = findViewById(R.id.liveTrackCodeEntry);
        Snackbar snackbar = Snackbar.make(view, msg, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    private void startMaps(int sessionCode, String ObjectID) {
        Intent maps = new Intent(this, LiveTrackMapsActivity.class);
        maps.putExtra("SESSION_CODE", sessionCode);
        maps.putExtra("OBJECT_ID", ObjectID);
        startActivity(maps);
    }

    @Override
    protected void onDestroy() {

        unregisterReceiver(cannotLocateBR);
        unregisterReceiver(gotYaBR);
        unregisterReceiver(gpsDisabledBR);
        unregisterReceiver(noLocationPermissionBR);
        unregisterReceiver(sessionInterruptedBR);

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        if (isSessionOn)
            alertSessionEnd();
        else super.onBackPressed();
    }

    void alertSessionEnd() {
        if (!alerted) {
            new AlertDialog.Builder(LiveTrack.this)
                    .setCancelable(false)
                    .setTitle("End Session")
                    .setMessage("Do you want to end this LeFo Session?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            boss.stopLiveTrackSession(LiveTrack.this);
                            isSessionOn = false;
                            LiveTrack.this.finish();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            alerted=false;
                            dialog.cancel();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            alerted = true;
        }
    }

}
