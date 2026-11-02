package com.nesty.pos;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.nesty.pos.data.SessionManager;
import com.nesty.pos.printer.Model1008PrinterDriver;

public class NestyPosApplication extends Application {
    private static NestyPosApplication instance;
    private SessionManager sessionManager;
    private Model1008PrinterDriver printerDriver;

    @Override
    public void onCreate() {
        super.onCreate();
        // Force Light Mode so Dark Mode on device never inverts text to white
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        instance = this;
        sessionManager = new SessionManager(this);
        printerDriver = new Model1008PrinterDriver(this);
        printerDriver.connect();
    }

    public static NestyPosApplication getInstance() {
        return instance;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public Model1008PrinterDriver getPrinterDriver() {
        return printerDriver;
    }
}

