package com.example.gaodemap.overlay;

import android.graphics.Bitmap;

import com.amap.api.maps.model.LatLng;
import com.amap.api.services.core.LatLonPoint;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

/**
 * 地图服务工具类
 */
public class AMapServicesUtil {
    public static int BUFFER_SIZE = 2048;

    public static byte[] inputStreamToByte(InputStream in) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] data = new byte[BUFFER_SIZE];
        int count = 1;
        while ((count = in.read(data, 0, BUFFER_SIZE)) != -1) {
            outputStream.write(data, 0, count);
        }
        data = null;
        return outputStream.toByteArray();
    }

    public static LatLonPoint convertToLatLonPoint(LatLng latLng) {
        return new LatLonPoint(latLng.latitude, latLng.longitude);
    }

    public static LatLng convertToLatLng(LatLonPoint latLonPoint) {
        return new LatLng(latLonPoint.getLatitude(), latLonPoint.getLongitude());
    }

    public static ArrayList<LatLng> convertArrayList(List<LatLonPoint> shapes) {
        ArrayList<LatLng> latShapes = new ArrayList<LatLng>();
        for (LatLonPoint lonPoint : shapes) {
            LatLng latTemp = AMapServicesUtil.convertToLatLng(lonPoint);
            latShapes.add(latTemp);
        }
        return latShapes;
    }

    public static Bitmap zoomBitmap(Bitmap bitmap, float res) {
        if (bitmap == null) {
            return null;
        }
        int width, height;
        width = (int) (bitmap.getWidth() * res);
        height = (int) (bitmap.getHeight() * res);
        Bitmap newBmp = Bitmap.createScaledBitmap(bitmap, width, height, true);
        return newBmp;
    }
}
