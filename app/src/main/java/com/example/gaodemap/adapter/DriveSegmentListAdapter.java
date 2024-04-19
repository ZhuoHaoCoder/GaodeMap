package com.example.gaodemap.adapter;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amap.api.services.route.DriveStep;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.example.gaodemap.R;
import com.example.gaodemap.util.MapUtil;

import java.util.List;

public class DriveSegmentListAdapter extends BaseQuickAdapter<DriveStep, BaseViewHolder> {
    private List<DriveStep> driveSteps;

    public DriveSegmentListAdapter(int layoutResId, @Nullable List<DriveStep> data) {
        super(layoutResId, data);
        driveSteps = data;
    }

    @Override
    protected void convert(@NonNull BaseViewHolder baseViewHolder, DriveStep driveStep) {
        TextView lineName = baseViewHolder.getView(R.id.bus_line_name);
        ImageView dirIcon = baseViewHolder.getView(R.id.bus_dir_icon);
        ImageView dirUp = baseViewHolder.getView(R.id.bus_dir_icon_up);
        ImageView dirDown = baseViewHolder.getView(R.id.bus_dir_icon_down);
        ImageView splitLine = baseViewHolder.getView(R.id.bus_seg_split_line);
        int position = getItemPosition(driveStep);
        if (position == 0) {
            dirIcon.setImageResource(R.drawable.dir_start);
            lineName.setText("出发");
            dirUp.setVisibility(View.INVISIBLE);
            dirDown.setVisibility(View.VISIBLE);
            splitLine.setVisibility(View.INVISIBLE);
        } else if (position == driveSteps.size() - 1) {
            dirIcon.setImageResource(R.drawable.dir_end);
            lineName.setText("到达终点");
            dirUp.setVisibility(View.VISIBLE);
            dirDown.setVisibility(View.INVISIBLE);
        } else {
            splitLine.setVisibility(View.VISIBLE);
            dirUp.setVisibility(View.VISIBLE);
            dirDown.setVisibility(View.VISIBLE);
            String actionName = driveStep.getAction();
            int resId = MapUtil.getDriveActionId(actionName);
            dirIcon.setImageResource(resId);
            lineName.setText(driveStep.getInstruction());
        }
    }
}
