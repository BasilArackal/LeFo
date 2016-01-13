package com.lmntrx.lefo;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/**
 * Created by livin on 12/1/16.
 */


public class CloseLeFoSessionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Lead.isLeadWindowActive)
            Lead.alertSessionEnd();
        else {
            Boss.deleteSession(context);
            Lead.currentLeadActivity.finish();

        }
    }
}
