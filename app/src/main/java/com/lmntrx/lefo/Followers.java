package com.lmntrx.lefo;

import android.content.Context;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;

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

        SESSION_CODE = getIntent().getIntExtra("SESSION_CODE", -1);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        progressBar.setVisibility(View.INVISIBLE);

        listView = (ListView) findViewById(R.id.followersList);

        context = this;

        loadFollowers();

        t = new Timer();
        final Handler handler = new Handler();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        refreshList();
                    }
                });
            }
        };
        t.schedule(task, REFRESH_FREQUENCY, REFRESH_FREQUENCY);



    }

    public void refreshList() {
        ParseQuery query = new ParseQuery(Boss.PARSE_FCLASS);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> results, com.parse.ParseException e) {
                if (e == null) {
                    ArrayList<HashMap<String, String>> infos = new ArrayList<>();
                    for (ParseObject result : results) {
                        HashMap<String, String> info = new HashMap<>();
                        if ((SESSION_CODE + "").equals(result.getString(Boss.KEY_CON_CODE)) && result.getBoolean(Boss.KEY_isActive)) {
                            info.put(Boss.KEY_DEVICE, result.getString(Boss.KEY_DEVICE));
                            infos.add(info);
                        }
                    }
                    if (infos.isEmpty()) {
                        HashMap<String, String> info = new HashMap<>();
                        info.put(Boss.KEY_DEVICE, "No Followers");
                        infos.add(info);
                    }
                    adapter = new SimpleAdapter(context, infos, R.layout.followers_list_item, new String[]{Boss.KEY_DEVICE}, new int[]{R.id.list_item_field});
                    listView.setAdapter(adapter);
                } else {
                    e.printStackTrace();
                    System.out.print(e.getMessage());
                }
            }
        });

    }


    public void loadFollowers() {
        progressBar.setVisibility(View.VISIBLE);

        ParseQuery query = new ParseQuery(Boss.PARSE_FCLASS);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> results, com.parse.ParseException e) {
                progressBar.setVisibility(View.INVISIBLE);
                if (e == null) {
                    ArrayList<HashMap<String, String>> infos = new ArrayList<>();
                    for (ParseObject result : results) {
                        HashMap<String, String> info = new HashMap<>();
                        if ((SESSION_CODE + "").equals(result.getString(Boss.KEY_CON_CODE)) && result.getBoolean(Boss.KEY_isActive)) {
                            info.put(Boss.KEY_DEVICE, result.getString(Boss.KEY_DEVICE));
                            infos.add(info);
                        }
                    }
                    if (infos.isEmpty()) {
                        HashMap<String, String> info = new HashMap<>();
                        info.put(Boss.KEY_DEVICE, "No Followers");
                        infos.add(info);
                    }
                    SimpleAdapter adapter = new SimpleAdapter(context, infos, R.layout.followers_list_item, new String[]{Boss.KEY_DEVICE}, new int[]{R.id.list_item_field});
                    listView.setAdapter(adapter);
                } else {
                    e.printStackTrace();
                    System.out.print(e.getMessage());
                }
            }
        });
    }


    @Override
    protected void onDestroy() {

        t.cancel();
        super.onDestroy();
    }
}
