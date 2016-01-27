package com.lmntrx.lefo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;

public class Lead extends AppCompatActivity {

    ImageView qrImg;
    static Boss boss = new Boss();
    public static String SESSION_CODE = null;
    public static Context CON;
    public static boolean isLeadWindowActive = false;

    public static boolean canRefresh;

    public static Boolean isSessionOn = false;

    //URL for generating QRCode for generated random code
    //Use any of the following servers
    //public String qrUrl1 = "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=";
    public String qrUrl2 = "http://qrickit.com/api/qr?d=";
    public String qrUrl2Size = "&qrsize=500"; //500px. PS: When using url1 remove qrUrl2Size from ImageLoadTask
    //Choosing server for qrCode generation
    public String qrUrl = qrUrl2;

    public static Activity currentLeadActivity = null;

    static boolean alerted = false;

    public static FloatingActionButton fab;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int NoActionBar=1;
        Utils.RetainTheme(this,NoActionBar);
        setContentView(R.layout.activity_lead);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        qrImg = (ImageView) findViewById(R.id.qrIMG);

        CON = this;
        currentLeadActivity = this;

        Boss.notified=false;


        SESSION_CODE = Boss.genLeFoCode() + "";


        canRefresh = true;

        isSessionOn=false;

        //Setting FAB
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isSessionOn) {
                    Snackbar.make(view, "No Followers", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else {
                   // Snackbar.make(view, "Started LeFo Session. Go back to exit. Safe Journey!", Snackbar.LENGTH_LONG)
                    //        .setAction("Action", null).show();
                    isSessionOn = true;
                    canRefresh = false;
                    fab.setImageResource(R.drawable.ic_menu_view);
                    boss.startSession(CON, SESSION_CODE);
                }
            }
        });


        //Display Lefo_Connection_Code
        TextView lcodeTXT = (TextView) findViewById(R.id.sessionCodeTxt);
        lcodeTXT.setText(SESSION_CODE);

        //This Async Task Loads QRCode from qrUrl to qrImg
        new Load_QRCode(qrUrl + SESSION_CODE + qrUrl2Size, qrImg).execute();

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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_lead, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_share) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "LeFo Connection Code");
            String shareBody = SESSION_CODE;
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
            startActivity(shareIntent);
            return true;
        } else if (id == R.id.action_refresh) {
            refresh();
        }

        return super.onOptionsItemSelected(item);
    }

    private void refresh() {
        if (canRefresh) {
            SESSION_CODE = Boss.genLeFoCode() + "";

            //Display Lefo_Connection_Code
            TextView lcodeTXT = (TextView) findViewById(R.id.sessionCodeTxt);
            lcodeTXT.setText(SESSION_CODE);

            //This Async Task Loads QRCode from qrUrl to qrImg
            new Load_QRCode(qrUrl + SESSION_CODE + qrUrl2Size, qrImg).execute();
        } else {
            Toast.makeText(this, "Can't refresh during an ongoing LeFo Session", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onBackPressed() {

        if (isSessionOn)
            alertSessionEnd();
        else super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isLeadWindowActive = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isLeadWindowActive = true;
    }

    @Override
    protected void onDestroy() {
        if (isSessionOn)
        Boss.deleteSession(this);
        isSessionOn = false;
        SESSION_CODE = null;
        isLeadWindowActive = false;
        currentLeadActivity = null;
        alerted = false;
        unregisterReceiver(gpsDisabledBR);
        unregisterReceiver(cannotLocateBR);
        unregisterReceiver(noLocationPermissionBR);
        unregisterReceiver(gotYaBR);
        super.onDestroy();
    }

    public static void alertSessionEnd() {
        if (!alerted) {
            new AlertDialog.Builder(CON)
                    .setCancelable(false)
                    .setTitle("End Session")
                    .setMessage("Do you want to end this LeFo Session?")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Boss.deleteSession(CON);
                            isSessionOn = false;
                            currentLeadActivity.finish();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            alerted = true;
        }
    }

    private BroadcastReceiver gpsDisabledBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Boss.buildAlertMessageLostGps(context,currentLeadActivity);
        }
    };

    private BroadcastReceiver cannotLocateBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            View view = fab;
            Snackbar.make(view, "Couldn't locate you, try moving your device", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

        }
    };

    private BroadcastReceiver noLocationPermissionBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            View view = fab;
            Log.d(Boss.LOG_TAG, "Recieved BR");
            Snackbar.make(view, "Sorry, Session Failed. Please grant permission to access location and try again.", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Action", null).show();
            Boss.askPermission("LOCATION",currentLeadActivity);
        }
    };

    private BroadcastReceiver gotYaBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            View view = fab;
            Log.d(Boss.LOG_TAG, "Recieved BR");
            Snackbar.make(view, "Started LeFo Session. Go back to exit. Safe Journey!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    };

}
