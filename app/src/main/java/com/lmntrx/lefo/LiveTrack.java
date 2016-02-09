package com.lmntrx.lefo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class LiveTrack extends AppCompatActivity {

    public static String LIVETRACK_CODE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LIVETRACK_CODE=Boss.genLeFoCode()+"";

        Utils.RetainTheme(this);

        setContentView(R.layout.activity_livetrack);

        Button tab1= (Button) findViewById(R.id.tab1);

        View.OnClickListener button1=new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),Home.class);
                startActivity(intent);
                LiveTrack.this.finish();
            }
        };
        tab1.setOnClickListener(button1);

        final Switch switch1 = (Switch) findViewById(R.id.liveTrackSwitch);
        //switch1.setChecked(false);
        final TextView liveTrackCodeLabel = (TextView) findViewById(R.id.liveTrackCodeLabel);
        final TextView liveTrackCodeTXT = (TextView) findViewById(R.id.TextView_LiveTrackCode);
        final TextView textView3 = (TextView) findViewById(R.id.liveTrackTextView1);
        final TextView textView4= (TextView) findViewById(R.id.liveTrackSwitchMsg);



        switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled

                  //  switch1.toggle();
                    liveTrackCodeLabel.setVisibility(View.VISIBLE);
                    liveTrackCodeTXT.setVisibility(View.VISIBLE);
                    liveTrackCodeTXT.setText(LIVETRACK_CODE);
                    textView3.setText(R.string.live_track_enabled_warning);
                    textView4.setText(R.string.live_track_enabled_text);
                } else {
                    // The toggle is disabled
                  //  switch1.toggle();
                    liveTrackCodeLabel.setVisibility(View.GONE);
                    liveTrackCodeTXT.setVisibility(View.GONE);
                    textView3.setText(R.string.live_track_disabled_warning_text);
                    textView4.setText(R.string.live_track_disabled_text);
                }
            }
        });

    }
}
