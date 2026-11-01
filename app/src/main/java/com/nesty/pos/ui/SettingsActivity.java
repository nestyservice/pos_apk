package com.nesty.pos.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.nesty.pos.NestyPosApplication;
import com.nesty.pos.R;
import com.nesty.pos.data.SessionManager;
import com.nesty.pos.printer.IPrinterDriver;
import com.nesty.pos.printer.Model1008PrinterDriver;

public class SettingsActivity extends AppCompatActivity {
    private TextInputEditText etBaseUrl;
    private Button btnSaveBaseUrl;
    private Button btnTestPrint;
    private Button btnFeedPaper;
    private Button btnLogout;
    private ImageButton btnBack;

    private SessionManager sessionManager;
    private Model1008PrinterDriver printerDriver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sessionManager = NestyPosApplication.getInstance().getSessionManager();
        printerDriver = NestyPosApplication.getInstance().getPrinterDriver();

        etBaseUrl = findViewById(R.id.etBaseUrl);
        btnSaveBaseUrl = findViewById(R.id.btnSaveBaseUrl);
        btnTestPrint = findViewById(R.id.btnTestPrint);
        btnFeedPaper = findViewById(R.id.btnFeedPaper);
        btnLogout = findViewById(R.id.btnLogout);
        btnBack = findViewById(R.id.btnBack);

        etBaseUrl.setText(sessionManager.getBaseUrl());

        btnBack.setOnClickListener(v -> finish());

        btnSaveBaseUrl.setOnClickListener(v -> {
            String url = etBaseUrl.getText() != null ? etBaseUrl.getText().toString().trim() : "";
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                Toast.makeText(this, "Please enter a valid URL starting with https://", Toast.LENGTH_SHORT).show();
                return;
            }
            sessionManager.setBaseUrl(url);
            Toast.makeText(this, "Server URL updated!", Toast.LENGTH_SHORT).show();
        });

        btnFeedPaper.setOnClickListener(v -> {
            printerDriver.feedPaper(3);
            Toast.makeText(this, "Paper fed 3 lines", Toast.LENGTH_SHORT).show();
        });

        btnTestPrint.setOnClickListener(v -> {
            btnTestPrint.setEnabled(false);
            printerDriver.testPrint(new IPrinterDriver.PrintCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        btnTestPrint.setEnabled(true);
                        Toast.makeText(SettingsActivity.this, "Test receipt printed successfully!", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onFailure(String reason) {
                    runOnUiThread(() -> {
                        btnTestPrint.setEnabled(true);
                        Toast.makeText(SettingsActivity.this, "Printer error: " + reason, Toast.LENGTH_LONG).show();
                    });
                }
            });
        });

        btnLogout.setOnClickListener(v -> {
            sessionManager.clearSession();
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
