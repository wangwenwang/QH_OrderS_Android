package com.kaidongyuan.qh_orders_android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.os.Bundle;
import android.text.TextUtils;
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
import android.widget.Toast;

import com.allenliu.versionchecklib.v2.AllenVersionChecker;
import com.allenliu.versionchecklib.v2.builder.UIData;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.android.volley.RequestQueue;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.Poi;
import com.baidu.location.service.LocationService;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baoyz.actionsheet.ActionSheet;
import com.kaidongyuan.qh_orders_android.Tools.Constants;
import com.kaidongyuan.qh_orders_android.Tools.DownPicUtil;
import com.kaidongyuan.qh_orders_android.Tools.LocationApplication;
import com.kaidongyuan.qh_orders_android.Tools.MPermissionsUtil;
import com.kaidongyuan.qh_orders_android.Tools.SystemUtil;
import com.kaidongyuan.qh_orders_android.Tools.Tools;
import com.kaidongyuan.qh_orders_android.print.PrintActivity;
import com.kaidongyuan.qh_orders_android.track.OrderTrackActivity;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.widget.Toast.LENGTH_LONG;


public class MainActivity extends FragmentActivity implements
        ActionSheet.ActionSheetListener {

    private final int SDK_PERMISSION_REQUEST = 127;
    private final int RequestAddContact = 1001;
    private String permissionInfo;
    private LocationService locationService;

    public static WebView mWebView;

    public static Context mContext;

    String inputName = "";

    String appName;

    String address;

    double lng;

    double lat;

    // APP当前版本号
    public static String mAppVersion;


    // 微信开放平台APP_ID
    private static final String APP_ID = Constants.WXLogin_AppID;

    static public IWXAPI mWxApi;

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

    private String CURR_ZIP_VERSION = "0.6.4";
    private String WhoCheckVersion;

    //检测版本更新
    private final String TAG_CHECKVERSION = "check_version";
//    private OrderAsyncHttpClient mClient;
    private final int RequestPermission_STATUS_CODE0 = 8800;
    private RequestQueue mRequestQueue;

    // 点击物理返回键的次数
    private int return_key_times = 0;

    private AMapLocationClient locationClient = null;

    //声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 修复targetSdkVersion为28时，拍照闪退问题
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy( builder.build());
        }

        mContext = this;
        Log.d("LM", "程序启动");

        try {
            mAppVersion = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        getPersimmions();

        unZipOutPath = "/data/data/" + getPackageName() + "/upzip/";

        // 首次安装APP，设置ZIP版本号
        String curr_zip_version = Tools.getAppZipVersion(mContext);
        if (curr_zip_version != null && curr_zip_version.equals("")) {

            Tools.setAppZipVersion(mContext, CURR_ZIP_VERSION);
        }

        curr_zip_version = Tools.getAppZipVersion(mContext);
        Log.d("LM", "本地zip版本号：： " + curr_zip_version);

        appName = getResources().getString(R.string.app_name);

        mWebView = (WebView) findViewById((R.id.lmwebview));





        /************************** 高德地图H5辅助定位开始 **************************/
        //初始化AMapLocationClientOption对象
        mLocationOption = new AMapLocationClientOption();
        /**
         * 设置定位场景，目前支持三种场景（签到、出行、运动，默认无场景）
         */
        mLocationOption.setLocationPurpose(AMapLocationClientOption.AMapLocationPurpose.SignIn);
        if(null != locationClient){
            locationClient.setLocationOption(mLocationOption);
            //设置场景模式后最好调用一次stop，再调用start以保证场景模式生效
            locationClient.stopLocation();
            locationClient.startLocation();
        }
        //设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式。
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //获取一次定位结果：
        //该方法默认为false。
        mLocationOption.setOnceLocation(true);
        //获取最近3s内精度最高的一次定位结果：
        //设置setOnceLocationLatest(boolean b)接口为true，启动定位时SDK会返回最近3s内精度最高的一次定位结果。如果设置其为true，setOnceLocation(boolean b)接口也会被设置为true，反之不会，默认为false。
        mLocationOption.setOnceLocationLatest(true);
        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.setNeedAddress(true);
        locationClient = new AMapLocationClient(getApplicationContext());
        //给定位客户端对象设置定位参数
        locationClient.setLocationOption(mLocationOption);
        //启动定位
        locationClient.startLocation();
        //建议在设置webView参数之前调用启动H5辅助定位接口
        locationClient.startAssistantLocation(mWebView);
        /************************** 高德地图H5辅助定位结束 **************************/





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

        // 长按点击事件
        mWebView.setOnLongClickListener(new View.OnLongClickListener() {

            Handler handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    String picFile = (String) msg.obj;
                    String[] split = picFile.split("/");
                    String fileName = split[split.length - 1];
                    try {
                        MediaStore.Images.Media.insertImage(getApplicationContext().getContentResolver(), picFile, fileName, null);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    // 最后通知图库更新
                    getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + picFile)));
                    Toast.makeText(mContext, "图片保存成功", Toast.LENGTH_LONG).show();
                }
            };

            @Override
            public boolean onLongClick(View view) {
                final WebView.HitTestResult hitTestResult = mWebView.getHitTestResult();
                // 如果是图片类型或者是带有图片链接的类型
                if (hitTestResult.getType() == WebView.HitTestResult.IMAGE_TYPE ||
                        hitTestResult.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                    // 弹出保存图片的对话框
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle("");
                    builder.setMessage("保存图片到本地");
                    builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            final String url = hitTestResult.getExtra();

                            // 下载图片到本地
                            DownPicUtil.downPic(url, new DownPicUtil.DownFinishListener() {

                                @Override
                                public void getDownPath(String s) {
                                    Toast.makeText(mContext, "下载完成", Toast.LENGTH_LONG).show();
                                    Message msg = Message.obtain();
                                    msg.obj = s;
                                    handler.sendMessage(msg);
                                }
                            });
                        }
                    });
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        // 自动dismiss
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return true;
                }else{
                    return false;
                }
            }
        });


        // 获取上次启动记录的版本号
        String lastVersion = Tools.getAppLastTimeVersion(mContext);
        Log.d("LM", "上次启动记录的版本号: " + lastVersion);


        boolean isExists = Tools.fileIsExists("/data/data/" + getPackageName() + "/upzip/dist/index.html");
        if (!lastVersion.equals("")) {

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

        // 注册微信登录
        registToWX();

        if(this.isLocationEnabled() == false){
            Toast.makeText(mContext, "请开启GPS位置权限，用于客户拜访功能", Toast.LENGTH_LONG).show();
        }
    }

    public boolean isLocationEnabled() {
        int locationMode = 0;
        String locationProviders;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        } else {
            locationProviders = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    private void registToWX() {
        //AppConst.WEIXIN.APP_ID是指你应用在微信开放平台上的AppID，记得替换。
        mWxApi = WXAPIFactory.createWXAPI(this, APP_ID, false);
        // 将该app注册到微信
        mWxApi.registerApp(APP_ID);
    }

    private void initPermission() {

        Log.d("LM", "申请存储权限");

        try {

            if (Build.VERSION.SDK_INT >= 23) {
                if (MPermissionsUtil.checkAndRequestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}
                        , RequestPermission_STATUS_CODE0)) {

                    new Thread() {
                        public void run() {
                            while (true){

                                Log.d("LM", "未获取服务器地址，等待一秒1");
                                try {
                                    sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if(Tools.getServerAddress(MainActivity.mContext) != ""){
                                    checkVersion("原生");
                                    break;
                                }
                            }
                        }
                    }.start();
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

                new Thread() {
                    public void run() {
                        while (true){

                            Log.d("LM", "未获取服务器地址，等待一秒2");
                            try {
                                sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if(Tools.getServerAddress(MainActivity.mContext) != ""){
                                String fds = Tools.getServerAddress(MainActivity.mContext);
                                checkVersion("原生");
                                break;
                            }
                        }
                    }
                }.start();
            }
        } catch (Exception e) {

            Log.d("LM", "initPermission: " + e.getMessage());
        }
    }


    private void openImageChooserActivity() {

        takeCamera();
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
        else if(requestCode== 1089) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                intentToContact();
            } else {
                Toast.makeText(MainActivity.this,"授权被禁止",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void intentToContact() {
        // 跳转到联系人界面
        Intent intent = new Intent();
        intent.setAction("android.intent.action.PICK");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setType("vnd.android.cursor.dir/phone_v2");
        startActivityForResult(intent, 1088);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d("LM", "onActivityResult: ----");

        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==1088) {
            if (data != null) {
                Uri uri = data.getData();
                String phoneNum = null;
                String contactName = null;
                // 创建内容解析者
                ContentResolver contentResolver = getContentResolver();
                Cursor cursor = null;
                if (uri != null) {
                    cursor = contentResolver.query(uri,
                            new String[]{"display_name","data1"}, null, null, null);
                }
                while (cursor.moveToNext()) {
                    contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    phoneNum = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                }
                cursor.close();
                //  把电话号码中的  -  符号 替换成空格
                if (phoneNum != null) {
                    phoneNum = phoneNum.replaceAll("-", " ");
                    // 空格去掉  为什么不直接-替换成"" 因为测试的时候发现还是会有空格 只能这么处理
                    phoneNum= phoneNum.replaceAll(" ", "");
                }

                Log.d("LM", "contactName:" + contactName);
                Log.d("LM", "phoneNum:" + phoneNum);

                String url1 = "javascript:SetContactPeople('" + contactName + "','" + phoneNum + "')";
                MainActivity.mWebView.loadUrl(url1);
                Log.d("LM", url1);
            }
        }

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
                if(address != null){
                    MainActivity.mWebView.loadUrl(url);
                }else{
                    if(isLocationEnabled() == false){
                        Toast.makeText(mContext, "请开启GPS位置权限，用于客户拜访功能", Toast.LENGTH_LONG).show();
                    }
                }
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

            if (exceName.equals("检查APP和VUE版本更新")) {
                new Thread() {
                    public void run() {

                        checkVersion("vue");

                    }
                }.start();
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

                        if (!mWxApi.isWXAppInstalled()) {
                            Log.d("LM", "您还未安装微信客户端");
                            return;
                        } else {
                            Log.d("LM", "微信客户端已安装");
                        }
                        SendAuth.Req req = new SendAuth.Req();
                        req.scope = "snsapi_userinfo";//官方固定写法
                        req.state = "wechat_sdk_tms";//自定义一个字串

                        mWxApi.sendReq(req);
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
            } else if (exceName.equals("查看路线")) {

                Log.d("LM", "查看路线");

            } else if (exceName.equals("打印")) {

                new Thread() {

                    public void run() {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                Intent printIntent = new Intent(MainActivity.mContext, PrintActivity.class);
                                printIntent.putExtra("json_print", inputName);
                                mContext.startActivity(printIntent);
                            }
                        });
                    }
                }.start();
            }
            // 服务器地址
            else if(exceName.equals("服务器地址")) {

                Tools.setServerAddress(mContext, inputName);
            }
            // 调用通讯录
            else if(exceName.equals("调用通讯录")) {
//
                new Thread() {
                    public void run() {

//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {

                                //**版本判断。当手机系统大于 23 时，才有必要去判断权限是否获取**
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    //ContextCompat.checkSelfPermission() 方法 指定context和某个权限 返回PackageManager.PERMISSION_DENIED或者PackageManager.PERMISSION_GRANTED
                                    if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.READ_CONTACTS)
                                            != PackageManager.PERMISSION_GRANTED) {
                                        // 若不为GRANTED(即为DENIED)则要申请权限了
                                        // 申请权限 第一个为context 第二个可以指定多个请求的权限 第三个参数为请求码
                                        ActivityCompat.requestPermissions(MainActivity.this,
                                                new String[]{android.Manifest.permission.READ_CONTACTS},
                                                1089);
                                    } else {
                                        //权限已经被授予，在这里直接写要执行的相应方法即可
                                        Log.d("LM", "通讯录已授权");
                                        intentToContact();
                                    }
                                }else {
                                    // 低于6.0的手机直接访问
                                    intentToContact();
                                }
//                            }
//                        });
                    }
                }.start();
            }
            // 四级标准地址库
            else if(exceName.equals("四级标准地址库")) {

                sp.edit().putString("P_C_D_F", inputName).apply();
            }
            // 新建门店页面已加载
            else if(exceName.equals("新建门店页面已加载")) {

                String p_c_d_f = sp.getString("P_C_D_F", "");
                String url = "javascript:P_C_D_F('" + p_c_d_f + "')";
                MainActivity.mWebView.loadUrl(url);
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
                new Thread() {
                    public void run() {

                        checkVersion("原生");

                    }
                }.start();
            }
        }

        @JavascriptInterface
        public void callAndroid(String exceName, final String lng1, final String lat1, final String address1) {

            Log.d("LM", "执行:" + exceName + "    " + "经度:" + lng + "    " + "纬度:" + lat + "    " + "地址:" + address);

            if (exceName.equals("导航")) {

                Log.d("LM", "导航");

                lng = Double.valueOf(lng1).doubleValue();
                lat = Double.valueOf(lat1).doubleValue();
                address = address1;


                new Thread() {

                    public void run() {


                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                List list = new ArrayList();
                                if (SystemUtil.isInstalled(mContext, "com.autonavi.minimap")) {

                                    list.add("高德地图");
                                }
                                if (SystemUtil.isInstalled(mContext, "com.baidu.BaiduMap")) {

                                    list.add("百度地图");
                                }

                                if (list.size() == 2) {

                                    ActionSheet.createBuilder(MainActivity.this, getSupportFragmentManager())
                                            .setCancelButtonTitle("取消")
                                            .setOtherButtonTitles("高德地图", "百度地图")
                                            .setCancelableOnTouchOutside(true)
                                            .setListener(MainActivity.this).show();
                                } else if (list.size() == 1) {

                                    if (list.get(0).equals("高德地图")) {

                                        Log.d("LM", "调用高德地图");
                                        minimap(mContext, lng, lat, address, appName);
                                    } else if (list.get(0).equals("百度地图")) {

                                        Log.d("LM", "调用百度地图");
                                        BaiduMap(mContext, lng, lat, address, appName);
                                    }
                                } else {

                                    Toast.makeText(mContext, "未检索到本机已安装‘百度地图’或‘高德地图’App", LENGTH_LONG).show();
                                }
                            }
                        });
                    }
                }.start();
            }

            else if (exceName.equals("查看路线")) {

                new Thread() {

                    public void run() {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                Intent intent2 = new Intent(MainActivity.mContext, OrderTrackActivity.class);
                                intent2.putExtra("order_IDX", lng1);
                                intent2.putExtra("shipment_Code", lat1);
                                intent2.putExtra("shipment_Status", address1);
                                mContext.startActivity(intent2);
                            }
                        });
                    }
                }.start();
            }
        }
    }


    @SuppressLint("WrongConstant")
    @Override
    public void onOtherButtonClick(ActionSheet actionSheet, int index) {

        if(index == 0) {

            Log.d("LM", "调用高德地图");
            minimap(mContext, lng, lat, address, appName);
        }else if(index == 1) {

            Log.d("LM", "调用百度地图");
            BaiduMap(mContext, lng, lat, address, appName);
        }
    }

    @Override
    public void onDismiss(ActionSheet actionSheet, boolean isCancel) {

    }

    private static void BaiduMap(Context mContext, double lng, double lat, String address, String appName) {

        //跳转到百度导航
        try {
            Intent baiduintent = Intent.parseUri("intent://map/direction?" +
                    "origin=" + "" +
                    "&destination=" + address +
                    "&mode=driving" +
                    "&src=Name|AppName" +
                    "#Intent;scheme=bdapp;package=com.baidu.BaiduMap;end", 0);
            mContext.startActivity(baiduintent);
        } catch (URISyntaxException e) {
            Log.d("LM", "URISyntaxException:" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void minimap(Context mContext, double lng, double lat, String address, String appName) {
        //跳转到高德导航
        Intent autoIntent = new Intent();
        try {
            autoIntent.setData(Uri
                    .parse("androidamap://route?" +
                            "sourceApplication=" + appName +
                            "&slat=" + "" +
                            "&slon=" + "" +
                            "&dlat=" + lat +
                            "&dlon=" + lng +
                            "&dname=" + address +
                            "&dev=0" +
                            "&m=2" +
                            "&t=0"
                    ));
        } catch (Exception e) {
            Log.i("LM", "高德地图异常" + e);
        }
        mContext.startActivity(autoIntent);
    }

    public void checkVersion(String who) {

        this.WhoCheckVersion = who;

        Log.d("LM", "检查apk及zip版本");
        String params2 = "{\"tenantCode\":\"KDY\"}";
        String paramsEncoding = URLEncoder.encode(params2);
        String Strurl = Tools.getServerAddress(MainActivity.mContext) + "queryAppVersion.do?params=" + paramsEncoding;

        HttpURLConnection conn=null;
        try {

            URL url = new URL(Strurl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestMethod("GET");
            if(HttpURLConnection.HTTP_OK==conn.getResponseCode()){

                InputStream in=conn.getInputStream();
                String resultStr = Tools.inputStream2String(in);
                resultStr = URLDecoder.decode(resultStr,"UTF-8");

                try {
                    JSONObject jsonObj = (JSONObject)(new JSONParser().parse(resultStr));
                    Log.i("LM",jsonObj.toJSONString() + "\n" + jsonObj.getClass());
                    String status = (String)jsonObj.get("status");
                    String Msg = (String)jsonObj.get("Msg");

                    String apkDownloadUrl = null;
                    String server_apkVersion = null;
                    String zipDownloadUrl = null;
                    String server_zipVersion = null;
                    if (status.equals("1")) {

                        JSONObject dict = (JSONObject) jsonObj.get("data");
                        apkDownloadUrl = (String) dict.get("downloadUrl");
                        server_apkVersion = (String) dict.get("versionNo");
                        zipDownloadUrl = (String) dict.get("zipDownloadUrl");
                        server_zipVersion = (String) dict.get("zipVersionNo");
                    }
                    if (server_apkVersion != null && apkDownloadUrl != null) {
                        try {
                            String current_apkVersion = mAppVersion;
                            Log.d("LM","server_apkVersion:" + server_apkVersion + "\tcurrent_apkVersion:" + current_apkVersion);
                            int compareVersion = Tools.compareVersion(server_apkVersion, current_apkVersion);
                            if (compareVersion == 1) {
                                createUpdateDialog(current_apkVersion, server_apkVersion, apkDownloadUrl);
                            } else {
                                Log.d("LM", "apk为最新版本");
                                String curr_zipVersion = Tools.getAppZipVersion(mContext);
                                compareVersion = Tools.compareVersion(server_zipVersion, curr_zipVersion);
                                if (compareVersion == 1) {
                                    Log.d("LM", "服务器zip版本：" + server_zipVersion + "    " + "本地zip版本：" + CURR_ZIP_VERSION);
                                    CURR_ZIP_VERSION = server_zipVersion;
                                    Log.d("LM", "更新zip...");

                                    final String finalZipDownloadUrl = zipDownloadUrl;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            showUpdataZipDialog(finalZipDownloadUrl);
                                        }
                                    });

                                } else {
                                    Log.d("LM", "zip为最新版本");
                                    if(WhoCheckVersion.equals("vue")) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {

                                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                                builder.setTitle("");                                                builder.setMessage("已经是最新版本！");
                                                builder.setPositiveButton("确定", null);
                                                builder.show();
                                            }
                                        });
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.d("LM", "NameNotFoundException" + e.getMessage());
                            e.printStackTrace();
                        }
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                }
                in.close();
            }
            else {
                Log.d("LM", "检查版本接口|queryAppVersion.do|请求失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally{
            conn.disconnect();
        }
        Log.d("LM", "checkVersion: ");
    }

    /******************************************************** HTML版本更新功能 ********************************************************/
    /**
     * 弹出对话框
     */
    protected void showUpdataZipDialog(final String downUrl) {

        downLoadZip(downUrl);
    }

    protected void downLoadZip(final String downUrl) {
        //进度条
        final ProgressDialog pd;
        pd = new ProgressDialog(this);
        pd.setCancelable(false);
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setMessage("");
        pd.show();
        pd.setOnKeyListener(onKeyListener);
        new Thread() {
            @Override
            public void run() {
                try {
                    File file = Tools.getFileFromServer(downUrl, pd);
                    Log.d("LM", "Zip下载完毕，地址：" + file.getPath());

                    Log.d("LM", "取出验证为：" + Tools.getAppZipVersion(mContext));


                    try {
                        Log.d("LM", "SD卡开始解压...");
                        Tools.UnZipFolder("/storage/emulated/0/dist.zip", unZipOutPath);
                        Log.d("LM", "SD卡完成解压...");
                        // 更新ZIP版本号
                        Tools.setAppZipVersion(mContext, CURR_ZIP_VERSION);
                        Log.d("LM", "zip更新成功，设置版本号为：" + CURR_ZIP_VERSION);
                    } catch (Exception e) {
                        Log.d("LM", "SD卡解压异常..." + e.getMessage());
                        e.printStackTrace();
                    }

                    pd.dismiss(); //结束掉进度条对话框

                    new Thread() {
                        public void run() {

                            for (int i = 0; i < 5; i++) {
                                try {
                                    sleep(1 * 300);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                Log.d("LM", "开始刷新HTML  " + i);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        mWebView.reload();
                                    }
                                });
                                Log.d("LM", "完成刷新HTML  " + i);
                            }
                        }
                    }.start();
                } catch (Exception e) {

                    Log.d("", "run: ");
                }
            }
        }.start();
    }

    // 下载进度时，点击屏幕不可取消
    private DialogInterface.OnKeyListener onKeyListener = new DialogInterface.OnKeyListener() {
        @Override
        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {

            }
            return false;
        }
    };

    /**
     * 版本更新对话框
     *
     * @param currentVersion 当前版本versionName
     * @param version        最新版本versionName
     * @param downUrl        最新版本安装包下载url
     */
    public void createUpdateDialog(String currentVersion, String version, final String downUrl) {

        AllenVersionChecker
            .getInstance()
            .downloadOnly(
                UIData.create()
                    .setDownloadUrl(downUrl)
                    .setTitle("更新版本")
                    .setContent("当前版本：" + currentVersion + "\n最新版本：" + version)
            )
            .executeMission(mContext);
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
            // 通宇查单
            String QueryOrder_TY = "file:///data/data/com.kaidongyuan.qh_orders_android/upzip/dist/index.html#/QueryOrder_TY";
            // 我的
            String HomeIndex = "file:///data/data/com.kaidongyuan.qh_orders_android/upzip/dist/index.html#/Home";

            // 主菜单时不允许返回上一页
            if (
                    curURL.indexOf(Index + "?") != -1 || curURL.equals(Index) ||
                            curURL.indexOf(ReportForms + "?") != -1 || curURL.equals(ReportForms) ||
                            curURL.indexOf(QueryOrder_TY + "?") != -1 || curURL.equals(QueryOrder_TY) ||
                            curURL.indexOf(HomeIndex + "?") != -1 || curURL.equals(HomeIndex)
                    ) {

                Log.d("LM", "到达程序根节点后，点击安卓自带返回键，返回到桌面" + curURL);
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                startActivity(intent);
                return false;
            }
            mWebView.goBack();
            Log.d("LM", "curURL: " + curURL);
            Log.d("LM", "orgURL: " + orgURL);
        }
        return false;
    }

//    @SuppressLint("RestrictedApi")
//    @Override
//    public boolean dispatchKeyEvent(KeyEvent event) {
//        if(event.getKeyCode() == KeyEvent.KEYCODE_BACK ) {
//            //do something.
//            this.return_key_times++;
//            if(this.return_key_times % 6 == 0) {
//
//                Toast.makeText(mContext, "请使用左上角的返回键", LENGTH_LONG).show();
//            }
//            return true;
//        }else {
//            return super.dispatchKeyEvent(event);
//        }
//    }
}