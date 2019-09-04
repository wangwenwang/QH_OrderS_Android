package com.kaidongyuan.qh_orders_android;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.alibaba.fastjson.JSON;
import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.Poi;
import com.baidu.location.service.LocationService;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.kaidongyuan.qh_orders_android.Tools.Constants;
import com.kaidongyuan.qh_orders_android.Tools.LocationApplication;
import com.kaidongyuan.qh_orders_android.Tools.MPermissionsUtil;
import com.kaidongyuan.qh_orders_android.Tools.Tools;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends Activity {

    private final int SDK_PERMISSION_REQUEST = 127;
    private String permissionInfo;
    private LocationService locationService;


    public static WebView mWebView;

    public static Context mContext;

    String inputName;

    String appName;

    String address;

    double lng;

    double lat;

    public static String mAppVersion;

    // 微信开放平台APP_ID
    private static final String APP_ID = "";

//    static public IWXAPI mWxApi;

    public final static String DestFileName = "kdy-qh.apk";
    public final static String ZipFileName = "dist.zip";

    // zip解压路径
    String unZipOutPath;

    //5.0以下使用
    private ValueCallback<Uri> uploadMessage;
    // 5.0及以上使用
    private ValueCallback<Uri[]> uploadMessageAboveL;
    //图片
    private final static int FILE_CHOOSER_RESULT_CODE = 128;
    //拍照
    private final static int FILE_CAMERA_RESULT_CODE = 129;
    //拍照图片路径
    private String cameraFielPath;
    private Uri mImageUri;
    private static final String FILE_PROVIDER_AUTHORITY = "com.kaidongyuan.qh_orders_android.fileprovider";

    private String CURR_ZIP_VERSION = "0.0.0";
    private String WhoCheckVersion;

    //检测版本更新
    private final String TAG_CHECKVERSION = "check_version";
//    private OrderAsyncHttpClient mClient;
    private final int RequestPermission_STATUS_CODE0 = 8800;
    private RequestQueue mRequestQueue;
    private AlertDialog mUpdataVersionDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;
        Log.d("LM", "程序启动");

        try {
            mAppVersion = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        getPersimmions();

        unZipOutPath = "/data/data/" + getPackageName() + "/upzip/";

        // 设置ZIP版本号
        String curr_zip_version = Tools.getAppZipVersion(mContext);
        if (curr_zip_version != null && curr_zip_version.equals("")) {

            Tools.setAppZipVersion(mContext, CURR_ZIP_VERSION);
        }
        curr_zip_version = Tools.getAppZipVersion(mContext);
        Log.d("LM", "本地zip版本号：： " + curr_zip_version);

        appName = getResources().getString(R.string.app_name);

        mWebView = (WebView) findViewById((R.id.lmwebview));
        mWebView.getSettings().setTextZoom(100);

        mWebView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d("LM", "当前位置: " + url);
            }

            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);

            }

            // js拔打电话
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d("LM", "------------------------: ");

                if (url.startsWith("mailto:") || url.startsWith("geo:") || url.startsWith("tel:")) {

                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                }
                return true;
            }
        });


        //覆盖WebView默认使用第三方或系统默认浏览器打开网页的行为，使网页用WebView打开
        mWebView.setWebChromeClient(new WebChromeClient() {
            // For Android < 3.0
            public void openFileChooser(ValueCallback<Uri> valueCallback) {
                uploadMessage = valueCallback;
                openImageChooserActivity();
            }

            // For Android  >= 3.0
            public void openFileChooser(ValueCallback valueCallback, String acceptType) {
                uploadMessage = valueCallback;
                openImageChooserActivity();
            }

            //For Android  >= 4.1
            public void openFileChooser(ValueCallback<Uri> valueCallback, String acceptType, String capture) {
                uploadMessage = valueCallback;
                openImageChooserActivity();
            }

            // For Android >= 5.0
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                uploadMessageAboveL = filePathCallback;
                openImageChooserActivity();
                return true;
            }


            // 处理javascript中的alert
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                return false;
            }

            // 处理javascript中的confirm
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                return true;
            }

            // 处理定位权限请求
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
                super.onGeolocationPermissionsShowPrompt(origin, callback);
            }

            //            @Override
//            // 设置网页加载的进度条
//            public void onProgressChanged(WebView view, int newProgress) {
//                TestJSLocation.this.getWindow().setFeatureInt(
//                        Window.FEATURE_PROGRESS, newProgress * 100);
//                super.onProgressChanged(view, newProgress);
//            }
            // 设置应用程序的标题title
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
            }
        });

        // 获取上次启动记录的版本号
        String lastVersion = Tools.getAppLastTimeVersion(mContext);
        Log.d("LM", "上次启动记录的版本号: " + lastVersion);


        boolean isExists = Tools.fileIsExists("/data/data/" + getPackageName() + "/upzip/dist/index.html");
        if (lastVersion.equals(mAppVersion)) {

            Log.d("LM", "html已存在，无需解压");
        } else {

            Log.d("LM", "html不存在或有新版本，开始解压");
            try {
                Tools.unZip(mContext, "dist.zip", unZipOutPath, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.d("LM", "解压完成，加载html");
        }
        mWebView.loadUrl("file:///data/data/" + getPackageName() + "/upzip/dist/index.html");
        Tools.setAppLastTimeVersion(mContext);
        lastVersion = Tools.getAppLastTimeVersion(mContext);
        Log.d("LM", "上次启动记录的版本号已设置为: " + lastVersion);

        // 启用javascript
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setVerticalScrollbarOverlay(true);
//        mWebView.loadUrl("http://163xw.com/jsAlbum.html");

        // 在js中调用本地java方法
        mWebView.addJavascriptInterface(new JsInterface(this), "CallAndroidOrIOS");

        mWebView.setLongClickable(true);
        mWebView.setScrollbarFadingEnabled(true);
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mWebView.setDrawingCacheEnabled(true);

        initPermission();
    }

    private void initPermission() {

        Log.d("LM", "申请存储权限");

        try {

            if (Build.VERSION.SDK_INT >= 23) {
                if (MPermissionsUtil.checkAndRequestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}
                        , RequestPermission_STATUS_CODE0)) {
                    checkVersion("原生");
                }else {

                    new Thread() {
                        public void run() {
                            try {
                                sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    initPermission();
                                }
                            });
                        }
                    }.start();
                }
            } else {

                checkVersion("原生");
            }
        } catch (Exception e) {

            Log.d("LM", "initPermission: " + e.getMessage());
        }
    }


    private void openImageChooserActivity() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("拍照/相册");
//        builder.setPositiveButton("相册", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                takePhoto();
//            }
//        });
        builder.setNegativeButton("拍照", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                takeCamera();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    //拍照
    private void takeCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (hasSDCard()) {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File imageFile;
            try {

                imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
                cameraFielPath = imageFile.getPath();
            } catch (IOException e) {

                e.printStackTrace();
            }
            File outputImage = new File(cameraFielPath);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(outputImage));
            startActivityForResult(intent, FILE_CAMERA_RESULT_CODE);
        }
    }


    /**
     * 判断手机是否有SD卡。
     *
     * @return 有SD卡返回true，没有返回false。
     */
    public boolean hasSDCard() {

        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    private void takeCameraM() {
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//打开相机的Intent
        if (takePhotoIntent.resolveActivity(getPackageManager()) != null) {//这句作用是如果没有相机则该应用不会闪退，要是不加这句则当系统没有相机应用的时候该应用会闪退
            File imageFile = createImageFile();//创建用来保存照片的文件
            if (imageFile != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    /*7.0以上要通过FileProvider将File转化为Uri*/
                    mImageUri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, imageFile);
                } else {
                    /*7.0以下则直接使用Uri的fromFile方法将File转化为Uri*/
                    mImageUri = Uri.fromFile(imageFile);
                }
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);//将用于输出的文件Uri传递给相机
                startActivityForResult(takePhotoIntent, FILE_CAMERA_RESULT_CODE);//打开相机
            }
        } else {
        }
    }


    /**
     * 创建用来存储图片的文件，以时间来命名就不会产生命名冲突
     *
     * @return 创建的图片文件
     */
    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = null;
        try {
            imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
            cameraFielPath = imageFile.getPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageFile;
    }


    @TargetApi(23)
    private void getPersimmions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissions = new ArrayList<String>();
            /***
             * 定位权限为必须权限，用户如果禁止，则每次进入都会申请
             */
            // 定位精确位置
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            /*
             * 读写权限和电话状态权限非必要权限(建议授予)只会申请一次，用户同意或者禁止，只会弹一次
             */
            // 读写权限
            if (addPermission(permissions, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissionInfo += "Manifest.permission.WRITE_EXTERNAL_STORAGE Deny \n";
            }
            if (permissions.size() > 0) {
                requestPermissions(permissions.toArray(new String[permissions.size()]), SDK_PERMISSION_REQUEST);
            }
        }
    }


    @TargetApi(23)
    private boolean addPermission(ArrayList<String> permissionsList, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) { // 如果应用没有获得对应权限,则添加到列表中,准备批量申请
            if (shouldShowRequestPermissionRationale(permission)) {
                return true;
            } else {
                permissionsList.add(permission);
                return false;
            }

        } else {
            return true;
        }
    }


//    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // TODO Auto-generated method stub
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101) {


            Log.d("LM", "拍照5.9.1: ");
            takeCameraM();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d("LM", "onActivityResult: ----");

        super.onActivityResult(requestCode, resultCode, data);

        if (null == uploadMessage && null == uploadMessageAboveL) return;

        if (resultCode != RESULT_OK) {//同上所说需要回调onReceiveValue方法防止下次无法响应js方法

            if (uploadMessageAboveL != null) {
                uploadMessageAboveL.onReceiveValue(null);
                uploadMessageAboveL = null;
            }
            if (uploadMessage != null) {
                uploadMessage.onReceiveValue(null);
                uploadMessage = null;
            }
            return;
        }

        Uri result = null;
        if (requestCode == FILE_CAMERA_RESULT_CODE) {

            if (result == null && hasFile(cameraFielPath)) {

                result = Uri.fromFile(new File(cameraFielPath));
            }
            if (uploadMessageAboveL != null) {

                uploadMessageAboveL.onReceiveValue(new Uri[]{result});

                uploadMessageAboveL = null;
            } else if (uploadMessage != null) {

                uploadMessage.onReceiveValue(result);
                uploadMessage = null;
            }
        } else if (requestCode == FILE_CHOOSER_RESULT_CODE) {

            if (data != null) {
                result = data.getData();
            }
            if (uploadMessageAboveL != null) {
                onActivityResultAboveL(data);
            } else if (uploadMessage != null) {
                uploadMessage.onReceiveValue(result);
                uploadMessage = null;
            }
        }
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void onActivityResultAboveL(Intent intent) {
        Uri[] results = null;
        if (intent != null) {
            String dataString = intent.getDataString();
            ClipData clipData = intent.getClipData();
            if (clipData != null) {
                results = new Uri[clipData.getItemCount()];
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    ClipData.Item item = clipData.getItemAt(i);
                    results[i] = item.getUri();
                }
            }
            if (dataString != null)
                results = new Uri[]{Uri.parse(dataString)};
        }
        uploadMessageAboveL.onReceiveValue(results);
        uploadMessageAboveL = null;
    }


    /**
     * 判断文件是否存在
     */
    public static boolean hasFile(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) {
                return false;
            }
        } catch (Exception e) {
            // TODO: handle exception
            return false;
        }
        return true;
    }


    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
        // -----------location config ------------
        locationService = ((LocationApplication) getApplication()).locationService;
        //获取locationservice实例，建议应用中只初始化1个location实例，然后使用，可以参考其他示例的activity，都是通过此种方式获取locationservice实例的
        locationService.registerListener(mListener);
        //注册监听
        int type = getIntent().getIntExtra("from", 0);
        if (type == 0) {
            locationService.setLocationOption(locationService.getDefaultLocationClientOption());
        } else if (type == 1) {
            locationService.setLocationOption(locationService.getOption());
        }
    }


    /***
     * Stop location service
     */
    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        locationService.unregisterListener(mListener); //注销掉监听
        locationService.stop(); //停止定位服务
        super.onStop();
    }


    /*****
     *
     * 定位结果回调，重写onReceiveLocation方法，可以直接拷贝如下代码到自己工程中修改
     *
     */
    private BDAbstractLocationListener mListener = new BDAbstractLocationListener() {

        @Override
        public void onReceiveLocation(BDLocation location) {
            // TODO Auto-generated method stub
            if (null != location && location.getLocType() != BDLocation.TypeServerError) {

                address = location.getAddrStr();
                lng = location.getLongitude();
                lat = location.getLatitude();

                String url = "javascript:SetCurrAddress('" + address + "','" + lng + "','" + lat + "')";
                MainActivity.mWebView.loadUrl(url);
                Log.d("LM", url);

                StringBuffer sb = new StringBuffer(256);
                sb.append("time : ");
                /**
                 * 时间也可以使用systemClock.elapsedRealtime()方法 获取的是自从开机以来，每次回调的时间；
                 * location.getTime() 是指服务端出本次结果的时间，如果位置不发生变化，则时间不变
                 */
                sb.append(location.getTime());
                sb.append("\nlocType : ");// 定位类型
                sb.append(location.getLocType());
                Log.d("LM", "" + location.getLocType());
                sb.append("\nlocType description : ");// *****对应的定位类型说明*****
                sb.append(location.getLocTypeDescription());
                sb.append("\nlatitude : ");// 纬度
                sb.append(location.getLatitude());
                sb.append("\nlontitude : ");// 经度
                sb.append(location.getLongitude());
                sb.append("\nradius : ");// 半径
                sb.append(location.getRadius());
                sb.append("\nCountryCode : ");// 国家码
                sb.append(location.getCountryCode());
                sb.append("\nCountry : ");// 国家名称
                sb.append(location.getCountry());
                sb.append("\ncitycode : ");// 城市编码
                sb.append(location.getCityCode());
                sb.append("\ncity : ");// 城市
                sb.append(location.getCity());
                sb.append("\nDistrict : ");// 区
                sb.append(location.getDistrict());
                sb.append("\nStreet : ");// 街道
                sb.append(location.getStreet());
                sb.append("\naddr : ");// 地址信息
                sb.append(location.getAddrStr());
                sb.append("\nUserIndoorState: ");// *****返回用户室内外判断结果*****
                sb.append(location.getUserIndoorState());
                sb.append("\nDirection(not all devices have value): ");
                sb.append(location.getDirection());// 方向
                sb.append("\nlocationdescribe: ");
                sb.append(location.getLocationDescribe());// 位置语义化信息
                sb.append("\nPoi: ");// POI信息
                if (location.getPoiList() != null && !location.getPoiList().isEmpty()) {
                    for (int i = 0; i < location.getPoiList().size(); i++) {
                        Poi poi = (Poi) location.getPoiList().get(i);
                        sb.append(poi.getName() + ";");
                    }
                }
                if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
                    sb.append("\nspeed : ");
                    sb.append(location.getSpeed());// 速度 单位：km/h
                    sb.append("\nsatellite : ");
                    sb.append(location.getSatelliteNumber());// 卫星数目
                    sb.append("\nheight : ");
                    sb.append(location.getAltitude());// 海拔高度 单位：米
                    sb.append("\ngps status : ");
                    sb.append(location.getGpsAccuracyStatus());// *****gps质量判断*****
                    sb.append("\ndescribe : ");
                    sb.append("gps定位成功");
                } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
                    // 运营商信息
                    if (location.hasAltitude()) {// *****如果有海拔高度*****
                        sb.append("\nheight : ");
                        sb.append(location.getAltitude());// 单位：米
                    }
                    sb.append("\noperationers : ");// 运营商信息
                    sb.append(location.getOperators());
                    sb.append("\ndescribe : ");
                    sb.append("网络定位成功");
                } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
                    sb.append("\ndescribe : ");
                    sb.append("离线定位成功，离线定位结果也是有效的");
                } else if (location.getLocType() == BDLocation.TypeServerError) {
                    sb.append("\ndescribe : ");
                    sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
                } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                    sb.append("\ndescribe : ");
                    sb.append("网络不同导致定位失败，请检查网络是否通畅");
                } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
                    sb.append("\ndescribe : ");
                    sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
                }

                locationService.stop();
            }
        }

    };

    // js调用java
    private class JsInterface extends Activity {
        private Context mContext;

        public JsInterface(Context context) {
            this.mContext = context;
        }

        // 经纬坐标转地址，抽象函数
        OnGetGeoCoderResultListener listener = new OnGetGeoCoderResultListener() {
            public void onGetGeoCodeResult(GeoCodeResult result) {
            }

            @Override
            public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
            }
        };

        @JavascriptInterface
        public void callAndroid(String exceName) {

            Log.d("LM", "执行:" + exceName);

            if (exceName.equals("检查版本更新")) {

                // 开启定位服务
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        checkVersion("vue");
                    }
                });
            }
        }

        //在js中调用window.CallAndroidOrIOS.callAndroid(name)，便会触发此方法。
        @JavascriptInterface
        public void callAndroid(String exceName, final String inputName) {

            Log.d("LM", "执行:" + exceName + "    " + "输入框:" + inputName);
            MainActivity.this.inputName = inputName;

            final SharedPreferences sp = mContext.getSharedPreferences(Constants.SP_W_UserInfo_Key, MODE_MULTI_PROCESS);


            if (exceName.equals("微信登录")) {

                Log.d("LM", "微信登录");

                new Thread() {
                    public void run() {

//                        if (!mWxApi.isWXAppInstalled()) {
//                            Log.d("LM", "您还未安装微信客户端");
//                            return;
//                        } else {
//                            Log.d("LM", "微信客户端已安装");
//                        }
//                        SendAuth.Req req = new SendAuth.Req();
//                        req.scope = "snsapi_userinfo";//官方固定写法
//                        req.state = "wechat_sdk_tms";//自定义一个字串
//
//                        mWxApi.sendReq(req);
                    }
                }.start();
            } else if (exceName.equals("登录页面已加载")) {

                Log.d("LM", "登录页面已加载");

                final String u = sp.getString("UserName", "");
                final String p = sp.getString("Password", "");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        String url = "javascript:SetUserNameAndPassword('" + u + "','" + p + "')";
                        MainActivity.mWebView.loadUrl(url);
                        Log.d("LM", url);

                        url = "javascript:VersionShow('" + "版本:" + Tools.getVerName(mContext) + "')";
                        MainActivity.mWebView.loadUrl(url);
                        Log.d("LM", url);

                        url = "javascript:Device_Ajax('android')";
                        MainActivity.mWebView.loadUrl(url);
                        Log.d("LM", url);
                    }
                });
            } else if (exceName.equals("获取当前位置页面已加载")) {

                Log.d("LM", "获取当前位置页面已加载");
                locationService.start();
            } else if (exceName.equals("导航")) {

                Log.d("LM", "导航");

                new Thread() {

                    public void run() {


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                Tools.ToNavigation(inputName, mContext, appName);
                            }
                        });
                    }
                }.start();
            } else if (exceName.equals("查看路线")) {

                Log.d("LM", "查看路线");

//                new Thread() {
//
//                    public void run() {
//
//
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//
//                                Intent intent2 = new Intent(LoginActivity.mContext, OrderTrackActivity.class);
//                                intent2.putExtra("order_IDX", inputName);
//                                mContext.startActivity(intent2);
//                            }
//                        });
//                    }
//                }.start();
            }
        }

        @JavascriptInterface
        public void callAndroid(String exceName, String u, String p) {

            Log.d("LM", "执行:" + exceName + "    " + "SetCurrAddressSetCurrAddress名:" + u + "    " + "密码:" + p);

            if (exceName.equals("记住帐号密码")) {

                if (u != null && p != null && !u.equals("") && !p.equals("")) {

                    SharedPreferences sp = mContext.getSharedPreferences(Constants.SP_W_UserInfo_Key, MODE_MULTI_PROCESS);
                    sp.edit().putString("UserName", u).apply();
                    sp.edit().putString("Password", p).apply();
                }
            }
        }
    }

    public void checkVersion(String who) {
        return;
//        this.WhoCheckVersion = who;
//
//        if (mRequestQueue == null) {
//            mRequestQueue = Volley.newRequestQueue(this.getApplicationContext());
//
//        }
//
//        Log.d("LM", "检查apk及zip版本");
//        Map<String, String> params = new HashMap<>();
//        params.put("params", "{\"tenantCode\":\"KDY\"}");
////        mClient.sendRequest(Constants.URL.SAAS_API_BASE + "queryAppVersion.do", params, TAG_CHECKVERSION);
//
//
//        StringRequest mStringRequest = new StringRequest(Request.Method.POST,
//                "http://192.168.20.113:8880/cyscm/easyToSell/queryAppVersion.do?params={\"tenantCode\":\"KDY\"}", new Response.Listener<String>() {
//            @Override
//            public void onResponse(String response) {
//
//                Log.d("LM", "checkVersion1: ");
////                com.alibaba.fastjson.JSONObject jo = JSON.parseObject(response);
////                int type = Integer.parseInt(jo.getString("type"));
//
//                com.alibaba.fastjson.JSONObject jo = JSON.parseObject(response);
//
//                String status = jo.getString("status");
//
//                String apkDownloadUrl = null;
//                String server_apkVersion = null;
//                String zipDownloadUrl = null;
//                String server_zipVersion = null;
//                if (status.equals("1")) {
//
//                    com.alibaba.fastjson.JSONObject dict = jo.getJSONObject("data");
//                    apkDownloadUrl = dict.getString("downloadUrl");
//                    server_apkVersion = dict.getString("versionNo");
//                    zipDownloadUrl = dict.getString("zipDownloadUrl");
//                    server_zipVersion = dict.getString("zipVersionNo");
//                }
//
//                if (server_apkVersion != null && apkDownloadUrl != null) {
//                    try {
//                        String current_apkVersion = mAppVersion;
//                        Log.d("LM","server_apkVersion:" + server_apkVersion + "\tcurrent_apkVersion:" + current_apkVersion);
//
//                        int compareVersion = Tools.compareVersion(server_apkVersion, current_apkVersion);
//                        if (compareVersion == 1) {
//
//                            createUpdateDialog(current_apkVersion, server_apkVersion, apkDownloadUrl);
////                            minefragment.isupdate = true;
//                        } else {
//
//                            Log.d("LM", "apk为最新版本");
//
//                            String curr_zipVersion = Tools.getAppZipVersion(mContext);
//                            compareVersion = Tools.compareVersion(server_zipVersion, curr_zipVersion);
//                            if (compareVersion == 1) {
//
//                                Log.d("LM", "服务器zip版本：" + server_zipVersion + "    " + "本地zip版本：" + CURR_ZIP_VERSION);
//                                CURR_ZIP_VERSION = server_zipVersion;
//                                Log.d("LM", "更新zip...");
////                                showUpdataZipDialog(zipDownloadUrl);
//                            } else {
//
//                                Log.d("LM", "zip为最新版本");
//
//                                if(WhoCheckVersion.equals("vue")) {
//                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//                                    builder.setTitle("");
//                                    builder.setMessage("已经是最新版本！");
//                                    builder.setPositiveButton("确定", null);
//                                    builder.show();
//                                }
//                            }
////                            checkGpsState();
//                        }
//                    } catch (Exception e) {
//                        Log.d("LM", "NameNotFoundException" + e.getMessage());
//                        e.printStackTrace();
//                    }
//                }
//
//            }
//        }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//
//                Log.d("LM", "checkVersion2: ");
//                error.printStackTrace();
//            }
//        });
//        mStringRequest.setRetryPolicy(new DefaultRetryPolicy(30*1000, 1, 1.0f));  // 设置超时
//        mStringRequest.setTag("FJAKSLDFJ");
//        mRequestQueue.add(mStringRequest);
    }


    /**
     * 版本更新对话框
     *
     * @param currentVersion 当前版本versionName
     * @param version        最新版本versionName
     * @param downUrl        最新版本安装包下载url
     */
    public void createUpdateDialog(String currentVersion, String version, final String downUrl) {
        if (mUpdataVersionDialog == null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setMessage("当前版本：" + currentVersion + "\n最新版本：" + version);
            builder.setCancelable(false);
            builder.setTitle("更新版本");
            builder.setPositiveButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mUpdataVersionDialog.cancel();
                }
            });
            builder.setNegativeButton("下载", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    mUpdataVersionDialog.cancel();
                    Log.d("LM", "update.url:" + downUrl);
                    //以存储文件名为Tag名
//                    mClient.sendFileRequest(downUrl, DestFileName);
                }
            });
            mUpdataVersionDialog = builder.create();
        }
        mUpdataVersionDialog.show();
    }


    /**
     * 返回上一页
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {

            // 登录页时不允许返回上一页
            String curURL = mWebView.getUrl();
            String orgURL = mWebView.getOriginalUrl();
            if (curURL.equals("file:///data/data/com.kaidongyuan.qh_orders_android/upzip/dist/index.html#/")) {

                Log.d("LM", "禁止返回上一页1：" + curURL);
                return false;
            }

            // 首页
            String Index = "file:///data/data/com.kaidongyuan.qh_orders_android/upzip/dist/index.html#/Index";
            // 下单
            String Waybill = "file:///data/data/com.kaidongyuan.qh_orders_android/upzip/dist/index.html#/SalesOrder";
            // 查单
            String ReportForms = "file:///data/data/com.kaidongyuan.qh_orders_android/upzip/dist/index.html#/QuerySalesOrder";
            // 我的
            String HomeIndex = "file:///data/data/com.kaidongyuan.qh_orders_android/upzip/dist/index.html#/Home";

            // 主菜单时不允许返回上一页
            if (
                    curURL.indexOf(Index + "?") != -1 || curURL.equals(Index) ||
                            curURL.indexOf(Waybill + "?") != -1 || curURL.equals(Waybill) ||
                            curURL.indexOf(ReportForms + "?") != -1 || curURL.equals(ReportForms) ||
                            curURL.indexOf(HomeIndex + "?") != -1 || curURL.equals(HomeIndex)
                    ) {

                Log.d("LM", "禁止返回上一页2：" + curURL);
                return false;
            }
            mWebView.goBack();
            Log.d("LM", "curURL: " + curURL);
            Log.d("LM", "orgURL: " + orgURL);
        }
        return false;
    }
}