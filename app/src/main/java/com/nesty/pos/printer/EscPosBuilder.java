package com.nesty.pos.printer;

import com.nesty.pos.api.models.OrderModels;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EscPosBuilder {
    private static final int LINE_WIDTH = 32; // Exact 32 chars for 2-inch (58mm) paper

    public static byte[] buildReceiptBytes(OrderModels.OrderRecord order, String terminalId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            // 1. ESC @ : Initialize printer
            out.write(new byte[]{0x1B, 0x40});

            // 2. Center Align + Bold: Brand Header
            out.write(new byte[]{0x1B, 0x61, 0x01}); // Center
            out.write(new byte[]{0x1B, 0x45, 0x01}); // Bold ON
            out.write("NESTY CANTEEN\n".getBytes("UTF-8"));
            out.write(new byte[]{0x1B, 0x45, 0x00}); // Bold OFF
            out.write("Smart Campus Dining Hub\n".getBytes("UTF-8"));
            out.write("CVR College of Engineering\n".getBytes("UTF-8"));
            out.write(divider('=').getBytes("UTF-8"));

            // 3. Left Align: Order Info
            out.write(new byte[]{0x1B, 0x61, 0x00}); // Left
            out.write(formatRow("Order ID", "#" + order.getId()).getBytes("UTF-8"));
            if (order.getBillCode() != null && !order.getBillCode().isEmpty()) {
                out.write(new byte[]{0x1B, 0x45, 0x01}); // Bold ON
                out.write(formatRow("Bill Code", order.getBillCode()).getBytes("UTF-8"));
                out.write(new byte[]{0x1B, 0x45, 0x00}); // Bold OFF
            }
            if (order.getPickupToken() != null && !order.getPickupToken().isEmpty()) {
                out.write(formatRow("Pickup Token", order.getPickupToken()).getBytes("UTF-8"));
            }

            String dateStr = new SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault()).format(new Date());
            out.write(formatRow("Date/Time", dateStr).getBytes("UTF-8"));
            out.write(formatRow("Student", order.getStudentName() != null ? order.getStudentName() : "Student").getBytes("UTF-8"));
            out.write(formatRow("Status", "PAID (Verified)").getBytes("UTF-8"));
            if (order.getTransactionId() != null) {
                out.write(formatRow("Txn Ref", order.getTransactionId()).getBytes("UTF-8"));
            }
            out.write(divider('-').getBytes("UTF-8"));

            // 4. Items Table (32-column: ITEM 18, QTY 4, PRICE 10)
            out.write(new byte[]{0x1B, 0x45, 0x01});
            out.write(formatItemRow("ITEM", "QTY", "PRICE").getBytes("UTF-8"));
            out.write(new byte[]{0x1B, 0x45, 0x00});
            out.write(divider('-').getBytes("UTF-8"));

            if (order.getItems() != null) {
                for (OrderModels.OrderItem item : order.getItems()) {
                    String name = item.getName() != null ? item.getName() : "Item";
                    String qty = String.valueOf(item.getQuantity());
                    String price = String.format(Locale.getDefault(), "Rs.%.2f", item.getPrice() * item.getQuantity());
                    out.write(formatItemRow(name, qty, price).getBytes("UTF-8"));
                }
            }
            out.write(divider('-').getBytes("UTF-8"));

            // 5. Total Amount Row (Bold)
            out.write(new byte[]{0x1B, 0x45, 0x01});
            String totalStr = String.format(Locale.getDefault(), "Rs.%.2f", order.getFinalAmount());
            out.write(formatRow("TOTAL PAID", totalStr).getBytes("UTF-8"));
            out.write(new byte[]{0x1B, 0x45, 0x00});
            out.write(divider('=').getBytes("UTF-8"));

            // 6. Center Align: Footer & Tear Cut
            out.write(new byte[]{0x1B, 0x61, 0x01});
            out.write("Thank you for dining at NESTY!\n".getBytes("UTF-8"));
            out.write(("Terminal: " + (terminalId != null ? terminalId : "POS-1008") + "\n").getBytes("UTF-8"));
            out.write("Please collect food at Counter\n\n".getBytes("UTF-8"));

            // 7. Feed 3 lines & Paper cut
            out.write(new byte[]{0x1B, 0x64, 0x03}); // Feed 3 lines
            out.write(new byte[]{0x1D, 0x56, 0x42, 0x00}); // Partial cut

        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    public static byte[] buildTestPrintBytes(String terminalId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(new byte[]{0x1B, 0x40});
            out.write(new byte[]{0x1B, 0x61, 0x01});
            out.write(new byte[]{0x1B, 0x45, 0x01});
            out.write("NESTY POS - TEST PRINT\n".getBytes("UTF-8"));
            out.write(new byte[]{0x1B, 0x45, 0x00});
            out.write("Model 1008 Handheld Terminal\n".getBytes("UTF-8"));
            out.write("Paper Width: 2 inch (58mm)\n".getBytes("UTF-8"));
            out.write(divider('=').getBytes("UTF-8"));
            out.write(new byte[]{0x1B, 0x61, 0x00});
            out.write(formatRow("Printer", "Thermal Built-In OK").getBytes("UTF-8"));
            out.write(formatRow("Columns", "32 Characters OK").getBytes("UTF-8"));
            out.write(formatRow("Terminal", terminalId != null ? terminalId : "POS-1008").getBytes("UTF-8"));
            out.write(formatRow("Test Time", new SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(new Date())).getBytes("UTF-8"));
            out.write(divider('=').getBytes("UTF-8"));
            out.write(new byte[]{0x1B, 0x61, 0x01});
            out.write("PRINT HEAD DIAGNOSTIC: PASS\n\n".getBytes("UTF-8"));
            out.write(new byte[]{0x1B, 0x64, 0x03});
            out.write(new byte[]{0x1D, 0x56, 0x42, 0x00});
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

    private static String divider(char c) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LINE_WIDTH; i++) {
            sb.append(c);
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String formatRow(String label, String value) {
        String lbl = label + ": ";
        int spaceNeeded = LINE_WIDTH - lbl.length() - value.length();
        if (spaceNeeded < 1) {
            spaceNeeded = 1;
        }
        StringBuilder sb = new StringBuilder(lbl);
        for (int i = 0; i < spaceNeeded; i++) {
            sb.append(' ');
        }
        sb.append(value).append("\n");
        return sb.toString();
    }

    private static String formatItemRow(String name, String qty, String price) {
        // Name: max 16 chars, Qty: 4 chars, Price: 10 chars -> Total 32 chars
        String n = name.length() > 16 ? name.substring(0, 15) + "." : name;
        return String.format(Locale.getDefault(), "%-16s %4s %10s\n", n, qty, price);
    }
}
