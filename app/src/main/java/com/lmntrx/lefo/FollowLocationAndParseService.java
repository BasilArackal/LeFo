package com.lmntrx.lefo;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by livin on 15/1/16.
 */
public class FollowLocationAndParseService extends Service {


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        



        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
