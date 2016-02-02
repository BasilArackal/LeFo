package com.lmntrx.lefo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

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
    ListView listView;
    Context context;

    Activity thisActivity;

    final int REFRESH_FREQUENCY = 2000;

    Timer t;

    public static ArrayList<HashMap<String, String>> FOLLOWERS_DETAILS = new ArrayList<HashMap<String, String>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.RetainTheme(this);
        setContentView(R.layout.activity_followers);

        thisActivity = this;

        try {
            SESSION_CODE = Integer.parseInt(Lead.SESSION_CODE);
        } catch (Exception e) {
            Log.e(Boss.LOG_TAG, e.getMessage());
        }


        listView = (ListView) findViewById(R.id.followersList);

        context = this;


        IntentFilter lostConnection = new IntentFilter();
        lostConnection.addAction(ConnectivityReporter.CONNECTION_STATUS_TOKEN_NEGATIVE);
        registerReceiver(lostConnectionBR, lostConnection);

        IntentFilter connectionResumed = new IntentFilter();
        connectionResumed.addAction(ConnectivityReporter.CONNECTION_STATUS_TOKEN_POSITIVE);
        registerReceiver(connectionResumedBR, connectionResumed);


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

        ParseQuery<ParseObject> query = ParseQuery.getQuery(Boss.PARSE_FCLASS);
        try {
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> results, com.parse.ParseException e) {
                    if (results != null) {
                        if (results.isEmpty()) {
                            Log.e(Boss.LOG_TAG, "Empty");
                        }
                        if (e == null) {
                            ArrayList<HashMap<String, String>> infos = new ArrayList<HashMap<String, String>>();
                            for (ParseObject result : results) {
                                HashMap<String, String> info = new HashMap<String, String>();
                                if ((SESSION_CODE + "").equals(result.getString(Boss.KEY_CON_CODE))) {
                                    info.put(Boss.KEY_DEVICE_ID, result.getString(Boss.KEY_DEVICE_ID));
                                    info.put(Boss.KEY_DEVICE, result.getString(Boss.KEY_DEVICE));
                                    info.put(Boss.KEY_isActive, result.getBoolean(Boss.KEY_isActive) + "");
                                    info.put(Boss.KEY_DEVICE + ":" + Boss.KEY_isActive, result.getString(Boss.KEY_DEVICE) + ":" + result.getBoolean(Boss.KEY_isActive));
                                    infos.add(info);
                                }
                            }
                            if (infos.isEmpty()) {
                                HashMap<String, String> info = new HashMap<String, String>();
                                info.put(Boss.KEY_DEVICE, "No Followers:false");
                                infos.add(info);
                            }
                            String devices[] = new String[infos.size()];
                            for (int i = 0; i < infos.size(); i++) {
                                devices[i] = infos.get(i).get(Boss.KEY_DEVICE + ":" + Boss.KEY_isActive);
                            }
                            FollowersListAdapter listAdapter = new FollowersListAdapter(context, devices);
                            //SimpleAdapter adapter = new SimpleAdapter(context, infos, R.layout.followers_list_item, new String[]{Boss.KEY_DEVICE}, new int[]{R.id.list_item_field});
                            listView.setAdapter(listAdapter);
                            FOLLOWERS_DETAILS.clear();
                            FOLLOWERS_DETAILS = infos;
                        } else {
                            e.printStackTrace();
                            System.out.print(e.getMessage());
                        }
                    } else {
                        Log.e(Boss.LOG_TAG, "Lost Connection Probably");
                    }
                }
            });
        } catch (NullPointerException e) {
            Log.e(Boss.LOG_TAG, e.getMessage());
        }

    }


    @Override
    protected void onResume() {

        try {
            SESSION_CODE = Integer.parseInt(Lead.SESSION_CODE);
        } catch (Exception e) {
            Log.e(Boss.LOG_TAG, e.getMessage());
        }

        super.onResume();
    }

    @Override
    protected void onDestroy() {

        unregisterReceiver(lostConnectionBR);
        unregisterReceiver(connectionResumedBR);

        t.cancel();
        super.onDestroy();
    }

    private BroadcastReceiver lostConnectionBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Boss.inform("Lost Connection", thisActivity.findViewById(R.id.followersList), 0);
        }
    };

    private BroadcastReceiver connectionResumedBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Boss.inform("Connected", thisActivity.findViewById(R.id.followersList), 1);
        }
    };

}
