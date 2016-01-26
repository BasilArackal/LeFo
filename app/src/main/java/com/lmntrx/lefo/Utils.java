package com.lmntrx.lefo;

/**
 * Created by ACJLionsRoar on 1/25/16.
 */
import android.app.Activity; import android.content.Intent;
import android.support.annotation.RequiresPermission;

public class Utils {
    private static int sTheme;



    /** * Set the theme of the Activity, and restart it by creating a new Activity of the same type. */

    public static void changeToTheme(Activity activity, int theme) {
        sTheme = theme;
        activity.finish();
        activity.startActivity(new Intent(activity, activity.getClass()));
    }
    /** Set the theme of the activity, according to the configuration. */
    public static void onActivityCreateSetTheme(Activity activity) {
        switch (sTheme)
        { default:
            case 0: activity.setTheme(R.style.AppTheme); break;
            case 1: activity.setTheme(R.style.AppTheme_Dark); break;
            } } }


