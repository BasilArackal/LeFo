package com.lmntrx.lefo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class LiveTrack extends AppCompatActivity {

    public static int LIVE_TRACK_CODE;

    Switch liveTrackSwitch;

    Boss boss;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.RetainTheme(this);

        setContentView(R.layout.activity_livetrack);

        boss = new Boss();

        Button tab1 = (Button) findViewById(R.id.tab1);


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

        View.OnClickListener button1 = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Home.class);
                startActivity(intent);
                LiveTrack.this.finish();
            }
        };
        tab1.setOnClickListener(button1);

        liveTrackSwitch = (Switch) findViewById(R.id.liveTrackSwitch);
        //liveTrackSwitch.setChecked(false);
        final TextView liveTrackCodeLabel = (TextView) findViewById(R.id.liveTrackCodeLabel);
        final TextView liveTrackCodeTXT = (TextView) findViewById(R.id.TextView_LiveTrackCode);
        final TextView textView3 = (TextView) findViewById(R.id.liveTrackTextView1);
        final TextView textView4 = (TextView) findViewById(R.id.liveTrackSwitchMsg);


        liveTrackSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled

                    //  liveTrackSwitch.toggle();
                    LIVE_TRACK_CODE = Boss.genLeFoCode();
                    liveTrackCodeLabel.setVisibility(View.VISIBLE);
                    liveTrackCodeTXT.setVisibility(View.VISIBLE);
                    liveTrackCodeTXT.setText(LIVE_TRACK_CODE + "");
                    textView3.setText(R.string.live_track_enabled_warning);
                    textView4.setText(R.string.live_track_enabled_text);
                    try {
                        boss.startLiveTrackSession(LiveTrack.this, LIVE_TRACK_CODE);
                    } catch (Exception n) {
                        Log.e(Boss.LOG_TAG + "LiveTrack", "Unable to start session " + n.getMessage());
                    }
                } else {
                    // The toggle is disabled
                    //  liveTrackSwitch.toggle();
                    liveTrackCodeLabel.setVisibility(View.GONE);
                    liveTrackCodeTXT.setVisibility(View.GONE);
                    boss.stopLiveTrackSession(LiveTrack.this);
                    textView3.setText(R.string.live_track_disabled_warning_text);
                    textView4.setText(R.string.live_track_disabled_text);
                }
            }
        });

    }

    private BroadcastReceiver gpsDisabledBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Snackbar.make(LiveTrack.this.findViewById(R.id.liveTrackSwitch), "Your GPS seems to be disabled, please enable it to continue.", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
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
            Snackbar.make(LiveTrack.this.findViewById(R.id.liveTrackSwitch), "Sorry, Session Failed. Please grant permission to access location and try again.", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Action", null).show();
            Boss.askPermission("LOCATION", LiveTrack.this);
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

}
