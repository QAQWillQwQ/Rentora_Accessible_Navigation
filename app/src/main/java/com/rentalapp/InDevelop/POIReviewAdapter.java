// 🟣 POIReviewAdapter.java
package com.rentalapp.InDevelop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rentalapp.R;

import java.util.List;

public class POIReviewAdapter extends RecyclerView.Adapter<POIReviewAdapter.ViewHolder> {

    private final List<AccessibilityReviewItem> list;

    public POIReviewAdapter(List<AccessibilityReviewItem> list) {
        this.list = list;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, review;

        public ViewHolder(@NonNull View v) {
            super(v);
            title = v.findViewById(R.id.poiName);
            review = v.findViewById(R.id.reviewText);
        }
    }

    @NonNull
    @Override
    public POIReviewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull POIReviewAdapter.ViewHolder holder, int position) {
        AccessibilityReviewItem item = list.get(position);
        holder.title.setText("📍 " + item.poiName + " — " + item.source);
        holder.review.setText(item.fullText);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
