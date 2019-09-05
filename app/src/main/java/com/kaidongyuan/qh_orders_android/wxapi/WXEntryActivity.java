package com.kaidongyuan.qh_orders_android.wxapi;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.kaidongyuan.qh_orders_android.MainActivity;
import com.kaidongyuan.qh_orders_android.Tools.Constants;
import com.kaidongyuan.qh_orders_android.Tools.Tools;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;


import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;


public class WXEntryActivity extends Activity implements IWXAPIEventHandler {

    private static final int RETURN_MSG_TYPE_LOGIN = 1;
    private static final int RETURN_MSG_TYPE_SHARE = 2;
    String wxCode = "";
    private static String wxOpenId = "";
    private static String WXBind_YES_Ajax_PARAMS = "";
    private static String WXBind_NO_Ajax_PARAMS = "";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("LM", "WXEntryActivity onCreate: ");

        //如果没回调onResp，八成是这句没有写
        MainActivity.mWxApi.handleIntent(getIntent(), this);
    }

    // 微信发送请求到第三方应用时，会回调到该方法
    @Override
    public void onReq(BaseReq req) {

        Toast.makeText(this, "执行了onReq函数", Toast.LENGTH_LONG).show();
    }

    // 第三方应用发送到微信的请求处理后的响应结果，会回调到该方法
    //app发送消息给微信，处理返回消息的回调
    @Override
    public void onResp(BaseResp resp) {
        Log.d("LM", "微信登录错误码 : " + resp.errCode + "");
        switch (resp.errCode) {

            case BaseResp.ErrCode.ERR_AUTH_DENIED:
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                if (RETURN_MSG_TYPE_SHARE == resp.getType()) {

                    Toast.makeText(this, "分享失败", Toast.LENGTH_LONG).show();
                }
                else  {

                    Toast.makeText(this, "登录失败", Toast.LENGTH_LONG).show();
                }
                break;
            case BaseResp.ErrCode.ERR_OK:
                switch (resp.getType()) {
                    case RETURN_MSG_TYPE_LOGIN:
                        //拿到了微信返回的code,立马再去请求access_token
                        String code = ((SendAuth.Resp) resp).code;
                        this.wxCode = code;
                        Log.d("LM", "code = " + code);

                        //就在这个地方，用网络库什么的或者自己封的网络api，发请求去咯，注意是get请求
                        new Thread(){
                            public void run() {

                                GetOpenId();
                            };
                        }.start();
                        break;

                    case RETURN_MSG_TYPE_SHARE:
                        Toast.makeText(this, "登录微信分享成功", Toast.LENGTH_LONG).show();
                        finish();
                        break;
                }
                break;
        }
    }

    /**
     * 使用get方式与服务器通信
     * @return
     */
    public String GetOpenId(){

        String appid = Constants.WXLogin_AppID;
        String appsecret = Constants.WXLogin_AppSecret;
        String Strurl = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + appid + "&secret=" + appsecret + "&code=" + this.wxCode + "&grant_type=authorization_code";

        HttpURLConnection conn=null;
        try {

            URL url = new URL(Strurl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestMethod("GET");
            if(HttpURLConnection.HTTP_OK==conn.getResponseCode()){

                Log.i("LM","get请求成功");
                InputStream in=conn.getInputStream();
                String resultStr = Tools.inputStream2String(in);
                resultStr = URLDecoder.decode(resultStr,"UTF-8");
                Log.i("LM",resultStr);

                try {
                    JSONObject jsonObj = (JSONObject)(new JSONParser().parse(resultStr));
                    String openid = (String)jsonObj.get("openid");
                    wxOpenId = openid;
                    Log.i("LM","openid：" + openid);


                    new Thread(){
                        public void run() {

                            bindingWX(wxOpenId);

                        };
                    }.start();


                } catch (ParseException e) {
                    e.printStackTrace();
                }
                in.close();
            }
            else {
                Log.i("LM","get请求失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        finally{
            conn.disconnect();
        }
        return null;
    }

    /**
     * 使用get方式与服务器通信
     * @return
     */
    public String bindingWX(String openid){

//        openid = "fds";

        String params = "{\"wxOpenid\":\"" + openid + "\"}";
        String paramsEncoding = URLEncoder.encode(params);
        String Strurl = Tools.getServerAddress(MainActivity.mContext) + "login.do?params=" + paramsEncoding;
        Log.d("LM", "登录链接: " + Strurl);

        HttpURLConnection conn=null;
        try {

            URL url = new URL(Strurl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestMethod("GET");
            if(HttpURLConnection.HTTP_OK==conn.getResponseCode()){

                Log.d("LM","bindingWX请求成功");
                InputStream in=conn.getInputStream();
                String resultStr = Tools.inputStream2String(in);
                resultStr = URLDecoder.decode(resultStr,"UTF-8");

                try {
                    JSONObject jsonObj = (JSONObject)(new JSONParser().parse(resultStr));
                    Log.i("LM",jsonObj.toJSONString() + "\n" + jsonObj.getClass());
                    String status = (String)jsonObj.get("status");
                    String Msg = (String)jsonObj.get("Msg");
                    if(status.equals("1")) {

                        String jsonArray = jsonObj.toJSONString().toString();
                        String LMEncoding = URLEncoder.encode(jsonArray);
                        WXBind_YES_Ajax_PARAMS = LMEncoding;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String LMurl = "javascript:WXBind_YES_Ajax('" + WXBind_YES_Ajax_PARAMS + "')";
                                MainActivity.mWebView.loadUrl(LMurl);
                            }
                        });
                    } else if(status.equals("3")) {

                            WXBind_NO_Ajax_PARAMS = openid;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String LMurl = "javascript:WXBind_NO_Ajax('" + WXBind_NO_Ajax_PARAMS + "')";
                                    MainActivity.mWebView.loadUrl(LMurl);
                                }
                            });
                    }else {

                        Toast.makeText(this, "bindingWX接口返回值status：" + status, Toast.LENGTH_LONG).show();
                    }


                } catch (ParseException e) {
                    e.printStackTrace();
                }
                in.close();
            }
            else {
                Log.i("LM","get请求失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        finally{
            conn.disconnect();
            finish();
        }
        return null;
    }
}
