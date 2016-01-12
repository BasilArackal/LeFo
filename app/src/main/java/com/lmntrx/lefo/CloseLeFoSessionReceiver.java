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
        Intent service=new Intent();
        service.setComponent(new ComponentName(context, LeadLocationAndParseService.class));
        context.stopService(service);
        if (Lead.isLeadWindowActive)
            Lead.alertSessionEnd();
        else {
            Boss.deleteSession();
            Lead.currentLeadActivity.finish();
        }
    }
}