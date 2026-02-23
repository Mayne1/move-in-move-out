package com.mayneline.moveinmoveout.model;

import java.util.List;

public class ComparisonResult {
    private final List<ComparisonResultItem> items;
    private final int missingCount;
    private final int noChangeCount;
    private final int needsReviewCount;

    public ComparisonResult(List<ComparisonResultItem> items, int missingCount, int noChangeCount, int needsReviewCount) {
        this.items = items;
        this.missingCount = missingCount;
        this.noChangeCount = noChangeCount;
        this.needsReviewCount = needsReviewCount;
    }

    public List<ComparisonResultItem> getItems() {
        return items;
    }

    public int getMissingCount() {
        return missingCount;
    }

    public int getNoChangeCount() {
        return noChangeCount;
    }

    public int getNeedsReviewCount() {
        return needsReviewCount;
    }
}
