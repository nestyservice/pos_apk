package com.nesty.pos.api.models;

import com.google.gson.annotations.SerializedName;

public class AuthModels {

    public static class LoginRequest {
        @SerializedName("identifier")
        private String identifier;

        @SerializedName("password")
        private String password;

        public LoginRequest(String identifier, String password) {
            this.identifier = identifier;
            this.password = password;
        }

        public String getIdentifier() { return identifier; }
        public String getPassword() { return password; }
    }

    public static class OperatorProfile {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("role")
        private String role;

        @SerializedName("email")
        private String email;

        public String getId() { return id; }
        public String getName() { return name; }
        public String getRole() { return role; }
        public String getEmail() { return email; }
    }

    public static class LoginResponse {
        @SerializedName("success")
        private boolean success;

        @SerializedName("token")
        private String token;

        @SerializedName("operator")
        private OperatorProfile operator;

        @SerializedName("message")
        private String message;

        public boolean isSuccess() { return success; }
        public String getToken() { return token; }
        public OperatorProfile getOperator() { return operator; }
        public String getMessage() { return message; }
    }
}
