package com.kaidongyuan.qh_orders_android.Tools;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.kaidongyuan.qh_orders_android.MainActivity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static android.content.Context.MODE_MULTI_PROCESS;
import static android.widget.Toast.LENGTH_LONG;

public class Tools {

    /**
     * 记住服务器地址
     * @param mContext 上下文
     * @throws Exception
     */
    public static void setServerAddress(Context mContext, String apiUrl) {

        SharedPreferences pre_appinfo = mContext.getSharedPreferences("w_AppInfo", MODE_MULTI_PROCESS);
        pre_appinfo.edit().putString("ServerAddress", apiUrl).commit();
    }

    /**
     * 取服务器地址
     * @param mContext 上下文
     * @return
     */
    public static String getServerAddress(Context mContext) {

        SharedPreferences pre_appinfo = mContext.getSharedPreferences("w_AppInfo", MODE_MULTI_PROCESS);
        return pre_appinfo.getString("ServerAddress", "");
    }

    /**
     * 获取版本号名称
     *
     * @param context 上下文
     * @return
     */
    public static String getVerName(Context context) {
        String verName = "";
        try {
            verName = context.getPackageManager().
                    getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return verName;
    }


    /**
     * 设置上一次启动的版本号
     * @param mContext 上下文
     * @throws Exception
     */
    public static void setAppLastTimeVersion(Context mContext) {

        SharedPreferences pre_appinfo = mContext.getSharedPreferences("w_AppInfo", MODE_MULTI_PROCESS);
        pre_appinfo.edit().putString("LastTimeVersion", MainActivity.mAppVersion).commit();
    }

    /**
     * 获取上一次启动的版本号
     * @param mContext 上下文
     * @return
     */
    public static String getAppLastTimeVersion(Context mContext) {

        SharedPreferences pre_appinfo = mContext.getSharedPreferences("w_AppInfo", MODE_MULTI_PROCESS);
        return pre_appinfo.getString("LastTimeVersion", "");
    }

    /**
     * 设置zip版本号
     * @param mContext 上下文
     * @param CURR_ZIP_VERSION 版本号
     * @throws Exception
     */
    public static void setAppZipVersion(Context mContext, String CURR_ZIP_VERSION) {

        SharedPreferences pre_appinfo = mContext.getSharedPreferences("w_AppInfo", MODE_MULTI_PROCESS);
        pre_appinfo.edit().putString("ZipVersion", CURR_ZIP_VERSION).commit();
    }


    /**
     * 获取zip版本号
     * @param mContext 上下文
     * @return
     */
    public static String getAppZipVersion(Context mContext) {

        SharedPreferences pre_appinfo = mContext.getSharedPreferences("w_AppInfo", MODE_MULTI_PROCESS);
        return pre_appinfo.getString("ZipVersion", "");
    }


    /**
     * 版本号比较
     *
     * @param server 服务器版本号
     * @param locati 本地版本号
     * @return   server > locati 返回1，server < locati 返回-1，server 0 locati 返回0
     */
    public static int compareVersion(String server, String locati){
        if (server.equals(locati)) {
            return 0;
        }
        String[] version1Array = server.split("\\.");
        String[] version2Array = locati.split("\\.");
        Log.d("LM", "version1Array==" + version1Array.length);
        Log.d("LM", "version2Array==" + version2Array.length);
        int index = 0;
        // 获取最小长度值
        int minLen = Math.min(version1Array.length, version2Array.length);
        int diff = 0;
        // 循环判断每位的大小
        Log.d("LM", "verTag2=2222=" + version1Array[index]);
        while (index < minLen
                && (diff = Integer.parseInt(version1Array[index])
                - Integer.parseInt(version2Array[index])) == 0) {
            index++;
        }
        if (diff == 0) {
            // 如果位数不一致，比较多余位数
            for (int i = index; i < version1Array.length; i++) {
                if (Integer.parseInt(version1Array[i]) > 0) {
                    return 1;
                }
            }

            for (int i = index; i < version2Array.length; i++) {
                if (Integer.parseInt(version2Array[i]) > 0) {
                    return -1;
                }
            }
            return 0;
        } else {
            return diff > 0 ? 1 : -1;
        }
    }


    public static boolean fileIsExists(String strFile) {
        try {
            File f=new File(strFile);
            if(!f.exists()) {
                return false;
            }
        }
        catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     *  解压assets的zip压缩文件到指定目录
     *  @param  context 上下文对象
     *  @param  assetName 压缩文件名
     *  @param  outputDirectory 输出目录
     *  @param  isReWrite 是否覆盖
     *  @throws IOException
     */
    public static void unZip(Context context, String assetName, String outputDirectory, boolean isReWrite) throws IOException {
        // 创建解压目标目录
        File file = new File(outputDirectory);
        //如果目标目录不存在，则创建
        if (!file.exists()) {
            file.mkdirs();
        }
        // 打开压缩文件
        InputStream inputStream = context.getAssets().open(assetName);
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        // 读取一个进入点
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        // 使用1Mbuffer
        byte[] buffer = new byte[1024 * 1024];
        //解压时字节计数
        int count = 0;
        // 如果进入点为空说明已经遍历完所有压缩包中文件和目录
        while (zipEntry != null) {
            //如果是一个目录
            if (zipEntry.isDirectory()) {
                file = new File(outputDirectory + File.separator + zipEntry.getName());                // 文件需要覆盖或者是文件不存在
                if (isReWrite || !file.exists()) {
                    file.mkdir();
                }
            } else {
                // 如果是文件
                file = new File(outputDirectory + File.separator + zipEntry.getName());
                // 文件需要覆盖或者文件不存在，则解压文件
                if (isReWrite || !file.exists()) {
                    file.createNewFile();
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    while ((count = zipInputStream.read(buffer)) > 0) {
                        fileOutputStream.write(buffer, 0, count);
                    }
                    fileOutputStream.close();
                }
            }
            // 定位到下一个文件入口
            zipEntry = zipInputStream.getNextEntry();
        }
        zipInputStream.close();
    }

    /**
     * 解压zip到指定的路径
     * @param zipFileString  ZIP的名称
     * @param outPathString   要解压缩路径
     * @throws Exception
     */
    public static void UnZipFolder(String zipFileString, String outPathString) throws Exception {
        ZipInputStream inZip = new ZipInputStream(new FileInputStream(zipFileString));
        ZipEntry zipEntry;
        String  szName = "";
        while ((zipEntry = inZip.getNextEntry()) != null) {
            szName = zipEntry.getName();
            if (zipEntry.isDirectory()) {
                //获取部件的文件夹名
                szName = szName.substring(0, szName.length() - 1);
                File folder = new File(outPathString + File.separator + szName);
                folder.mkdirs();
            } else {
                Log.d("LM","解压：" + outPathString + "   " + File.separator + "   " + szName);
                File file = new File(outPathString + File.separator + szName);
                if (!file.exists()){
                    Log.e("LM", "Create the file:" + outPathString + File.separator + szName);
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                // 获取文件的输出流
                FileOutputStream out = new FileOutputStream(file);
                int len;
                byte[] buffer = new byte[1024];
                // 读取（字节）字节到缓冲区
                while ((len = inZip.read(buffer)) != -1) {
                    // 从缓冲区（0）位置写入（字节）字节
                    out.write(buffer, 0, len);
                    out.flush();
                }
                out.close();
            }
        }
        inZip.close();
    }


    /**
     * 跳转手机地图APP
     *
     * @param address 地址
     * @param mContext  上下文
     * @param appName   APP名字
     *
     * @return
     */
    public static void ToNavigation(final String address, final Context mContext, final String appName) {


        List list = new ArrayList();
        if (SystemUtil.isInstalled(mContext, "com.autonavi.minimap")) {

            list.add("高德地图");
        }
        if (SystemUtil.isInstalled(mContext, "com.baidu.BaiduMap")) {

            list.add("百度地图");
        }

//        PromptDialog promptDialog = new PromptDialog((Activity) LoginActivity.mContext);
//        promptDialog.getDefaultBuilder().touchAble(true).round(3).loadingDuration(3000);
//
//        PromptButton cancle = new PromptButton("取消", null);
//        cancle.setTextColor(Color.parseColor("#0076ff"));
//        if (list.size() == 2) {
//            promptDialog.showAlertSheet("请选择地图", true, cancle,
//                    new PromptButton("高德地图", new PromptButtonListener() {
//                        @Override
//                        public void onClick(PromptButton button) {
//
//                            Log.d("LM", "调用高德地图");
//                            minimap(mContext, address, appName);
//                        }
//                    }),
//                    new PromptButton("百度地图", new PromptButtonListener() {
//                        @Override
//                        public void onClick(PromptButton button) {
//
//                            Log.d("LM", "调用百度地图");
//                            BaiduMap(mContext, address);
//                        }
//                    })
//            );
//        } else if (list.size() == 1) {
//
//            if (list.get(0).equals("高德地图")) {
//
//                Log.d("LM", "调用高德地图");
//                minimap(mContext, address, appName);
//            } else if (list.get(0).equals("百度地图")) {
//
//                Log.d("LM", "调用百度地图");
//                BaiduMap(mContext, address);
//            }
//        } else {
//
//            Toast.makeText(mContext, "未检索到本机已安装‘百度地图’或‘高德地图’App", LENGTH_LONG).show();
//        }
    }

    public static String inputStream2String (InputStream in) throws IOException {

        StringBuffer out = new StringBuffer();
        byte[]  b = new byte[4096];
        int n;
        while ((n = in.read(b))!= -1){
            out.append(new String(b,0,n));
        }
        Log.i("String的长度",new Integer(out.length()).toString());
        return  out.toString();
    }



    /**
     * 下载进度条
     */
    public static File getFileFromServer(String path, ProgressDialog pd) throws Exception{
        //如果相等的话表示当前的sdcard挂载在手机上并且是可用的
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            URL url = new URL(path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            //获取到文件的大小
            pd.setMax(conn.getContentLength() / 1000 / 1000);
            InputStream is = conn.getInputStream();
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), MainActivity.ZipFileName);
            FileOutputStream fos = new FileOutputStream(file);
            BufferedInputStream bis = new BufferedInputStream(is);
            byte[] buffer = new byte[1024];
            int len ;
            int total=0;
            while((len =bis.read(buffer))!=-1){
                fos.write(buffer, 0, len);
                total+= len;
                //获取当前下载量
                double progressD = total / 1000 / 1000.0;
                String progressS = doubleToString(progressD);
                pd.setProgress(total / 1000 / 1000);
                pd.setProgressNumberFormat(progressS + "m");
            }
            fos.close();
            bis.close();
            is.close();
            return file;
        }
        else{
            return null;
        }
    }

    /**
     * double转String,保留小数点后一位
     * @param num
     * @return
     */
    public static String doubleToString(double num){
        //使用0.0不足位补0，#.#仅保留有效位
        return new DecimalFormat("0.0").format(num);
    }
}
