package com.kaidongyuan.qh_orders_android.track;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.TimeoutError;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.builder.PostFormBuilder;
import com.zhy.http.okhttp.callback.FileCallBack;
import com.zhy.http.okhttp.callback.StringCallback;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okio.Timeout;

/**
 * 采用第三方框架Volley
 * Created by changwei on 2015/8/30
 */
public abstract class BaseAsyncHttpClient implements IAsyncHttpLient{
    OkHttpUtils okHttpUtils;
  //  private RequestQueue mRequestQueue;//请求队列
    protected BaseActivityListener activityListener;
    protected AsyncHttpCallback callback;
//    protected String requst_tag;//作为请求的id，防止在多个请求同时进行时数据传递错误。
    private String url;
    private Map<String, String> params;
    private boolean need_show_dialog = true;
    protected boolean need_show_toast = true;
    private int IntStringCallBack=0;
    private int IntBitmapCallBack=1;
    private int IntFileCallBack=2;//设置三种常见网络请求类型的回调方式选择标识
    private String destFileName;
    private Map<String,File>uploadFilesParams;//批量上传文件

    // 修改时间 2016-03-11 ，增加 Volley 网络请求失败回调接口，登陆选择鼎葵是咨询为空，提示用户联系供应商
    //----------------------------------------------------------------------------------------------------------
    /**
     * 处理 Volley 网络请求失败的接口
     */
    private HttpclientErrorListener mErrorListener;

    public void setmErrorListener(HttpclientErrorListener errorListener){
        this.mErrorListener = errorListener;
    }

    //----------------------------------------------------------------------------------------------------------
    // 修改时间 2016-03-11 ，增加 Volley 网络请求失败回调接口，登陆选择鼎葵是咨询为空，提示用户联系供应商


    public BaseAsyncHttpClient(BaseActivityListener activityListener, AsyncHttpCallback callback) {

        final Context mContext = activityListener.getMContext();
//        if (mRequestQueue == null) {
//            mRequestQueue = Volley.newRequestQueue(mContext);
//
//        }
        if (okHttpUtils==null){
          okHttpUtils=createOkhttpUtils(30*1000L,30*1000L,60*1000L);
        }
        this.activityListener = activityListener;
        this.callback = callback;
    }

    @Override
    public void sendRequest(String url, Map<String, String> params, String request_tag, boolean need_show_dialog) {
        this.url = url;
        this.params = params;
        /*if (request_tag != null){
            this.requst_tag = request_tag;
        }*/
        this.need_show_dialog = need_show_dialog;
        mSend(request_tag);
    }

    @Override
    public void sendRequest(String url, Map<String, String> params, String request_tag) {
        this.url = url;
        this.params = params;
        Log.d("LM", "url: " + url);
        Log.d("LM", "params: " + params);
        /*if (requst_tag != null){
            this.request_tag = requst_tag;
        }*/
//        this.need_show_dialog = false;
        mSend(request_tag);
    }

    public void setDialogVisible(boolean need_show){
        this.need_show_dialog = need_show;
    }
    public void setShowToast(boolean showToast){
        need_show_toast = showToast;
    }

    @Override
    public void sendRequest(String url) {
        sendRequest(url, null, null);
    }

    /**
     *@auther: Tom
     *created at 2016/6/7 11:34
     * 文件下载请求
     * destFileName 为设置存储网络下载文件的文件名
     */
    @Override
    public void sendFileRequest(String url, String destFileName ) {
        this.url=url;
        this.destFileName=destFileName;
        Log.d("LM", "url: " + url);
        Log.d("LM", "destFileName: " + destFileName);
        mLoadFile(destFileName);//以文件名destFileName作为Tag
    }
    /**
     *@auther: Tom
     *created at 2016/7/7 17:56
     *文件批量上传
     * params为附加的参数
     */
    @Override
    public void uploadFileRequest(String url, Map<String, File> uploadFilesparams, Map<String, String> params, String requst_tag, boolean need_show_dialog) {
        this.url=url;
        this.uploadFilesParams=uploadFilesparams;
        this.params=params;
        this.need_show_dialog=need_show_dialog;
        mUploadFile(requst_tag);
    }
    //    @Override
//    public void uploadFileRequest(String url, File uploadFile, Map<String, String> headparams, String requst_tag, boolean need_show_dialog) {
//        this.url=url;
//        this.uploadFile=uploadFile;
//
//        this.params=headparams;
//        this.need_show_dialog=need_show_dialog;
//        mUploadFile(requst_tag);
//    }
    //    @Override
//    public void sendBitMapRequest(String url) {
//        this.url=url;
//    }

    @Override
    public void mSend(final String request_tag) {

        Log.d("LM", "网络请求标签：" + request_tag);
        String log_msg = url + "?";
        if (params != null){
//            UUID uuid=UUID.randomUUID();
//            params.put("UUID",uuid.toString() );//给每一个接口请求添加唯一识别码参数UUID
            Iterator<Map.Entry<String, String>> iterator = params.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry<String, String> entry =  iterator.next();
                log_msg += entry.getKey() + "=" + entry.getValue() + "&";
            }
        }
        if (need_show_dialog){
            activityListener.showLoadingDialog();
        }
        try {
            okHttpUtils.post().url(url).tag(request_tag).params(params).build().execute(new MyStringCallBack(request_tag));
        }catch (Exception ex){
            Log.d("LM", "okHttpUtils" + ex.getMessage());
        }
    }

    @Override
    public void mLoadFile(String tag) {

        final String strtag=tag;
        if (need_show_dialog){
            activityListener.showLoadingDialog();
        }
        // 用 get方式请求 下载文件，根据服务器来决定请求方式
        okHttpUtils.get().url(url).tag(strtag).build().execute(new MyFileCallBack(Environment.getExternalStorageDirectory().getAbsolutePath(),destFileName));

    }

    public void mUploadFile(String requst_tag){
        if (need_show_dialog) {
            activityListener.showLoadingDialog();
        }
          // okHttpUtils.postFile().url(url).file(uploadFile).tag(requst_tag).headers(params).build().execute();
          PostFormBuilder postformbuilder=okHttpUtils.post().url(url).params(params);
            Iterator iterator=uploadFilesParams.entrySet().iterator();
            while (iterator.hasNext()){
                Map.Entry entry= (Map.Entry) iterator.next();
                String strfile= (String) entry.getKey();
                File file= (File) entry.getValue();
                postformbuilder.addFile(strfile,file.getName(),file);
            }
            postformbuilder.build().execute(new MyStringCallBack(requst_tag));
    }

    public void cancleRequest(String...tags){
//        if (null != mRequestQueue){
//            for (String tag : tags)
//            mRequestQueue.cancelAll(tag);
//        }
        if (null!=okHttpUtils){
            for (String tag:tags){
                okHttpUtils.cancelTag(tag);
            }
        }
    }

    /**
     *@auther: Tom
     *created at 2016/6/6 11:15
     * 初始化OKhttpUtils对象
     */
    private OkHttpUtils createOkhttpUtils(long connectTimeOut,long readTimeOut,long writeTimeOuts) {
        OkHttpClient okHttpClient = new OkHttpClient.Builder().connectTimeout(connectTimeOut, TimeUnit.MILLISECONDS).readTimeout(readTimeOut,TimeUnit.MILLISECONDS).writeTimeout(writeTimeOuts,TimeUnit.MILLISECONDS).build();
        OkHttpUtils okHttpUtils = OkHttpUtils.initClient(okHttpClient);

        return okHttpUtils;
    }

    /**
     *@auther: Tom
     *created at 2016/6/7 10:18
     *OkHttpUtils的StringCallBack 回调对象类
     */
    private class MyStringCallBack extends StringCallback {
        private  String request_tag;

        public MyStringCallBack(String request_tag) {
            this.request_tag = request_tag;
        }
        @Override
        public void onResponse(String response, int id) {
//            Log.d("LM", "response: " + response);
            activityListener.cancelLoadingDialog();
            postMsg(response, request_tag);
        }
        @Override
        public void onError(Call call, Exception e, int id) {
            activityListener.cancelLoadingDialog();
            //----------------------------------------------------------------------------------------------------------
            if(mErrorListener!=null){
                mErrorListener.handleErrorListener();
            }else {
            //----------------------------------------------------------------------------------------------------------
                if (e instanceof SocketTimeoutException) {
                    if (need_show_toast) activityListener.showToastMsg("网络连接超时，请重试！");
                } else {
                    if (need_show_toast) activityListener.showToastMsg("网络请求失败！"+e.getClass());
                }
            }
            /**
             * 之所以在error中postmsg 是为了在activity中取消listview的刷新状态
             */
            postMsg("error", request_tag);
            e.printStackTrace();

        }


    }

    /**
     *@auther: Tom
     *created at 2016/6/7 10:19
     *OkHttpUtils 文件下载的FileCallBack 回调对象类
     * destFileDir  目标文件存储的文件夹路径
     * destFileName 目标文件存储的文件名
     */
    private class MyFileCallBack extends FileCallBack {
        float mprogress;
        public MyFileCallBack(String destFileDir, String destFileName) {
            super(destFileDir, destFileName);
        }

        @Override
        public void onError(Call call, Exception e, int id) {
            activityListener.cancelLoadingDialog();
            //----------------------------------------------------------------------------------------------------------
            if(mErrorListener!=null){
                mErrorListener.handleErrorListener();
            }else {
                //----------------------------------------------------------------------------------------------------------
                if (e instanceof TimeoutError) {
                    if (need_show_toast) activityListener.showToastMsg("网络超时，请重试！");
                } else {
                    if (need_show_toast) activityListener.showToastMsg("网络请求失败！");
                }
            }
            /**
             * 在文件下载中以文件名为Tag
             */
            postMsg("error", destFileName);
            e.printStackTrace();
        }

        @Override
        public void onResponse(File response, int id) {
            activityListener.cancelLoadingDialog();
            /**
             * 在文件下载中以文件名为Tag,以文件路径为返回的String值
             */
         //   postMsg(response.getAbsolutePath(), destFileName);
        }

        @Override
        public void inProgress(float progress, long total, int id) {
            super.inProgress(progress, total, id);
            if ((progress-mprogress)>0.01||progress==1) {
                 activityListener.setProgressBarLoading((int)(progress*100));
                   mprogress = progress;
                }
        }
    }

//    private class MyBitmapCallback extends BitmapCallback{
//
//        @Override
//        public void onError(Call call, Exception e, int id) {
//
//        }
//
//        @Override
//        public void onResponse(Bitmap response, int id) {
//
//        }
//    }
}
