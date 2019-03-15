package com.crazydroid.apm.aop;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

public class FragmentAOP {

    //Pointcut表示式
    @Pointcut("execution(* com.savage.aop.MessageSender.*(..))")
    //Point签名
    public void log1(){

    }

    @Before("onCreateView(container,)")
    public void oncreateView(){
//        LayoutInflater inflater, @Nullable ViewGroup container,
//        Bundle savedInstanceState

    }

    @Pointcut("execution(* android.app.Application.attachBaseContext(android.content.Context)) && args(context)")
    public void applicationAttachBaseContext(Context context) {
    }

    @Before("applicationAttachBaseContext(context)")
    public void applicationAttachBaseContextAdvice(Context context) {
    }
}
