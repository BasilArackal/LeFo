package com.lmntrx.lefo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

public class SplashScreen extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_splashscreen);


    }

    public void guestSession(View view) {
        Intent intent=new Intent(this,Home.class);

        startActivity(intent);
        this.finish();
    }


    public void login(View view) {
        Toast.makeText(this,"Coming Soon",Toast.LENGTH_LONG).show();
    }
}
