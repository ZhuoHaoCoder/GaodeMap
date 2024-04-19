package com.example.gaodemap;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.MyLocationStyleCreator;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.route.BusPath;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRoutePlanResult;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RidePath;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkPath;
import com.amap.api.services.route.WalkRouteResult;
import com.example.gaodemap.overlay.BusRouteOverlay;
import com.example.gaodemap.overlay.DrivingRouteOverlay;
import com.example.gaodemap.overlay.RideRouteOverlay;
import com.example.gaodemap.overlay.WalkRouteOverlay;
import com.example.gaodemap.util.MapUtil;

import java.util.List;

public class RouteActivity extends AppCompatActivity implements AMapLocationListener,
        LocationSource, AMap.OnMapClickListener, RouteSearch.OnRouteSearchListener,
        EditText.OnKeyListener, GeocodeSearch.OnGeocodeSearchListener {

    private static final String TAG = "RouteActivity";
    //    地图
    private MapView mapView;
    //    地图控制器
    private AMap aMap = null;
    //    声明AMapLocationClient类对象
    public AMapLocationClient mLocationClient = null;
    //    声明AMapLocationClientOption对象
    public AMapLocationClientOption mLocationOption = null;
    //    位置更改监听
    private LocationSource.OnLocationChangedListener listener = null;
    //    定义一个UiSettings对象
    private UiSettings uiSettings;
    //    定位样式
    private MyLocationStyle myLocationStyle = new MyLocationStyle();
    //    起点
    private LatLonPoint mStartPoint;
    //    终点
    private LatLonPoint mEndPoint;
    //    路线搜索对象
    private RouteSearch routeSearch;
    //    出行方式数组
    private static final String[] travelModeArray = {"步行出行", "骑行出行", "驾车出行", "公交出行"};
    //    出行方式值
    private static int TRAVEL_MODE = 0;
    //    数组适配器
    private ArrayAdapter<String> arrayAdapter;
    //    城市
    private String city;
    //    路线规划详情
    private RelativeLayout bottomLayout;
    //    花费时间
    private TextView tvTime;
    //起点,终点
    private EditText etStartAddress, etEndAddress;
    //地理编码搜索
    private GeocodeSearch geocodeSearch;
    //    解析成功标识符
    private static final int PARSE_SUCCESS_CODE = 1000;
    //    定位地址
    private String locationAddress;
    //起点地址转坐标标识 1
    private int tag = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);
//        初始化定位
        initLocation();
//        初始化地图
        initMap(savedInstanceState);
//        启动定位
        mLocationClient.startLocation();
//        初始化路线
        initRoute();
//        初始化出行方式
        initTravelMode();
    }

    /**
     * 初始化定位
     */
    private void initLocation() {
        try {
            mLocationClient = new AMapLocationClient(getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mLocationClient != null) {
            mLocationClient.setLocationListener(this);
            mLocationOption = new AMapLocationClientOption();
            //            设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
//            获取3s内精度最高的一次结果
            mLocationOption.setOnceLocationLatest(true);
//            是否返回地址信息
            mLocationOption.setNeedAddress(true);
//            请求超时时间,单位毫秒
            mLocationOption.setHttpTimeOut(20000);
//            关闭缓存机制
            mLocationOption.setLocationCacheEnable(false);
//            给定位客户端对象绑定定位参数
            mLocationClient.setLocationOption(mLocationOption);
        }
    }

    /**
     * 初始化地图
     *
     * @param savedInstanceState
     */
    private void initMap(Bundle savedInstanceState) {
        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
//        初始化地图控制器对象
        aMap = mapView.getMap();
//        设置最小缩放等级为16,缩放级别范围为[3, 20]
        aMap.setMinZoomLevel(10);
//        开启室内地图
        aMap.showIndoorMap(true);
//        实例化UiSetting对象
        uiSettings = aMap.getUiSettings();
//        隐藏缩放按钮 模式显示
        uiSettings.setZoomControlsEnabled(false);
//        显示比例尺 默认隐藏
        uiSettings.setScaleControlsEnabled(true);
//        自定义定位蓝点图标
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.gps_point));
//        设置定位蓝点的style
        aMap.setMyLocationStyle(myLocationStyle);
//        设置定位监听
        aMap.setLocationSource(this);
//        设置为true表示显示定位层并可触发定位,false表示隐藏定位层并不可触发定位 默认是false
        aMap.setMyLocationEnabled(true);
//        地图点击监听
        aMap.setOnMapClickListener(this);
//        构建GeocodeSearch对象
        try {
            geocodeSearch = new GeocodeSearch(this);
        } catch (AMapException e) {
            e.printStackTrace();
        }
        geocodeSearch.setOnGeocodeSearchListener(this);
    }

    /**
     * 接收异步返回的定位结果
     *
     * @param aMapLocation
     */
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (aMapLocation != null) {
            if (aMapLocation.getErrorCode() == 0) {
//                地址
                locationAddress = aMapLocation.getAddress();
//                经度
                double latitude = aMapLocation.getLatitude();
//                纬度
                double longitude = aMapLocation.getLongitude();

//                设置当前所在地
                etStartAddress.setText(locationAddress);
//                etStartAddress.setEnabled(false);//禁止输入
//                设置起点
                mStartPoint = MapUtil.convertToLatLonPoint(new LatLng(latitude, longitude));
                city = aMapLocation.getCity();
//                停止定位后，本地定位服务不会销毁
                mLocationClient.stopLocation();
//                显示地图定位结果
                if (listener != null) {
//                    显示系统图标
                    listener.onLocationChanged(aMapLocation);
                }
            } else {
                Log.e("AmapError", "location Error, ErrCode:"
                        + aMapLocation.getErrorCode() + ", errInfo:"
                        + aMapLocation.getErrorInfo());
            }
        }
    }

    /**
     * 激活定位
     *
     * @param onLocationChangedListener
     */
    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        listener = onLocationChangedListener;
        if (listener == null) {
            mLocationClient.startLocation();
        }
    }

    /**
     * 停止定位
     */
    @Override
    public void deactivate() {
        listener = null;
        if (mLocationClient != null) {
            mLocationClient.stopLocation();
            mLocationClient.onDestroy();
        }
        mLocationClient = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLocationClient != null) {
            mLocationClient.onDestroy();
        }
        mapView.onDestroy();
    }

    /**
     * 点击地图
     *
     * @param latLng
     */
    @Override
    public void onMapClick(LatLng latLng) {
        //终点
        mEndPoint = MapUtil.convertToLatLonPoint(latLng);
//        开始路线搜索
        startRouteSearch();
    }

    /**
     * 初始化路线
     */
    private void initRoute() {
        try {
            routeSearch = new RouteSearch(this);
        } catch (AMapException e) {
            e.printStackTrace();
        }
        routeSearch.setRouteSearchListener(this);
    }

    /**
     * 公交规划路径结果
     *
     * @param busRouteResult 结果
     * @param i              结果码
     */
    @Override
    public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {
        aMap.clear();
        if (i == AMapException.CODE_AMAP_SUCCESS) {
            if (busRouteResult != null && busRouteResult.getPaths() != null) {
                if (busRouteResult.getPaths().size() > 0) {
                    final BusPath busPath = busRouteResult.getPaths().get(0);
                    if (busPath == null) {
                        return;
                    }
                    BusRouteOverlay busRouteOverlay = new BusRouteOverlay(this, aMap, busPath,
                            busRouteResult.getStartPos(), busRouteResult.getTargetPos());

                    busRouteOverlay.removeFromMap();
                    busRouteOverlay.addToMap();
                    busRouteOverlay.zoomToSpan();
                    int dis = (int) busPath.getDistance();
                    int dur = (int) busPath.getDuration();
                    String des = MapUtil.getFriendlyTime(dur) + "(" + MapUtil.getFriendlyLength(dis) + ")";
                    tvTime.setText(des);
                    bottomLayout.setVisibility(View.VISIBLE);
                    bottomLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(RouteActivity.this, RouteDetailActivity.class);
                            intent.putExtra("type", 3);
                            intent.putExtra("path", busPath);
                            startActivity(intent);
                        }
                    });
                } else if (busRouteResult.getPaths() == null) {
                    showMsg("对不起没有查询到数据");
                }
            } else {
                showMsg("对不起没有查询到数据");
            }
        } else {
            showMsg("错误码:" + i);
        }
    }

    /**
     * 驾车规划路径结果
     *
     * @param driveRouteResult 结果
     * @param i                结果码
     */
    @Override
    public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int i) {
        aMap.clear();//清理地图上的所有障碍物
        if (i == AMapException.CODE_AMAP_SUCCESS) {
            if (driveRouteResult != null && driveRouteResult.getPaths() != null) {
                if (driveRouteResult.getPaths().size() > 0) {
                    final DrivePath drivePath = driveRouteResult.getPaths().get(0);
                    if (drivePath == null) {
                        return;
                    }
                    DrivingRouteOverlay drivingRouteOverlay = new DrivingRouteOverlay(this, aMap, drivePath,
                            driveRouteResult.getStartPos(), driveRouteResult.getTargetPos(), null);
                    drivingRouteOverlay.removeFormMap();
                    drivingRouteOverlay.addToMap();
                    drivingRouteOverlay.zoomToSpan();

                    int dis = (int) drivePath.getDistance();
                    int dur = (int) drivePath.getDuration();
                    String des = MapUtil.getFriendlyTime(dur) + "(" + MapUtil.getFriendlyLength(dis) + ")";
                    tvTime.setText(des);
                    bottomLayout.setVisibility(View.VISIBLE);
                    bottomLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(RouteActivity.this, RouteDetailActivity.class);
                            intent.putExtra("type", 2);
                            intent.putExtra("path", drivePath);
                            startActivity(intent);
                        }
                    });
                } else if (driveRouteResult.getPaths() == null) {
                    showMsg("对不起，没有搜索到相关数据！");
                }
            } else {
                showMsg("对不起，没有搜索到相关数据！");
            }
        } else {
            showMsg("错误码" + i);
        }
    }

    /**
     * 步行规划路径结果
     *
     * @param walkRouteResult 结果
     * @param code            结果码
     */
    @Override
    public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int code) {
        aMap.clear();//清理地图上所有覆盖物
        if (code == AMapException.CODE_AMAP_SUCCESS) {
            if (walkRouteResult != null && walkRouteResult.getPaths() != null) {
                if (walkRouteResult.getPaths().size() > 0) {
                    final WalkPath walkPath = walkRouteResult.getPaths().get(0);
                    if (walkPath == null) {
                        return;
                    }
                    //绘制路线
                    WalkRouteOverlay walkRouteOverlay = new WalkRouteOverlay(
                            this, aMap, walkPath,
                            walkRouteResult.getStartPos(),
                            walkRouteResult.getTargetPos()
                    );
                    walkRouteOverlay.removeFromMap();
                    walkRouteOverlay.addToMap();
                    walkRouteOverlay.zoomToSpan();

                    int dis = (int) walkPath.getDistance();
                    int dur = (int) walkPath.getDuration();
                    String des = MapUtil.getFriendlyTime(dur) + "(" + MapUtil.getFriendlyLength(dis) + ")";
//                    显示步行划分时间
                    tvTime.setText(des);
                    bottomLayout.setVisibility(View.VISIBLE);
//                    跳转到路线详情页面
                    bottomLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(RouteActivity.this, RouteDetailActivity.class);
                            intent.putExtra("type", 0);
                            intent.putExtra("path", walkPath);
                            startActivity(intent);
                        }
                    });
                } else if (walkRouteResult.getPaths() == null) {
                    showMsg("对不起,没有搜索到相关数据");
                }
            } else {
                showMsg("对不起，没有搜索到相关数据！");
            }
        } else {
            showMsg("错误码:" + code);
        }
    }

    private void showMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * 骑行规划路径结果
     *
     * @param rideRouteResult 结果
     * @param code            结果码
     */
    @Override
    public void onRideRouteSearched(RideRouteResult rideRouteResult, int code) {
        aMap.clear();//清理地图上的所有覆盖物
        if (code == AMapException.CODE_AMAP_SUCCESS) {
            if (rideRouteResult != null && rideRouteResult.getPaths() != null) {
                if (rideRouteResult.getPaths().size() > 0) {
                    final RidePath ridePath = rideRouteResult.getPaths().get(0);
                    if (ridePath == null) return;
                    RideRouteOverlay rideRouteOverlay = new RideRouteOverlay(
                            this, aMap, ridePath,
                            rideRouteResult.getStartPos(),
                            rideRouteResult.getTargetPos()
                    );
                    rideRouteOverlay.removeFromMap();
                    rideRouteOverlay.addToMap();
                    rideRouteOverlay.zoomToSpan();

                    int dis = (int) ridePath.getDistance();
                    int dur = (int) ridePath.getDuration();
                    String des = MapUtil.getFriendlyTime(dur) + "(" + MapUtil.getFriendlyLength(dis) + ")";

                    tvTime.setText(des);
                    bottomLayout.setVisibility(View.VISIBLE);
                    bottomLayout.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(RouteActivity.this, RouteDetailActivity.class);
                            intent.putExtra("type", 1);
                            intent.putExtra("path", ridePath);
                            startActivity(intent);
                        }
                    });
                } else if (rideRouteResult.getPaths() == null) {
                    showMsg("对不起,没有搜索到相关数据!");
                }
            } else {
                showMsg("对不起,没有搜索到相关数据!");
            }
        } else {
            showMsg("错误码" + code);
        }
    }

    /**
     * 开始路线搜索
     */
    private void startRouteSearch() {
//        在地图上添加起点Marker
        aMap.addMarker(new MarkerOptions()
                .position(MapUtil.convertToLatLng(mStartPoint))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.start)));
//        在地图上添加终点Marker
        aMap.addMarker(new MarkerOptions()
                .position(MapUtil.convertToLatLng(mEndPoint))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.end)));
//        搜索路线 构建路径的起终点
        final RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(mStartPoint, mEndPoint);

//        出行方式判断
        switch (TRAVEL_MODE) {
            case 0://步行
                //构建步行路线搜索对象
                RouteSearch.WalkRouteQuery query = new RouteSearch.WalkRouteQuery(fromAndTo, RouteSearch.WALK_DEFAULT);
                //异步路径规划步行模式查询
                routeSearch.calculateWalkRouteAsyn(query);
                break;
            case 1://骑行
                //构建骑行路线搜索对象
                RouteSearch.RideRouteQuery rideQuery = new RouteSearch.RideRouteQuery(fromAndTo, RouteSearch.WALK_DEFAULT);
                //异步路径规划骑行模式查询
                routeSearch.calculateRideRouteAsyn(rideQuery);
                break;
            case 2://驾车
                //构建驾车路线对象 剩余三个参数分别是途径点，避让区域，避让道路
                RouteSearch.DriveRouteQuery driveQuery = new RouteSearch.DriveRouteQuery(fromAndTo, RouteSearch.WALK_DEFAULT, null, null, "");
                routeSearch.calculateDriveRouteAsyn(driveQuery);
                break;
            case 3://公交
                //构建驾车路线搜索对象 第三个参数表示公交查询城市区号，第四个参数表示是否计算夜班车，0表示不计算，1表示计算
                RouteSearch.BusRouteQuery busQuery = new RouteSearch.BusRouteQuery(fromAndTo, RouteSearch.BusLeaseWalk, city, 0);
                //公交规则路径计算
                routeSearch.calculateBusRouteAsyn(busQuery);
                break;
            default:
                break;
        }
    }

    private void initTravelMode() {
        Spinner spinner = findViewById(R.id.spinner);
        bottomLayout = findViewById(R.id.bottom_layout);
        tvTime = findViewById(R.id.tv_time);
        etStartAddress = findViewById(R.id.et_start_address);
        etEndAddress = findViewById(R.id.et_end_address);
        etEndAddress.setOnKeyListener(this);
        etStartAddress.setOnKeyListener(this);

//        将可选内容与ArrayAdapter连接起来
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, travelModeArray);
//        设置下拉列表的风格
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        将Adapter添加到Spinner中
        spinner.setAdapter(arrayAdapter);

//        添加事件spinner事件监听
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TRAVEL_MODE = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
//            获取输入框的值 出发地
            String startAddress = etStartAddress.getText().toString().trim();
//            获取输入框的值 目的地
            String endAddress = etEndAddress.getText().toString().trim();
//            判断起始地是否有值
            if (startAddress.isEmpty()) {
                showMsg("请输入当前的出发地");
                return false;
            }
//            判断目的地是否有值
            if (endAddress.isEmpty()) {
                showMsg("请输入要前往的目的地");
                return false;
            }
//            当出发地输入框有值的时候,判断这个值是否是定位的地址,是则说明你没有更改过,则不需要进行地址转坐标,不是则需要转坐标
            if (!locationAddress.equals(startAddress)) {
                tag = 1;
                GeocodeQuery startQuery = new GeocodeQuery(startAddress, city);
                geocodeSearch.getFromLocationNameAsyn(startQuery);
            } else {
                tag = -1;
            }

            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//                隐藏软键盘
            imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
            //通过输入的目的地转为经纬度，然后进行地图上添加标点，最后计算出行路线规划

            // name表示地址，第二个参数表示查询城市，中文或者中文全拼，citycode、adcode
            GeocodeQuery query = new GeocodeQuery(endAddress, city);
            geocodeSearch.getFromLocationNameAsyn(query);

            return true;
        }
        return false;
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {

    }

    /**
     * 地址转坐标
     *
     * @param geocodeResult
     * @param i
     */
    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {
        if (i == PARSE_SUCCESS_CODE) {
            List<GeocodeAddress> geocodeAddressList = geocodeResult.getGeocodeAddressList();
            if (geocodeAddressList != null && geocodeAddressList.size() > 0) {
//                判断是不是起点的搜索
                if (tag == 1) {
//                    起点
                    mStartPoint = geocodeAddressList.get(0).getLatLonPoint();
                } else {
                    //                终点
                    mEndPoint = geocodeAddressList.get(0).getLatLonPoint();
                }
                if (mStartPoint != null && mEndPoint != null) {
//            开始路线搜索
                    startRouteSearch();
                }
            }
        } else {
            showMsg("获取坐标失败");
        }
    }
}