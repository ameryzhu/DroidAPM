package com.crazydroid.apm;

import com.crazydroid.apm.hook.Hooker;

public class MyApplication extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Hooker hooker =new Hooker();
        hooker.onCreate(this);
    }
}
