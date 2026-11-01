package com.nesty.pos.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.nesty.pos.NestyPosApplication;
import com.nesty.pos.R;
import com.nesty.pos.api.ApiClient;
import com.nesty.pos.api.NestyApiService;
import com.nesty.pos.api.models.AuthModels;
import com.nesty.pos.data.SessionManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText etIdentifier;
    private TextInputEditText etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private TextView tvTerminalHint;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = NestyPosApplication.getInstance().getSessionManager();

        etIdentifier = findViewById(R.id.etIdentifier);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        tvTerminalHint = findViewById(R.id.tvTerminalHint);

        tvTerminalHint.setText("Target Server: " + sessionManager.getBaseUrl());

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String identifier = etIdentifier.getText() != null ? etIdentifier.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (identifier.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter your Operator ID and Password", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        NestyApiService api = ApiClient.getService(this);
        api.login(new AuthModels.LoginRequest(identifier, password)).enqueue(new Callback<AuthModels.LoginResponse>() {
            @Override
            public void onResponse(Call<AuthModels.LoginResponse> call, Response<AuthModels.LoginResponse> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AuthModels.LoginResponse body = response.body();
                    String opId = body.getOperator() != null ? body.getOperator().getId() : "POS-1008";
                    String opName = body.getOperator() != null ? body.getOperator().getName() : "Counter Operator";
                    String opRole = body.getOperator() != null ? body.getOperator().getRole() : "pos_operator";

                    sessionManager.saveSession(body.getToken(), opId, opName, opRole);
                    Toast.makeText(LoginActivity.this, "Welcome " + opName, Toast.LENGTH_SHORT).show();

                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    String msg = "Login failed: Incorrect Operator ID or Password";
                    if (response.body() != null && response.body().getMessage() != null) {
                        msg = response.body().getMessage();
                    }
                    Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<AuthModels.LoginResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }
}
