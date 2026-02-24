package com.mayneline.moveinmoveout.report;

public class ReportComparisonItem {
    public long roomId;
    public long itemId;
    public String roomName;
    public String itemName;
    public String status;

    public String moveInPath;
    public String moveInCapturedAt;
    public String moveInSha256;
    public long moveInFileBytes;

    public String moveOutPath;
    public String moveOutCapturedAt;
    public String moveOutSha256;
    public long moveOutFileBytes;
}
