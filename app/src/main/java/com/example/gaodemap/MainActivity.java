package com.example.gaodemap;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.animation.Animation;
import com.amap.api.maps.model.animation.RotateAnimation;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends Activity implements
        AMapLocationListener, LocationSource, PoiSearch.OnPoiSearchListener, AMap.OnMapClickListener,
        AMap.OnMapLongClickListener, GeocodeSearch.OnGeocodeSearchListener, EditText.OnKeyListener,
        AMap.OnMarkerClickListener, AMap.OnMarkerDragListener, AMap.InfoWindowAdapter, AMap.OnInfoWindowClickListener {
    // 请求权限码
    private static final int REQUEST_PERMISSIONS = 9527;
    private AMapLocationClient mapLocationClient = null;
    private AMapLocationClientOption mLocationOption = null;
    private MapView mapView;
    // 地图控制器
    private AMap aMap = null;
    // 更改位置监听器
    private LocationSource.OnLocationChangedListener mListener;
    //    定位样式
    private MyLocationStyle myLocationStyle = new MyLocationStyle();
    private UiSettings mUiSettings;
    //    POI查询对象
    private PoiSearch.Query query;
    //    POI搜索对象
    private PoiSearch poiSearch;
    //    城市码
    private String cityCode = null;
    //    浮动按钮
    private FloatingActionButton fabPOI;
    //    地理编码搜索
    private GeocodeSearch geocodeSearch;
    //    解析成功标志码
    private static final int PARSE_SUCCESS_CODE = 1000;
    //    搜索输入框
    private EditText editText;
    //    城市
    private String city;
    //    浮动按钮 清空地图标点
    private FloatingActionButton fabClearMarker;
    //    标点列表
    private List<Marker> markerList = new ArrayList<>();
    private String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fabPOI = (FloatingActionButton) findViewById(R.id.fab_poi);
        fabClearMarker = (FloatingActionButton) findViewById(R.id.fab_clear_marker);
        editText = findViewById(R.id.et_address);
//        键盘按键监听
        editText.setOnKeyListener(this);
//        初始化定位
        initLocation();
//        初始化地图
        initMap(savedInstanceState);
//        检查安卓版本
        checkingAndroidVersion();
    }

    private void checkingAndroidVersion() {
//        检查安卓版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//           Android6.0以上需要先动态获取权限
            requestPermission();
        } else {
            //Android6.0以下直接进入，启动定位
            mapLocationClient.startLocation();
        }
    }

    /**
     * 动态请求权限
     **/
    @AfterPermissionGranted(REQUEST_PERMISSIONS)
    private void requestPermission() {
        String[] permissions = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
        };
        if (EasyPermissions.hasPermissions(this, permissions)) {
            showMsg("已获得权限，可以定位啦");
            //启动定位
            mapLocationClient.startLocation();
        } else {
            EasyPermissions.requestPermissions(this, "需要权限", REQUEST_PERMISSIONS, permissions);
        }
    }

    /**
     * 请求权限结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //设置权限请求结果
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    /**
     * Toast消息提示
     *
     * @param msg 提示内容
     */
    private void showMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * 初始化定位
     */
    private void initLocation() {
        try {
            mapLocationClient = new AMapLocationClient(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mapLocationClient != null) {
//            设置定位监听回调
            mapLocationClient.setLocationListener(this);
            mLocationOption = new AMapLocationClientOption();
//            设置定位模式为AMapLocationMode.Hight_Accuracy，高精度模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
//            获取3s内精度最高的一次定位结果
            mLocationOption.setOnceLocationLatest(true);
//            设置是否返回地址信息
            mLocationOption.setNeedAddress(true);
//            请求超时时间，单位毫秒，
            mLocationOption.setHttpTimeOut(20000);
//            关闭缓存机制
            mLocationOption.setLocationCacheEnable(false);
//            给定位客户端对象设置定位参数
            mapLocationClient.setLocationOption(mLocationOption);
        }
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
                String address = aMapLocation.getAddress();
                double latitude = aMapLocation.getLatitude();
                double longitude = aMapLocation.getLongitude();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("纬度：" + latitude);
                stringBuilder.append("经度：" + longitude);
                stringBuilder.append("地址：" + address);
                Log.d("MainActivity", stringBuilder.toString());
                showMsg(address);

//                停止定位后，本地定位服务并不会被销毁
                mapLocationClient.stopLocation();
                if (mListener != null) {
//                    显示系统图标
                    mListener.onLocationChanged(aMapLocation);
                }
//              显示浮动框
                fabPOI.show();
                //赋值
                cityCode = aMapLocation.getCityCode();
                city = aMapLocation.getCity();
            } else {
//                定位失败时,可通过错误码信息来确定失败原因
                Log.e("AmapError", "location Error, ErrCode:"
                        + aMapLocation.getErrorCode() + ", errInfo:"
                        + aMapLocation.getErrorInfo());
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        销毁定位客户端，同时销毁本地定位服务。
        mapLocationClient.onDestroy();
//        在Activity执行onDestroy时mapView执行onDestroy销毁地图
        mapView.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
//      在Activity执行onResume时mapView执行onResume 重新绘制地图
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
//      在Activity执行onPause时mapView执行onPause 暂停地图
        mapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
//      在Activity执行onSaveInstanceState时mapView执行onSaveInstanceState 保存地图当前的状态地图
        mapView.onSaveInstanceState(outState);
    }

    /**
     * 初始化地图
     *
     * @param bundle
     */
    private void initMap(Bundle bundle) {
        mapView = findViewById(R.id.map_view);
//        在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mapView.onCreate(bundle);
//        初始化地图控制器对象
        aMap = mapView.getMap();

//        自定义定位图标
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.gps_point));
//        自定义精度范围的圆形边框颜色，都为0则透明
        myLocationStyle.strokeColor(Color.argb(0, 0, 0, 0));
//        自定义精度范围的圆形边框宽度 0 无宽度
        myLocationStyle.strokeWidth(0);
//        设置圆形的填充颜色 都为0则透明
        myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0));

//        设置定位蓝点的图标
        aMap.setMyLocationStyle(myLocationStyle);
//        设置定位监听
        aMap.setLocationSource(this);
//        设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认false
        aMap.setMyLocationEnabled(true);
//        设置缩放等级， 级别范围为[3, 20]
        aMap.setMinZoomLevel(12);
//         开启室内地图
        aMap.showIndoorMap(true);
//        设置地图点击事件
        aMap.setOnMapClickListener(this);
//        设置地图长按事件
        aMap.setOnMapLongClickListener(this);
//        设置地图Marker点击事件
        aMap.setOnMarkerClickListener(this);
//        设置地图Marker拖拽事件
        aMap.setOnMarkerDragListener(this);
//        设置InfoWindowAdapter监听
        aMap.setInfoWindowAdapter(this);
//        设置InfoWindow点击事件
        aMap.setOnInfoWindowClickListener(this);

        mUiSettings = aMap.getUiSettings();
//        隐藏缩放按钮
        mUiSettings.setZoomControlsEnabled(false);
//        显示比例尺 默认不显示
        mUiSettings.setScaleControlsEnabled(true);
        try {
            geocodeSearch = new GeocodeSearch(this);
        } catch (AMapException e) {
            e.printStackTrace();
        }
        geocodeSearch.setOnGeocodeSearchListener(this);
    }

    /**
     * 激活定位
     *
     * @param onLocationChangedListener
     */
    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
        if (mapLocationClient != null) {
//            启动定位
            mapLocationClient.startLocation();
        }
    }

    /**
     * 停止定位
     */
    @Override
    public void deactivate() {
        mListener = null;
        if (mapLocationClient != null) {
            mapLocationClient.stopLocation();
            mapLocationClient.onDestroy();
        }
        mapLocationClient = null;
    }

    /**
     * POI搜索返回
     *
     * @param poiResult POI所有数据
     * @param i
     */
    @Override
    public void onPoiSearched(PoiResult poiResult, int i) {
        ArrayList<PoiItem> poiItems = poiResult.getPois();
        for (PoiItem poiItem : poiItems) {
            Log.d("MainActivity", " Title：" + poiItem.getTitle() + " Snippet：" + poiItem.getSnippet());
        }
    }

    /**
     * POI中的项目搜索返回
     *
     * @param poiItem 获取POI item
     * @param i
     */
    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {

    }

    /**
     * 浮动按钮点击查询附件POI
     *
     * @param view
     */
    public void queryPOI(View view) {
//        构造query对象
        query = new PoiSearch.Query("购物", "", cityCode);
//        设置每页最多返回多少poiitem
        query.setPageSize(10);
//        设置查询页码
        query.setPageNum(1);
        try {
            poiSearch = new PoiSearch(this, query);
        } catch (AMapException e) {
            e.printStackTrace();
        }
//        设置搜索回调监听
        poiSearch.setOnPoiSearchListener(this);
//        发起搜索附件POI异步请求
        poiSearch.searchPOIAsyn();
    }

    /**
     * 地图单击事件
     *
     * @param latLng
     */
    @Override
    public void onMapClick(LatLng latLng) {
//        通过经纬度获取地址
//        latlonToAddress(latLng);
//        添加标点
        addMarker(latLng);
//        改变地图中心点
        updateMapCenter(latLng);
    }

    /**
     * 地图长按事件
     *
     * @param latLng
     */
    @Override
    public void onMapLongClick(LatLng latLng) {
        latlonToAddress(latLng);
    }

    /**
     * 坐标转地址
     *
     * @param regeocodeResult
     * @param i
     */
    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
//        解析result获取地址描述信息
        if (i == PARSE_SUCCESS_CODE) {
            RegeocodeAddress address = regeocodeResult.getRegeocodeAddress();
//            显示解析后的地址
            showMsg(address.getFormatAddress());
        } else {
            showMsg("获取地址失败");
        }
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
            List<GeocodeAddress> geocodeAddresses = geocodeResult.getGeocodeAddressList();
            if (geocodeAddresses != null && geocodeAddresses.size() > 0) {
                LatLonPoint latLonPoint = geocodeAddresses.get(0).getLatLonPoint();
//                显示解析后的坐标
                showMsg("坐标：" + latLonPoint.getLongitude() + "，" + latLonPoint.getLatitude());
            }
        } else {
            showMsg("获取坐标失败");
        }
    }

    /**
     * 通过经纬度获取地址
     *
     * @param latLng
     */
    private void latlonToAddress(LatLng latLng) {
//      位置点，通过经纬度构造
        LatLonPoint latLonPoint = new LatLonPoint(latLng.latitude, latLng.longitude);
//        逆编码查询， 第一个参数表示Lating,第二个参数表示多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
        RegeocodeQuery regeocodeQuery = new RegeocodeQuery(latLonPoint, 20, GeocodeSearch.AMAP);
//        异步获取地址信息
        geocodeSearch.getFromLocationAsyn(regeocodeQuery);
    }

    /**
     * 键盘点击
     *
     * @param v       The view the key has been dispatched to.
     * @param keyCode The code for the physical key that was pressed
     * @param event   The KeyEvent object containing full information about
     *                the event.
     * @return
     */
    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
//            获取输入框的值
            String address = editText.getText().toString().trim();
            if (address == null || address.isEmpty()) {
                showMsg("请输入地址");
            } else {
                InputMethodManager imn = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//                隐藏软键盘
                imn.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
//                name表示地址，第二个参数表示查询城市，中文或英文全拼，citycode ,adcode
                GeocodeQuery geocodeQuery = new GeocodeQuery(address, city);
                geocodeSearch.getFromLocationNameAsyn(geocodeQuery);
            }
            return true;
        }
        return false;
    }

    /**
     * 添加地图标点
     *
     * @param latLng
     */
    private void addMarker(LatLng latLng) {
//        显示浮动按钮
        fabClearMarker.show();
//        添加标点
        Marker marker = aMap.addMarker(new MarkerOptions()
                .draggable(true) //可拖动
                .position(latLng)
                .title("标题")
                .snippet("详细信息"));
//        绘制Marker时显示InfoWindow
        marker.showInfoWindow();
//        设置标点的绘制动画效果
        Animation animation = new RotateAnimation(marker.getRotateAngle(), marker.getRotateAngle() + 180, 0, 0, 0);
        long duration = 1000L;
        animation.setDuration(duration);
        animation.setInterpolator(new LinearInterpolator());

        marker.setAnimation(animation);
        marker.startAnimation();
        markerList.add(marker);
    }

    /**
     * 清空地图Marker
     *
     * @param view
     */
    public void clearAllMarker(View view) {
        if (markerList != null && markerList.size() > 0) {
            for (Marker marker : markerList) {
                marker.remove();
            }
        }
        fabClearMarker.hide();
    }

    /**
     * Marker点击事件
     *
     * @param marker
     * @return
     */
    @Override
    public boolean onMarkerClick(Marker marker) {
//        showMsg("点击了标点");
//        显示Information
        if (!marker.isInfoWindowShown()) {
//            显示
            marker.showInfoWindow();
        } else {
//            隐藏
            marker.hideInfoWindow();
        }
        return true;
    }

    /**
     * 开始拖动
     *
     * @param marker
     */
    @Override
    public void onMarkerDragStart(Marker marker) {
        Log.d(TAG, "开始拖动");
    }

    /**
     * 拖动中
     *
     * @param marker
     */
    @Override
    public void onMarkerDrag(Marker marker) {
        Log.d(TAG, "拖动中");
    }

    /**
     * 拖动完成
     *
     * @param marker
     */
    @Override
    public void onMarkerDragEnd(Marker marker) {
        Log.d(TAG, "拖动完成");
    }

    /**
     * 修改背景
     *
     * @param marker
     * @return
     */
    @Override
    public View getInfoWindow(Marker marker) {
        View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_window, null);
        render(marker, infoWindow);
        return infoWindow;
    }

    /**
     * 修改内容
     *
     * @param marker
     * @return
     */
    @Override
    public View getInfoContents(Marker marker) {
        View infoContent = getLayoutInflater().inflate(
                R.layout.custom_info_contents, null);
        render(marker, infoContent);
        return infoContent;
    }

    /**
     * 渲染
     *
     * @param marker
     * @param view
     */
    private void render(Marker marker, View view) {
        ((ImageView) view.findViewById(R.id.badge)).setImageResource(R.drawable.icon_yuan);

        //修改InfoWindow标题内容样式
        String title = marker.getTitle();
        TextView titleUi = ((TextView) view.findViewById(R.id.title));
        if (title != null) {
            SpannableString titleText = new SpannableString(title);
            titleText.setSpan(new ForegroundColorSpan(Color.RED), 0, titleText.length(), 0);
            titleUi.setTextSize(15);
            titleUi.setText(titleText);
        } else {
            titleUi.setText("");
        }

//        修改InfoWindow片段内容
        String snippet = marker.getSnippet();
        TextView snippetUi = ((TextView) view.findViewById(R.id.snippet));
        if (snippet != null) {
            SpannableString snippetText = new SpannableString(snippet);
            snippetText.setSpan(new ForegroundColorSpan(Color.GREEN), 0, snippetText.length(), 0);
            snippetUi.setTextSize(20);
            snippetUi.setText(snippetText);
        } else {
            snippetUi.setText("");
        }
    }

    /**
     * InfoWindow点击事件
     *
     * @param marker
     */
    @Override
    public void onInfoWindowClick(Marker marker) {
        showMsg("弹窗内容：标题：" + marker.getTitle() + "\n片段:" + marker.getSnippet());
    }

    /**
     * 改变地图中心位置
     *
     * @param latLng 位置
     */
    private void updateMapCenter(LatLng latLng) {
        /*
        CameraPosition第一个参数：目标位置的屏幕中心点经纬度坐标
        CameraPosition第二个参数，目标可视区域的缩放级别
        CameraPosition第三个参数，目标可视区域的倾斜度，以角度为单位
        CameraPosition第四个参数，可视区域指向的方向，以角度为单位，从正北方顺时针计算，从0度到360度
         */
        CameraPosition cameraPosition = new CameraPosition(latLng, 16, 30, 0);
//        位置变更
        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
//        改变位置
        aMap.animateCamera(cameraUpdate);
    }

    public void jumpRouteActivity(View view) {
        startActivity(new Intent(MainActivity.this, RouteActivity.class));
    }
}