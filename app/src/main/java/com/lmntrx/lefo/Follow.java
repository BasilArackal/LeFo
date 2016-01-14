package com.lmntrx.lefo;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class Follow extends AppCompatActivity {

    String SESSION_CODE="1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow);
    }

    public void startJourney(View view) {
    }

    public void openQRScanner(View view) {
        /*try {

            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            intent.setPackage("com.google.android.apps.unveil");
            startActivityForResult(intent,0);

        } catch (Exception e) {
            try {
                Uri marketUri = Uri.parse("market://details?id=com.google.android.apps.unveil");
                Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
                startActivity(marketIntent);
            }catch (Exception e2){
                Toast.makeText(this,"Your device doesn't support this task. Please enter code manually",Toast.LENGTH_LONG).show();
            }

        }*/
        startActivity(new Intent(this,Scanner.class));
    }
}
