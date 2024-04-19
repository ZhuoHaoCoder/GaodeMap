package com.example.gaodemap.overlay;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.PolygonHoleOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.example.gaodemap.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 路线图层增加
 */
public class RouteOverlay {
    protected List<Marker> stationMarkers = new ArrayList<Marker>();
    protected List<Polyline> allPolyLines = new ArrayList<Polyline>();
    protected Marker startMarker;
    protected Marker endMarker;
    protected LatLng startPoint;
    protected LatLng endPoint;
    protected AMap aMap;
    private Context mContext;
    private Bitmap startBit, endBit, busBit, walkBit, driveBit;
    protected boolean nodeIconVisible = true;

    public RouteOverlay(Context context) {
        mContext = context;
    }

    /**
     * 去掉BusRouteOverlay上的所有Marker
     */
    public void removeFromMap() {
        if (startMarker != null) {
            startMarker.remove();
        }
        if (endMarker != null) {
            endMarker.remove();
        }
        for (Marker marker : stationMarkers) {
            marker.remove();
        }
        for (Polyline polyline : allPolyLines) {
            polyline.remove();
        }
        destroyBit();
    }

    private void destroyBit() {
        if (startBit != null) {
            startBit.recycle();
            startBit = null;
        }
        if (endBit != null) {
            endBit.recycle();
            endBit = null;
        }
        if (busBit != null) {
            busBit.recycle();
            busBit = null;
        }
        if (walkBit != null) {
            walkBit.recycle();
            walkBit = null;
        }
        if (driveBit != null) {
            driveBit.recycle();
            driveBit = null;
        }
    }

    /**
     * 给起点Marker设置图标，并返回更新图标的图片。如不用默认图片，需要重写此方法
     *
     * @return 更换的Marker图片
     */
    protected BitmapDescriptor getStartBitmapDescriptor() {
        return BitmapDescriptorFactory.fromResource(R.drawable.amap_start);
    }

    /**
     * 给终点Marker设置图标，并返回更新图标的图片，如不用默认图片，需要重写此方法
     *
     * @return 更换的Marker图片
     */
    protected BitmapDescriptor getEndBitmapDescriptor() {
        return BitmapDescriptorFactory.fromResource(R.drawable.amap_end);
    }

    /**
     * 给公交车Marker设置图标，并返回更新图标的图片，如不用默认图片，需要重写此方法
     *
     * @return 更换的Marker图片
     */
    protected BitmapDescriptor getBusBitmapDescriptor() {
        return BitmapDescriptorFactory.fromResource(R.drawable.amap_bus);
    }

    /**
     * 给步行Marker设置图标，并返回更新图标的图片，如不用默认图片，需要重写此方法
     *
     * @return 更换的Marker
     */
    protected BitmapDescriptor getWalkBitmapDescriptor() {
        return BitmapDescriptorFactory.fromResource(R.drawable.amap_man);
    }

    /**
     * 给开车Marker设置图标，并返回更新图标的图片，如不用默认图片，需要重写此方法
     *
     * @return 更换后的Marker
     */
    protected BitmapDescriptor getDriveBitmapDescriptor() {
        return BitmapDescriptorFactory.fromResource(R.drawable.amap_car);
    }

    protected void addStartAndEndMarker() {
        startMarker = aMap.addMarker((new MarkerOptions())
                .position(startPoint).icon(getStartBitmapDescriptor())
                .title("\\u8D77\\u70B9"));
        endMarker = aMap.addMarker((new MarkerOptions())
                .position(endPoint).icon(getEndBitmapDescriptor())
                .title("\\u7EC8\\u70B9"));
    }

    /**
     * 移动镜头到当前的视角
     */
    public void zoomToSpan() {
        if (startPoint != null) {
            if (aMap == null) {
                return;
            }
            try {
                LatLngBounds bounds = getLatLngBounds();
                aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    protected LatLngBounds getLatLngBounds() {
        LatLngBounds.Builder b = LatLngBounds.builder();
        b.include(new LatLng(startPoint.latitude, startPoint.longitude));
        b.include(new LatLng(endPoint.latitude, endPoint.longitude));
        return b.build();
    }

    /**
     * 路段节点图标控制显示窗口
     *
     * @param visible true为显示， false为不显示
     */
    public void setNodeIconVisibility(boolean visible) {
        try {
            nodeIconVisible = visible;
            if (this.stationMarkers != null && this.stationMarkers.size() > 0) {
                for (int i = 0; i < this.stationMarkers.size(); ++i) {
                    this.stationMarkers.get(i).setVisible(visible);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    protected void addStationMarker(MarkerOptions options) {
        if (options == null) {
            return;
        }
        Marker marker = aMap.addMarker(options);
        if (marker != null) {
            stationMarkers.add(marker);
        }
    }

    protected void addPolyLine(PolylineOptions options) {
        if (options == null) {
            return;
        }
        Polyline polyline = aMap.addPolyline(options);
        if (polyline != null) {
            allPolyLines.add(polyline);
        }
    }

    protected float getRouteWidth() {
        return 18f;
    }

    protected int getWalkColor() {
        return Color.parseColor("#6db74d");
    }

    /**
     * 自定义路线颜色
     *
     * @return
     */
    protected int getBusColor() {
        return Color.parseColor("#537edc");
    }

    protected int getDriveColor() {
        return Color.parseColor("#537edc");
    }
}
