package com.crazydroid.apm;

import com.crazydroid.apm.hook.ActivityAPM;

public class MyApplication extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ActivityAPM.getInstance().init(this);
    }
}
