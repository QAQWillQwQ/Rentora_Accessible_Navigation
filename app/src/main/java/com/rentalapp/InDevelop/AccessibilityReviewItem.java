// 🟣 AccessibilityReviewItem.java
package com.rentalapp.InDevelop;

import java.util.List;

/**
 * 📄 表示一条与无障碍出行相关的评论数据结构
 * 来源可以是 Google Maps、Yelp、或其他开放平台
 */
public class AccessibilityReviewItem {

    public String poiName;           // 商户或设施名称
    public double latitude;          // 商户纬度
    public double longitude;         // 商户经度
    public String source;            // 来源平台，如 "Google Maps", "Yelp", "NYC.gov"
    public String fullText;          // 原始评论内容
    public List<String> matchedKeywords; // 匹配的关键词（如 "stairs", "elevator"）
    public String timestamp;         // 评论时间（可选）

    public AccessibilityReviewItem(String poiName, double latitude, double longitude,
                                   String source, String fullText, List<String> matchedKeywords,
                                   String timestamp) {
        this.poiName = poiName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.source = source;
        this.fullText = fullText;
        this.matchedKeywords = matchedKeywords;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "📌 [" + poiName + "] from " + source + ":\n"
                + fullText + "\nKeywords: " + matchedKeywords + "\n";
    }
}
