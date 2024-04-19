package com.example.gaodemap.overlay;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import com.amap.api.maps.AMap;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveStep;
import com.amap.api.services.route.TMC;
import com.example.gaodemap.R;
import com.example.gaodemap.util.MapUtil;

import java.util.ArrayList;
import java.util.List;

public class DrivingRouteOverlay extends RouteOverlay {
    private DrivePath drivePath;
    private List<LatLonPoint> throughPointList;
    private List<Marker> throughMarkerList = new ArrayList<Marker>();
    private boolean throughPointMarkerVisible = true;
    private List<TMC> tmcs;
    private PolylineOptions mPolylineOptions;
    private PolylineOptions mPolylineOptionsColor;
    private Context context;

    private boolean isColorFulline = true;
    private float mWidth = 25;
    private List<LatLng> mLatLngsOdPath;

    public void setColorFulline(boolean colorFulline) {
        isColorFulline = colorFulline;
    }

    /**
     * 根据给定的参数，构造一个导航路线图层类的对象
     *
     * @param context          当前activity对象
     * @param aMap             地图对象
     * @param path             导航路线规划方案
     * @param start            起点
     * @param end              终点
     * @param throughPointList
     */
    public DrivingRouteOverlay(Context context, AMap aMap, DrivePath path, LatLonPoint start, LatLonPoint end, List<LatLonPoint> throughPointList) {
        super(context);
        this.context = context;
        this.aMap = aMap;
        this.drivePath = path;
        this.startPoint = MapUtil.convertToLatLng(start);
        this.endPoint = MapUtil.convertToLatLng(end);
        this.throughPointList = throughPointList;
    }

    @Override
    public float getRouteWidth() {
        return mWidth;
    }

    /**
     * 设置路线的宽度
     *
     * @param mWidth 路线宽度，取值范围大于0
     */
    public void setRouteWidth(float mWidth) {
        this.mWidth = mWidth;
    }

    /**
     * 添加驾车路线到地图显示
     */
    public void addToMap() {
        initPolylineOptions();
        try {
            if (aMap == null) {
                return;
            }
            if (mWidth == 0 || drivePath == null) {
                return;
            }
            mLatLngsOdPath = new ArrayList<LatLng>();
            tmcs = new ArrayList<TMC>();
            List<DriveStep> drivePaths = drivePath.getSteps();
            for (DriveStep step : drivePaths) {
                List<LatLonPoint> lonPoints = step.getPolyline();
                List<TMC> tmcList = step.getTMCs();
                tmcs.addAll(tmcList);
                addDrivingStationMarkers(step, MapUtil.convertToLatLng(lonPoints.get(0)));
                for (LatLonPoint latLonPoint : lonPoints) {
                    mPolylineOptions.add(MapUtil.convertToLatLng(latLonPoint));
                    mLatLngsOdPath.add(MapUtil.convertToLatLng(latLonPoint));
                }
            }
            if (startMarker != null) {
                startMarker.remove();
                startMarker = null;
            }
            if (endMarker != null) {
                endMarker.remove();
                endMarker = null;
            }
            addStartAndEndMarker();
            addThroughPointMarker();
            if (isColorFulline && tmcs.size() > 0) {
                colorWayUpdate(tmcs);
                showColorPolyline();
            } else {
                showPolyline();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化线段属性
     */
    private void initPolylineOptions() {
        mPolylineOptions = null;
        mPolylineOptions = new PolylineOptions();
        mPolylineOptions.color(getDriveColor()).width(getRouteWidth());
    }

    private void addDrivingStationMarkers(DriveStep driveStep, LatLng latLng) {
        addStationMarker(new MarkerOptions()
                .position(latLng)
                .title("\u65B9\u5411:" + driveStep.getAction()
                        + "\u9053\u8DEF:" + driveStep.getRoad())
                .snippet(driveStep.getInstruction()).visible(nodeIconVisible)
                .anchor(0.5f, 0.5f).icon(getDriveBitmapDescriptor()));
    }

    private void addThroughPointMarker() {
        if (this.throughPointList != null && this.throughPointList.size() > 0) {
            LatLonPoint latLonPoint = null;
            for (int i = 0; i < this.throughPointList.size(); ++i) {
                latLonPoint = this.throughPointList.get(i);
                if (latLonPoint != null) {
                    throughMarkerList.add(
                            aMap.addMarker((new MarkerOptions())
                                    .position(new LatLng(latLonPoint.getLatitude(), latLonPoint.getLongitude()))
                                    .visible(throughPointMarkerVisible)
                                    .icon(getThroughPointBitDes())
                                    .title("\u9014\u7ECF\u70B9")));
                }
            }
        }
    }

    private BitmapDescriptor getThroughPointBitDes() {
        return BitmapDescriptorFactory.fromResource(R.drawable.amap_through);
    }

    private void colorWayUpdate(List<TMC> tmcSection) {
        if (aMap == null) {
            return;
        }
        if (tmcSection == null || tmcSection.size() <= 0) {
            return;
        }
        TMC segmentTrafficStatus;
        mPolylineOptionsColor = null;
        mPolylineOptionsColor = new PolylineOptions();
        mPolylineOptionsColor.width(getRouteWidth());
        List<Integer> colorList = new ArrayList<Integer>();
        mPolylineOptionsColor.add(MapUtil.convertToLatLng(tmcSection.get(0).getPolyline().get(0)));
        colorList.add(getDriveColor());
        for (int i = 0; i < tmcSection.size(); ++i) {
            segmentTrafficStatus = tmcSection.get(i);
            int color = getColor(segmentTrafficStatus.getStatus());
            List<LatLonPoint> points = segmentTrafficStatus.getPolyline();
            for (int j = 0; j < points.size(); ++j) {
                mPolylineOptionsColor.add(MapUtil.convertToLatLng(points.get(j)));
                colorList.add(color);
            }
        }
        colorList.add(getDriveColor());
        mPolylineOptionsColor.colorValues(colorList);
    }

    private int getColor(String status) {
        if (status.equals("畅通")) {
            return Color.GREEN;
        } else if (status.equals("缓行")) {
            return Color.YELLOW;
        } else if (status.equals("拥堵")) {
            return Color.RED;
        } else if (status.equals("严重拥堵")) {
            return Color.parseColor("#990033");
        } else {
            return Color.parseColor("#537edc");
        }
    }

    private void showColorPolyline() {
        addPolyLine(mPolylineOptionsColor);
    }

    private void showPolyline() {
        addPolyLine(mPolylineOptions);
    }

    @Override
    protected LatLngBounds getLatLngBounds() {
        LatLngBounds.Builder b = LatLngBounds.builder();
        b.include(new LatLng(startPoint.latitude, startPoint.longitude));
        b.include(new LatLng(endPoint.latitude, endPoint.longitude));
        if (this.throughPointList != null && this.throughPointList.size() > 0) {
            for (int i = 0; i < this.throughPointList.size(); ++i) {
                b.include(new LatLng(this.throughPointList.get(i).getLatitude(), this.throughPointList.get(i).getLongitude()));
            }
        }
        return b.build();
    }

    public void setThroughPointIconVisibility(boolean visibility) {
        try {
            throughPointMarkerVisible = visibility;
            if (this.throughMarkerList != null && this.throughMarkerList.size() > 0) {
                for (int i = 0; i < this.throughMarkerList.size(); ++i) {
                    this.throughMarkerList.get(i).setVisible(visibility);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            ;
        }
    }

    /**
     * 获取两点间的距离
     *
     * @param start
     * @param end
     * @return
     */
    public static int calculateDistance(LatLng start, LatLng end) {
        double x1 = start.longitude;
        double y1 = start.latitude;
        double x2 = end.longitude;
        double y2 = end.latitude;
        return calculateDistance(x1, y1, x1, y2);
    }

    public static int calculateDistance(double x1, double y1, double x2, double y2) {
        final double NF_pi = 0.01745329251994329;
        x1 *= NF_pi;
        y1 *= NF_pi;
        x2 *= NF_pi;
        y2 *= NF_pi;
        double sinx1 = Math.sin(x1);
        double siny1 = Math.sin(y1);
        double cosx1 = Math.cos(x1);
        double cosy1 = Math.cos(y1);
        double sinx2 = Math.sin(x2);
        double siny2 = Math.sin(y2);
        double cosx2 = Math.cos(x2);
        double cosy2 = Math.cos(y2);
        double[] v1 = new double[3];
        v1[0] = cosy1 * cosx1 - cosy2 * cosx2;
        v1[1] = cosy1 * sinx1 - cosy2 * sinx2;
        v1[2] = siny1 - siny2;
        double dist = Math.sqrt(v1[0] * v1[0] + v1[1] * v1[1] + v1[2] * v1[2]);

        return (int) (Math.asin(dist / 2) * 12742001.5798544);
    }

    /**
     * 获取两点之间的固定聚聚
     *
     * @param spt
     * @param ept
     * @param dis
     * @return
     */
    public static LatLng getPointForDis(LatLng spt, LatLng ept, double dis) {
        double lSegLength = calculateDistance(spt, ept);
        double preResult = dis / lSegLength;
        return new LatLng((ept.latitude - spt.latitude) * preResult + spt.latitude, (ept.longitude - spt.longitude) * preResult + spt.longitude);
    }

    public void removeFormMap() {
        try {
            if (this.throughMarkerList != null && this.throughMarkerList.size() > 0) {
                for (int i = 0; i < this.throughMarkerList.size(); ++i) {
                    this.throughMarkerList.get(i).remove();
                }
                this.throughMarkerList.clear();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
