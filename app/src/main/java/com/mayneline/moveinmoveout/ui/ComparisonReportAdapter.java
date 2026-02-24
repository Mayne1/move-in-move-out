package com.mayneline.moveinmoveout.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
        private final TextView textMoveInMediaType;
        private final TextView textMoveOutMediaType;
        private final TextView textStatus;
        private final View layoutDetails;
        private final TextView textMoveInDetails;
        private final TextView textMoveOutDetails;

        ComparisonViewHolder(@NonNull View itemView) {
            super(itemView);
            textRoom = itemView.findViewById(R.id.textRoom);
            textItem = itemView.findViewById(R.id.textItem);
            imageMoveIn = itemView.findViewById(R.id.imageMoveIn);
            imageMoveOut = itemView.findViewById(R.id.imageMoveOut);
            textMoveInMediaType = itemView.findViewById(R.id.textMoveInMediaType);
            textMoveOutMediaType = itemView.findViewById(R.id.textMoveOutMediaType);
            textStatus = itemView.findViewById(R.id.textStatus);
            layoutDetails = itemView.findViewById(R.id.layoutDetails);
            textMoveInDetails = itemView.findViewById(R.id.textMoveInDetails);
            textMoveOutDetails = itemView.findViewById(R.id.textMoveOutDetails);
        }

        void bind(ComparisonRow row) {
            textRoom.setText("Room: " + row.getRoom());
            textItem.setText("Item: " + row.getItem());
            bindImage(imageMoveIn, textMoveInMediaType, row.getMoveInPath(), row.getMoveInMediaType());
            bindImage(imageMoveOut, textMoveOutMediaType, row.getMoveOutPath(), row.getMoveOutMediaType());
            textStatus.setText("Status: " + row.getStatus());
            textStatus.setTextColor(statusColor(row.getStatus()));

            textMoveInDetails.setText(formatDetails("Move In", row.getMoveInCapturedAt(), row.getMoveInSha256(), row.getMoveInFileBytes()));
            textMoveOutDetails.setText(formatDetails("Move Out", row.getMoveOutCapturedAt(), row.getMoveOutSha256(), row.getMoveOutFileBytes()));
            layoutDetails.setVisibility(row.isExpanded() ? View.VISIBLE : View.GONE);

            textStatus.setOnClickListener(v -> {
                row.toggleExpanded();
                layoutDetails.setVisibility(row.isExpanded() ? View.VISIBLE : View.GONE);
            });

            textMoveInDetails.setOnClickListener(v -> copyHash(v.getContext(), row.getMoveInSha256()));
            textMoveOutDetails.setOnClickListener(v -> copyHash(v.getContext(), row.getMoveOutSha256()));
        }

        private void bindImage(ImageView imageView, TextView mediaTypeView, String path, String mediaType) {
            String normalizedType = mediaType == null ? "PHOTO" : mediaType.toUpperCase();
            mediaTypeView.setText(normalizedType);

            if (TextUtils.isEmpty(path)) {
                imageView.setImageResource(android.R.drawable.ic_menu_report_image);
                return;
            }

            File file = new File(path);
            if (!file.exists()) {
                imageView.setImageResource(android.R.drawable.ic_menu_report_image);
                return;
            }

            if ("VIDEO".equals(normalizedType)) {
                imageView.setImageResource(android.R.drawable.ic_media_play);
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
            if ("No Change".equals(status) || "NO_CHANGE".equals(status)) {
                return Color.parseColor("#1B5E20");
            }
            if ("Media Missing".equals(status) || "MISSING".equals(status)) {
                return Color.parseColor("#B71C1C");
            }
            return Color.parseColor("#E65100");
        }

        private String formatDetails(String label, String capturedAt, String hash, long fileBytes) {
            String shortHash = hash == null || hash.isEmpty() ? "-" : hash.substring(0, Math.min(10, hash.length()));
            return label
                    + " | Time: " + (capturedAt == null || capturedAt.isEmpty() ? "-" : capturedAt)
                    + " | Hash: " + shortHash
                    + " | Size: " + fileBytes + " bytes";
        }

        private void copyHash(Context context, String hash) {
            if (hash == null || hash.isEmpty()) {
                return;
            }
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) {
                return;
            }
            clipboard.setPrimaryClip(ClipData.newPlainText("sha256", hash));
        }
    }
}
