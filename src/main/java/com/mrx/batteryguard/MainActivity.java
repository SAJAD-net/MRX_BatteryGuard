package com.mrx.batteryguard;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView batteryPercent, chargingStatus, limitText, lastChargeText;
    private SeekBar limitSeekBar;
    private ToggleButton alarmToggle;
    private LinearLayout historyContainer, tempCard, avgCard;
    private int chargingLimit = 80;
    private boolean alarmEnabled = true;
    private boolean alarmTriggered = false;
    private int currentLevel = 0;
    private boolean isCharging = false;
    private List history = new ArrayList();
    private List levelHistory = new ArrayList();
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createUI());
        registerBatteryReceiver();
    }

    private View createUI() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 60, 40, 40);
        root.setBackgroundColor(Color.parseColor("#0a0a1a"));

        TextView header = new TextView(this);
        header.setText("MRX BatteryGuard");
        header.setTextSize(28);
        header.setTextColor(Color.parseColor("#00d4aa"));
        header.setGravity(Gravity.CENTER);
        root.addView(header);

        batteryPercent = new TextView(this);
        batteryPercent.setText("--%");
        batteryPercent.setTextSize(72);
        batteryPercent.setTextColor(Color.parseColor("#00d4aa"));
        batteryPercent.setGravity(Gravity.CENTER);
        root.addView(batteryPercent);

        chargingStatus = new TextView(this);
        chargingStatus.setText("Waiting...");
        chargingStatus.setTextSize(18);
        chargingStatus.setTextColor(Color.parseColor("#888899"));
        chargingStatus.setGravity(Gravity.CENTER);
        chargingStatus.setPadding(0, 0, 0, 30);
        root.addView(chargingStatus);

        LinearLayout limitSection = card();
        TextView limitLabel = new TextView(this);
        limitLabel.setText("Alarm Target");
        limitLabel.setTextSize(14);
        limitLabel.setTextColor(Color.parseColor("#888899"));
        limitLabel.setGravity(Gravity.CENTER);
        limitSection.addView(limitLabel);

        limitText = new TextView(this);
        limitText.setText("80%");
        limitText.setTextSize(36);
        limitText.setTextColor(Color.parseColor("#00d4aa"));
        limitText.setGravity(Gravity.CENTER);
        limitSection.addView(limitText);

        limitSeekBar = new SeekBar(this);
        limitSeekBar.setMax(90);
        limitSeekBar.setProgress(70);
        limitSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                chargingLimit = p + 10;
                limitText.setText(chargingLimit + "%");
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        limitSection.addView(limitSeekBar);
        root.addView(limitSection);

        LinearLayout toggleRow = new LinearLayout(this);
        toggleRow.setOrientation(LinearLayout.HORIZONTAL);
        toggleRow.setGravity(Gravity.CENTER);
        toggleRow.setPadding(0, 10, 0, 10);

        alarmToggle = new ToggleButton(this);
        alarmToggle.setChecked(true);
        alarmToggle.setTextOn("ON");
        alarmToggle.setTextOff("OFF");
        alarmToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                alarmEnabled = checked;
            }
        });
        toggleRow.addView(alarmToggle);

        TextView toggleLabel = new TextView(this);
        toggleLabel.setText(" Alarm Active");
        toggleLabel.setTextSize(16);
        toggleLabel.setTextColor(Color.parseColor("#888899"));
        toggleRow.addView(toggleLabel);
        root.addView(toggleRow);

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.setPadding(0, 10, 0, 10);
        tempCard = statCard("Temp", "--C", "Temperature");
        avgCard = statCard("Avg", "--%", "Average");
        stats.addView(tempCard);
        stats.addView(avgCard);
        root.addView(stats);

        lastChargeText = new TextView(this);
        lastChargeText.setText("Last update: --");
        lastChargeText.setTextSize(12);
        lastChargeText.setTextColor(Color.parseColor("#555566"));
        lastChargeText.setGravity(Gravity.CENTER);
        root.addView(lastChargeText);

        Button historyBtn = new Button(this);
        historyBtn.setText("Charging History");
        historyBtn.setTextColor(Color.WHITE);
        historyBtn.setBackgroundColor(Color.parseColor("#00b894"));
        historyBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showHistory();
            }
        });
        root.addView(historyBtn);

        historyContainer = new LinearLayout(this);
        historyContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(historyContainer);

        scrollView.addView(root);
        return scrollView;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 15, 20, 15);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1a1a2e"));
        bg.setCornerRadius(16);
        bg.setStroke(1, Color.parseColor("#2a2a4a"));
        card.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, 15);
        card.setLayoutParams(p);
        return card;
    }

    private LinearLayout statCard(String icon, String value, String label) {
        LinearLayout c = card();
        TextView ic = new TextView(this);
        ic.setText(icon);
        ic.setTextSize(20);
        ic.setTextColor(Color.parseColor("#00d4aa"));
        ic.setGravity(Gravity.CENTER);
        c.addView(ic);
        TextView val = new TextView(this);
        val.setText(value);
        val.setTextSize(16);
        val.setTextColor(Color.parseColor("#00d4aa"));
        val.setGravity(Gravity.CENTER);
        c.addView(val);
        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextSize(11);
        lbl.setTextColor(Color.parseColor("#888899"));
        lbl.setGravity(Gravity.CENTER);
        c.addView(lbl);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(5, 0, 5, 0);
        c.setLayoutParams(lp);
        return c;
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
        batteryPercent.setText(level + "%");
        int color;
        if (charging) color = Color.parseColor("#ffd700");
        else if (level < 20) color = Color.parseColor("#ff4757");
        else if (level < 50) color = Color.parseColor("#ffa502");
        else color = Color.parseColor("#00d4aa");
        batteryPercent.setTextColor(color);
        chargingStatus.setText(charging ? "Charging" : "Discharging");
        
        tempCard.removeAllViews();
        TextView t1 = new TextView(this);
        t1.setText("Temp");
        t1.setTextSize(20);
        t1.setTextColor(Color.parseColor("#00d4aa"));
        t1.setGravity(Gravity.CENTER);
        tempCard.addView(t1);
        TextView t2 = new TextView(this);
        t2.setText(String.format("%.0fC", temp));
        t2.setTextSize(16);
        t2.setTextColor(Color.parseColor("#00d4aa"));
        t2.setGravity(Gravity.CENTER);
        tempCard.addView(t2);

        avgCard.removeAllViews();
        TextView a1 = new TextView(this);
        a1.setText("Avg");
        a1.setTextSize(20);
        a1.setTextColor(Color.parseColor("#00d4aa"));
        a1.setGravity(Gravity.CENTER);
        avgCard.addView(a1);
        int avg = 0;
        if (!levelHistory.isEmpty()) {
            for (int i = 0; i < levelHistory.size(); i++) {
                avg += ((Integer)levelHistory.get(i)).intValue();
            }
            avg /= levelHistory.size();
        }
        TextView a2 = new TextView(this);
        a2.setText(avg + "%");
        a2.setTextSize(16);
        a2.setTextColor(Color.parseColor("#00d4aa"));
        a2.setGravity(Gravity.CENTER);
        avgCard.addView(a2);

        lastChargeText.setText("Last: " + sdf.format(new Date()));
    }

    private void checkAlarm(int level, boolean charging) {
        if (!alarmEnabled || alarmTriggered || !charging || level < chargingLimit) {
            if (!charging || level < chargingLimit - 5) alarmTriggered = false;
            return;
        }
        alarmTriggered = true;
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            Ringtone ringtone = RingtoneManager.getRingtone(this, alarmUri);
            ringtone.play();
        } catch (Exception e) {}
        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (v != null) v.vibrate(new long[]{0, 500, 200, 500}, -1);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notif = new Notification.Builder(this)
            .setContentTitle("MRX BatteryGuard")
            .setContentText("Battery reached " + level + "%!")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(Notification.PRIORITY_HIGH)
            .build();
        nm.notify(1, notif);
    }

    private void addHistory(int level, boolean charging) {
        levelHistory.add(Integer.valueOf(level));
        if (levelHistory.size() > 100) levelHistory.remove(0);
        String entry = sdf.format(new Date()) + " " + level + "% " + (charging ? "Charging" : "Idle");
        history.add(0, entry);
        if (history.size() > 20) history.remove(history.size() - 1);
    }

    private void showHistory() {
        historyContainer.removeAllViews();
        for (int i = 0; i < history.size(); i++) {
            TextView tv = new TextView(this);
            tv.setText((String)history.get(i));
            tv.setTextSize(13);
            tv.setTextColor(Color.parseColor("#888899"));
            tv.setPadding(0, 5, 0, 5);
            historyContainer.addView(tv);
        }
    }
}
