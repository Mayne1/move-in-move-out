package com.mayneline.moveinmoveout.engine;

import com.mayneline.moveinmoveout.data.AppDatabase;
import com.mayneline.moveinmoveout.data.ChecklistStructureEntity;
import com.mayneline.moveinmoveout.data.InspectionRunEntity;
import com.mayneline.moveinmoveout.data.MediaDao;
import com.mayneline.moveinmoveout.data.RoomItemMedia;
import com.mayneline.moveinmoveout.model.ComparisonResult;
import com.mayneline.moveinmoveout.model.ComparisonResultItem;
import com.mayneline.moveinmoveout.model.ComparisonStatus;

import java.util.ArrayList;
import java.util.List;

public class ComparisonService {
    private final MediaDao dao;

    public ComparisonService(AppDatabase database) {
        this.dao = database.mediaDao();
    }

    public ComparisonResult compareRuns(InspectionRunEntity moveInRun, InspectionRunEntity moveOutRun) {
        if (moveInRun == null || moveOutRun == null) {
            return new ComparisonResult(new ArrayList<>(), 0, 0, 0);
        }
        if (!moveInRun.propertyId.equals(moveOutRun.propertyId)) {
            throw new IllegalStateException("Runs must belong to the same property");
        }

        List<ChecklistStructureEntity> structure = dao.getChecklistStructureForProperty(moveInRun.propertyId);
        List<ComparisonResultItem> results = new ArrayList<>();
        int missing = 0;
        int noChange = 0;
        int needsReview = 0;

        for (ChecklistStructureEntity row : structure) {
            RoomItemMedia moveInMedia = dao.getLatestMediaForRunItem(moveInRun.runId, row.roomId, row.itemId);
            RoomItemMedia moveOutMedia = dao.getLatestMediaForRunItem(moveOutRun.runId, row.roomId, row.itemId);

            ComparisonStatus status;
            if (moveInMedia == null || moveOutMedia == null) {
                status = ComparisonStatus.MISSING;
                missing++;
            } else if (moveInMedia.mediaSha256.equals(moveOutMedia.mediaSha256)) {
                status = ComparisonStatus.NO_CHANGE;
                noChange++;
            } else {
                status = ComparisonStatus.NEEDS_REVIEW;
                needsReview++;
            }

            results.add(new ComparisonResultItem(
                    row.roomId,
                    row.itemId,
                    moveInMedia == null ? "" : moveInMedia.mediaPath,
                    moveOutMedia == null ? "" : moveOutMedia.mediaPath,
                    status
            ));
        }

        return new ComparisonResult(results, missing, noChange, needsReview);
    }
}
