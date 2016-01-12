package com.lmntrx.lefo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Random;

public class Lead extends AppCompatActivity {

    ImageView qrImg;
    static Boss boss=new Boss();
    public static final String SESSION_CODE=boss.genLeFoCode()+"";
    public static Context CON;

    public static Boolean isSessionOn=false;

    //URL for generating QRCode for generated random code
    //Use any of the following servers
    //public String qrUrl1 = "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=";
    public String qrUrl2 = "http://qrickit.com/api/qr?d=";
    public String qrUrl2Size = "&qrsize=500"; //500px. PS: When using url1 remove qrUrl2Size from ImageLoadTask
    //Choosing server for qrCode generation
    public String qrUrl = qrUrl2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lead);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        qrImg=(ImageView) findViewById(R.id.qrIMG);
        CON=this;

        //Setting FAB
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isSessionOn){
                    Snackbar.make(view, "No Followers", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }else {
                    fab.setImageResource(R.drawable.ic_menu_view);
                    Snackbar.make(view, "Started LeFo Session. Go back to exit. Safe Journey!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    isSessionOn=true;
                    boss.startSession(CON,SESSION_CODE);
                }


            }
        });

        //Display Lefo_Connection_Code
        TextView lcodeTXT=(TextView)findViewById(R.id.sessionCodeTxt);
        lcodeTXT.setText(SESSION_CODE);

        //This Async Task Loads QRCode from qrUrl to qrImg
        new Load_QRCode(qrUrl + SESSION_CODE + qrUrl2Size, qrImg).execute();


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
            Intent shareIntent=new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,"LeFo Connection Code");
            String shareBody=SESSION_CODE;
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
            startActivity(shareIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



}
