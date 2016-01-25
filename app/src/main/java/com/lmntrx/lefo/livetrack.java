package com.lmntrx.lefo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Livetrack extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_livetrack);

        Button tab1= (Button) findViewById(R.id.tab1);

        View.OnClickListener button1=new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),Home.class);
                startActivity(intent);
            }
        };
        tab1.setOnClickListener(button1);

    }
}
