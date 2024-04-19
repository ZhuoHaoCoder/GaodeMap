package com.example.gaodemap.adapter;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amap.api.services.busline.BusStationItem;
import com.amap.api.services.route.RailwayStationItem;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.example.gaodemap.R;
import com.example.gaodemap.util.SchemeBusStep;

import java.util.List;

/**
 * 公交段列表适配器
 */
public class BusSegmentListAdapter extends BaseQuickAdapter<SchemeBusStep, BaseViewHolder> {
    private List<SchemeBusStep> busStepList;

    public BusSegmentListAdapter(int layoutResId, @Nullable List<SchemeBusStep> data) {
        super(layoutResId, data);
        busStepList = data;
    }

    @Override
    protected void convert(@NonNull BaseViewHolder baseViewHolder, SchemeBusStep schemeBusStep) {
        RelativeLayout busItem = baseViewHolder.getView(R.id.bus_item);
        TextView busLineName = baseViewHolder.getView(R.id.bus_line_name);
        ImageView busDirIcon = baseViewHolder.getView(R.id.bus_dir_icon);
        TextView busStationNum = baseViewHolder.getView(R.id.bus_station_num);
        final ImageView busExpandImage = baseViewHolder.getView(R.id.bus_expand_image);
        ImageView busDirUp = baseViewHolder.getView(R.id.bus_dir_icon_up);
        ImageView busDirDown = baseViewHolder.getView(R.id.bus_dir_icon_down);
        ImageView splitLine = baseViewHolder.getView(R.id.bus_seg_split_line);
        final LinearLayout expandContent = baseViewHolder.getView(R.id.expand_content);

        int position = getItemPosition(schemeBusStep);

        if (position == 0) {
            busDirIcon.setImageResource(R.drawable.dir_start);
            busLineName.setText("出发");
            busDirUp.setVisibility(View.INVISIBLE);
            busDirDown.setVisibility(View.VISIBLE);
            splitLine.setVisibility(View.GONE);
            busStationNum.setVisibility(View.GONE);
            busExpandImage.setVisibility(View.GONE);
        } else if (position == busStepList.size() - 1) {
            busDirIcon.setImageResource(R.drawable.dir_end);
            busLineName.setText("到达终点");
            busDirUp.setVisibility(View.VISIBLE);
            busDirDown.setVisibility(View.INVISIBLE);
            busStationNum.setVisibility(View.INVISIBLE);
            busExpandImage.setVisibility(View.INVISIBLE);
        } else {
            if (schemeBusStep.isWalk() && schemeBusStep.getWalk() != null && schemeBusStep.getWalk().getDistance() > 0) {
                busDirIcon.setImageResource(R.drawable.dir13);
                busDirUp.setVisibility(View.VISIBLE);
                busDirDown.setVisibility(View.VISIBLE);
                busLineName.setText("步行" + (int) schemeBusStep.getWalk().getDistance() + "米");
                busStationNum.setVisibility(View.GONE);
                busExpandImage.setVisibility(View.GONE);
            } else if (schemeBusStep.isBus() && schemeBusStep.getBusLines().size() > 0) {
                busDirIcon.setImageResource(R.drawable.dir14);
                busDirUp.setVisibility(View.VISIBLE);
                busDirDown.setVisibility(View.VISIBLE);
                busLineName.setText(schemeBusStep.getBusLines().get(0).getBusLineName());
                busStationNum.setVisibility(View.VISIBLE);
                busStationNum.setText((schemeBusStep.getBusLines().get(0).getPassStationNum() + 1) + "站");
                busExpandImage.setVisibility(View.VISIBLE);
            } else if (schemeBusStep.isRailway() && schemeBusStep.getRailway() != null) {
                busDirIcon.setImageResource(R.drawable.dir16);
                busDirUp.setVisibility(View.VISIBLE);
                busDirDown.setVisibility(View.VISIBLE);
                busLineName.setText(schemeBusStep.getRailway().getName());
                busStationNum.setVisibility(View.VISIBLE);
                busStationNum.setText((schemeBusStep.getRailway().getViastops().size() + 1) + "站");
                busExpandImage.setVisibility(View.VISIBLE);
            } else if (schemeBusStep.isTaxi() && schemeBusStep.getTaxi() != null) {
                busDirIcon.setImageResource(R.drawable.dir14);
                busDirUp.setVisibility(View.VISIBLE);
                busDirDown.setVisibility(View.VISIBLE);
                busLineName.setText("打车到终点");
                busStationNum.setVisibility(View.GONE);
                busExpandImage.setVisibility(View.GONE);
            }
        }
        busItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (schemeBusStep.isBus()) {
                    if (schemeBusStep.isArrowExpend() == false) {
                        schemeBusStep.setArrowExpend(true);
                        busExpandImage.setImageResource(R.drawable.up);
                        addBusStation(schemeBusStep.getBusLine().getDepartureBusStation(), expandContent);
                        for (BusStationItem station : schemeBusStep.getBusLine().getPassStations()) {
                            addBusStation(station, expandContent);
                        }
                        addBusStation(schemeBusStep.getBusLine().getArrivalBusStation(), expandContent);
                    } else {
                        schemeBusStep.setArrowExpend(false);
                        busExpandImage.setImageResource(R.drawable.down);
                        expandContent.removeAllViews();
                    }
                } else if (schemeBusStep.isRailway()) {
                    if (schemeBusStep.isArrowExpend() == false) {
                        schemeBusStep.setArrowExpend(true);
                        busExpandImage.setImageResource(R.drawable.up);
                        addRailwayStation(schemeBusStep.getRailway().getDeparturestop(), expandContent);
                        for (RailwayStationItem item : schemeBusStep.getRailway().getViastops()) {
                            addRailwayStation(item, expandContent);
                        }
                        addRailwayStation(schemeBusStep.getRailway().getArrivalstop(), expandContent);
                    } else {
                        schemeBusStep.setArrowExpend(false);
                        busExpandImage.setImageResource(R.drawable.down);
                        expandContent.removeAllViews();
                    }
                }
            }
        });
    }

    /**
     * 添加公交站
     *
     * @param station
     * @param linearLayout
     */
    private void addBusStation(BusStationItem station, LinearLayout linearLayout) {
        LinearLayout ll = (LinearLayout) View.inflate(getContext(), R.layout.item_segment_ex, null);
        TextView tv = ll.findViewById(R.id.bus_Line_station_name);
        tv.setText(station.getBusStationName());
        linearLayout.addView(ll);
    }

    /**
     * 添加火车站
     *
     * @param station
     * @param linearLayout
     */
    private void addRailwayStation(RailwayStationItem station, LinearLayout linearLayout) {
        LinearLayout ll = (LinearLayout) View.inflate(getContext(), R.layout.item_segment_ex, null);
        TextView tv = ll.findViewById(R.id.bus_Line_station_name);
        tv.setText(station.getName() + " " + getRailwayTime(station.getTime()));
        linearLayout.addView(ll);
    }

    /**
     * 获取铁路时间
     *
     * @param time
     * @return
     */
    public static String getRailwayTime(String time) {
        return time.substring(0, 2) + ":" + time.substring(2, time.length());
    }
}
