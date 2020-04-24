package com.kaidongyuan.qh_orders_android.track;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;


/**
 * 打印生命周期方法
 * Created by changwei on 2015/8/29.
 */
public class BaseLifecyclePrintActivity extends Activity {
    String className;
//    private SystemBarTintManager tintManager;
    private View mDecorView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        className = getClass().getSimpleName();
        initWindow();
    }
    //实现Android 4.4以上状态栏沉浸式效果
    @TargetApi(19)
    public void initWindow(){

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT){
            //状态栏透明化
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            Log.d("LM", "设置沉浸式体验2");
            //导航栏透明化
            // getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
//            tintManager = new SystemBarTintManager(this);
//            tintManager.setStatusBarTintDrawable(getResources().getDrawable(R.drawable.theme_color_app));
         //   tintManager.setStatusBarTintColor(getResources().getColor(R.color.yb_green));
//            tintManager.setStatusBarTintEnabled(true);
        }
        mDecorView = getWindow().getDecorView();
    }
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }
}
