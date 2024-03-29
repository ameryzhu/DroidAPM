/*
 * Copyright 2015-present wequick.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.crazydroid.apm.hook;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ProviderInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.util.Log;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.FrameLayout;

import com.crazydroid.apm.internal.InstrumentationInternal;
import com.crazydroid.apm.utils.ReflectAccelerator;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import androidx.annotation.RequiresApi;

/**
 * This class launch the plugin activity by it's class name.
 *
 * <p>This class resolve the bundle who's <tt>pkg</tt> is specified as
 * <i>"*.app.*"</i> or <i>*.lib.*</i> in <tt>bundle.json</tt>.
 *
 * <ul>
 * <li>The <i>app</i> plugin contains some activities usually, while launching,
 * takes the bundle's <tt>uri</tt> as default activity. the other activities
 * can be specified by the bundle's <tt>rules</tt>.</li>
 *
 * <li>The <i>lib</i> plugin which can be included by <i>app</i> plugin
 * consists exclusively of global methods that operate on your product services.</li>
 * </ul>
 */
public class ActivityAPM {

    private static final String PACKAGE_NAME = ActivityAPM.class.getPackage().getName();
    private static final String TAG = "time";

    private static long startTime = 0;
    private static long startTimeFromPerformLaunch = 0;
    private static long startTimeFromExecuteStartActivity = 0;
    private static String activityName = "";


    private static Instrumentation sHostInstrumentation;
    private static InstrumentationWrapper sBundleInstrumentation;
    private static ActivityThreadHandlerCallback sActivityThreadHandlerCallback;

    private static Object sActivityThread;

    private static volatile ActivityAPM instance;

    private ActivityAPM(){

    }

    public static ActivityAPM getInstance(){
        if(instance==null){
            synchronized (ActivityAPM.class){
                if(instance==null){
                    instance = new ActivityAPM();
                }
                return instance;
            }
        }
        return  instance;
    }

    /**
     * Class for restore activity info from Stub to Real
     */
    private static class ActivityThreadHandlerCallback implements Handler.Callback {

        private static final int LAUNCH_ACTIVITY = 100;
        private static final int CREATE_SERVICE = 114;
        private static final int EXECUTE_TRANSACTION = 159; // since Android P

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case LAUNCH_ACTIVITY:
                    redirectActivity(msg);
                    break;

                case EXECUTE_TRANSACTION:
                    redirectActivityForP(msg);
                    break;

                default:
                    break;
            }

            return false;
        }

//        case CREATE_SERVICE:
//                    final Object/*ActivityClientRecord*/ r3 = msg.obj;
//                    Intent intent3 = ReflectAccelerator.getIntent(r3);
//                    if (intent3.getComponent() != null) {
//                        Log.i(TAG, "#start activity#" + intent3.getComponent().getClassName());
//                    }
//                    break;

        interface ActivityInfoReplacer {
            void replace(ActivityInfo info);
        }


        private void redirectActivityForP(Message msg) {
            if (Build.VERSION.SDK_INT >= 28) {
                // Following APIs cannot be called again since android 9.0.
                Object/*android.app.servertransaction.ClientTransaction*/ t = msg.obj;
                List callbacks = ReflectAccelerator.getLaunchActivityItems(t);
                if (callbacks == null) return;
                for (final Object/*LaunchActivityItem*/ item : callbacks) {
                    Intent intent = ReflectAccelerator.getIntentOfLaunchActivityItem(item);
                    if(intent!=null&&intent.getComponent()!=null) {
                        Log.i("hook", intent.getComponent().getClassName());
                        activityName = intent.getComponent().getClassName();
                        startTime = System.currentTimeMillis();
                    }
                }


                return;
            }

            Object/*android.app.servertransaction.ClientTransaction*/ t = msg.obj;
            List callbacks = ReflectAccelerator.getLaunchActivityItems(t);
            if (callbacks == null) return;

            for (final Object/*LaunchActivityItem*/ item : callbacks) {
                Intent intent = ReflectAccelerator.getIntentOfLaunchActivityItem(item);
                tryReplaceActivityInfo(intent, new ActivityInfoReplacer() {
                    @Override
                    public void replace(ActivityInfo targetInfo) {
                        ReflectAccelerator.setActivityInfoToLaunchActivityItem(item, targetInfo);
                    }
                });
            }
        }

        private void redirectActivity(Message msg) {
            final Object/*ActivityClientRecord*/ r = msg.obj;
            Intent intent = ReflectAccelerator.getIntent(r);

            if (intent.getComponent() != null) {
                activityName = intent.getComponent().getClassName();
                startTimeFromPerformLaunch = System.currentTimeMillis();
//                Log.i(TAG, "#start activity#" + intent.getComponent().getClassName());
            }

            tryReplaceActivityInfo(intent, new ActivityInfoReplacer() {
                @Override
                public void replace(ActivityInfo targetInfo) {
                    ReflectAccelerator.setActivityInfo(r, targetInfo);
                }
            });
        }

        static void tryReplaceActivityInfo(Intent intent, ActivityInfoReplacer replacer) {
        }

    }

    /**
     * Class for redirect activity from Stub(AndroidManifest.xml) to Real(Plugin)
     */
    protected static class InstrumentationWrapper extends Instrumentation
            implements InstrumentationInternal{

        private Instrumentation mBase;
        private static final int STUB_ACTIVITIES_COUNT = 4;

        public InstrumentationWrapper(Instrumentation base) {
            mBase = base;
        }

        /**
         * @Override V21+
         */
        public ActivityResult execStartActivity(
                Context who, IBinder contextThread, IBinder token, Activity target,
                Intent intent, int requestCode, android.os.Bundle options) {
            activityName = target.getComponentName().getClassName();
            startTime = System.currentTimeMillis();
//            startTimeFromPerformLaunch = System.currentTimeMillis();
            startTimeFromExecuteStartActivity = System.currentTimeMillis();
            Log.i(TAG,"#execStartActivity()#"+activityName);
            ensureInjectMessageHandler(sActivityThread);
            return ReflectAccelerator.execStartActivity(mBase,
                    who, contextThread, token, target, intent, requestCode, options);
        }

        /**
         * @Override V20-
         * Wrap activity from REAL to STUB
         */
        public ActivityResult execStartActivity(
                Context who, IBinder contextThread, IBinder token, Activity target,
                Intent intent, int requestCode) {
            activityName = target.getComponentName().getClassName();
            startTime = System.currentTimeMillis();
            Log.i(TAG,"#execStartActivity#"+activityName);
            ensureInjectMessageHandler(sActivityThread);
            return ReflectAccelerator.execStartActivity(mBase,
                    who, contextThread, token, target, intent, requestCode);
        }

        @Override
        public Activity newActivity(ClassLoader cl, final String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
            final String[] targetClassName = {className};
            if (Build.VERSION.SDK_INT >= 28) {
                ActivityThreadHandlerCallback.tryReplaceActivityInfo(intent, new ActivityThreadHandlerCallback.ActivityInfoReplacer() {
                    @Override
                    public void replace(ActivityInfo info) {
                        targetClassName[0] = info.targetActivity; // Redirect to the plugin activity
                    }
                });
            }

            if (intent.getComponent().getClassName().equals(activityName)) {
                if(startTimeFromExecuteStartActivity!=0){
                    long time = System.currentTimeMillis() - startTimeFromExecuteStartActivity;
                    startTime = System.currentTimeMillis();
                    Log.i(TAG,"#executeActivity()--->newActivity# "+intent.getComponent().getShortClassName()+" cost milli seconds:"+ time);
                }
            }
            Activity newActivity = mBase.newActivity(cl, targetClassName[0], intent);
            if (intent.getComponent().getClassName().equals(activityName)) {
                if(startTimeFromExecuteStartActivity!=0) {
                    long time = System.currentTimeMillis() - startTime;
                    Log.i(TAG, "#newActivity()--->newActivity finish# " + intent.getComponent().getShortClassName() + " cost milli seconds:" + time);
                }
            }
            return newActivity;
        }

        @Override
        /** Prepare resources for REAL */
        public void callActivityOnCreate(Activity activity, android.os.Bundle icicle) {
            if (activity.getComponentName().getClassName().equals(activityName)) {
//                long time = System.currentTimeMillis() - startTime;
//                Log.i(TAG, "#startActivity()--->init()#"+time);
//                Log.i(TAG, "#init() start#");
                startTime = System.currentTimeMillis();
            }

            sHostInstrumentation.callActivityOnCreate(activity, icicle);
            if (activity.getComponentName().getClassName().equals(activityName)) {
                long time = System.currentTimeMillis() - startTime;
                Log.i(TAG, "#onCreate()--->onCreate() finish# " + activity.getComponentName().getShortClassName() + " cost milli seconds:" + time);
                startTime = System.currentTimeMillis();
            }
//            hookDecorView(activity.getWindow(),activity);
        }

        @Override
        public void callActivityOnStart(Activity activity) {

            if (activity.getComponentName().getClassName().equals(activityName)) {
                startTime = System.currentTimeMillis();
            }
            super.callActivityOnStart(activity);
            if (activity.getComponentName().getClassName().equals(activityName)) {
                long time = System.currentTimeMillis() - startTime;
                Log.i(TAG, "#onStart()--->onStart() finish# " + activity.getComponentName().getShortClassName() + " cost milli seconds:" + time);
                startTime = System.currentTimeMillis();
            }
            sHostInstrumentation.callActivityOnStart(activity);
        }

        @Override
        public void callActivityOnSaveInstanceState(Activity activity, android.os.Bundle outState) {
            sHostInstrumentation.callActivityOnSaveInstanceState(activity, outState);
        }

        @Override
        public void callActivityOnRestoreInstanceState(Activity activity, android.os.Bundle savedInstanceState) {
            sHostInstrumentation.callActivityOnRestoreInstanceState(activity, savedInstanceState);
        }

        @Override
        public void callActivityOnResume(final Activity activity) {
            if (activity.getComponentName().getClassName().equals(activityName)) {
                startTime = System.currentTimeMillis();
            }
            super.callActivityOnResume(activity);
            if (activity.getComponentName().getClassName().equals(activityName)) {
                long time = System.currentTimeMillis() - startTime;
                Log.i(TAG, "#onResume()--->onResume() finish# " + activity.getComponentName().getShortClassName() + " cost milli seconds:" + time);
                startTime = System.currentTimeMillis();
            }
            Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
                @Override
                public boolean queueIdle() {
                    long time = System.currentTimeMillis() - startTime;
                    long timeTotal = System.currentTimeMillis() - startTimeFromPerformLaunch;
                    Log.i(TAG, "#UI render Finish# " + activity.getComponentName().getShortClassName() + " cost milli seconds:" + time);
                    Log.i(TAG, "#totalTime from performLaunchActivity: # " + activity.getComponentName().getShortClassName() + " cost milli seconds:" + timeTotal);
                    //First time launch application executeStartActivity will not execute
                    if(startTimeFromExecuteStartActivity!=0){
                        long timeTotal2 = System.currentTimeMillis() - startTimeFromExecuteStartActivity;
                        Log.i(TAG, "#totalTime from executeStartActivity:# " + activity.getComponentName().getShortClassName() + " cost milli seconds:" + timeTotal2);
                    }
                    return false;
                }
            });

        }

        @Override
        public void callActivityOnStop(Activity activity) {
            sHostInstrumentation.callActivityOnStop(activity);

        }

        @Override
        public void callActivityOnDestroy(Activity activity) {
            sHostInstrumentation.callActivityOnDestroy(activity);
        }

        @Override
        public boolean onException(Object obj, Throwable e) {

            return super.onException(obj, e);
        }
    }

    private static void ensureInjectMessageHandler(Object thread) {
        try {
            Field f = thread.getClass().getDeclaredField("mH");
            f.setAccessible(true);
            Handler ah = (Handler) f.get(thread);
            f = Handler.class.getDeclaredField("mCallback");
            f.setAccessible(true);

            boolean needsInject = false;
            if (sActivityThreadHandlerCallback == null) {
                needsInject = true;
            } else {
                Object callback = f.get(ah);
                if (callback != sActivityThreadHandlerCallback) {
                    needsInject = true;
                }
            }

            if (needsInject) {
                // Inject message handler
                sActivityThreadHandlerCallback = new ActivityThreadHandlerCallback();
                f.set(ah, sActivityThreadHandlerCallback);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to replace message handler for thread: " + thread);
        }
    }

    public void init(Application app) {


        List<ProviderInfo> providers;
        Object/*ActivityThread*/ thread;
        Instrumentation base;
        ActivityAPM.InstrumentationWrapper wrapper;
        Field f;
        // Get activity thread
        thread = ReflectAccelerator.getActivityThread(app);

        // Replace instrumentation
        try {
            f = thread.getClass().getDeclaredField("mInstrumentation");
            f.setAccessible(true);
            base = (Instrumentation) f.get(thread);
            wrapper = new ActivityAPM.InstrumentationWrapper(base);
            f.set(thread, wrapper);
        } catch (Exception e) {
            throw new RuntimeException("Failed to replace instrumentation for thread: " + thread);
        }

        // Inject message handler
        ensureInjectMessageHandler(thread);

        sActivityThread = thread;
        sHostInstrumentation = base;
        sBundleInstrumentation = wrapper;
    }

//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
//    public static void hookDecorView(Window window, Context context){
//        // AOP for pending intent
//        window.getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                Log.i(TAG,"onGlobalLayout finish");
//            }
//        });
//        ViewTreeObserver observer = window.getDecorView().getViewTreeObserver();
//        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//            @Override
//            public boolean onPreDraw() {
////                Log.i(TAG,"onPreDraw finish");
//                return false;
//            }
//        });
//        observer.addOnDrawListener(new ViewTreeObserver.OnDrawListener() {
//            @Override
//            public void onDraw() {
//                Log.i(TAG,"onDraw ");
//            }
//        });
//        try {
//            Field f = window.getClass().getDeclaredField("mDecor");
////            Field f = PhoneWindow.class.getDeclaredField("IMPL");
//            f.setAccessible(true);
////            final Object impl = f.get(window);
//            final Object impl = window.getDecorView();
//            InvocationHandler aop = new InvocationHandler() {
//                @Override
//                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//                    if("onLayout".equals(method.getName())){
//                        Log.i(TAG,"onlayout");
//                    }else if("onMeasure".equals(method.getName())){
//                        Log.i(TAG,"onMeasure");
//                    }
//                    return method.invoke(impl, args);
//                }
//            };
//            FrameLayout newImpl = (FrameLayout) Proxy.newProxyInstance(context.getClassLoader(), new Class[]{ViewGroup.class}, aop);
//            f.set(window, newImpl);
//        } catch (Exception ignored) {
//            Log.e(TAG, ignored.toString());
//        }
//    }




}
