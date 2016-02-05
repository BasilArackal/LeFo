package com.lmntrx.lefo;

import android.app.Activity;
import android.content.Intent;
import android.os.Vibrator;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;

import eu.livotov.labs.android.camview.ScannerLiveView;
import eu.livotov.labs.android.camview.scanner.decoder.zxing.ZXDecoder;

public class Scanner extends AppCompatActivity {

    String SESSION_CODE;
    ScannerLiveView scannerLiveView;
    boolean flashStatus=false;

    Activity thisActivity;

    String device_id="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        device_id=Boss.getDeviceID(this);

        thisActivity=this;

        scannerLiveView=(ScannerLiveView)findViewById(R.id.scanner);
        scannerLiveView.setScannerViewEventListener(new ScannerLiveView.ScannerViewEventListener() {
            @Override
            public void onScannerStarted(ScannerLiveView scanner) {
                Log.d(Boss.LOG_TAG + "Scanner", "Scanning");
            }

            @Override
            public void onScannerStopped(ScannerLiveView scanner) {
                Log.d(Boss.LOG_TAG + "Scanner", "Scanning Stopped");
            }

            @Override
            public void onScannerError(Throwable err) {
                Toast.makeText(Scanner.this, "Cannot Open Camera. Try restarting the device.", Toast.LENGTH_LONG).show();
                Log.e(Boss.LOG_TAG + "Scanner", err.getMessage());
            }

            @Override
            public void onCodeScanned(String data) {

                SESSION_CODE = data;

                verifyAndStart();

            }
        });



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

                            inform("Please scan a valid lefo QR code");

                            return;
                        }
                        ParseQuery<ParseObject> queryID = ParseQuery.getQuery(Boss.PARSE_CLASS);
                        queryID.whereEqualTo(Boss.KEY_QRCODE, integer_code);
                        queryID.findInBackground(new FindCallback<ParseObject>() {
                            @Override
                            public void done(List<ParseObject> parseObjects, ParseException e) {
                                if (e == null) {
                                    if (parseObjects.isEmpty()) {

                                        inform("Please scan a valid lefo QR code");

                                        return;
                                    } else {
                                        for (ParseObject result : parseObjects) {
                                            Boss.OBJECT_ID = result.getObjectId();
                                            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                                            vibrator.vibrate(50);
                                            startMaps();
                                        }
                                    }
                                } else {
                                    inform("Please scan a valid lefo QR code");
                                }
                            }
                        });


                    } else {
                        inform("Sorry, You were kicked from this session");
                    }
                }
            });

        } else {
            inform("Please scan a valid lefo QR code");
        }
    }

    private void inform(String msg) {
        View view = findViewById(R.id.scanner);
        Snackbar snackbar = Snackbar.make(view, msg, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    private void startMaps() {
        Intent maps=new Intent(this,MapsActivity.class);
        maps.putExtra("SESSION_CODE",SESSION_CODE);
        startActivity(maps);
        thisActivity.finish();
    }

    @Override
    protected void onPause() {
        scannerLiveView.stopScanner();
        super.onPause();
    }

    @Override
    protected void onStop() {
        scannerLiveView.stopScanner();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        scannerLiveView.stopScanner();
        super.onDestroy();
    }

    @Override
    protected void onResume() {

        ZXDecoder decoder=new ZXDecoder();
        decoder.setScanAreaPercent(0.5);
        scannerLiveView.setDecoder(decoder);

        try {
            scannerLiveView.startScanner();
        }catch (Exception e){
            Toast.makeText(this,"This device does not have a camera",Toast.LENGTH_LONG).show();
            Scanner.this.finish();
        }

        super.onResume();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_scanner, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_flash) {
            toggleFlash();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void toggleFlash() {
        flashStatus = !flashStatus;
        try{
            scannerLiveView.getCamera().getController().switchFlashlight(flashStatus);
        }catch (Exception e){
            Log.e(Boss.LOG_TAG,e.getMessage());
        }
    }

    @Override
    public void onBackPressed() {

        Thread thread=new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.run();

        if (!thread.isAlive())
            super.onBackPressed();

    }
}
