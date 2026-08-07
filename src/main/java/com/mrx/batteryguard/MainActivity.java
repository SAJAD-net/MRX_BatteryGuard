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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private TextView batteryPercent, chargingStatus, limitText, lastChargeText, alarmStatusText;
    private SeekBar limitSeekBar;
    private LinearLayout homeContent, historyContent, graphBar;
    private Button tabHome, tabHistory;
    private boolean alarmEnabled = true;
    private int chargingLimit = 80;
    private int currentLevel = 0;
    private boolean isCharging = false;
    private int previousLevel = -1;
    private List history = new ArrayList();
    private List levelHistory = new ArrayList();
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private SharedPreferences prefs;
    private Vibrator vibrator;
    private Ringtone ringtone;
    private boolean alarmActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("bg", MODE_PRIVATE);
        chargingLimit = prefs.getInt("limit", 80);
        alarmEnabled = prefs.getBoolean("alarm", true);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        setContentView(createRootUI());
        registerBatteryReceiver();
        switchTab(0);
    }

    private View createRootUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0a0a0f"));

        // HEADER
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(0, 45, 0, 18);
        header.setBackgroundColor(Color.parseColor("#0a0a0f"));
        TextView mrx = new TextView(this);
        mrx.setText("MRX"); mrx.setTextSize(26); mrx.setTextColor(Color.parseColor("#ff2244")); mrx.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(mrx);
        TextView rest = new TextView(this);
        rest.setText(" BatteryGuard"); rest.setTextSize(26); rest.setTextColor(Color.parseColor("#00ff88")); rest.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(rest);
        root.addView(header);

        // CONTENT AREA
        LinearLayout contentArea = new LinearLayout(this);
        contentArea.setOrientation(LinearLayout.VERTICAL);
        contentArea.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(contentArea);

        homeContent = createHomeTab();
        historyContent = createHistoryTab();

        // ABOUT - fixed at bottom above tabs
        LinearLayout aboutBar = new LinearLayout(this);
        aboutBar.setOrientation(LinearLayout.VERTICAL);
        aboutBar.setGravity(Gravity.CENTER);
        aboutBar.setBackgroundColor(Color.parseColor("#0a0a0f"));
        aboutBar.setPadding(0, 12, 0, 0);

        View line = new View(this);
        line.setLayoutParams(new LinearLayout.LayoutParams(-1, 1));
        line.setBackgroundColor(Color.parseColor("#1a3a2a"));
        aboutBar.addView(line);

        TextView aboutMrx = new TextView(this);
        aboutMrx.setText("MRX BatteryGuard");
        aboutMrx.setTextSize(12);
        aboutMrx.setTextColor(Color.parseColor("#558866"));
        aboutMrx.setGravity(Gravity.CENTER);
        aboutBar.addView(aboutMrx);

        TextView aboutBy = new TextView(this);
        aboutBy.setText("by Sajad Chehrazi");
        aboutBy.setTextSize(11);
        aboutBy.setTextColor(Color.parseColor("#ff2244"));
        aboutBy.setTypeface(Typeface.DEFAULT_BOLD);
        aboutBy.setGravity(Gravity.CENTER);
        aboutBy.setPadding(0, 2, 0, 8);
        aboutBar.addView(aboutBy);
        root.addView(aboutBar);

        // BOTTOM TABS
        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setBackgroundColor(Color.parseColor("#0a0a0f"));
        tabs.setPadding(0, 6, 0, 20);
        tabs.setGravity(Gravity.CENTER);

        tabHome = makeTab("[ HOME ]");
        tabHome.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { switchTab(0); } });
        tabs.addView(tabHome);
        tabHistory = makeTab("[ HISTORY ]");
        tabHistory.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { switchTab(1); } });
        tabs.addView(tabHistory);
        root.addView(tabs);

        return root;
    }

    private Button makeTab(String t) {
        Button b = new Button(this);
        b.setText(t); b.setTextSize(12); b.setTypeface(Typeface.MONOSPACE);
        b.setTextColor(Color.parseColor("#335544")); b.setBackgroundColor(Color.TRANSPARENT);
        b.setPadding(20, 10, 20, 10);
        b.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        return b;
    }

    private void switchTab(int n) {
        View root = (View) tabHome.getParent().getParent();
        LinearLayout contentArea = null;
        if (root instanceof LinearLayout) {
            contentArea = (LinearLayout) ((LinearLayout) root).getChildAt(1);
        }
        if (contentArea == null) return;
        contentArea.removeAllViews();
        tabHome.setTextColor(n == 0 ? Color.parseColor("#00ff88") : Color.parseColor("#335544"));
        tabHistory.setTextColor(n == 1 ? Color.parseColor("#00ff88") : Color.parseColor("#335544"));
        if (n == 0) contentArea.addView(homeContent);
        else { refreshHistoryTab(); contentArea.addView(historyContent); }
    }

    private LinearLayout createHomeTab() {
        ScrollView sv = new ScrollView(this);
        LinearLayout home = new LinearLayout(this);
        home.setOrientation(LinearLayout.VERTICAL);
        home.setPadding(25, 20, 25, 20);
        home.setBackgroundColor(Color.parseColor("#0a0a0f"));
        home.setGravity(Gravity.CENTER_HORIZONTAL);

        // Battery Circle
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.parseColor("#0d0d18"));
        circle.setStroke(5, Color.parseColor("#00ff88"));
        circle.setSize(200, 200);
        LinearLayout circleInner = new LinearLayout(this);
        circleInner.setOrientation(LinearLayout.VERTICAL);
        circleInner.setGravity(Gravity.CENTER);
        circleInner.setBackground(circle);
        circleInner.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
        batteryPercent = new TextView(this);
        batteryPercent.setText("--%"); batteryPercent.setTextSize(50); batteryPercent.setTextColor(Color.parseColor("#00ff88"));
        batteryPercent.setTypeface(Typeface.DEFAULT_BOLD); batteryPercent.setGravity(Gravity.CENTER);
        circleInner.addView(batteryPercent);
        chargingStatus = new TextView(this);
        chargingStatus.setText("..."); chargingStatus.setTextSize(12); chargingStatus.setTextColor(Color.parseColor("#558866"));
        chargingStatus.setGravity(Gravity.CENTER); chargingStatus.setPadding(0, 4, 0, 0);
        circleInner.addView(chargingStatus);
        LinearLayout circleWrap = new LinearLayout(this);
        circleWrap.setGravity(Gravity.CENTER); circleWrap.setPadding(0, 0, 0, 20);
        circleWrap.addView(circleInner);
        home.addView(circleWrap);

        // Alarm pill
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setColor(alarmEnabled ? Color.parseColor("#00ff88") : Color.parseColor("#1a1a2e"));
        pillBg.setCornerRadius(40); pillBg.setStroke(2, Color.parseColor("#00ff88"));
        LinearLayout pill = new LinearLayout(this);
        pill.setOrientation(LinearLayout.HORIZONTAL); pill.setGravity(Gravity.CENTER);
        pill.setBackground(pillBg); pill.setPadding(35, 14, 35, 14);
        pill.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { alarmEnabled = !alarmEnabled; updateAlarmPill(); saveSettings(); }
        });
        alarmStatusText = new TextView(this);
        alarmStatusText.setText(alarmEnabled ? "🔔 ALARM ON" : "🔕 ALARM OFF");
        alarmStatusText.setTextSize(14); alarmStatusText.setTextColor(Color.parseColor("#0a0a0f"));
        alarmStatusText.setTypeface(Typeface.DEFAULT_BOLD);
        pill.addView(alarmStatusText);
        LinearLayout pillWrap = new LinearLayout(this);
        pillWrap.setGravity(Gravity.CENTER); pillWrap.setPadding(0, 0, 0, 22);
        pillWrap.addView(pill);
        home.addView(pillWrap);

        // Target
        TextView targetLabel = new TextView(this);
        targetLabel.setText("TARGET CHARGE"); targetLabel.setTextSize(10);
        targetLabel.setTextColor(Color.parseColor("#335544")); targetLabel.setTypeface(Typeface.DEFAULT_BOLD);
        targetLabel.setGravity(Gravity.CENTER);
        home.addView(targetLabel);

        LinearLayout limitRow = new LinearLayout(this);
        limitRow.setOrientation(LinearLayout.HORIZONTAL); limitRow.setGravity(Gravity.CENTER); limitRow.setPadding(0, 5, 0, 5);
        Button minusBtn = circleBtn("-");
        minusBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { chargingLimit = Math.max(10, chargingLimit - 5); limitSeekBar.setProgress(chargingLimit - 10); limitText.setText(chargingLimit + "%"); saveSettings(); }
        });
        limitRow.addView(minusBtn);
        limitText = new TextView(this);
        limitText.setText(chargingLimit + "%"); limitText.setTextSize(38); limitText.setTextColor(Color.parseColor("#00ff88"));
        limitText.setTypeface(Typeface.DEFAULT_BOLD); limitText.setGravity(Gravity.CENTER); limitText.setPadding(20, 0, 20, 0);
        limitRow.addView(limitText);
        Button plusBtn = circleBtn("+");
        plusBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { chargingLimit = Math.min(100, chargingLimit + 5); limitSeekBar.setProgress(chargingLimit - 10); limitText.setText(chargingLimit + "%"); saveSettings(); }
        });
        limitRow.addView(plusBtn);
        home.addView(limitRow);

        limitSeekBar = new SeekBar(this);
        limitSeekBar.setMax(90); limitSeekBar.setProgress(chargingLimit - 10); limitSeekBar.setPadding(30, 0, 30, 0);
        limitSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) { chargingLimit = p + 10; limitText.setText(chargingLimit + "%"); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) { saveSettings(); }
        });
        home.addView(limitSeekBar);

        // Presets
        LinearLayout presets = new LinearLayout(this);
        presets.setOrientation(LinearLayout.HORIZONTAL); presets.setGravity(Gravity.CENTER); presets.setPadding(0, 10, 0, 15);
        int[] vals = {50, 60, 70, 80};
        for (int i = 0; i < vals.length; i++) {
            final int v = vals[i];
            Button b = new Button(this);
            b.setText(v + "%"); b.setTextSize(12); b.setTextColor(Color.parseColor("#558866")); b.setBackgroundColor(Color.parseColor("#0d0d18")); b.setPadding(14, 8, 14, 8);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v2) { chargingLimit = v; limitSeekBar.setProgress(v - 10); limitText.setText(v + "%"); saveSettings(); }
            });
            presets.addView(b);
        }
        home.addView(presets);

        lastChargeText = new TextView(this);
        lastChargeText.setText("--°C"); lastChargeText.setTextSize(13); lastChargeText.setTextColor(Color.parseColor("#558866"));
        lastChargeText.setGravity(Gravity.CENTER); lastChargeText.setPadding(0, 0, 0, 15);
        home.addView(lastChargeText);

        Button stopBtn = new Button(this);
        stopBtn.setText("STOP ALARM"); stopBtn.setTextSize(14); stopBtn.setTextColor(Color.WHITE); stopBtn.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable stopBg = new GradientDrawable();
        stopBg.setColor(Color.parseColor("#ff3344")); stopBg.setCornerRadius(25);
        stopBtn.setBackground(stopBg); stopBtn.setPadding(0, 14, 0, 14);
        stopBtn.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        stopBtn.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { stopAlarm(); } });
        home.addView(stopBtn);

        sv.addView(home);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setGravity(Gravity.CENTER_HORIZONTAL);
        wrapper.addView(sv);
        return wrapper;
    }

    private LinearLayout createHistoryTab() {
        ScrollView sv = new ScrollView(this);
        LinearLayout hist = new LinearLayout(this);
        hist.setOrientation(LinearLayout.VERTICAL); hist.setPadding(20, 20, 20, 20);
        hist.setBackgroundColor(Color.parseColor("#0a0a0f")); hist.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView title = new TextView(this);
        title.setText("CHARGING HISTORY"); title.setTextSize(14); title.setTextColor(Color.parseColor("#00ff88"));
        title.setTypeface(Typeface.DEFAULT_BOLD); title.setGravity(Gravity.CENTER); title.setPadding(0, 0, 0, 10);
        hist.addView(title);
        graphBar = new LinearLayout(this);
        graphBar.setOrientation(LinearLayout.HORIZONTAL); graphBar.setGravity(Gravity.BOTTOM);
        graphBar.setPadding(0, 0, 0, 15);
        graphBar.setLayoutParams(new LinearLayout.LayoutParams(-1, 150));
        hist.addView(graphBar);
        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL); listContainer.setTag("list"); listContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        hist.addView(listContainer);
        sv.addView(hist);
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setGravity(Gravity.CENTER_HORIZONTAL);
        wrapper.addView(sv);
        return wrapper;
    }

    private void updateAlarmPill() {
        if (alarmStatusText == null) return;
        View pill = (View) alarmStatusText.getParent();
        if (pill == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(alarmEnabled ? Color.parseColor("#00ff88") : Color.parseColor("#1a1a2e"));
        bg.setCornerRadius(40); bg.setStroke(2, Color.parseColor("#00ff88"));
        pill.setBackground(bg);
        alarmStatusText.setText(alarmEnabled ? "🔔 ALARM ON" : "🔕 ALARM OFF");
    }

    private Button circleBtn(String text) {
        Button btn = new Button(this);
        btn.setText(text); btn.setTextSize(18); btn.setTextColor(Color.parseColor("#00ff88"));
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL); bg.setColor(Color.parseColor("#0d0d18")); bg.setStroke(2, Color.parseColor("#00ff88"));
        btn.setBackground(bg);
        btn.setLayoutParams(new LinearLayout.LayoutParams(55, 55));
        return btn;
    }

    private void refreshHistoryTab() {
        LinearLayout list = (LinearLayout) findViewByTag(historyContent, "list");
        if (list == null) return;
        list.removeAllViews();
        if (graphBar != null) {
            graphBar.removeAllViews();
            int max = 0;
            for (int i = 0; i < levelHistory.size(); i++) { int l = ((Integer) levelHistory.get(i)).intValue(); if (l > max) max = l; }
            if (max == 0) max = 100;
            int count = Math.min(levelHistory.size(), 30);
            int start = levelHistory.size() - count;
            for (int i = start; i < levelHistory.size(); i++) {
                int l = ((Integer) levelHistory.get(i)).intValue();
                View bar = new View(this);
                int h = (int) (120 * l / (float) max);
                bar.setLayoutParams(new LinearLayout.LayoutParams(8, h > 2 ? h : 2));
                if (l >= chargingLimit) bar.setBackgroundColor(Color.parseColor("#ff3344"));
                else if (l >= 80) bar.setBackgroundColor(Color.parseColor("#ffdd00"));
                else bar.setBackgroundColor(Color.parseColor("#00ff88"));
                graphBar.addView(bar);
            }
        }
        if (history.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No data yet"); tv.setTextSize(12); tv.setTextColor(Color.parseColor("#335544"));
            tv.setGravity(Gravity.CENTER); tv.setPadding(0, 20, 0, 0);
            list.addView(tv); return;
        }
        int show = Math.min(history.size(), 20);
        for (int i = 0; i < show; i++) {
            TextView tv = new TextView(this);
            tv.setText("▸ " + (String) history.get(i));
            tv.setTextSize(11); tv.setTextColor(Color.parseColor("#558866")); tv.setPadding(4, 4, 4, 4); tv.setGravity(Gravity.CENTER);
            list.addView(tv);
        }
    }

    private View findViewByTag(View parent, String tag) {
        if (parent == null) return null;
        if (tag.equals(parent.getTag())) return parent;
        if (parent instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) parent;
            for (int i = 0; i < vg.getChildCount(); i++) {
                View found = findViewByTag(vg.getChildAt(i), tag);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void registerBatteryReceiver() {
        registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context c, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (scale > 0) currentLevel = (level * 100) / scale;
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);
                float temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f;
                updateUI(currentLevel, isCharging, temp);
                checkAlarm(currentLevel, isCharging);
                if (currentLevel != previousLevel) { addHistory(currentLevel, isCharging); previousLevel = currentLevel; }
            }
        }, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private void updateUI(int level, boolean charging, float temp) {
        if (batteryPercent == null) return;
        batteryPercent.setText(level + "%");
        int color;
        if (charging) color = Color.parseColor("#ffdd00");
        else if (level < 20) color = Color.parseColor("#ff3344");
        else if (level < 50) color = Color.parseColor("#ffa500");
        else color = Color.parseColor("#00ff88");
        batteryPercent.setTextColor(color);
        chargingStatus.setText(charging ? "CHARGING" : "IDLE");
        if (lastChargeText != null) lastChargeText.setText(String.format("%.0f°C", temp));
    }

    private void checkAlarm(int level, boolean charging) {
        if (!alarmEnabled || !charging || level < chargingLimit || alarmActive) return;
        triggerAlarm(level);
    }

    private void triggerAlarm(int level) {
        alarmActive = true;
        try { ringtone = RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)); if (ringtone != null) ringtone.play(); } catch (Exception e) {}
        if (vibrator != null) vibrator.vibrate(new long[]{0, 500, 200, 500}, 0);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(1, new Notification.Builder(this)
            .setContentTitle("MRX BatteryGuard").setContentText("Battery at " + level + "%! Unplug.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert).setPriority(Notification.PRIORITY_HIGH).setAutoCancel(true).build());
        new AlertDialog.Builder(this).setTitle("TARGET REACHED").setMessage("Battery: " + level + "%\n\nUnplug charger.")
            .setPositiveButton("STOP", new android.content.DialogInterface.OnClickListener() { public void onClick(android.content.DialogInterface d, int w) { stopAlarm(); } })
            .setCancelable(false).show();
    }

    private void stopAlarm() {
        alarmActive = false;
        if (ringtone != null) { ringtone.stop(); ringtone = null; }
        if (vibrator != null) vibrator.cancel();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(1);
    }

    private void addHistory(int level, boolean charging) {
        levelHistory.add(Integer.valueOf(level));
        if (levelHistory.size() > 200) levelHistory.remove(0);
        history.add(0, sdf.format(new Date()) + " → " + level + "% " + (charging ? "⚡" : "🔋"));
        if (history.size() > 50) history.remove(history.size() - 1);
    }

    private void saveSettings() {
        prefs.edit().putInt("limit", chargingLimit).putBoolean("alarm", alarmEnabled).commit();
    }
}
