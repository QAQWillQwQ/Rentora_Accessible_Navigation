// 🟡 新建文件：GeocodingUtils.java
package com.rentalapp.utils;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import java.util.List;
import java.util.Locale;

public class Geocoding {

    public static double[] getLatLngFromAddress(Context context, String address) {
        double[] result = {0.0, 0.0}; // 默认 fallback

        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> geoResults = geocoder.getFromLocationName(address, 1);
            if (geoResults != null && !geoResults.isEmpty()) {
                Address loc = geoResults.get(0);
                result[0] = loc.getLatitude();
                result[1] = loc.getLongitude();
            }
        } catch (Exception e) {
            Log.e("GeocodingUtils", "Failed to get lat/lng from address", e);
        }

        return result;
    }
}
