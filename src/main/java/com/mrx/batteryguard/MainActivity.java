package com.mrx.batteryguard;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        TextView tv = new TextView(this);
        tv.setText("MRX BatteryGuard Ready!");
        tv.setTextSize(28);
        tv.setPadding(50, 100, 50, 50);
        setContentView(tv);
    }
}
