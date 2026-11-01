package com.nesty.pos.api.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class OrderModels {

    public static class OrderItem {
        @SerializedName("id")
        private String id;

        @SerializedName("name")
        private String name;

        @SerializedName("price")
        private double price;

        @SerializedName("quantity")
        private int quantity;

        @SerializedName("isVeg")
        private boolean isVeg;

        public String getId() { return id; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public int getQuantity() { return quantity; }
        public boolean isVeg() { return isVeg; }
    }

    public static class OrderRecord {
        @SerializedName("id")
        private String id;

        @SerializedName("studentEmail")
        private String studentEmail;

        @SerializedName("studentName")
        private String studentName;

        @SerializedName("studentPhone")
        private String studentPhone;

        @SerializedName("block")
        private String block;

        @SerializedName("items")
        private List<OrderItem> items;

        @SerializedName("totalAmount")
        private double totalAmount;

        @SerializedName("discount")
        private double discount;

        @SerializedName("finalAmount")
        private double finalAmount;

        @SerializedName("paymentMethod")
        private String paymentMethod;

        @SerializedName("transactionId")
        private String transactionId;

        @SerializedName("paymentStatus")
        private String paymentStatus;

        @SerializedName("orderStatus")
        private String orderStatus;

        @SerializedName("billStatus")
        private String billStatus;

        @SerializedName("pickupToken")
        private String pickupToken;

        @SerializedName("billCode")
        private String billCode;

        @SerializedName("createdAt")
        private String createdAt;

        @SerializedName("printedAt")
        private String printedAt;

        @SerializedName("printedBy")
        private String printedBy;

        public String getId() { return id; }
        public String getStudentEmail() { return studentEmail; }
        public String getStudentName() { return studentName; }
        public String getStudentPhone() { return studentPhone; }
        public String getBlock() { return block; }
        public List<OrderItem> getItems() { return items; }
        public double getTotalAmount() { return totalAmount; }
        public double getDiscount() { return discount; }
        public double getFinalAmount() { return finalAmount; }
        public String getPaymentMethod() { return paymentMethod; }
        public String getTransactionId() { return transactionId; }
        public String getPaymentStatus() { return paymentStatus; }
        public String getOrderStatus() { return orderStatus; }
        public String getBillStatus() { return billStatus; }
        public String getPickupToken() { return pickupToken; }
        public String getBillCode() { return billCode; }
        public String getCreatedAt() { return createdAt; }
        public String getPrintedAt() { return printedAt; }
        public String getPrintedBy() { return printedBy; }
    }

    public static class VerifyRequest {
        @SerializedName("code")
        private String code;

        public VerifyRequest(String code) {
            this.code = code;
        }

        public String getCode() { return code; }
    }

    public static class VerifyResponse {
        @SerializedName("success")
        private boolean success;

        @SerializedName("order")
        private OrderRecord order;

        @SerializedName("isEligibleForPrint")
        private boolean isEligibleForPrint;

        @SerializedName("billCode")
        private String billCode;

        @SerializedName("pickupToken")
        private String pickupToken;

        @SerializedName("message")
        private String message;

        @SerializedName("code")
        private String code;

        public boolean isSuccess() { return success; }
        public OrderRecord getOrder() { return order; }
        public boolean isEligibleForPrint() { return isEligibleForPrint; }
        public String getBillCode() { return billCode; }
        public String getPickupToken() { return pickupToken; }
        public String getMessage() { return message; }
        public String getCode() { return code; }
    }

    public static class ClaimRequest {
        @SerializedName("orderId")
        private String orderId;

        @SerializedName("operatorId")
        private String operatorId;

        @SerializedName("printTransactionId")
        private String printTransactionId;

        public ClaimRequest(String orderId, String operatorId, String printTransactionId) {
            this.orderId = orderId;
            this.operatorId = operatorId;
            this.printTransactionId = printTransactionId;
        }
    }

    public static class GenericResponse {
        @SerializedName("success")
        private boolean success;

        @SerializedName("message")
        private String message;

        @SerializedName("code")
        private String code;

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getCode() { return code; }
    }

    public static class ConfirmPrintRequest {
        @SerializedName("orderId")
        private String orderId;

        @SerializedName("operatorId")
        private String operatorId;

        @SerializedName("printTransactionId")
        private String printTransactionId;

        public ConfirmPrintRequest(String orderId, String operatorId, String printTransactionId) {
            this.orderId = orderId;
            this.operatorId = operatorId;
            this.printTransactionId = printTransactionId;
        }
    }

    public static class PrintFailureRequest {
        @SerializedName("orderId")
        private String orderId;

        @SerializedName("reason")
        private String reason;

        @SerializedName("operatorId")
        private String operatorId;

        public PrintFailureRequest(String orderId, String reason, String operatorId) {
            this.orderId = orderId;
            this.reason = reason;
            this.operatorId = operatorId;
        }
    }
}
