package com.mayneline.moveinmoveout.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mayneline.moveinmoveout.R;
import com.mayneline.moveinmoveout.model.ComparisonRow;

import java.io.File;
import java.util.List;

public class ComparisonReportAdapter extends RecyclerView.Adapter<ComparisonReportAdapter.ComparisonViewHolder> {
    private final List<ComparisonRow> rows;

    public ComparisonReportAdapter(List<ComparisonRow> rows) {
        this.rows = rows;
    }

    @NonNull
    @Override
    public ComparisonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_comparison_item, parent, false);
        return new ComparisonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComparisonViewHolder holder, int position) {
        holder.bind(rows.get(position));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class ComparisonViewHolder extends RecyclerView.ViewHolder {
        private final TextView textRoom;
        private final TextView textItem;
        private final ImageView imageMoveIn;
        private final ImageView imageMoveOut;
        private final TextView textStatus;

        ComparisonViewHolder(@NonNull View itemView) {
            super(itemView);
            textRoom = itemView.findViewById(R.id.textRoom);
            textItem = itemView.findViewById(R.id.textItem);
            imageMoveIn = itemView.findViewById(R.id.imageMoveIn);
            imageMoveOut = itemView.findViewById(R.id.imageMoveOut);
            textStatus = itemView.findViewById(R.id.textStatus);
        }

        void bind(ComparisonRow row) {
            textRoom.setText("Room: " + row.getRoom());
            textItem.setText("Item: " + row.getItem());
            bindImage(imageMoveIn, row.getMoveInPath());
            bindImage(imageMoveOut, row.getMoveOutPath());
            textStatus.setText("Status: " + row.getStatus());
            textStatus.setTextColor(statusColor(row.getStatus()));
        }

        private void bindImage(ImageView imageView, String path) {
            if (TextUtils.isEmpty(path)) {
                imageView.setImageResource(android.R.drawable.ic_menu_report_image);
                return;
            }

            File file = new File(path);
            if (!file.exists()) {
                imageView.setImageResource(android.R.drawable.ic_menu_report_image);
                return;
            }

            Bitmap bitmap = BitmapFactory.decodeFile(path);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        }

        private int statusColor(String status) {
            if ("No Change".equals(status)) {
                return Color.parseColor("#1B5E20");
            }
            if ("Media Missing".equals(status)) {
                return Color.parseColor("#B71C1C");
            }
            return Color.parseColor("#E65100");
        }
    }
}
