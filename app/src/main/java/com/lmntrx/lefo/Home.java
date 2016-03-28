package com.lmntrx.lefo;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import com.parse.ParseAnalytics;

// extending from just Activity removes Actionbar automatically. extending from AppCompatActivity includes Actionbar
public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    Boss boss;

    public static Activity HOME_ACTIVITY;
    boolean ManuallyChanged=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);

        HOME_ACTIVITY=this;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boss.DarkTheme = sharedPreferences.getBoolean("DARK_THEME", true);

        int NoActionBar=1;
        if(!ManuallyChanged)
            Utils.RetainTheme(this,NoActionBar);
        else
            Utils.onActivityCreateSetTheme(this);

        setContentView(R.layout.activity_home);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        boss = new Boss();

        boss.initializeParse(this);
        ParseAnalytics.trackAppOpenedInBackground(getIntent());

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Opens up Lefo Circle/ Later Update", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        //   /*
        else if (id == R.id.changeThemeTemporary) {
            ManuallyChanged=true;
            if (!Boss.DarkTheme) {
                Boss.DarkTheme = true;
                Utils.changeToTheme(this, Utils.SET_THEME_TO_DARK_NO_ACTION_BAR);  //  means dark theme
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("DARK_THEME", true);
                editor.apply();

            } else {
                Boss.DarkTheme = false;
                Utils.changeToTheme(this, Utils.SET_THEME_T0_LIGHT_NO_ACTION_BAR); //  is light theme
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("DARK_THEME", false);
                editor.apply();
            }
        }

        //   */


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void startLead(View v) {
        if (Boss.isNetworkAvailable(this)) {
            if (boss.isGpsEnabled(this)) {
                Intent intent = new Intent(this, Lead.class);
                startActivity(intent);
            } else Boss.buildAlertMessageNoGps(this);
        } else {
            Snackbar.make(v, "Please connect to a working network and continue!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    public void startFollow(View v) {
        if (Boss.isNetworkAvailable(this)) {
            if (boss.isGpsEnabled(this)) {
                Intent intent = new Intent(this, Follow.class);
                startActivity(intent);
            } else Boss.buildAlertMessageNoGps(this);
        } else {
            Snackbar.make(v, "Please connect to a working network and continue!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    public void startLiveTrack(View v) {
        Intent intent = new Intent(this, LiveTrack.class);

        startActivity(intent);
        Home.this.finish();
    }

    @Override
    protected void onResume() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boss.DarkTheme = sharedPreferences.getBoolean("DARK_THEME", true);

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("DARK_THEME", Boss.DarkTheme);
        editor.apply();

        super.onPause();
    }


}
