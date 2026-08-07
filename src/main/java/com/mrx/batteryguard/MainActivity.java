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
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView batteryPercent, chargingStatus, limitText, lastChargeText, titleText;
    private SeekBar limitSeekBar;
    private ToggleButton alarmToggle, themeToggle;
    private LinearLayout historyContainer, tempCard, avgCard, mainLayout, settingsPanel;
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

    // Theme colors
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
        setContentView(createMainUI());
        registerBatteryReceiver();
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
            bgColor = Color.parseColor("#f0f4f8");
            surfaceColor = Color.parseColor("#ffffff");
            primaryColor = Color.parseColor("#00897b");
            textColor = Color.parseColor("#1a1a2e");
            textSecondary = Color.parseColor("#666677");
            accentColor = Color.parseColor("#ff8f00");
            cardBg = Color.parseColor("#ffffff");
            cardBorder = Color.parseColor("#e0e0e0");
        }
        if (mainLayout != null) {
            mainLayout.setBackgroundColor(bgColor);
            refreshAllViews();
        }
    }

    private View createMainUI() {
        ScrollView scrollView = new ScrollView(this);
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(30, 50, 30, 30);
        mainLayout.setBackgroundColor(bgColor);

        // App icon
        LinearLayout iconRow = new LinearLayout(this);
        iconRow.setGravity(Gravity.CENTER);
        iconRow.setPadding(0, 0, 0, 10);

        TextView iconView = new TextView(this);
        iconView.setText("\u26A1");
        iconView.setTextSize(48);
        iconView.setGravity(Gravity.CENTER);
        iconRow.addView(iconView);
        mainLayout.addView(iconRow);

        // Title
        titleText = new TextView(this);
        titleText.setText("BatteryGuard");
        titleText.setTextSize(30);
        titleText.setTextColor(primaryColor);
        titleText.setTypeface(Typeface.DEFAULT_BOLD);
        titleText.setGravity(Gravity.CENTER);
        mainLayout.addView(titleText);

        TextView subtitle = new TextView(this);
        subtitle.setText("Smart Charging Monitor");
        subtitle.setTextSize(14);
        subtitle.setTextColor(textSecondary);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 4, 0, 30);
        mainLayout.addView(subtitle);

        // Battery percentage circle (simulated with text)
        LinearLayout circleContainer = new LinearLayout(this);
        circleContainer.setGravity(Gravity.CENTER);
        circleContainer.setPadding(0, 0, 0, 10);

        GradientDrawable circleBg = new GradientDrawable();
        circleBg.setShape(GradientDrawable.OVAL);
        circleBg.setColor(surfaceColor);
        circleBg.setStroke(4, primaryColor);
        circleBg.setSize(220, 220);

        LinearLayout circleInner = new LinearLayout(this);
        circleInner.setOrientation(LinearLayout.VERTICAL);
        circleInner.setGravity(Gravity.CENTER);
        circleInner.setBackground(circleBg);
        circleInner.setLayoutParams(new LinearLayout.LayoutParams(220, 220));

        batteryPercent = new TextView(this);
        batteryPercent.setText("--%");
        batteryPercent.setTextSize(56);
        batteryPercent.setTextColor(primaryColor);
        batteryPercent.setTypeface(Typeface.DEFAULT_BOLD);
        batteryPercent.setGravity(Gravity.CENTER);
        circleInner.addView(batteryPercent);

        chargingStatus = new TextView(this);
        chargingStatus.setText("Waiting...");
        chargingStatus.setTextSize(14);
        chargingStatus.setTextColor(textSecondary);
        chargingStatus.setGravity(Gravity.CENTER);
        chargingStatus.setPadding(0, 5, 0, 0);
        circleInner.addView(chargingStatus);

        circleContainer.addView(circleInner);
        mainLayout.addView(circleContainer);

        // Alarm target section
        mainLayout.addView(sectionLabel("Alarm Target"));
        LinearLayout limitSection = card();
        
        LinearLayout limitRow = new LinearLayout(this);
        limitRow.setOrientation(LinearLayout.HORIZONTAL);
        limitRow.setGravity(Gravity.CENTER);
        limitRow.setPadding(0, 5, 0, 10);

        Button minusBtn = new Button(this);
        minusBtn.setText("-");
        minusBtn.setTextColor(primaryColor);
        minusBtn.setBackgroundColor(surfaceColor);
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
        limitText.setTextSize(40);
        limitText.setTextColor(primaryColor);
        limitText.setTypeface(Typeface.DEFAULT_BOLD);
        limitText.setGravity(Gravity.CENTER);
        limitText.setPadding(20, 0, 20, 0);
        limitRow.addView(limitText);

        Button plusBtn = new Button(this);
        plusBtn.setText("+");
        plusBtn.setTextColor(primaryColor);
        plusBtn.setBackgroundColor(surfaceColor);
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
            public void onStopTrackingTouch(SeekBar s) {
                saveSettings();
            }
        });
        limitSection.addView(limitSeekBar);
        mainLayout.addView(limitSection);

        // Quick preset buttons
        LinearLayout presets = new LinearLayout(this);
        presets.setOrientation(LinearLayout.HORIZONTAL);
        presets.setGravity(Gravity.CENTER);
        presets.setPadding(0, 5, 0, 15);
        
        int[] presetValues = {50, 60, 70, 80, 90, 100};
        for (int i = 0; i < presetValues.length; i++) {
            final int val = presetValues[i];
            Button btn = new Button(this);
            btn.setText(val + "%");
            btn.setTextSize(12);
            btn.setTextColor(textSecondary);
            btn.setBackgroundColor(surfaceColor);
            btn.setPadding(8, 4, 8, 4);
            btn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    chargingLimit = val;
                    limitSeekBar.setProgress(val - 10);
                    limitText.setText(val + "%");
                    saveSettings();
                }
            });
            presets.addView(btn);
        }
        mainLayout.addView(presets);

        // Alarm toggle
        LinearLayout toggleRow = new LinearLayout(this);
        toggleRow.setOrientation(LinearLayout.HORIZONTAL);
        toggleRow.setGravity(Gravity.CENTER);
        toggleRow.setPadding(0, 5, 0, 5);

        alarmToggle = new ToggleButton(this);
        alarmToggle.setChecked(alarmEnabled);
        alarmToggle.setTextOn("ON");
        alarmToggle.setTextOff("OFF");
        alarmToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                alarmEnabled = checked;
                saveSettings();
            }
        });
        toggleRow.addView(alarmToggle);

        TextView toggleLabel = new TextView(this);
        toggleLabel.setText("  Alarm Active");
        toggleLabel.setTextSize(16);
        toggleLabel.setTextColor(textSecondary);
        toggleRow.addView(toggleLabel);
        mainLayout.addView(toggleRow);

        // Theme toggle
        LinearLayout themeRow = new LinearLayout(this);
        themeRow.setOrientation(LinearLayout.HORIZONTAL);
        themeRow.setGravity(Gravity.CENTER);
        themeRow.setPadding(0, 5, 0, 15);

        themeToggle = new ToggleButton(this);
        themeToggle.setChecked(isDarkTheme);
        themeToggle.setTextOn("DARK");
        themeToggle.setTextOff("LIGHT");
        themeToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                isDarkTheme = checked;
                prefs.edit().putBoolean("dark", isDarkTheme).commit();
                applyTheme();
                refreshAllViews();
            }
        });
        themeRow.addView(themeToggle);

        TextView themeLabel = new TextView(this);
        themeLabel.setText("  Dark Theme");
        themeLabel.setTextSize(16);
        themeLabel.setTextColor(textSecondary);
        themeRow.addView(themeLabel);
        mainLayout.addView(themeRow);

        // Stats cards
        mainLayout.addView(sectionLabel("Statistics"));
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setPadding(0, 5, 0, 10);
        tempCard = statCard("Temperature", "--°C");
        avgCard = statCard("Average", "--%");
        stats.addView(tempCard);
        stats.addView(avgCard);
        mainLayout.addView(stats);

        lastChargeText = new TextView(this);
        lastChargeText.setText("Last update: --");
        lastChargeText.setTextSize(12);
        lastChargeText.setTextColor(textSecondary);
        lastChargeText.setGravity(Gravity.CENTER);
        lastChargeText.setPadding(0, 5, 0, 5);
        mainLayout.addView(lastChargeText);

        // Buttons
        mainLayout.addView(sectionLabel("Actions"));

        Button historyBtn = bigButton("Charging History", "#00b894");
        historyBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showHistory(); }
        });
        mainLayout.addView(historyBtn);

        Button stopAlarmBtn = bigButton("Stop Alarm", "#ff4757");
        stopAlarmBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { stopAlarm(); }
        });
        mainLayout.addView(stopAlarmBtn);

        historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        historyContainer.setPadding(0, 10, 0, 10);
        mainLayout.addView(historyContainer);

        // Settings
        mainLayout.addView(sectionLabel("Settings"));
        settingsPanel = new LinearLayout(this);
        settingsPanel.setOrientation(LinearLayout.VERTICAL);
        mainLayout.addView(settingsPanel);

        scrollView.addView(mainLayout);
        return scrollView;
    }

    private void refreshAllViews() {
        if (mainLayout == null) return;
        mainLayout.setBackgroundColor(bgColor);
        titleText.setTextColor(primaryColor);
        // Re-create the view to apply all theme changes
        setContentView(createMainUI());
        updateUI(currentLevel, isCharging, 25f);
    }

    private TextView sectionLabel(String text) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(14);
        label.setTextColor(primaryColor);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setPadding(0, 15, 0, 8);
        return label;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 15, 20, 15);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(cardBg);
        bg.setCornerRadius(20);
        bg.setStroke(1, cardBorder);
        card.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 5, 0, 10);
        card.setLayoutParams(p);
        return card;
    }

    private LinearLayout statCard(String title, String value) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(15, 15, 15, 15);
        c.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(cardBg);
        bg.setCornerRadius(16);
        bg.setStroke(1, cardBorder);
        c.setBackground(bg);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(12);
        tvTitle.setTextColor(textSecondary);
        tvTitle.setGravity(Gravity.CENTER);
        c.addView(tvTitle);

        TextView tvValue = new TextView(this);
        tvValue.setText(value);
        tvValue.setTextSize(22);
        tvValue.setTextColor(primaryColor);
        tvValue.setTypeface(Typeface.DEFAULT_BOLD);
        tvValue.setGravity(Gravity.CENTER);
        tvValue.setPadding(0, 5, 0, 0);
        tvValue.setTag("value");
        c.addView(tvValue);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(5, 0, 5, 0);
        c.setLayoutParams(lp);
        return c;
    }

    private Button bigButton(String text, String colorStr) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(16);
        btn.setTextColor(Color.WHITE);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor(colorStr));
        bg.setCornerRadius(30);
        btn.setBackground(bg);
        btn.setPadding(0, 20, 0, 20);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 5, 0, 10);
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

        // Update temp card
        if (tempCard != null && tempCard.getChildCount() > 1) {
            TextView tv = (TextView) tempCard.getChildAt(1);
            if (tv != null) tv.setText(String.format("%.0f°C", temp));
        }

        // Update avg card
        if (avgCard != null && avgCard.getChildCount() > 1) {
            int avg = 0;
            if (!levelHistory.isEmpty()) {
                for (int i = 0; i < levelHistory.size(); i++) {
                    avg += ((Integer)levelHistory.get(i)).intValue();
                }
                avg /= levelHistory.size();
            }
            TextView tv = (TextView) avgCard.getChildAt(1);
            if (tv != null) tv.setText(avg + "%");
        }

        if (lastChargeText != null) {
            lastChargeText.setText("Updated: " + sdf.format(new Date()));
        }
    }

    private void checkAlarm(int level, boolean charging) {
        if (!alarmEnabled || !charging || level < chargingLimit || alarmActive) return;
        triggerAlarm(level);
    }

    private void triggerAlarm(int level) {
        alarmActive = true;
        
        // Sound
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            ringtone = RingtoneManager.getRingtone(this, alarmUri);
            if (ringtone != null) ringtone.play();
        } catch (Exception e) {}

        // Vibrate
        if (vibrator != null) vibrator.vibrate(new long[]{0, 500, 200, 500}, 0);

        // Notification
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notif = new Notification.Builder(this)
            .setContentTitle("BatteryGuard Alarm!")
            .setContentText("Battery reached " + level + "% - Unplug now!")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(Notification.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build();
        nm.notify(1, notif);

        // Alert dialog
        new AlertDialog.Builder(this)
            .setTitle("Battery Target Reached!")
            .setMessage("Battery is at " + level + "%.\nUnplug your charger to protect battery health.")
            .setPositiveButton("STOP ALARM", new android.content.DialogInterface.OnClickListener() {
                public void onClick(android.content.DialogInterface d, int w) {
                    stopAlarm();
                }
            })
            .setCancelable(false)
            .show();
    }

    private void stopAlarm() {
        alarmActive = false;
        if (ringtone != null) {
            ringtone.stop();
            ringtone = null;
        }
        if (vibrator != null) vibrator.cancel();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(1);
    }

    private void addHistory(int level, boolean charging) {
        levelHistory.add(Integer.valueOf(level));
        if (levelHistory.size() > 500) levelHistory.remove(0);
        String entry = sdf.format(new Date()) + "  " + level + "%  " + (charging ? "Charging" : "Idle");
        history.add(0, entry);
        if (history.size() > 50) history.remove(history.size() - 1);
    }

    private void showHistory() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Charging History");
        
        StringBuilder sb = new StringBuilder();
        int count = Math.min(history.size(), 15);
        for (int i = 0; i < count; i++) {
            sb.append(history.get(i)).append("\n");
        }
        builder.setMessage(sb.length() > 0 ? sb.toString() : "No data yet");
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private void saveSettings() {
        prefs.edit()
            .putInt("limit", chargingLimit)
            .putBoolean("alarm", alarmEnabled)
            .putBoolean("dark", isDarkTheme)
            .commit();
    }
}
