package com.mayneline.moveinmoveout.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mayneline.moveinmoveout.R;
import com.mayneline.moveinmoveout.model.InspectionItem;
import com.mayneline.moveinmoveout.model.RoomSection;

import java.util.ArrayList;
import java.util.List;

public class InspectionChecklistAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    public interface OnAddMediaClickListener {
        void onAddMediaClicked(String roomName, String itemName);
    }

    private static class ListEntry {
        private final int viewType;
        private final String roomName;
        private final InspectionItem item;

        private ListEntry(int viewType, String roomName, InspectionItem item) {
            this.viewType = viewType;
            this.roomName = roomName;
            this.item = item;
        }
    }

    private final List<ListEntry> entries;
    private final OnAddMediaClickListener listener;

    public InspectionChecklistAdapter(List<RoomSection> sections, OnAddMediaClickListener listener) {
        this.listener = listener;
        this.entries = flattenSections(sections);
    }

    @Override
    public int getItemViewType(int position) {
        return entries.get(position).viewType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER) {
            View view = inflater.inflate(R.layout.row_room_header, parent, false);
            return new RoomHeaderViewHolder(view);
        }

        View view = inflater.inflate(R.layout.row_inspection_item, parent, false);
        return new InspectionItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListEntry entry = entries.get(position);
        if (holder instanceof RoomHeaderViewHolder) {
            ((RoomHeaderViewHolder) holder).bind(entry.roomName);
        } else if (holder instanceof InspectionItemViewHolder && entry.item != null) {
            ((InspectionItemViewHolder) holder).bind(entry.roomName, entry.item, listener);
        }
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    private List<ListEntry> flattenSections(List<RoomSection> sections) {
        List<ListEntry> flattened = new ArrayList<>();
        for (RoomSection section : sections) {
            flattened.add(new ListEntry(VIEW_TYPE_HEADER, section.getRoomName(), null));
            for (InspectionItem item : section.getItems()) {
                flattened.add(new ListEntry(VIEW_TYPE_ITEM, section.getRoomName(), item));
            }
        }
        return flattened;
    }

    static class RoomHeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView textRoomName;

        RoomHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textRoomName = itemView.findViewById(R.id.textRoomHeader);
        }

        void bind(String roomName) {
            textRoomName.setText(roomName);
        }
    }

    static class InspectionItemViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox checkCompleted;
        private final TextView textItemName;
        private final Button buttonAddMedia;

        InspectionItemViewHolder(@NonNull View itemView) {
            super(itemView);
            checkCompleted = itemView.findViewById(R.id.checkItemCompleted);
            textItemName = itemView.findViewById(R.id.textItemName);
            buttonAddMedia = itemView.findViewById(R.id.buttonAddMedia);
        }

        void bind(String roomName, InspectionItem item, OnAddMediaClickListener listener) {
            textItemName.setText(item.getLabel());
            checkCompleted.setOnCheckedChangeListener(null);
            checkCompleted.setChecked(item.isChecked());
            checkCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> item.setChecked(isChecked));
            buttonAddMedia.setOnClickListener(v -> listener.onAddMediaClicked(roomName, item.getLabel()));
        }
    }
}
