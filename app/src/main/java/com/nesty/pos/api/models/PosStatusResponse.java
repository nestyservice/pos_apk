package com.nesty.pos.api.models;

import com.google.gson.annotations.SerializedName;

public class PosStatusResponse {
    @SerializedName("status")
    private String status;

    @SerializedName("service")
    private String service;

    @SerializedName("terminalModel")
    private String terminalModel;

    @SerializedName("activeOrdersCount")
    private int activeOrdersCount;

    @SerializedName("timestamp")
    private String timestamp;

    public String getStatus() { return status; }
    public String getService() { return service; }
    public String getTerminalModel() { return terminalModel; }
    public int getActiveOrdersCount() { return activeOrdersCount; }
    public String getTimestamp() { return timestamp; }
}
