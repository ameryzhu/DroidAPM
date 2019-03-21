package com.crazydroid.apm;

import com.crazydroid.apm.hook.ActivityAPM;

public class MyApplication extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ActivityAPM activityAPM =new ActivityAPM();
        activityAPM.init(this);
//        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
//            @Override
//            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
//                Log.i("application","oncreate"+":"+activity.getComponentName());
//            }
//
//            @Override
//            public void onActivityStarted(Activity activity) {
//                Log.i("application","onstart"+":"+activity.getComponentName());
//            }
//
//            @Override
//            public void onActivityResumed(Activity activity) {
//                Log.i("application","onresume"+":"+activity.getComponentName());
//            }
//
//            @Override
//            public void onActivityPaused(Activity activity) {
//                Log.i("application","onpause"+":"+activity.getComponentName());
//            }
//
//            @Override
//            public void onActivityStopped(Activity activity) {
//                Log.i("application","onstop"+":"+activity.getComponentName());
//            }
//
//            @Override
//            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
//
//            }
//
//            @Override
//            public void onActivityDestroyed(Activity activity) {
//
//            }
//        });
    }
}
