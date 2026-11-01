package com.nesty.pos.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.nesty.pos.NestyPosApplication;
import com.nesty.pos.R;
import com.nesty.pos.api.ApiClient;
import com.nesty.pos.api.NestyApiService;
import com.nesty.pos.api.models.OrderModels;
import com.nesty.pos.data.SessionManager;
import com.nesty.pos.printer.IPrinterDriver;
import com.nesty.pos.printer.Model1008PrinterDriver;
import java.util.Locale;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailsDialog extends Dialog {
    public interface OnPrintCompletedListener {
        void onCompleted();
    }

    private final OrderModels.OrderRecord order;
    private final OnPrintCompletedListener listener;
    private final SessionManager sessionManager;
    private final Model1008PrinterDriver printerDriver;

    public OrderDetailsDialog(@NonNull Context context, OrderModels.OrderRecord order, OnPrintCompletedListener listener) {
        super(context);
        this.order = order;
        this.listener = listener;
        this.sessionManager = NestyPosApplication.getInstance().getSessionManager();
        this.printerDriver = NestyPosApplication.getInstance().getPrinterDriver();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_order_details);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvDialogOrderId = findViewById(R.id.tvDialogOrderId);
        TextView tvDialogBillCode = findViewById(R.id.tvDialogBillCode);
        TextView tvDialogStudentName = findViewById(R.id.tvDialogStudentName);
        TextView tvDialogTxnId = findViewById(R.id.tvDialogTxnId);
        TextView tvDialogItems = findViewById(R.id.tvDialogItems);
        TextView tvDialogTotalAmount = findViewById(R.id.tvDialogTotalAmount);
        Button btnDialogCancel = findViewById(R.id.btnDialogCancel);
        Button btnDialogPrint = findViewById(R.id.btnDialogPrint);

        tvDialogOrderId.setText("Order #" + order.getId());

        String codesText = "";
        if (order.getBillCode() != null && !order.getBillCode().isEmpty()) {
            codesText += "Bill Code: " + order.getBillCode();
        }
        if (order.getPickupToken() != null && !order.getPickupToken().isEmpty()) {
            if (!codesText.isEmpty()) codesText += " • ";
            codesText += "Token: " + order.getPickupToken();
        }
        tvDialogBillCode.setText(codesText);

        tvDialogStudentName.setText("Student: " + (order.getStudentName() != null ? order.getStudentName() : "Student"));
        tvDialogTxnId.setText("Txn: " + (order.getTransactionId() != null ? order.getTransactionId() : "N/A"));

        StringBuilder itemsText = new StringBuilder();
        if (order.getItems() != null) {
            for (OrderModels.OrderItem it : order.getItems()) {
                itemsText.append("• ")
                    .append(it.getQuantity())
                    .append("x ")
                    .append(it.getName())
                    .append(String.format(Locale.getDefault(), " (₹%.2f)\n", it.getPrice() * it.getQuantity()));
            }
        }
        tvDialogItems.setText(itemsText.toString().trim());
        tvDialogTotalAmount.setText(String.format(Locale.getDefault(), "₹%.2f", order.getFinalAmount()));

        btnDialogCancel.setOnClickListener(v -> dismiss());

        btnDialogPrint.setOnClickListener(v -> {
            btnDialogPrint.setEnabled(false);
            btnDialogPrint.setText("PRINTING...");
            executePrintFlow();
        });
    }

    private void executePrintFlow() {
        NestyApiService api = ApiClient.getService(getContext());
        String operatorId = sessionManager.getOperatorName();
        String ptxnId = "ptxn_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);

        // 1. Acquire Atomic Claim Lock
        api.claimOrder(new OrderModels.ClaimRequest(order.getId(), operatorId, ptxnId)).enqueue(new Callback<OrderModels.GenericResponse>() {
            @Override
            public void onResponse(Call<OrderModels.GenericResponse> call, Response<OrderModels.GenericResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // 2. Physical Thermal Print
                    printerDriver.printReceipt(order, sessionManager.getTerminalId(), new IPrinterDriver.PrintCallback() {
                        @Override
                        public void onSuccess() {
                            // 3. Confirm Print on Backend -> Release Bill Code immediately
                            api.confirmPrint(new OrderModels.ConfirmPrintRequest(order.getId(), operatorId, ptxnId)).enqueue(new Callback<OrderModels.GenericResponse>() {
                                @Override
                                public void onResponse(Call<OrderModels.GenericResponse> call, Response<OrderModels.GenericResponse> confirmResp) {
                                    dismiss();
                                    if (listener != null) listener.onCompleted();
                                }

                                @Override
                                public void onFailure(Call<OrderModels.GenericResponse> call, Throwable t) {
                                    dismiss();
                                    if (listener != null) listener.onCompleted();
                                }
                            });
                        }

                        @Override
                        public void onFailure(String reason) {
                            // Report print failure -> releases lock so it can be retried
                            api.reportFailure(new OrderModels.PrintFailureRequest(order.getId(), reason, operatorId)).enqueue(new Callback<OrderModels.GenericResponse>() {
                                @Override
                                public void onResponse(Call<OrderModels.GenericResponse> call, Response<OrderModels.GenericResponse> response) {}
                                @Override
                                public void onFailure(Call<OrderModels.GenericResponse> call, Throwable t) {}
                            });

                            Toast.makeText(getContext(), "Printer error: " + reason + ". Order remains active.", Toast.LENGTH_LONG).show();
                            dismiss();
                        }
                    });
                } else {
                    String msg = "Could not claim order: already printed or locked by another terminal";
                    if (response.body() != null && response.body().getMessage() != null) {
                        msg = response.body().getMessage();
                    }
                    Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
                    dismiss();
                }
            }

            @Override
            public void onFailure(Call<OrderModels.GenericResponse> call, Throwable t) {
                Toast.makeText(getContext(), "Network error claiming print lock: " + t.getMessage(), Toast.LENGTH_LONG).show();
                dismiss();
            }
        });
    }
}
