package com.lmntrx.lefo;

/**
 * Created by ACJLionsRoar on 1/25/16.
 */

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.RequiresPermission;
import android.support.v4.content.IntentCompat;
import android.view.View;
import android.widget.Toast;

public class Utils {
    private static int sTheme;
    public final static int SET_THEME_T0_LIGHT_NOACTIONBAR = 0;
    public final static int SET_THEME_TO_DARK_NOACTIONBAR = 1;
    public final static int SET_THEME_T0_LIGHT = 2;
    public final static int SET_THEME_TO_DARK = 3;


    /**
     * Set the theme of the Activity, and restart it by creating a new Activity of the same type.
     */

    public static void changeToTheme(Activity activity, int theme) {
        sTheme = theme;
        activity.finish();

        //activity.startActivity(new Intent(activity, activity.getClass()));

        Intent intent = new Intent(activity, activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        activity.startActivity(intent);
    }

    /**
     * Set the theme of the activity, according to the configuration.
     */
    public static void onActivityCreateSetTheme(Activity activity) {
        if (sTheme == 0 || sTheme == 1)
            switch (sTheme) {
                default:
                case 0:
                    activity.setTheme(R.style.AppTheme_Light_NoActionBar);
                    View view = activity.getWindow().getDecorView();
                    view.setBackgroundColor(Color.WHITE);
                    break;
                case 1:
                    activity.setTheme(R.style.AppTheme_Dark_NoActionBar);
                    View view2 = activity.getWindow().getDecorView();
                    view2.setBackgroundColor(Color.BLACK);
                    Utils obj = new Utils();
                    obj.showtoast(activity);
                    break;
            }

        else
            switch (sTheme) {
                default:
                case 2:
                    activity.setTheme(R.style.AppTheme);
                    View view = activity.getWindow().getDecorView();
                    view.setBackgroundColor(Color.WHITE);
                    break;
                case 3:
                    activity.setTheme(R.style.AppTheme_Dark);
                    View view2 = activity.getWindow().getDecorView();
                    view2.setBackgroundColor(Color.BLACK);
                    Utils obj = new Utils();
                    obj.showtoast(activity);
                    break;
            }
    }


    public static void RetainTheme(Activity activity) {
        if (Boss.DarkTheme) {
            activity.setTheme(R.style.AppTheme_Dark);
            View view2 = activity.getWindow().getDecorView();
            view2.setBackgroundColor(Color.BLACK);
            Utils obj = new Utils();
            obj.showtoast(activity);
        }

    }

    public static void RetainTheme(Activity activity, int NoActionBar) {


        if (Boss.DarkTheme) {
            activity.setTheme(R.style.AppTheme_Dark_NoActionBar);
            View view2 = activity.getWindow().getDecorView();
            view2.setBackgroundColor(Color.BLACK);
            Utils obj = new Utils();
            obj.showtoast(activity);
        }

    }

    public void showtoast(Activity activity) {
        Toast.makeText(activity, "DarkTheme Colors Not Optimised. Only Functionality Enabled.", Toast.LENGTH_SHORT).show();
    }

}




