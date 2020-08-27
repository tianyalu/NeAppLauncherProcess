package com.sty.ne.app.launcherprocess;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * @Author: tian
 * @UpdateDate: 2020-08-25 16:36
 */
public class MyBroadcastReceiver extends BroadcastReceiver {
    public void test() {
//        Context context = this;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Context context1 = context;
    }
}
