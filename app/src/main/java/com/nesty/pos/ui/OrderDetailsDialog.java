package com.nesty.pos.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.ViewGroup;
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
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Button btnDialogPrint;
    private Button btnDialogCancel;

    public OrderDetailsDialog(@NonNull Context context, OrderModels.OrderRecord order, OnPrintCompletedListener listener) {
        super(context, R.style.Theme_NestyPOS_Dialog);
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
        btnDialogCancel = findViewById(R.id.btnDialogCancel);
        btnDialogPrint = findViewById(R.id.btnDialogPrint);

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
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            for (OrderModels.OrderItem it : order.getItems()) {
                itemsText.append("• ")
                    .append(it.getQuantity())
                    .append("x ")
                    .append(it.getName())
                    .append(String.format(Locale.getDefault(), " (Rs. %.2f)\n", it.getPrice() * it.getQuantity()));
            }
        } else {
            itemsText.append("• Food Order (Total Paid)");
        }
        tvDialogItems.setText(itemsText.toString().trim());
        tvDialogTotalAmount.setText(String.format(Locale.getDefault(), "Rs. %.2f", order.getFinalAmount()));

        btnDialogCancel.setOnClickListener(v -> dismiss());

        btnDialogPrint.setOnClickListener(v -> {
            btnDialogPrint.setEnabled(false);
            btnDialogPrint.setText("PRINTING...");
            executePrintFlow();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getWindow() != null) {
            DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
            int width = Math.min((int) (metrics.widthPixels * 0.92), 480);
            getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            getWindow().setGravity(Gravity.CENTER);
        }
    }

    private void executePrintFlow() {
        NestyApiService api = ApiClient.getService(getContext());
        String operatorId = sessionManager.getOperatorName();
        String ptxnId = "ptxn_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);

        // Safety timeout (8 seconds): never leave button permanently stuck on PRINTING...
        mainHandler.postDelayed(() -> {
            if (isShowing() && btnDialogPrint != null && !btnDialogPrint.isEnabled()) {
                btnDialogPrint.setEnabled(true);
                btnDialogPrint.setText("RETRY PRINT");
            }
        }, 8000);

        // 1. Acquire Atomic Claim Lock (45 seconds)
        api.claimOrder(new OrderModels.ClaimRequest(order.getId(), operatorId, ptxnId)).enqueue(new Callback<OrderModels.GenericResponse>() {
            @Override
            public void onResponse(Call<OrderModels.GenericResponse> call, Response<OrderModels.GenericResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // 2. Physical Thermal Print
                    printerDriver.printReceipt(order, sessionManager.getTerminalId(), new IPrinterDriver.PrintCallback() {
                        @Override
                        public void onSuccess() {
                            // 3. Confirm Print on Backend -> Release Bill Code immediately & mark EXPIRED
                            api.confirmPrint(new OrderModels.ConfirmPrintRequest(order.getId(), operatorId, ptxnId)).enqueue(new Callback<OrderModels.GenericResponse>() {
                                @Override
                                public void onResponse(Call<OrderModels.GenericResponse> call, Response<OrderModels.GenericResponse> confirmResp) {
                                    mainHandler.post(() -> {
                                        Toast.makeText(getContext(), "Receipt printed successfully!", Toast.LENGTH_SHORT).show();
                                        dismiss();
                                        if (listener != null) listener.onCompleted();
                                    });
                                }

                                @Override
                                public void onFailure(Call<OrderModels.GenericResponse> call, Throwable t) {
                                    mainHandler.post(() -> {
                                        dismiss();
                                        if (listener != null) listener.onCompleted();
                                    });
                                }
                            });
                        }

                        @Override
                        public void onFailure(String reason) {
                            // Report print failure -> releases lock so it can be safely retried
                            api.reportFailure(new OrderModels.PrintFailureRequest(order.getId(), reason, operatorId)).enqueue(new Callback<OrderModels.GenericResponse>() {
                                @Override
                                public void onResponse(Call<OrderModels.GenericResponse> call, Response<OrderModels.GenericResponse> response) {}
                                @Override
                                public void onFailure(Call<OrderModels.GenericResponse> call, Throwable t) {}
                            });

                            mainHandler.post(() -> {
                                if (btnDialogPrint != null) {
                                    btnDialogPrint.setEnabled(true);
                                    btnDialogPrint.setText("RETRY PRINT");
                                }
                                Toast.makeText(getContext(), "Printer notice: " + reason + ". Tap RETRY PRINT or check paper.", Toast.LENGTH_LONG).show();
                            });
                        }
                    });
                } else {
                    String msg = "Order has already been processed or locked by another counter.";
                    if (response.body() != null && response.body().getMessage() != null) {
                        msg = response.body().getMessage();
                    }
                    final String alertMsg = msg;
                    mainHandler.post(() -> {
                        Toast.makeText(getContext(), alertMsg, Toast.LENGTH_LONG).show();
                        dismiss();
                    });
                }
            }

            @Override
            public void onFailure(Call<OrderModels.GenericResponse> call, Throwable t) {
                mainHandler.post(() -> {
                    if (btnDialogPrint != null) {
                        btnDialogPrint.setEnabled(true);
                        btnDialogPrint.setText("PRINT BILL");
                    }
                    Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}
