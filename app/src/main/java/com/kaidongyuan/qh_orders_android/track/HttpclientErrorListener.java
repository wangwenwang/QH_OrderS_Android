package com.kaidongyuan.qh_orders_android.track;

/**
 * Created by Administrator on 2016/3/11.
 * 修改时间2016-03-11
 * Volley 请求失败回调方法，设置了就回调，不设置调用已封装的默认方法
 */
public interface HttpclientErrorListener {
    /**
     * 处理 Volley 请求失败的方法
     */
    public abstract void handleErrorListener();
}
