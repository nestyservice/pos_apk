package com.nesty.pos.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.nesty.pos.NestyPosApplication;
import com.nesty.pos.R;
import com.nesty.pos.api.ApiClient;
import com.nesty.pos.api.NestyApiService;
import com.nesty.pos.api.models.OrderModels;
import com.nesty.pos.api.models.PosStatusResponse;
import com.nesty.pos.data.SessionManager;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private TextView tvOperatorName;
    private TextView tvNetworkStatus;
    private TextView tvStatusMessage;
    private TextInputEditText etBillCode;
    private Button btnVerify;
    private Button btnClear;
    private ImageButton btnSettings;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = NestyPosApplication.getInstance().getSessionManager();

        requestBluetoothPermissionsIfNeeded();
        initViews();
        checkServerHealth();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkServerHealth();
    }

    private void requestBluetoothPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                    android.Manifest.permission.BLUETOOTH_SCAN
                }, 101);
            }
        }
    }

    private void initViews() {
        tvOperatorName = findViewById(R.id.tvOperatorName);
        tvNetworkStatus = findViewById(R.id.tvNetworkStatus);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);
        etBillCode = findViewById(R.id.etBillCode);
        btnVerify = findViewById(R.id.btnVerify);
        btnClear = findViewById(R.id.btnClear);
        btnSettings = findViewById(R.id.btnSettings);

        tvOperatorName.setText("Operator: " + sessionManager.getOperatorName() + " (" + sessionManager.getTerminalId() + ")");

        // Auto-uppercase and 3-character maximum length filter
        etBillCode.setFilters(new InputFilter[]{
            new InputFilter.AllCaps(),
            new InputFilter.LengthFilter(3)
        });

        etBillCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s != null && s.length() == 3) {
                    showValidationMessage("Code entered: " + s.toString().toUpperCase(), false);
                } else {
                    showValidationMessage("Enter 3-character bill code (e.g. A7B)", false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClear.setOnClickListener(v -> {
            etBillCode.setText("");
            showValidationMessage("Ready for order verification", false);
            etBillCode.requestFocus();
        });

        btnVerify.setOnClickListener(v -> {
            String code = etBillCode.getText() != null ? etBillCode.getText().toString().trim().toUpperCase() : "";
            validateAndVerifyOrder(code);
        });

        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });
    }

    private void validateAndVerifyOrder(String code) {
        if (code.isEmpty()) {
            showValidationMessage("Please enter a bill code.", true);
            Toast.makeText(this, "Please enter a bill code.", Toast.LENGTH_SHORT).show();
            etBillCode.requestFocus();
            return;
        }

        if (code.length() != 3) {
            showValidationMessage("Bill code must contain exactly 3 characters.", true);
            Toast.makeText(this, "Bill code must contain exactly 3 characters.", Toast.LENGTH_SHORT).show();
            etBillCode.requestFocus();
            return;
        }

        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : code.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
        }

        if (!hasLetter || !hasDigit) {
            showValidationMessage("Bill code must contain both a letter and a number.", true);
            Toast.makeText(this, "Bill code must contain both a letter and a number.", Toast.LENGTH_SHORT).show();
            etBillCode.requestFocus();
            return;
        }

        setVerifying(true);
        showValidationMessage("Verifying code " + code + " on server...", false);

        NestyApiService api = ApiClient.getService(this);
        api.verifyOrder(new OrderModels.VerifyRequest(code)).enqueue(new Callback<OrderModels.VerifyResponse>() {
            @Override
            public void onResponse(Call<OrderModels.VerifyResponse> call, Response<OrderModels.VerifyResponse> response) {
                setVerifying(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    OrderModels.OrderRecord order = response.body().getOrder();
                    if (order != null) {
                        showValidationMessage("Order verified! Tap Print Bill to confirm.", false);
                        showOrderDetailsDialog(order);
                    } else {
                        showValidationMessage("Invalid or unavailable bill code.", true);
                    }
                } else if (response.code() == 409) {
                    showValidationMessage("Order has already been processed.", true);
                    Toast.makeText(MainActivity.this, "Order has already been processed.", Toast.LENGTH_LONG).show();
                } else if (response.code() == 404) {
                    showValidationMessage("Invalid or unavailable bill code.", true);
                    Toast.makeText(MainActivity.this, "Invalid or unavailable bill code.", Toast.LENGTH_LONG).show();
                } else {
                    String errorMsg = "Invalid or unavailable bill code.";
                    try {
                        if (response.errorBody() != null) {
                            String errStr = response.errorBody().string();
                            JSONObject json = new JSONObject(errStr);
                            if (json.has("message")) {
                                errorMsg = json.getString("message");
                            }
                        }
                    } catch (Exception ignored) {}
                    showValidationMessage(errorMsg, true);
                    Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<OrderModels.VerifyResponse> call, Throwable t) {
                setVerifying(false);
                showValidationMessage("Connection error: " + t.getMessage(), true);
                Toast.makeText(MainActivity.this, "Failed to connect to Nesty backend", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showOrderDetailsDialog(OrderModels.OrderRecord order) {
        OrderDetailsDialog dialog = new OrderDetailsDialog(this, order, () -> {
            etBillCode.setText("");
            showValidationMessage("Bill printed successfully! Order completed.", false);
        });
        dialog.show();
    }

    private void showValidationMessage(String message, boolean isError) {
        tvStatusMessage.setText(message);
        if (isError) {
            tvStatusMessage.setTextColor(Color.parseColor("#DC2626"));
        } else {
            tvStatusMessage.setTextColor(Color.parseColor("#059669"));
        }
    }

    private void setVerifying(boolean verifying) {
        btnVerify.setEnabled(!verifying);
        btnVerify.setText(verifying ? "VERIFYING..." : "VERIFY ORDER");
        btnClear.setEnabled(!verifying);
    }

    private void checkServerHealth() {
        NestyApiService api = ApiClient.getService(this);
        api.getStatus().enqueue(new Callback<PosStatusResponse>() {
            @Override
            public void onResponse(Call<PosStatusResponse> call, Response<PosStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvNetworkStatus.setText("ONLINE");
                    tvNetworkStatus.setTextColor(Color.WHITE);
                } else {
                    tvNetworkStatus.setText("OFFLINE");
                }
            }

            @Override
            public void onFailure(Call<PosStatusResponse> call, Throwable t) {
                tvNetworkStatus.setText("OFFLINE");
            }
        });
    }
}
