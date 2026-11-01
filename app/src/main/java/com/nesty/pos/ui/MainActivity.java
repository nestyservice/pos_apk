package com.nesty.pos.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.nesty.pos.NestyPosApplication;
import com.nesty.pos.R;
import com.nesty.pos.api.ApiClient;
import com.nesty.pos.api.NestyApiService;
import com.nesty.pos.api.models.OrderModels;
import com.nesty.pos.api.models.PosStatusResponse;
import com.nesty.pos.data.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private TextView tvOperatorName;
    private TextView tvCodeDisplay;
    private TextView tvStatusMessage;
    private TextView tvNetworkStatus;
    private Button btnVerify;
    private ImageButton btnSettings;

    private final StringBuilder inputCode = new StringBuilder();
    private SessionManager sessionManager;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = NestyPosApplication.getInstance().getSessionManager();
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        initViews();
        setupKeypad();
        checkServerHealth();
    }

    private void initViews() {
        tvOperatorName = findViewById(R.id.tvOperatorName);
        tvCodeDisplay = findViewById(R.id.tvCodeDisplay);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);
        tvNetworkStatus = findViewById(R.id.tvNetworkStatus);
        btnVerify = findViewById(R.id.btnVerify);
        btnSettings = findViewById(R.id.btnSettings);

        tvOperatorName.setText("Operator: " + sessionManager.getOperatorName() + " (" + sessionManager.getTerminalId() + ")");

        btnVerify.setOnClickListener(v -> {
            String code = inputCode.toString().trim();
            if (code.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter a Bill Code or Token first", Toast.LENGTH_SHORT).show();
                return;
            }
            verifyAndProcessCode(code);
        });

        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });
    }

    private void setupKeypad() {
        int[] digitIds = {
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        };

        View.OnClickListener digitListener = v -> {
            if (vibrator != null) vibrator.vibrate(20);
            Button b = (Button) v;
            if (inputCode.length() < 10) {
                inputCode.append(b.getText());
                updateDisplay();
            }
        };

        for (int id : digitIds) {
            Button btn = findViewById(id);
            if (btn != null) btn.setOnClickListener(digitListener);
        }

        Button btnClear = findViewById(R.id.btnClear);
        if (btnClear != null) {
            btnClear.setOnClickListener(v -> {
                if (vibrator != null) vibrator.vibrate(20);
                if (inputCode.length() > 0) {
                    inputCode.setLength(0);
                    updateDisplay();
                }
            });
            btnClear.setOnLongClickListener(v -> {
                inputCode.setLength(0);
                updateDisplay();
                return true;
            });
        }

        Button btnScan = findViewById(R.id.btnScan);
        if (btnScan != null) {
            btnScan.setOnClickListener(v -> startQrScanner());
        }
    }

    private void updateDisplay() {
        if (inputCode.length() == 0) {
            tvCodeDisplay.setText("-----");
            tvStatusMessage.setText("Type 5-digit Bill Code, Token, or Scan QR");
            tvStatusMessage.setTextColor(getResources().getColor(R.color.nesty_text_secondary));
        } else {
            tvCodeDisplay.setText(inputCode.toString());
            tvStatusMessage.setText("Tap VERIFY & PRINT to look up bill");
            tvStatusMessage.setTextColor(getResources().getColor(R.color.nesty_burgundy));
        }
    }

    private void startQrScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan Student QR Pass on Nesty Website");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String scanned = result.getContents().trim();
                inputCode.setLength(0);
                inputCode.append(scanned);
                updateDisplay();
                verifyAndProcessCode(scanned);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void verifyAndProcessCode(String code) {
        tvStatusMessage.setText("Verifying code " + code + " on server...");
        btnVerify.setEnabled(false);

        NestyApiService api = ApiClient.getService(this);
        api.verifyOrder(new OrderModels.VerifyRequest(code)).enqueue(new Callback<OrderModels.VerifyResponse>() {
            @Override
            public void onResponse(Call<OrderModels.VerifyResponse> call, Response<OrderModels.VerifyResponse> response) {
                btnVerify.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    OrderModels.VerifyResponse body = response.body();
                    if (body.isSuccess() && body.getOrder() != null) {
                        tvStatusMessage.setText("Order #" + body.getOrder().getId() + " verified!");
                        tvStatusMessage.setTextColor(getResources().getColor(R.color.nesty_success));

                        // Open Order Details Print Dialog
                        OrderDetailsDialog dialog = new OrderDetailsDialog(
                            MainActivity.this,
                            body.getOrder(),
                            () -> {
                                // On print complete
                                inputCode.setLength(0);
                                updateDisplay();
                                Toast.makeText(MainActivity.this, "Bill printed & code released for reuse!", Toast.LENGTH_SHORT).show();
                            }
                        );
                        dialog.show();
                    } else {
                        tvStatusMessage.setText(body.getMessage() != null ? body.getMessage() : "Verification failed");
                        tvStatusMessage.setTextColor(getResources().getColor(R.color.nesty_error));
                    }
                } else if (response.code() == 409) {
                    // Order already printed / collected
                    tvStatusMessage.setText("ALREADY PRINTED: Bill was collected earlier.");
                    tvStatusMessage.setTextColor(getResources().getColor(R.color.nesty_error));
                    Toast.makeText(MainActivity.this, "This bill code has already been printed!", Toast.LENGTH_LONG).show();
                } else {
                    tvStatusMessage.setText("Order not found for code: " + code);
                    tvStatusMessage.setTextColor(getResources().getColor(R.color.nesty_error));
                    Toast.makeText(MainActivity.this, "No active order found with code: " + code, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<OrderModels.VerifyResponse> call, Throwable t) {
                btnVerify.setEnabled(true);
                tvStatusMessage.setText("Network error: " + t.getMessage());
                tvStatusMessage.setTextColor(getResources().getColor(R.color.nesty_error));
                Toast.makeText(MainActivity.this, "Server unreachable: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void checkServerHealth() {
        NestyApiService api = ApiClient.getService(this);
        api.getStatus().enqueue(new Callback<PosStatusResponse>() {
            @Override
            public void onResponse(Call<PosStatusResponse> call, Response<PosStatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvNetworkStatus.setText("ONLINE (" + response.body().getActiveOrdersCount() + " Active)");
                } else {
                    tvNetworkStatus.setText("OFFLINE");
                }
            }

            @Override
            public void onFailure(Call<PosStatusResponse> call, Throwable t) {
                tvNetworkStatus.setText("NO CONN");
            }
        });
    }
}
