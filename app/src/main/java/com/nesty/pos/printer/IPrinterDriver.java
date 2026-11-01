package com.nesty.pos.printer;

import com.nesty.pos.api.models.OrderModels;

public interface IPrinterDriver {

    interface PrintCallback {
        void onSuccess();
        void onFailure(String reason);
    }

    boolean isConnected();
    void connect();
    void disconnect();
    void printReceipt(OrderModels.OrderRecord order, String terminalId, PrintCallback callback);
    void feedPaper(int lines);
    void testPrint(PrintCallback callback);
}
