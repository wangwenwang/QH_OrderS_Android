
package com.kaidongyuan.qh_orders_android.track;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;


/**
 * 继承自LifecyclePrintActivity的类，集成Toast,Dialog
 * Created by changwei on 2015/8/29.
 */
public class BaseToastDialogActivity extends BaseLifecyclePrintActivity implements BaseActivityListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void mStartActivity(Class<? extends Activity> cls) {
        startActivity(new Intent(this, cls));
    }

    @Override
    public void mStartActivity(Intent intent) {
        startActivity(intent);
    }

    @Override
    public void showToastMsg(String msg) {
        showToastMsg(msg, Toast.LENGTH_SHORT);
    }

    @Override
    public void showToastMsg(int msgId) {
        showToastMsg(getString(msgId), Toast.LENGTH_SHORT);
    }

    @Override
    public void showToastMsg(int msgId, int duration) {
        showToastMsg(getString(msgId), duration);
    }

    @Override
    public void showLoadingDialog() {

    }

    @Override
    public void showProgressBarLoadingDialog() {

    }

    @Override
    public void setProgressBarLoading(int progress) {

    }

    @Override
    public void cancelLoadingDialog() {

    }

    //在设置了
    //<item name="android:fitsSystemWindows">true</item> Toast信息会错位，需要用getActivity.getApplicationContext()避免
    Toast toast;
    @Override
    public void showToastMsg(String msg, int duration) {
        //取消之前的Toast信息
        if (toast != null){
            toast.cancel();
        }
        toast = Toast.makeText(this, msg, duration);
        toast.show();
    }

    @Override
    public Context getMContext() {
        return this;
    }



}
