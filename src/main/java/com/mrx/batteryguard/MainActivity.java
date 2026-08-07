package com.mrx.batteryguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView batteryPercent, chargingStatus, limitText, lastChargeText;
    private SeekBar limitSeekBar;
    private ToggleButton alarmToggle, themeToggle;
    private LinearLayout mainContainer, tabContent, homeTab, historyTab, settingsTab;
    private LinearLayout tempCard, avgCard, historyContainer;
    private Button tabHome, tabHistory, tabSettings;
    private int chargingLimit = 80;
    private boolean alarmEnabled = true;
    private boolean alarmTriggered = false;
    private boolean isDarkTheme = true;
    private int currentLevel = 0;
    private boolean isCharging = false;
    private List history = new ArrayList();
    private List levelHistory = new ArrayList();
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private SharedPreferences prefs;
    private Vibrator vibrator;
    private Ringtone ringtone;
    private boolean alarmActive = false;

    private int bgColor, surfaceColor, primaryColor, textColor, textSecondary, accentColor;
    private int cardBg, cardBorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("batteryguard", MODE_PRIVATE);
        chargingLimit = prefs.getInt("limit", 80);
        alarmEnabled = prefs.getBoolean("alarm", true);
        isDarkTheme = prefs.getBoolean("dark", true);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        
        applyTheme();
        setContentView(createRootUI());
        registerBatteryReceiver();
        switchTab(0);
    }

    private void applyTheme() {
        if (isDarkTheme) {
            bgColor = Color.parseColor("#0a0a1a");
            surfaceColor = Color.parseColor("#1a1a2e");
            primaryColor = Color.parseColor("#00d4aa");
            textColor = Color.parseColor("#e0e0e0");
            textSecondary = Color.parseColor("#8888aa");
            accentColor = Color.parseColor("#ffd700");
            cardBg = Color.parseColor("#1a1a2e");
            cardBorder = Color.parseColor("#2a2a4a");
        } else {
            bgColor = Color.parseColor("#f5f7fa");
            surfaceColor = Color.parseColor("#ffffff");
            primaryColor = Color.parseColor("#00897b");
            textColor = Color.parseColor("#1a1a2e");
            textSecondary = Color.parseColor("#666677");
            accentColor = Color.parseColor("#ff8f00");
            cardBg = Color.parseColor("#ffffff");
            cardBorder = Color.parseColor("#e0e0e0");
        }
    }

    private View createRootUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bgColor);

        // Top bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.VERTICAL);
        topBar.setGravity(Gravity.CENTER);
        topBar.setPadding(0, 40, 0, 15);
        topBar.setBackgroundColor(surfaceColor);

        TextView iconView = new TextView(this);
        iconView.setText("\u26A1");
        iconView.setTextSize(36);
        iconView.setGravity(Gravity.CENTER);
        topBar.addView(iconView);

        TextView titleText = new TextView(this);
        titleText.setText("MRX BatteryGuard");
        titleText.setTextSize(22);
        titleText.setTextColor(primaryColor);
        titleText.setTypeface(Typeface.DEFAULT_BOLD);
        titleText.setGravity(Gravity.CENTER);
        topBar.addView(titleText);

        root.addView(topBar);

        // Tab content area
        tabContent = new LinearLayout(this);
        tabContent.setOrientation(LinearLayout.VERTICAL);
        tabContent.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(tabContent);

        // Create tabs
        homeTab = createHomeTab();
        historyTab = createHistoryTab();
        settingsTab = createSettingsTab();

        // Bottom tabs
        LinearLayout bottomTabs = new LinearLayout(this);
        bottomTabs.setOrientation(LinearLayout.HORIZONTAL);
        bottomTabs.setBackgroundColor(surfaceColor);
        bottomTabs.setPadding(0, 10, 0, 20);
        bottomTabs.setGravity(Gravity.CENTER);

        tabHome = tabButton("Home", true);
        tabHome.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { switchTab(0); }
        });
        bottomTabs.addView(tabHome);

        tabHistory = tabButton("History", false);
        tabHistory.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { switchTab(1); }
        });
        bottomTabs.addView(tabHistory);

        tabSettings = tabButton("Settings", false);
        tabSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { switchTab(2); }
        });
        bottomTabs.addView(tabSettings);

        root.addView(bottomTabs);
        return root;
    }

    private Button tabButton(String text, boolean active) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(13);
        btn.setTextColor(active ? primaryColor : textSecondary);
        btn.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
        btn.setBackgroundColor(Color.TRANSPARENT);
        btn.setPadding(20, 12, 20, 12);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        btn.setLayoutParams(p);
        return btn;
    }

    private void switchTab(int tab) {
        tabContent.removeAllViews();
        tabHome.setTextColor(tab == 0 ? primaryColor : textSecondary);
        tabHome.setTypeface(null, tab == 0 ? Typeface.BOLD : Typeface.NORMAL);
        tabHistory.setTextColor(tab == 1 ? primaryColor : textSecondary);
        tabHistory.setTypeface(null, tab == 1 ? Typeface.BOLD : Typeface.NORMAL);
        tabSettings.setTextColor(tab == 2 ? primaryColor : textSecondary);
        tabSettings.setTypeface(null, tab == 2 ? Typeface.BOLD : Typeface.NORMAL);

        if (tab == 0) tabContent.addView(homeTab);
        else if (tab == 1) {
            refreshHistoryTab();
            tabContent.addView(historyTab);
        }
        else tabContent.addView(settingsTab);
    }

    private LinearLayout createHomeTab() {
        ScrollView sv = new ScrollView(this);
        LinearLayout home = new LinearLayout(this);
        home.setOrientation(LinearLayout.VERTICAL);
        home.setPadding(20, 20, 20, 20);
        home.setBackgroundColor(bgColor);

        // Battery circle
        LinearLayout circleContainer = new LinearLayout(this);
        circleContainer.setGravity(Gravity.CENTER);
        circleContainer.setPadding(0, 10, 0, 10);

        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        circleBg.setColor(cardBg);
        circleBg.setStroke(4, primaryColor);
        circleBg.setSize(200, 200);

        LinearLayout circleInner = new LinearLayout(this);
        circleInner.setOrientation(LinearLayout.VERTICAL);
        circleInner.setGravity(Gravity.CENTER);
        circleInner.setBackground(circleBg);
        circleInner.setLayoutParams(new LinearLayout.LayoutParams(200, 200));

        batteryPercent = new TextView(this);
        batteryPercent.setText("--%");
        batteryPercent.setTextSize(48);
        batteryPercent.setTextColor(primaryColor);
        batteryPercent.setTypeface(Typeface.DEFAULT_BOLD);
        batteryPercent.setGravity(Gravity.CENTER);
        circleInner.addView(batteryPercent);

        chargingStatus = new TextView(this);
        chargingStatus.setText("Waiting...");
        chargingStatus.setTextSize(13);
        chargingStatus.setTextColor(textSecondary);
        chargingStatus.setGravity(Gravity.CENTER);
        circleInner.addView(chargingStatus);

        circleContainer.addView(circleInner);
        home.addView(circleContainer);

        // Alarm target
        home.addView(sectionLabel("Alarm Target"));
        LinearLayout limitSection = card();
        LinearLayout limitRow = new LinearLayout(this);
        limitRow.setOrientation(LinearLayout.HORIZONTAL);
        limitRow.setGravity(Gravity.CENTER);

        Button minusBtn = smallBtn("-");
        minusBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                chargingLimit = Math.max(10, chargingLimit - 5);
                limitSeekBar.setProgress(chargingLimit - 10);
                limitText.setText(chargingLimit + "%");
                saveSettings();
            }
        });
        limitRow.addView(minusBtn);

        limitText = new TextView(this);
        limitText.setText(chargingLimit + "%");
        limitText.setTextSize(36);
        limitText.setTextColor(primaryColor);
        limitText.setTypeface(Typeface.DEFAULT_BOLD);
        limitText.setGravity(Gravity.CENTER);
        limitText.setPadding(15, 0, 15, 0);
        limitRow.addView(limitText);

        Button plusBtn = smallBtn("+");
        plusBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                chargingLimit = Math.min(100, chargingLimit + 5);
                limitSeekBar.setProgress(chargingLimit - 10);
                limitText.setText(chargingLimit + "%");
                saveSettings();
            }
        });
        limitRow.addView(plusBtn);
        limitSection.addView(limitRow);

        limitSeekBar = new SeekBar(this);
        limitSeekBar.setMax(90);
        limitSeekBar.setProgress(chargingLimit - 10);
        limitSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                chargingLimit = p + 10;
                limitText.setText(chargingLimit + "%");
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) { saveSettings(); }
        });
        limitSection.addView(limitSeekBar);
        home.addView(limitSection);

        // Presets (removed 90%)
        LinearLayout presets = new LinearLayout(this);
        presets.setOrientation(LinearLayout.HORIZONTAL);
        presets.setGravity(Gravity.CENTER);
        int[] vals = {50, 60, 70, 80, 100};
        for (int i = 0; i < vals.length; i++) {
            final int v = vals[i];
            Button b = new Button(this);
            b.setText(v + "%");
            b.setTextSize(11);
            b.setTextColor(textSecondary);
            b.setBackgroundColor(cardBg);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v2) {
                    chargingLimit = v;
                    limitSeekBar.setProgress(v - 10);
                    limitText.setText(v + "%");
                    saveSettings();
                }
            });
            presets.addView(b);
        }
        home.addView(presets);

        // Stats
        home.addView(sectionLabel("Statistics"));
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        tempCard = statCard("Temperature", "--°C");
        avgCard = statCard("Average", "--%");
        stats.addView(tempCard);
        stats.addView(avgCard);
        home.addView(stats);

        lastChargeText = new TextView(this);
        lastChargeText.setText("Last update: --");
        lastChargeText.setTextSize(11);
        lastChargeText.setTextColor(textSecondary);
        lastChargeText.setGravity(Gravity.CENTER);
        lastChargeText.setPadding(0, 8, 0, 8);
        home.addView(lastChargeText);

        // Stop alarm button
        Button stopAlarmBtn = bigButton("Stop Alarm", "#ff4757");
        stopAlarmBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { stopAlarm(); }
        });
        home.addView(stopAlarmBtn);

        sv.addView(home);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.addView(sv);
        return wrapper;
    }

    private LinearLayout createHistoryTab() {
        ScrollView sv = new ScrollView(this);
        LinearLayout hist = new LinearLayout(this);
        hist.setOrientation(LinearLayout.VERTICAL);
        hist.setPadding(20, 20, 20, 20);
        hist.setBackgroundColor(bgColor);

        TextView title = new TextView(this);
        title.setText("Charging History");
        title.setTextSize(20);
        title.setTextColor(primaryColor);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);
        hist.addView(title);

        historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        hist.addView(historyContainer);

        sv.addView(hist);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.addView(sv);
        return wrapper;
    }

    private LinearLayout createSettingsTab() {
        ScrollView sv = new ScrollView(this);
        LinearLayout sett = new LinearLayout(this);
        sett.setOrientation(LinearLayout.VERTICAL);
        sett.setPadding(20, 20, 20, 20);
        sett.setBackgroundColor(bgColor);

        TextView title = new TextView(this);
        title.setText("Settings");
        title.setTextSize(20);
        title.setTextColor(primaryColor);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);
        sett.addView(title);

        // Alarm toggle
        LinearLayout alarmRow = settingRow("Alarm");
        alarmToggle = new ToggleButton(this);
        alarmToggle.setChecked(alarmEnabled);
        alarmToggle.setTextOn("ON");
        alarmToggle.setTextOff("OFF");
        alarmToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean c) {
                alarmEnabled = c;
                saveSettings();
            }
        });
        alarmRow.addView(alarmToggle);
        sett.addView(alarmRow);

        // Theme toggle
        LinearLayout themeRow = settingRow("Dark Theme");
        themeToggle = new ToggleButton(this);
        themeToggle.setChecked(isDarkTheme);
        themeToggle.setTextOn("ON");
        themeToggle.setTextOff("OFF");
        themeToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean c) {
                isDarkTheme = c;
                prefs.edit().putBoolean("dark", isDarkTheme).commit();
                applyTheme();
                recreate();
            }
        });
        themeRow.addView(themeToggle);
        sett.addView(themeRow);

        // About
        sett.addView(sectionLabel("About"));
        TextView about = new TextView(this);
        about.setText("MRX BatteryGuard v2.0\nSmart battery charging monitor\n\nMonitors battery level and alerts\nyou when target charge is reached.");
        about.setTextSize(13);
        about.setTextColor(textSecondary);
        about.setPadding(10, 10, 10, 10);
        sett.addView(about);

        sv.addView(sett);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.addView(sv);
        return wrapper;
    }

    private LinearLayout settingRow(String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(15, 12, 15, 12);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(cardBg);
        bg.setCornerRadius(12);
        row.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, 10);
        row.setLayoutParams(p);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(15);
        tv.setTextColor(textColor);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tv);
        return row;
    }

    private void refreshHistoryTab() {
        if (historyContainer == null) return;
        historyContainer.removeAllViews();
        if (history.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No data yet...");
            tv.setTextSize(14);
            tv.setTextColor(textSecondary);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(0, 40, 0, 0);
            historyContainer.addView(tv);
            return;
        }
        int count = Math.min(history.size(), 30);
        for (int i = 0; i < count; i++) {
            TextView tv = new TextView(this);
            tv.setText((String)history.get(i));
            tv.setTextSize(12);
            tv.setTextColor(textSecondary);
            tv.setPadding(8, 6, 8, 6);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(cardBg);
            bg.setCornerRadius(8);
            tv.setBackground(bg);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            p.setMargins(0, 0, 0, 4);
            tv.setLayoutParams(p);
            historyContainer.addView(tv);
        }
    }

    private TextView sectionLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(13);
        label.setTextColor(primaryColor);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setPadding(0, 12, 0, 6);
        return label;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(15, 12, 15, 12);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(cardBg);
        bg.setCornerRadius(18);
        bg.setStroke(1, cardBorder);
        card.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 5, 0, 8);
        card.setLayoutParams(p);
        return card;
    }

    private LinearLayout statCard(String title, String value) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(12, 12, 12, 12);
        c.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(cardBg);
        bg.setCornerRadius(14);
        bg.setStroke(1, cardBorder);
        c.setBackground(bg);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(11);
        tvTitle.setTextColor(textSecondary);
        tvTitle.setGravity(Gravity.CENTER);
        c.addView(tvTitle);

        TextView tvValue = new TextView(this);
        tvValue.setText(value);
        tvValue.setTextSize(20);
        tvValue.setTextColor(primaryColor);
        tvValue.setTypeface(Typeface.DEFAULT_BOLD);
        tvValue.setGravity(Gravity.CENTER);
        tvValue.setPadding(0, 4, 0, 0);
        c.addView(tvValue);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(4, 0, 4, 0);
        c.setLayoutParams(lp);
        return c;
    }

    private Button smallBtn(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(18);
        btn.setTextColor(primaryColor);
        btn.setBackgroundColor(cardBg);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(cardBg);
        bg.setStroke(2, primaryColor);
        bg.setCornerRadius(25);
        btn.setBackground(bg);
        btn.setLayoutParams(new LinearLayout.LayoutParams(55, 55));
        return btn;
    }

    private Button bigButton(String text, String colorStr) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(15);
        btn.setTextColor(Color.WHITE);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(colorStr));
        bg.setCornerRadius(25);
        btn.setBackground(bg);
        btn.setPadding(0, 16, 0, 16);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 5, 0, 5);
        btn.setLayoutParams(p);
        return btn;
    }

    private void registerBatteryReceiver() {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            public void onReceive(Context c, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (scale > 0) currentLevel = (level * 100) / scale;
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);
                float temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f;
                updateUI(currentLevel, isCharging, temp);
                checkAlarm(currentLevel, isCharging);
                addHistory(currentLevel, isCharging);
            }
        };
        registerReceiver(receiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private void updateUI(int level, boolean charging, float temp) {
        if (batteryPercent == null) return;
        batteryPercent.setText(level + "%");
        int color;
        if (charging) color = accentColor;
        else if (level < 20) color = Color.parseColor("#ff4757");
        else if (level < 50) color = Color.parseColor("#ffa502");
        else color = primaryColor;
        batteryPercent.setTextColor(color);
        chargingStatus.setText(charging ? "Charging" : "Discharging");

        if (tempCard != null && tempCard.getChildCount() > 1) {
            ((TextView)tempCard.getChildAt(1)).setText(String.format("%.0f°C", temp));
        }
        if (avgCard != null && avgCard.getChildCount() > 1) {
            int avg = 0;
            if (!levelHistory.isEmpty()) {
                for (int i = 0; i < levelHistory.size(); i++) avg += ((Integer)levelHistory.get(i)).intValue();
                avg /= levelHistory.size();
            }
            ((TextView)avgCard.getChildAt(1)).setText(avg + "%");
        }
        if (lastChargeText != null) lastChargeText.setText("Updated: " + sdf.format(new Date()));
    }

    private void checkAlarm(int level, boolean charging) {
        if (!alarmEnabled || !charging || level < chargingLimit || alarmActive) return;
        triggerAlarm(level);
    }

    private void triggerAlarm(int level) {
        alarmActive = true;
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            ringtone = RingtoneManager.getRingtone(this, alarmUri);
            if (ringtone != null) ringtone.play();
        } catch (Exception e) {}
        if (vibrator != null) vibrator.vibrate(new long[]{0, 500, 200, 500}, 0);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notif = new Notification.Builder(this)
            .setContentTitle("MRX BatteryGuard Alarm!")
            .setContentText("Battery reached " + level + "%!")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(Notification.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build();
        nm.notify(1, notif);
        new AlertDialog.Builder(this)
            .setTitle("Target Reached!")
            .setMessage("Battery at " + level + "%.\nUnplug charger.")
            .setPositiveButton("STOP", new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface d, int w) { stopAlarm(); }
            })
            .setCancelable(false)
            .show();
    }

    private void stopAlarm() {
        alarmActive = false;
        if (ringtone != null) { ringtone.stop(); ringtone = null; }
        if (vibrator != null) vibrator.cancel();
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(1);
    }

    private void addHistory(int level, boolean charging) {
        levelHistory.add(Integer.valueOf(level));
        if (levelHistory.size() > 500) levelHistory.remove(0);
        history.add(0, sdf.format(new Date()) + "  " + level + "%  " + (charging ? "Charging" : "Idle"));
        if (history.size() > 50) history.remove(history.size() - 1);
    }

    private void saveSettings() {
        prefs.edit().putInt("limit", chargingLimit).putBoolean("alarm", alarmEnabled).putBoolean("dark", isDarkTheme).commit();
    }
}
