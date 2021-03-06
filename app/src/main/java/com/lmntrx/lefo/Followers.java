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
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

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

    public static ProgressBar progressBar;

    FollowersListAdapter listAdapter;

    Activity thisActivity;

    String selectedDeviceID = "", selectedDeviceName = "";


    public static HashMap<String, String> selectedListItemObject = new HashMap<>();

    final int REFRESH_FREQUENCY = 2000;

    Timer t;

    public static Boolean devicesActiveStatus[] = new Boolean[15];

    public static ArrayList<HashMap<String, String>> FOLLOWERS_DETAILS = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.RetainTheme(this);
        setContentView(R.layout.activity_followers);

        thisActivity = this;

        progressBar=(ProgressBar)findViewById(R.id.progressBar);


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

        IntentFilter kickedFollower = new IntentFilter();
        kickedFollower.addAction(Boss.KICKED_FOLLOWER);
        registerReceiver(kickedFollowerBR, kickedFollower);



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

        registerForContextMenu(listView);

    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        selectedDeviceName = ((TextView) (v.findViewById(R.id.list_item_field))).getText().toString();
        if (!selectedDeviceName.equals("No Followers")) {
            for (HashMap<String, String> object : FOLLOWERS_DETAILS) {
                if (object.get(Boss.KEY_DEVICE).equals(selectedDeviceName)) {
                    selectedListItemObject = object;
                    selectedDeviceID = object.get(Boss.KEY_DEVICE_ID);
                    break;
                }
            }
            menu.setHeaderTitle(selectedDeviceName);
            menu.add(0, v.getId(), 0, "Kick");//groupId, itemId, order, title
            super.onCreateContextMenu(menu, v, menuInfo);
        } else menu.close();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {


        if (item.getTitle() == "Kick") {
            progressBar.setVisibility(View.VISIBLE);
            Boss.kickUser(selectedDeviceID, selectedDeviceName, Lead.SESSION_CODE, thisActivity);
        } else {
            return false;
        }
        return true;
    }


    public void loadFollowers() {

        ParseQuery<ParseObject> query = ParseQuery.getQuery(Boss.PARSE_FOLLOWERS_CLASS);
        query.whereEqualTo(Boss.KEY_CON_CODE,SESSION_CODE+"");
        try {
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> results, com.parse.ParseException e) {
                    if (results != null) {
                        if (results.isEmpty()) {
                            Log.e(Boss.LOG_TAG, "Empty");
                        }
                        if (e == null) {
                            ArrayList<HashMap<String, String>> details = new ArrayList<>();
                            for (ParseObject result : results) {
                                HashMap<String, String> info = new HashMap<>();
                                    info.put(Boss.KEY_DEVICE_ID, result.getString(Boss.KEY_DEVICE_ID));
                                    info.put(Boss.KEY_DEVICE, result.getString(Boss.KEY_DEVICE));
                                    info.put(Boss.KEY_isActive, result.getBoolean(Boss.KEY_isActive) + "");
                                    details.add(info);
                            }
                            if (details.isEmpty()) {
                                HashMap<String, String> info = new HashMap<>();
                                info.put(Boss.KEY_DEVICE, "No Followers");
                                details.add(info);
                            }
                            String devices[] = new String[details.size()];
                            for (int i = 0; i < details.size(); i++) {
                                devices[i] = details.get(i).get(Boss.KEY_DEVICE);
                                devicesActiveStatus[i] = Boolean.valueOf(details.get(i).get(Boss.KEY_isActive));
                            }
                            listAdapter = new FollowersListAdapter(context, devices);
                            listView.setAdapter(listAdapter);
                            progressBar.setVisibility(View.INVISIBLE);
                            FOLLOWERS_DETAILS.clear();
                            FOLLOWERS_DETAILS = details;
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
        unregisterReceiver(kickedFollowerBR);

        t.cancel();
        super.onDestroy();
    }

    private BroadcastReceiver lostConnectionBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Boss.inform("Lost Connection", thisActivity.findViewById(R.id.followersList), Boss.SNACKBAR_INDEFINITE_LAUNCH);
        }
    };

    private BroadcastReceiver connectionResumedBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Boss.inform("Connected", thisActivity.findViewById(R.id.followersList), Boss.SNACKBAR_INDEFINITE_CLOSE);
        }
    };

    private BroadcastReceiver kickedFollowerBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Boss.inform("Kicked " + selectedDeviceName, thisActivity.findViewById(R.id.followersList), 2);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_followers, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_refresh:
                loadFollowers();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

}
