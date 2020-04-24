package com.kaidongyuan.qh_orders_android.track;

import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import com.kaidongyuan.qh_orders_android.R;


/**
 * baseActivity 封装一些通用方法 所有的activity继承自该Activity
 *
 * @author ke
 */
public abstract class BaseActivity extends BaseInputActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

//        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
//        AppManager.getAppManager().addActivity(this);
//        //友盟推送统计应用启动数据
//        PushAgent.getInstance(getMContext()).onAppStart();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }


    public void onResume() {
        super.onResume();
//		StatService.onResume(this);//百度统计页面
    }

    protected void showToast(String msg, int time) {
        Toast.makeText(this, msg, time).show();

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void finish() {
        super.finish();
//        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
//        AppManager.getAppManager().finishActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}
