package com.lmntrx.lefo;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import eu.livotov.labs.android.camview.ScannerLiveView;
import eu.livotov.labs.android.camview.scanner.decoder.zxing.ZXDecoder;

public class Scanner extends AppCompatActivity {

    String SESSION_CODE;
    ScannerLiveView scannerLiveView;
    boolean flashStatus=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);


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
                Thread thread=new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int status=Boss.verifySessionCode(SESSION_CODE);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (status == Boss.SESSION_APPROVED) {
                            Vibrator vibrator=(Vibrator)getSystemService(VIBRATOR_SERVICE);
                            vibrator.vibrate(50);
                            startMaps();
                            Scanner.this.finish();
                        } else {
                            Toast.makeText(Scanner.this, "Validation Error, Please scan a valid LeFo QR Code\n" + SESSION_CODE, Toast.LENGTH_LONG).show();
                        }
                    }
                });
                thread.run();

            }
        });



    }

    private void startMaps() {
        Intent maps=new Intent(this,MapsActivity.class);
        maps.putExtra("SESSION_CODE",SESSION_CODE);
        startActivity(maps);
    }

    @Override
    protected void onPause() {
        scannerLiveView.stopScanner();
        super.onPause();
    }

    @Override
    protected void onResume() {

        ZXDecoder decoder=new ZXDecoder();
        decoder.setScanAreaPercent(0.8);
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
}
