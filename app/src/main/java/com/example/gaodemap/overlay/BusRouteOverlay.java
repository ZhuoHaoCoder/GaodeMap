package com.example.gaodemap.overlay;

import android.content.Context;

import com.amap.api.maps.AMap;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.services.busline.BusStationItem;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusPath;
import com.amap.api.services.route.BusStep;
import com.amap.api.services.route.RailwayStationItem;
import com.amap.api.services.route.RouteBusLineItem;
import com.amap.api.services.route.RouteBusWalkItem;
import com.amap.api.services.route.RouteRailwayItem;
import com.amap.api.services.route.TaxiItem;
import com.amap.api.services.route.WalkStep;
import com.example.gaodemap.util.MapUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.LoggingPermission;

/**
 * 公交路线图层类
 */
public class BusRouteOverlay extends RouteOverlay {
    private BusPath busPath;
    private LatLng latLng;

    /**
     * 通过构造函数创建公交路线图层
     *
     * @param context 当前activity
     * @param aMap    地图对象
     * @param path    公交路径规划的一个路段
     * @param start   起点坐标
     * @param end     终点坐标
     */
    public BusRouteOverlay(Context context, AMap aMap, BusPath path, LatLonPoint start, LatLonPoint end) {
        super(context);
        this.busPath = path;
        startPoint = MapUtil.convertToLatLng(start);
        endPoint = MapUtil.convertToLatLng(end);
        this.aMap = aMap;
    }

    /**
     * 添加公交路线到地图上
     */
    public void addToMap() {
        /**
         * 绘制节点和线细节情况比较多
         * 两个step之间，用step和step1区分
         * 1.两个step内可能有步行和公交，然后可能他们之间连接有断开
         * 2.step的公交和step1的步行，有可能连接断开
         * 3.step和step1之间是公交换乘，且没有步行，需要把step的终点和step1的起点连起来
         * 4.公交最后一站和终点间有步行，加入步行路线，还会有一些步行marker
         * 5.公交最后一站和终点间无步行，直接连起来
         */
        try {
            List<BusStep> busSteps = busPath.getSteps();
            for (int i = 0; i < busSteps.size(); ++i) {
                BusStep busStep = busSteps.get(i);
                if (i < busSteps.size() - 1) {
                    BusStep busStep1 = busSteps.get(i + 1);//取得下一个对象
                    //加入步行和公交之间连接偶断开，就把步行最后一个经纬度点和公交第一个经纬度点连接起来，避免断线问题
                    if (busStep.getWalk() != null && busStep.getBusLines() != null) {
                        checkWalkToBusLine(busStep);
                    }
                    //假如公交车和步行之间连接有断开，就把上一公交经纬度和下一步行的第一个经纬度连接起来，避免断线问题
                    if (busStep.getBusLine() != null && busStep1.getWalk() != null && busStep1.getWalk().getSteps().size() > 0) {
                        checkBusLineToNextWalk(busStep, busStep1);
                    }
                    //假如两个公交换乘中间没有步行，就把上一公交经纬度和下一公交的第一个经纬度连接起来，避免断线问题
                    if (busStep.getBusLine() != null && busStep1.getWalk() == null && busStep1.getBusLine() != null) {
                        checkBusEndToNextBusStart(busStep, busStep1);
                    }
                    if (busStep.getBusLines() != null && busStep1.getWalk() == null && busStep1.getBusLine() != null) {
                        checkBusLineToBusNoWalk(busStep, busStep1);
                    }
                    if (busStep.getBusLine() != null && busStep1.getRailway() != null && busStep1.getBusLine() != null) {
                        checkBusLineToNextRailway(busStep, busStep1);
                    }
                    if (busStep1.getWalk() != null && busStep1.getWalk().getSteps().size() > 0 && busStep.getRailway() != null) {
                        checkRailwayToNextWalk(busStep, busStep1);
                    }
                    if (busStep1.getRailway() != null && busStep.getRailway() != null) {
                        checkRailwayToNextRailway(busStep, busStep1);
                    }
                    if (busStep.getRailway() != null && busStep1.getTaxi() != null) {
                        checkRailwayToNextTaxi(busStep, busStep1);
                    }
                }
                if (busStep.getWalk() != null && busStep.getWalk().getSteps().size() > 0) {
                    addWalkSteps(busStep);
                } else {
                    if (busStep.getBusLine() == null && busStep.getRailway() == null && busStep.getTaxi() == null) {
                        addWalkPolyline(latLng, endPoint);
                    }
                }
                if (busStep.getBusLine() != null) {
                    RouteBusLineItem routeBusLineItem = busStep.getBusLine();
                    addBusLineSteps(routeBusLineItem);
                    addBusStationMarkers(routeBusLineItem);
                    if (i == busSteps.size() - 1) {
                        addWalkPolyline(MapUtil.convertToLatLng(getLastBusLinePoint(busStep)), endPoint);
                    }
                }
                if (busStep.getRailway() != null) {
                    addRailwayStep(busStep.getRailway());
                    addRailwayMarkers(busStep.getRailway());
                    if (i == busSteps.size() - 1) {
                        addWalkPolyline(MapUtil.convertToLatLng(busStep.getRailway().getArrivalstop().getLocation()), endPoint);
                    }
                }
                if (busStep.getTaxi() != null) {
                    addTaxiStep(busStep.getTaxi());
                    addTaxiMarkers(busStep.getTaxi());
                }
            }
            addStartAndEndMarker();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void checkRailwayToNextTaxi(BusStep busStep, BusStep busStep1) {
        LatLonPoint railwayLastPoint = busStep.getRailway().getArrivalstop().getLocation();
        LatLonPoint taxiFirstPoint = busStep1.getTaxi().getOrigin();
        if (!railwayLastPoint.equals(taxiFirstPoint)) {
            addWalkPolyLineByLatLonPoints(railwayLastPoint, taxiFirstPoint);
        }
    }

    private void checkRailwayToNextRailway(BusStep busStep, BusStep busStep1) {
        LatLonPoint railwayLastPoint = busStep.getRailway().getArrivalstop().getLocation();
        LatLonPoint railwayFirstPoint = busStep1.getRailway().getDeparturestop().getLocation();
        if (!railwayLastPoint.equals(railwayFirstPoint)) {
            addWalkPolyLineByLatLonPoints(railwayLastPoint, railwayFirstPoint);
        }
    }

    private void checkRailwayToNextWalk(BusStep busStep, BusStep busStep1) {
        LatLonPoint railwayLastPoint = busStep.getRailway().getArrivalstop().getLocation();
        LatLonPoint walkFirstPoint = getFirstWalkPoint(busStep1);
        if (!railwayLastPoint.equals(walkFirstPoint)) {
            addWalkPolyLineByLatLonPoints(railwayLastPoint, walkFirstPoint);
        }
    }

    private void checkBusLineToNextRailway(BusStep busStep, BusStep busStep1) {
        LatLonPoint busLastPoint = getLastBusLinePoint(busStep);
        LatLonPoint railwayFirstPoint = busStep1.getRailway().getDeparturestop().getLocation();
        if (!busLastPoint.equals(railwayFirstPoint)) {
            addWalkPolyLineByLatLonPoints(busLastPoint, railwayFirstPoint);
        }
    }

    /**
     * 如果换乘没有步行，检查最后一个点和下一个step的bus起点是否一致
     *
     * @param busStep
     * @param busStep1
     */
    private void checkBusLineToBusNoWalk(BusStep busStep, BusStep busStep1) {
        LatLng endBusLatLng = MapUtil.convertToLatLng(getLastBusLinePoint(busStep));
        LatLng startBusLatLng = MapUtil.convertToLatLng(getFirstBusLinePoint(busStep1));
        if (startBusLatLng.latitude - endBusLatLng.latitude > 0.0001 || startBusLatLng.longitude - endBusLatLng.longitude > 0.0001) {
            drawLineArrow(endBusLatLng, startBusLatLng);
        }
    }

    private void checkBusEndToNextBusStart(BusStep busStep, BusStep busStep1) {
        LatLonPoint busLastPoint = getLastBusLinePoint(busStep);
        LatLng endBusLatLng = MapUtil.convertToLatLng(busLastPoint);
        LatLonPoint busFirstPoint = getFirstBusLinePoint(busStep1);
        LatLng startBusLatLng = MapUtil.convertToLatLng(busFirstPoint);
        if (!endBusLatLng.equals(startBusLatLng)) {
            drawLineArrow(endBusLatLng, startBusLatLng);
        }
    }

    /**
     * 检查bus最后一步和下一个step的步行起点是否一致
     *
     * @param busStep
     * @param busStep1
     */
    private void checkBusLineToNextWalk(BusStep busStep, BusStep busStep1) {
        LatLonPoint busLastPoint = getLastBusLinePoint(busStep);
        LatLonPoint walkFirst = getFirstWalkPoint(busStep1);
        if (!busLastPoint.equals(walkFirst)) {
            addWalkPolyLineByLatLonPoints(busLastPoint, walkFirst);
        }
    }

    /**
     * 检查步行最后一点和bus的起点是否一致
     *
     * @param busStep
     */
    private void checkWalkToBusLine(BusStep busStep) {
        LatLonPoint walkLastPoint = getLastWalkPoint(busStep);
        LatLonPoint busLineFirstPoint = getFirstBusLinePoint(busStep);
        if (!walkLastPoint.equals(busLineFirstPoint)) {
            addWalkPolyLineByLatLonPoints(walkLastPoint, busLineFirstPoint);
        }
    }

    private void addTaxiStep(TaxiItem taxiItem) {
        addPolyLine(new PolylineOptions().width(getRouteWidth())
                .color(getBusColor())
                .add(MapUtil.convertToLatLng(taxiItem.getOrigin()))
                .add(MapUtil.convertToLatLng(taxiItem.getDestination())));
    }

    private void addBusLineSteps(RouteBusLineItem item) {
        addBusLineSteps(item.getPolyline());
    }

    private void addRailwayStep(RouteRailwayItem item) {
        List<LatLng> railwayListPoint = new ArrayList<LatLng>();
        List<RailwayStationItem> railwayStationItems = new ArrayList<RailwayStationItem>();
        railwayStationItems.add(item.getDeparturestop());
        railwayStationItems.addAll(item.getViastops());
        railwayStationItems.add(item.getArrivalstop());
        for (int i = 0; i < railwayListPoint.size(); ++i) {
            railwayListPoint.add(MapUtil.convertToLatLng(railwayStationItems.get(i).getLocation()));
        }
        addRailwayPolyLine(railwayListPoint);
    }

    private void addBusLineSteps(List<LatLonPoint> lonPoints) {
        if (lonPoints.size() < 1) {
            return;
        }
        addPolyLine(new PolylineOptions().width(getRouteWidth())
                .color(getBusColor()).addAll(MapUtil.convertArrList(lonPoints)));
    }

    private void addWalkSteps(BusStep busStep) {
        RouteBusWalkItem routeBusWalkItem = busStep.getWalk();
        List<WalkStep> walkSteps = routeBusWalkItem.getSteps();
        for (int j = 0; j < walkSteps.size(); ++j) {
            WalkStep walkStep = walkSteps.get(j);
            if (j == 0) {
                LatLng latLng = MapUtil.convertToLatLng(walkStep.getPolyline().get(0));
                String road = walkStep.getRoad();
                String instruction = getWalkSnippet(walkSteps);
                addWalkStationMarkers(latLng, road, instruction);
            }
            List<LatLng> listWalkPolyline = MapUtil.convertArrList(walkStep.getPolyline());
            this.latLng = listWalkPolyline.get(listWalkPolyline.size() - 1);
            addWalkPolyline(listWalkPolyline);

            if (j < walkSteps.size() - 1) {
                LatLng lastLatLng = listWalkPolyline.get(listWalkPolyline.size() - 1);
                LatLng firstLatLng = MapUtil.convertToLatLng(walkSteps.get(j + 1).getPolyline().get(0));
                if (!(lastLatLng).equals(firstLatLng)) {
                    addWalkPolyline(lastLatLng, firstLatLng);
                }
            }
        }
    }

    private void addTaxiMarkers(TaxiItem taxiItem) {
        LatLng position = MapUtil.convertToLatLng(taxiItem.getOrigin());
        String title = taxiItem.getmSname() + "打车";
        String snippet = "到终点";
        addStationMarker(new MarkerOptions().position(position).title(title)
                .snippet(snippet).anchor(0.5f, 0.5f).visible(nodeIconVisible)
                .icon(getDriveBitmapDescriptor()));
    }

    private void addRailwayMarkers(RouteRailwayItem railway) {
        LatLng DeparturePosition = MapUtil.convertToLatLng(railway.getDeparturestop().getLocation());
        String departureTitle = railway.getDeparturestop().getName() + "上车";
        String departureSnippet = railway.getName();
        addStationMarker(new MarkerOptions().position(DeparturePosition).title(departureTitle)
                .snippet(departureSnippet).anchor(0.5f, 0.5f).visible(nodeIconVisible)
                .icon(getBusBitmapDescriptor()));

        LatLng arrivalPosition = MapUtil.convertToLatLng(railway.getArrivalstop().getLocation());
        String arrivalTitle = railway.getArrivalstop().getName() + "下车";
        String arrivalSnippet = railway.getName();

        addStationMarker(new MarkerOptions().position(arrivalPosition).title(arrivalTitle)
                .snippet(arrivalSnippet).anchor(0.5f, 0.5f).visible(nodeIconVisible)
                .icon(getBusBitmapDescriptor()));
    }

    private void addBusStationMarkers(RouteBusLineItem routeBusLineItem) {
        BusStationItem startBusStation = routeBusLineItem.getDepartureBusStation();
        LatLng position = MapUtil.convertToLatLng(startBusStation.getLatLonPoint());
        String title = routeBusLineItem.getBusLineName();
        String snippet = getBusSnippet(routeBusLineItem);
        addStationMarker(new MarkerOptions().position(position).title(title)
                .snippet(snippet).anchor(0.5f, 0.5f).visible(nodeIconVisible)
                .icon(getBusBitmapDescriptor()));
    }

    private void addWalkStationMarkers(LatLng latLng, String title, String snippet) {
        addStationMarker(new MarkerOptions().position(latLng).title(title).snippet(snippet)
                .anchor(0.5f, 0.5f).visible(nodeIconVisible)
                .icon(getWalkBitmapDescriptor()));
    }

    private void addWalkPolyLineByLatLonPoints(LatLonPoint pointFrom, LatLonPoint pointTo) {
        LatLng latLngForm = MapUtil.convertToLatLng(pointFrom);
        LatLng latLngTo = MapUtil.convertToLatLng(pointTo);
        addWalkPolyline(latLngForm, latLngTo);
    }

    private void addRailwayPolyLine(List<LatLng> latLngList) {
        addPolyLine(new PolylineOptions().addAll(latLngList).color(getDriveColor()).width(getRouteWidth()));
    }

    private void addWalkPolyline(LatLng latLngFrom, LatLng latLngTo) {
        addPolyLine(new PolylineOptions().add(latLngFrom, latLngTo)
                .width(getRouteWidth()).color(getWalkColor()).setDottedLine(true));
    }

    private void addWalkPolyline(List<LatLng> latLngList) {
        addPolyLine(new PolylineOptions().addAll(latLngList).color(getWalkColor())
                .width(getRouteWidth()).setDottedLine(true));
    }

    private String getBusSnippet(RouteBusLineItem routeBusLineItem) {
        return "("
                + routeBusLineItem.getDepartureBusStation().getBusStationName()
                + "-->"
                + routeBusLineItem.getArrivalBusStation().getBusStationName()
                + ") \u7ECF\u8FC7" + (routeBusLineItem.getPassStationNum() + 1) + "\u7AD9";
    }

    private String getWalkSnippet(List<WalkStep> walkSteps) {
        float disNum = 0;
        for (WalkStep step : walkSteps) {
            disNum += step.getDistance();
        }
        return "\u6B65\u884C" + disNum + "\u7C73";
    }

    private LatLonPoint getFirstWalkPoint(BusStep busStep)  {
        return busStep.getWalk().getSteps().get(0).getPolyline().get(0);
    }

    private LatLonPoint getLastWalkPoint(BusStep busStep) {
        List<WalkStep> walkSteps = busStep.getWalk().getSteps();
        WalkStep walkStep = walkSteps.get(walkSteps.size() - 1);
        List<LatLonPoint> lonPoints = walkStep.getPolyline();
        return lonPoints.get(lonPoints.size() - 1);
    }

    private LatLonPoint getFirstBusLinePoint(BusStep busStep) {
        return busStep.getBusLine().getPolyline().get(0);
    }

    private LatLonPoint getLastBusLinePoint(BusStep busStep) {
        List<LatLonPoint> lonPoints = busStep.getBusLine().getPolyline();
        return lonPoints.get(lonPoints.size() - 1);
    }

    public void drawLineArrow(LatLng latLngFrom, LatLng latLngTo) {
        addPolyLine(new PolylineOptions().add(latLngFrom, latLngTo).width(3)
                .color(getBusColor()).width(getRouteWidth()));
    }
}
