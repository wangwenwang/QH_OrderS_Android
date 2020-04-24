package com.kaidongyuan.qh_orders_android.track;

import android.os.Handler;

import java.io.File;
import java.util.Map;

/**
 * Created by Administrator on 2015/11/2.
 */
public interface IAsyncHttpLient {
    /**
     * 重载的发送请求
     */
    void sendRequest(String url, Map<String, String> params, String requst_tag, boolean need_show_dialog);
    void sendRequest(String url, Map<String, String> params, String requst_tag);
    void sendRequest(String url);
   // void sendBitMapRequest(String url);
    void sendFileRequest(String url, String destFileName);
    //2016.07.07 陈翔  文件上传的okhttp封装还未实际使用测试过！
    void uploadFileRequest(String url, Map<String,File> uploadFilesparams,Map<String,String> params,String requst_tag,boolean need_show_dialog);
    /**
     * 开始发送请求
     */
    void mSend(String tag);

    /**
     * 开始发送网络文件下载请求
     */
    void mLoadFile(String tag);

    /**
     *取消请求
     * @param tags
     */
    void cancleRequest(String...tags);

    /**
     * 返回结果
     * @param result
     */
    void postMsg(String result, String tag);
}
