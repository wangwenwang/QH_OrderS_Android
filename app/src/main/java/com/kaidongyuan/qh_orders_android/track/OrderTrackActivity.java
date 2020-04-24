package com.kaidongyuan.qh_orders_android.track;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteLine;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteResult;
import com.baidu.mapapi.utils.DistanceUtil;
import com.kaidongyuan.qh_orders_android.R;
import com.kaidongyuan.qh_orders_android.Tools.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ${PEOJECT_NAME}
 * Created by Administrator on 2016/10/19.
 */
public class OrderTrackActivity extends BaseActivity implements AsyncHttpCallback, View.OnClickListener {
    private TextView tvDistance, tvShipmentCode;
    private MapView mMapView;
    private final String Tag_Get_Locations = "Tag_Get_Locations";
    private OrderAsyncHttpClient mClient;
    BaiduMap mBaiduMap = null;
    public double distance = 0;
    public double distance0 = 0;
    private int agains = 10;//可以重新请求规划路段的次数
    private RoutePlanSearch mSearch;
    private TextView tv_prompt;

    /**
     * 返回上一界面按钮
     */
    private ImageView mImageViewGoBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ordertrack);
        mMapView = (MapView) findViewById(R.id.mapView_orderTrack);
        mBaiduMap = mMapView.getMap();
        mClient = new OrderAsyncHttpClient(this, this);
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NONE);
        tv_prompt = (TextView) findViewById(R.id.tv_prompt);
        initview();
        getPath();
        setListener();
    }

    private void getPath() {
        Intent intent = getIntent();
        String orderId = intent.getStringExtra("order_IDX");
        if (orderId == null || orderId.equals("")) {
            showToastMsg("装运编号号有误，请返回重新加载");
            finish();
            return;        }
        Log.d("LM", "获取配载轨迹点，规划路线------");
        Map<String, String> params = new HashMap<String, String>();
        params.put("params", "{shipmentId:" + orderId + "}");
        mClient.setShowToast(false);
        mClient.sendRequest(Constants.URL.SAAS_API_BASE + "getPathData.do", params, Tag_Get_Locations);
    }

    private void initview() {
        tvShipmentCode = (TextView) findViewById(R.id.tv_shipment_code);
        Intent intent = getIntent();
        String shipment_Code = intent.getStringExtra("shipment_Code");
        if (shipment_Code != null && !shipment_Code.equals("")) {
            tvShipmentCode.setText("配载单号：" + shipment_Code);
        }
        tvDistance = (TextView) findViewById(R.id.tv_distance);
        tvDistance.setText("正规划路线");
        mImageViewGoBack = (ImageView) this.findViewById(R.id.button_goback);
    }

    private void setListener() {
        try {
            mImageViewGoBack.setOnClickListener((View.OnClickListener) this);
        } catch (Exception e) {

        }
    }

    @Override
    public void onClick(View v) {
        try {
            switch (v.getId()) {
                case R.id.button_goback: //返回上一界面
                    this.finish();
                    break;
            }
        } catch (Exception e) {

        }
    }


    @Override
    public void initWindow() {
        //重写为空，针对满屏页面取消沉浸式状态栏
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mMapView != null) {
            mMapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mMapView != null) {
            mMapView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSearch != null) {
            mSearch.destroy();
        }
        if (mMapView != null) {
            mMapView.onDestroy();
        }
        mImageViewGoBack = null;

    }

    @Override
    public void postSuccessMsg(String msg, String request_tag) {
        if (msg.equals("error")) {
            mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
            showToastMsg("数据加载失败，请退出重进！");
            return;
        } else if (request_tag.equals(Tag_Get_Locations)) {
            JSONObject jo = JSON.parseObject(msg);
            String msglm = jo.getString("Msg");
            Log.d("LM", "Msg: " + msglm);
            List<Location> locationlist = JSON.parseArray(jo.getString("data"), Location.class);

            if(locationlist.size() < 4) {

                tv_prompt.setText("位置点个数为: " + locationlist.size() + "，小于4个点不能规划路线");
                return;
            }

            for (int i = 0; i < locationlist.size(); i++) {

                Location lo = locationlist.get(i);
                lo.CORDINATEX = lo.lon;
                lo.CORDINATEY = lo.lat;
            }
            if (locationlist == null || locationlist.size() < 0) {
                mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                return;
            }
            //绘制路线
            for (int i = 0; i < locationlist.size() / 100; i++) {
                List<Location> locationListfd = locationlist.subList(i * 100, i * 100 + 101);
                searchInMap(locationListfd, 0);
            }
            List<Location> locationListmr = locationlist.subList(locationlist.size() - locationlist.size() % 100, locationlist.size());
            searchInMap(locationListmr, 1);
            //计算路程，并显示到界面上
            distance0 = getDistance(locationlist);
            //绘制起点和终点的图标
            Location startLocation = locationlist.get(0);
            Location endLocation = locationlist.get(locationlist.size() - 1);
            LatLng stLatLng = new LatLng(startLocation.CORDINATEY, startLocation.CORDINATEX);
            LatLng enLatLng = new LatLng(endLocation.CORDINATEY, endLocation.CORDINATEX);
            BitmapDescriptor stbitmap = BitmapDescriptorFactory.fromResource(R.drawable.lm_map_start);
            BitmapDescriptor waybitmap = BitmapDescriptorFactory.fromResource(R.drawable.lm_map_way);

            BitmapDescriptor enbitmap = null;
            Intent intent = getIntent();
            String shipment_Status = intent.getStringExtra("shipment_Status");
            if(shipment_Status != null && shipment_Status.equals("在途")) {
                enbitmap = BitmapDescriptorFactory.fromResource(R.drawable.lm_map_curr);
            }else{
                enbitmap = BitmapDescriptorFactory.fromResource(R.drawable.lm_map_end);
            }

            MarkerOptions stOption = new MarkerOptions().position(stLatLng).icon(stbitmap).zIndex(12);
            MarkerOptions enOption = new MarkerOptions().position(enLatLng).icon(enbitmap).zIndex(12);
            mBaiduMap.addOverlay(stOption);
            mBaiduMap.addOverlay(enOption);
            Log.d("LM", "起点、终点标注Ok");
            //绘制司机上传位置的图标
            List<OverlayOptions> wayOptions = new ArrayList<>();
            for (int i = 0; i < locationlist.size(); i++) {

                if(i != 0 && i!= locationlist.size() - 1) {

                    Location wayLocation = locationlist.get(i);
                    LatLng wayLatLng = new LatLng(wayLocation.CORDINATEY, wayLocation.CORDINATEX);
                    MarkerOptions wayOption = new MarkerOptions().position(wayLatLng).icon(waybitmap).zIndex(11);
                    wayOptions.add(wayOption);
                }
            }
            mBaiduMap.addOverlays(wayOptions);
            Log.d("LM", "途经点标注Ok");

            if(locationlist.size() > 50) {

                showToastMsg("正在规划路线...", Toast.LENGTH_LONG);
            }else if(locationlist.size() > 20) {

                showToastMsg("正在规划路线...", Toast.LENGTH_SHORT);
            }

//            mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(stLatLng));
//            mBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(new MapStatus.Builder().zoom(16).build()));
        }
    }

    private double getDistance(List<Location> locationlist) {
        double distance = 0;
        int size = locationlist.size() - 1;
        for (int i = 1; i < size; i++) {
            Location nowLocation = locationlist.get(i);
            Location perviousLocation = locationlist.get(i - 1);
            LatLng nowLatLng = new LatLng(nowLocation.CORDINATEY, nowLocation.CORDINATEX);
            LatLng perviousLatLng = new LatLng(perviousLocation.CORDINATEY, perviousLocation.CORDINATEX);
            distance += DistanceUtil.getDistance(nowLatLng, perviousLatLng);
        }
        return distance;
    }

    private void searchInMap(final List<Location> locationListfd, int i) {
        final int again = i;
        agains = agains - 1;
        if (mBaiduMap == null) return;
        LatLng stlatlng = new LatLng(locationListfd.get(0).CORDINATEY, locationListfd.get(0).CORDINATEX);
        LatLng endlatlng = new LatLng(locationListfd.get(locationListfd.size() - 1).CORDINATEY, locationListfd.get(locationListfd.size() - 1).CORDINATEX);
        mSearch = RoutePlanSearch.newInstance();
        PlanNode stNode = PlanNode.withLocation(stlatlng);
        PlanNode endNode = PlanNode.withLocation(endlatlng);
        List<PlanNode> passBy = new ArrayList<>();
        for (int j = 1; j < locationListfd.size() - 2; j++) {
            passBy.add(PlanNode.withLocation(new LatLng(locationListfd.get(j).CORDINATEY, locationListfd.get(j).CORDINATEX)));
        }
        DrivingRoutePlanOption drivingRoutePlanOption = new DrivingRoutePlanOption().from(stNode).passBy(passBy).to(endNode).policy(DrivingRoutePlanOption.DrivingPolicy.ECAR_DIS_FIRST);
        OnGetRoutePlanResultListener listener = new OnGetRoutePlanResultListener() {
            @Override
            public void onGetWalkingRouteResult(WalkingRouteResult walkingRouteResult) {
                //获取步行线路规划结果
            }

            @Override
            public void onGetTransitRouteResult(TransitRouteResult transitRouteResult) {
                //获取公交换乘路径规划结果
            }

            @Override
            public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {

            }

            @Override
            public void onGetDrivingRouteResult(DrivingRouteResult drivingRouteResult) {
                //获取驾车线路规划结果
                if (mMapView == null || !mMapView.isShown()) {
                    return;
                }
                if (drivingRouteResult == null || drivingRouteResult.error != SearchResult.ERRORNO.NO_ERROR) {
                    if (!OrderTrackActivity.this.isFinishing() && agains > 0) {
                        searchInMap(locationListfd, again);
                        tvDistance.setText("查询路线中");
                    }
                    if (again == 1) {
                        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                    }
                    return;
                }
                if (drivingRouteResult.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
                    if (again == 1) {
                        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                    }
                    return;
                } else if (drivingRouteResult.error == SearchResult.ERRORNO.NO_ERROR) {
                    DrivingRouteOverlay overlay = new MyDrivingRouteOverlay(mBaiduMap);
                    distance += drivingRouteResult.getRouteLines().get(0).getDistance();
                    tvDistance.setText(distance0 > distance ? tvDistance.getText() : ("公里数：" + distance / 1000 + "公里"));
                    DrivingRouteLine ff = drivingRouteResult.getRouteLines().get(0);
                    int ii = drivingRouteResult.getRouteLines().size();
                    overlay.setData(drivingRouteResult.getRouteLines().get(0));
                    overlay.addToMap();
                    if (again == 1) {
                        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                    }
                    overlay.zoomToSpan();
                    mSearch.destroy();
                    return;
                }
                if (again == 1) {
                    mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                }
            }

            @Override
            public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {

            }

            @Override
            public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {
                //获取自行车线路规划结果
            }
        };
        mSearch.setOnGetRoutePlanResultListener(listener);
        //移动节点至起点
        tvDistance.setText(mSearch.drivingSearch(drivingRoutePlanOption) ? "正绘制路线" : "路线有误，请重新查看");
    }

    //定制RouteOverly
    private class MyDrivingRouteOverlay extends DrivingRouteOverlay {

        private boolean useDefaultST = false;
        private boolean useDefaultEN = false;

        public MyDrivingRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        public MyDrivingRouteOverlay(BaiduMap baiduMap, boolean stIcon, boolean enIcon) {
            super(baiduMap);
            useDefaultST = stIcon;
            useDefaultEN = enIcon;
        }

        @Override
        public BitmapDescriptor getStartMarker() {
//            if (useDefaultST){
//                return BitmapDescriptorFactory.fromResource(R.drawable.icon_st);
//            }
            return BitmapDescriptorFactory.fromResource(R.drawable.chose_cardwhite);
        }

        @Override
        public BitmapDescriptor getTerminalMarker() {
//            if (useDefaultEN) {
//                return BitmapDescriptorFactory.fromResource(R.drawable.icon_en);
//            }
            return BitmapDescriptorFactory.fromResource(R.drawable.chose_cardwhite);
        }

        @Override
        public List<BitmapDescriptor> getCustomTextureList() {
            return super.getCustomTextureList();
        }
    }
}
