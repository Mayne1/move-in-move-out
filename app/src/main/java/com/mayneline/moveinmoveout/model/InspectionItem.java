package com.mayneline.moveinmoveout.model;

public class InspectionItem {
    private final String label;
    private boolean checked;

    public InspectionItem(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
