package com.sty.ne.app.launcherprocess;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

/**
 * @Author: tian
 * @UpdateDate: 2020-08-25 16:31
 */
public class MyService extends Service {
    public void test() {
        Context context = this;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
