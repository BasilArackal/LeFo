package com.lmntrx.lefo;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Followers extends AppCompatActivity {

    int SESSION_CODE;
    ProgressBar progressBar;
    ListView listView;
    Context context;


    SimpleAdapter adapter;

    final int REFRESH_FREQUENCY=2000;

    Timer t;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.RetainTheme(this);
        setContentView(R.layout.activity_followers);

        //SESSION_CODE = getIntent().getIntExtra("SESSION_CODE", -1);
        try{
            SESSION_CODE=Integer.parseInt(Lead.SESSION_CODE);
        }catch (Exception e){
            Log.e(Boss.LOG_TAG,e.getMessage());
        }

        Toast.makeText(this,SESSION_CODE+"",Toast.LENGTH_LONG).show();

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        progressBar.setVisibility(View.INVISIBLE);

        listView = (ListView) findViewById(R.id.followersList);

        context = this;


        t = new Timer();
        final Handler handler = new Handler();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        loadFollowers();
                    }
                });
            }
        };
        t.scheduleAtFixedRate(task, REFRESH_FREQUENCY, REFRESH_FREQUENCY);



    }

    public void loadFollowers() {
       // progressBar.setVisibility(View.VISIBLE);

        ParseQuery<ParseObject> query = ParseQuery.getQuery(Boss.PARSE_FCLASS);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> results, com.parse.ParseException e) {
                if (results.isEmpty()){
                    Log.e(Boss.LOG_TAG,"Empty");
                }
               // progressBar.setVisibility(View.INVISIBLE);
                if (e == null) {
                    ArrayList<HashMap<String, String>> infos = new ArrayList<HashMap<String, String>>();
                    for (ParseObject result : results) {
                        HashMap<String, String> info = new HashMap<String, String>();
                        if ((SESSION_CODE + "").equals(result.getString(Boss.KEY_CON_CODE)) && result.getBoolean(Boss.KEY_isActive)) {
                            info.put(Boss.KEY_DEVICE, result.getString(Boss.KEY_DEVICE));
                            infos.add(info);
                        }else {
                            Log.e(Boss.LOG_TAG,"Sorry2");
                        }
                    }
                    if (infos.isEmpty()) {
                        HashMap<String, String> info = new HashMap<String, String>();
                        info.put(Boss.KEY_DEVICE, "No Followers");
                        infos.add(info);
                    }
                    SimpleAdapter adapter = new SimpleAdapter(context, infos, R.layout.followers_list_item, new String[]{Boss.KEY_DEVICE}, new int[]{R.id.list_item_field});
                    listView.setAdapter(adapter);
                } else {

                    Log.e(Boss.LOG_TAG,"Sorry "+e.getMessage());
                    e.printStackTrace();
                    System.out.print(e.getMessage());
                }
            }
        });
    }

    public void refreshList() {
        ParseQuery query = new ParseQuery(Boss.PARSE_FCLASS);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> results, com.parse.ParseException e) {
                if (e == null) {
                    ArrayList<HashMap<String, String>> infos = new ArrayList<HashMap<String, String>>();
                    for (ParseObject result : results) {
                        HashMap<String, String> info = new HashMap<String, String>();
                        if ( (SESSION_CODE + "").equals(result.getString(Boss.KEY_CON_CODE)) && result.get(Boss.KEY_isActive).equals("true")) {
                            info.put(Boss.KEY_DEVICE, result.getString(Boss.KEY_DEVICE));
                            infos.add(info);
                        }else {
                            Log.e(Boss.LOG_TAG, "Sorry");
                        }
                    }
                    if (infos.isEmpty()) {
                        HashMap<String, String> info = new HashMap<String, String>();
                        info.put(Boss.KEY_DEVICE, "No Followers");
                        infos.add(info);
                    }
                    adapter = new SimpleAdapter(context, infos, R.layout.followers_list_item, new String[]{Boss.KEY_DEVICE}, new int[]{R.id.list_item_field});
                    listView.setAdapter(adapter);
                } else {

                    Log.e(Boss.LOG_TAG, "Sorry "+e.getMessage());
                    e.printStackTrace();
                    System.out.print(e.getMessage());
                }
            }
        });

    }


    @Override
    protected void onResume() {

        try{
            SESSION_CODE=Integer.parseInt(Lead.SESSION_CODE);
        }catch (Exception e){
            Log.e(Boss.LOG_TAG,e.getMessage());
        }

        super.onResume();
    }

    @Override
    protected void onDestroy() {

        t.cancel();
        super.onDestroy();
    }
}
