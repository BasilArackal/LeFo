package com.lmntrx.lefo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class Follow extends AppCompatActivity {

    public static String SESSION_CODE="1";
    TextView codeTXT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow);
        codeTXT=(TextView)findViewById(R.id.sessionCodeTxt);
    }

    public void startJourney(View view) {
        SESSION_CODE=codeTXT.getText().toString().trim();
        if(!SESSION_CODE.isEmpty()){
            if (Boss.verifySessionCode(SESSION_CODE)==Boss.SESSION_APPROVED){
                startMaps(SESSION_CODE);
            }else {
                Toast.makeText(this,"Enter a valid LeFo Session Code",Toast.LENGTH_LONG).show();
            }
        }else {
            Toast.makeText(this,"Please enter a session code",Toast.LENGTH_LONG).show();
        }
    }

    private void startMaps(String sessionCode) {
        Intent maps=new Intent(this,MapsActivity.class);
        maps.putExtra("SESSION_CODE",sessionCode);
        startActivity(maps);
        Follow.this.finish();
    }

    public void openQRScanner(View view) {
        startActivity(new Intent(this,Scanner.class));
    }
}
