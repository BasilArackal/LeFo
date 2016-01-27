package com.lmntrx.lefo;


import android.content.Intent;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.RetainTheme(this);
        setContentView(R.layout.activity_follow);
        codeTXT = (EditText) findViewById(R.id.codeTxt);





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
}
