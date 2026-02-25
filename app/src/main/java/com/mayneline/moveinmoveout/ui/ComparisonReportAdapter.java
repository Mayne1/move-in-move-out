package com.mayneline.moveinmoveout.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mayneline.moveinmoveout.R;
import com.mayneline.moveinmoveout.model.ComparisonRow;

import java.io.File;
import java.util.List;

public class ComparisonReportAdapter extends RecyclerView.Adapter<ComparisonReportAdapter.ComparisonViewHolder> {
    public interface OnDetailsClickListener {
        void onDetailsClick(ComparisonRow row);
    }
    public interface OnRepairClickListener {
        void onRepairClick(ComparisonRow row);
    }

    private final List<ComparisonRow> rows;
    private final OnDetailsClickListener detailsClickListener;
    private final OnRepairClickListener repairClickListener;

    public ComparisonReportAdapter(
            List<ComparisonRow> rows,
            OnDetailsClickListener detailsClickListener,
            OnRepairClickListener repairClickListener
    ) {
        this.rows = rows;
        this.detailsClickListener = detailsClickListener;
        this.repairClickListener = repairClickListener;
    }

    @NonNull
    @Override
    public ComparisonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_comparison_item, parent, false);
        return new ComparisonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComparisonViewHolder holder, int position) {
        holder.bind(rows.get(position), detailsClickListener, repairClickListener);
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
        private final Button buttonDetails;
        private final Button buttonAddRepairEvidence;

        ComparisonViewHolder(@NonNull View itemView) {
            super(itemView);
            textRoom = itemView.findViewById(R.id.textRoom);
            textItem = itemView.findViewById(R.id.textItem);
            imageMoveIn = itemView.findViewById(R.id.imageMoveIn);
            imageMoveOut = itemView.findViewById(R.id.imageMoveOut);
            textStatus = itemView.findViewById(R.id.textStatus);
            buttonDetails = itemView.findViewById(R.id.buttonDetails);
            buttonAddRepairEvidence = itemView.findViewById(R.id.buttonAddRepairEvidence);
        }

        void bind(ComparisonRow row, OnDetailsClickListener detailsListener, OnRepairClickListener repairListener) {
            textRoom.setText("Room: " + row.getRoom());
            textItem.setText("Item: " + row.getItem());
            bindImage(imageMoveIn, row.getMoveInPath());
            bindImage(imageMoveOut, row.getMoveOutPath());
            textStatus.setText("Status: " + displayStatus(row.getStatus()));
            textStatus.setTextColor(statusColor(row.getStatus()));
            buttonDetails.setOnClickListener(v -> {
                if (detailsListener != null) {
                    detailsListener.onDetailsClick(row);
                }
            });
            buttonAddRepairEvidence.setOnClickListener(v -> {
                if (repairListener != null) {
                    repairListener.onRepairClick(row);
                }
            });
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

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            Bitmap bitmap = BitmapFactory.decodeFile(path, options);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        }

        private int statusColor(String status) {
            if ("NO_CHANGE".equals(status)) {
                return Color.parseColor("#1B5E20");
            }
            if ("MEDIA_MISSING".equals(status)) {
                return Color.parseColor("#B71C1C");
            }
            return Color.parseColor("#E65100");
        }

        private String displayStatus(String status) {
            if ("MEDIA_MISSING".equals(status)) {
                return "Media Missing";
            }
            if ("NO_CHANGE".equals(status)) {
                return "No Change";
            }
            return "Needs Review";
        }
    }
}
