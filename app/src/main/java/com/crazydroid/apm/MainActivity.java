package com.crazydroid.apm;

import android.Manifest;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import com.crazydroid.apm.fragments.Fragment1;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,Main2Activity.class));
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
//        final FragmentManager fragmentManager = getSupportFragmentManager();
//        fragmentManager.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
//            @Override
//            public void onFragmentCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @Nullable Bundle savedInstanceState) {
//                super.onFragmentCreated(fm, f, savedInstanceState);
////                Log.i("time","onFragmentCreated");
//            }
//        }, true);
//        Fragment fragment1 = new Fragment1();
//        fragmentManager.beginTransaction().replace(R.id.content_main,fragment1).commit();
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        new AlertDialog.Builder(this).setTitle("hello").setMessage("123").create().show();
//        getWindow().getDecorView().addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
//            @Override
//            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
//
//            }
//        });
//        startActivity(new Intent(this,Main2Activity.class));
//        new AsyncTask(){
//
//            @Override
//            protected Object doInBackground(Object[] objects) {
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                return "";
//            }
//
//            @Override
//            protected void onPostExecute(Object o) {
//                super.onPostExecute(o);
//                Log.i("time","onAsyncTaskFinish");
//                Fragment fragment1 = new Fragment1();
//                fragmentManager.beginTransaction().replace(R.id.content_main,fragment1).commit();
//            }
//        }.execute();
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                startActivity(new Intent(MainActivity.this,Main2Activity.class));
//            }
//        },2000);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},1);
//        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
