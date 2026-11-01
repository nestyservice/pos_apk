# Nesty POS – Android 14 POS Machine Application
### Handheld POS Model 1008 Edition (Built-in 2-inch / 58mm Thermal Printer)

Official native Android application for canteen operators to verify student pre-orders, print receipts on the internal 58mm thermal printer, and automatically fulfill orders in the [NESTY Campus Ecosystem](https://nesty-service-app.onrender.com/).

---

## 📱 Hardware Compatibility & Profile

| Parameter | Specification | Notes |
| :--- | :--- | :--- |
| **Target Device** | **Model 1008 Handheld POS Terminal** | Tested on Android 14 |
| **Processor** | Octa-Core High Performance Application CPU | Sub-100ms response & instant UI rendering |
| **Connectivity** | Dual SIM 4G LTE + 2.4/5GHz Wi-Fi | Continuous connectivity failover |
| **Thermal Printer** | Built-in 2-Inch (58mm roll, 384 dots/line) | 32-character fixed tabular column grid |
| **Driver Mode** | Adaptive Model 1008 AIDL + Direct ESC/POS | Plug-and-play across POS ROM builds |

---

## ⚡ Direct Download & Installation on Model 1008

You can install Nesty POS directly onto your terminal without a computer or USB cable:

1. Connect the Model 1008 machine to Wi-Fi or 4G LTE SIM.
2. Open **Chrome** on the Model 1008 terminal screen.
3. Visit the direct download link:
   👉 **`https://nesty-service-app.onrender.com/downloads/nesty-pos.apk`**  
   *(or download from GitHub Releases: [`releases/latest/download/nesty-pos.apk`](https://github.com/nestyservice/pos_apk/releases/latest/download/nesty-pos.apk))*
4. Tap **Download** ➔ **Open** ➔ **Install**.
5. Launch **Nesty POS** from the home screen!

---

## 🔐 Default Operator Login Credentials

| Role | Operator ID / Email | Default Password / PIN | Permissions |
| :--- | :--- | :--- | :--- |
| **POS Counter Operator** | `operator@cvr.ac.in` *(or `pos-1008`)* | `pos123` | Verify orders, claim, print bills |
| **Canteen Administrator** | `admin@cvr.ac.in` | *(Admin Password)* | Full POS access & terminal configuration |

---

## 🔄 Reusable Bill Code Workflow

1. **Student Places Order:**
   - On [nesty-service-app.onrender.com](https://nesty-service-app.onrender.com/), student places and pays for order.
   - Backend generates a 5-digit **Bill Code** (e.g. `10245`) and 3-char **Token** (e.g. `A7K`).
2. **Student Arrives at Counter:**
   - Gives 5-digit code or shows QR pass to the operator.
3. **Operator Verifies on POS:**
   - Enters code on large touch keypad or scans QR code with camera.
   - POS calls `POST /api/pos/verify-order`. Only active orders are returned.
4. **Operator Taps "PRINT BILL":**
   - POS acquires a 45-second atomic print lock (`POST /api/pos/claim-order`), preventing concurrent double-printing.
   - Model 1008 internal thermal print head generates receipt.
   - POS confirms print (`POST /api/pos/confirm-print`):
     - `orderStatus` transitions to `CLAIMED`
     - `billStatus` transitions to `EXPIRED`
     - `printedAt` timestamp & operator name recorded
     - **Bill code `10245` is IMMEDIATELY released and available for future orders!**
5. **Historical Permanence:**
   - The completed order stays permanently in the student's Order History and backend ledger.
   - If someone tries to enter `10245` again before reassignment, POS displays:
     *"Already Printed: Order #NST-... was collected at [date/time]"*.

---

## 🛠️ Project Structure

```text
com.nesty.pos
├── NestyPosApplication.java        # Application lifecycle & driver initialization
├── api/
│   ├── ApiClient.java              # Retrofit & OkHttp client with Bearer auth
│   ├── NestyApiService.java        # POS REST API interface
│   └── models/                     # GSON request & response models
├── data/
│   └── SessionManager.java         # SharedPreferences session & terminal config
├── printer/
│   ├── IPrinterDriver.java         # Universal driver interface
│   ├── Model1008PrinterDriver.java # Model 1008 AIDL + Serial ESC/POS driver
│   └── EscPosBuilder.java          # 32-column 58mm thermal receipt formatter
└── ui/
    ├── SplashActivity.java         # Splash & session router
    ├── LoginActivity.java          # Operator sign-in screen
    ├── MainActivity.java           # Touch keypad, QR scanner, live status
    ├── OrderDetailsDialog.java     # Order preview & 1-tap Print Bill flow
    └── SettingsActivity.java       # Terminal ID, server URL, printer diagnostics
```

---

## 🏗️ Automated CI/CD Build

Every push to the `main` branch triggers `.github/workflows/build_apk.yml`:
- Compiles the release APK using Ubuntu Android SDK 34 runner.
- Publishes installable release APK to **GitHub Releases**.
