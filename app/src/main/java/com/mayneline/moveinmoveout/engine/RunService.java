package com.mayneline.moveinmoveout.engine;

import com.mayneline.moveinmoveout.data.AppDatabase;
import com.mayneline.moveinmoveout.data.InspectionRunEntity;
import com.mayneline.moveinmoveout.data.MediaDao;
import com.mayneline.moveinmoveout.data.PropertyEntity;
import com.mayneline.moveinmoveout.data.RoomItemMedia;
import com.mayneline.moveinmoveout.model.InspectionMode;

import java.util.List;
import java.util.UUID;

public class RunService {
    private final MediaDao dao;

    public RunService(AppDatabase database) {
        this.dao = database.mediaDao();
    }

    public InspectionRunEntity getOrCreateActiveRun(String propertyId, InspectionMode mode) {
        InspectionRunEntity run = dao.getActiveRun(propertyId, mode.name());
        if (run != null) {
            return run;
        }
        return startNewRun(propertyId, mode);
    }

    public InspectionRunEntity startNewRun(String propertyId, InspectionMode mode) {
        List<InspectionRunEntity> runs = dao.getRunsForPropertyMode(propertyId, mode.name());
        String nextLabel = nextRunLabel(runs);
        InspectionRunEntity run = new InspectionRunEntity(
                UUID.randomUUID().toString(),
                propertyId,
                mode.name(),
                nextLabel,
                false,
                0L,
                null
        );
        dao.insertRun(run);
        return run;
    }

    public void finalizeRun(String runId, String initials) {
        InspectionRunEntity run = dao.getRunById(runId);
        if (run == null) {
            throw new IllegalStateException("Run not found");
        }
        if (run.finalized) {
            throw new IllegalStateException("Run is already finalized");
        }
        if (initials == null || initials.trim().isEmpty()) {
            throw new IllegalStateException("Initials are required to finalize");
        }

        int requiredItems = dao.countChecklistStructureForProperty(run.propertyId);
        int completedItems = dao.countCompletedChecklistItemsForRun(runId);
        if (requiredItems <= 0 || completedItems < requiredItems) {
            throw new IllegalStateException("Checklist must be 100% complete before finalizing");
        }

        List<RoomItemMedia> mediaRows = dao.getMediaForRun(runId);
        StringBuilder payload = new StringBuilder();
        payload.append(run.runId).append('|')
                .append(run.propertyId).append('|')
                .append(run.mode).append('|')
                .append(run.runLabel).append('|')
                .append(initials.trim().toUpperCase());

        for (RoomItemMedia media : mediaRows) {
            String mediaHash = media.mediaSha256;
            if (mediaHash == null || mediaHash.isEmpty()) {
                mediaHash = HashUtils.sha256File(media.mediaPath);
            }
            payload.append('|').append(media.roomId)
                    .append('|').append(media.itemId)
                    .append('|').append(mediaHash)
                    .append('|').append(media.timestamp)
                    .append('|').append(media.note == null ? "" : media.note);
        }

        run.sha256Hash = HashUtils.sha256String(payload.toString());
        run.finalized = true;
        run.finalizedAt = System.currentTimeMillis();
        dao.updateRun(run);
    }

    public int completionPercent(String runId) {
        InspectionRunEntity run = dao.getRunById(runId);
        if (run == null) {
            return 0;
        }
        int total = dao.countChecklistStructureForProperty(run.propertyId);
        if (total == 0) {
            return 0;
        }
        int completed = dao.countCompletedChecklistItemsForRun(runId);
        return Math.min(100, (int) ((completed * 100f) / total));
    }

    public InspectionRunEntity requireEditableRun(String runId) {
        InspectionRunEntity run = dao.getRunById(runId);
        if (run == null) {
            throw new IllegalStateException("Run not found");
        }
        if (run.finalized) {
            throw new IllegalStateException("Run is finalized and locked");
        }
        return run;
    }

    public PropertyEntity getProperty(String propertyId) {
        return dao.getPropertyById(propertyId);
    }

    private String nextRunLabel(List<InspectionRunEntity> runs) {
        int bestNumber = 0;
        char bestLetter = 'A' - 1;

        for (InspectionRunEntity run : runs) {
            String label = run.runLabel == null ? "" : run.runLabel.trim().toUpperCase();
            int idx = 0;
            while (idx < label.length() && Character.isDigit(label.charAt(idx))) {
                idx++;
            }
            if (idx == 0 || idx >= label.length()) {
                continue;
            }
            int number;
            try {
                number = Integer.parseInt(label.substring(0, idx));
            } catch (NumberFormatException exception) {
                continue;
            }
            char letter = label.charAt(idx);
            if (!Character.isLetter(letter)) {
                continue;
            }
            if (number > bestNumber || (number == bestNumber && letter > bestLetter)) {
                bestNumber = number;
                bestLetter = letter;
            }
        }

        if (bestNumber == 0) {
            return "1A";
        }

        if (bestLetter < 'Z') {
            return bestNumber + String.valueOf((char) (bestLetter + 1));
        }

        return (bestNumber + 1) + "A";
    }
}
