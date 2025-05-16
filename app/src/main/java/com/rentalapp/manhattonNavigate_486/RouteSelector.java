
package com.rentalapp.manhattonNavigate_486;

import com.rentalapp.InDevelop.BusinessAccessibilityAnalyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RouteSelector {

    public static class StartEndPair {
        public BusinessAccessibilityAnalyzer.AnnotatedNode start;
        public BusinessAccessibilityAnalyzer.AnnotatedNode end;

        public StartEndPair(BusinessAccessibilityAnalyzer.AnnotatedNode s,
                            BusinessAccessibilityAnalyzer.AnnotatedNode e) {
            this.start = s;
            this.end = e;
        }
    }

    public static StartEndPair selectPair(List<BusinessAccessibilityAnalyzer.AnnotatedNode> nodes) {
        Random random = new Random();
        List<StartEndPair> candidates = new ArrayList<>();

        for (int i = 0; i < nodes.size(); i++) {
            for (int j = i + 1; j < nodes.size(); j++) {
                var a = nodes.get(i);
                var b = nodes.get(j);

                double dist = haversineDistance(a.lat, a.lon, b.lat, b.lon);
                if (dist <= 2.0) { // 单位 km
                    candidates.add(new StartEndPair(a, b));
                }
            }
        }

        if (!candidates.isEmpty()) {
            return candidates.get(random.nextInt(candidates.size()));
        }

        return null;
    }

    public static double haversineDistance(double lat1, double lon1,
                                           double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(rLat1) * Math.cos(rLat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}
