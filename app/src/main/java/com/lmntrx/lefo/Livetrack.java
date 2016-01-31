package com.lmntrx.lefo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

public class Livetrack extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.RetainTheme(this);

        setContentView(R.layout.activity_livetrack);

        Button tab1= (Button) findViewById(R.id.tab1);

        View.OnClickListener button1=new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),Home.class);
                startActivity(intent);
                Livetrack.this.finish();
            }
        };
        tab1.setOnClickListener(button1);

        final Switch switch1 = (Switch) findViewById(R.id.liveTrackSwitch);
        //switch1.setChecked(false);
        final TextView textView1 = (TextView) findViewById(R.id.textView);
        final TextView textView2 = (TextView) findViewById(R.id.TextView_LiveTrackCode);
        final TextView textView3 = (TextView) findViewById(R.id.liveTrackTextView1);
        final TextView textView4= (TextView) findViewById(R.id.liveTrackSwitchMsg);



        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled

                  //  switch1.toggle();
                    textView1.setVisibility(View.VISIBLE);
                    textView2.setVisibility(View.VISIBLE);
                    textView3.setText("When enabled, any of your friends can track you via LeFo.Note: Disable after use.");
                    textView4.setText("Live Track Enabled");
                } else {
                    // The toggle is disabled
                  //  switch1.toggle();
                    textView1.setVisibility(View.GONE);
                    textView2.setVisibility(View.GONE);
                    textView3.setText("When disabled, no one can track you in live track mode.Note: Does not apply to Journey mode");
                    textView4.setText("Live Track Disabled");
                }
            }
        });

    }
}
