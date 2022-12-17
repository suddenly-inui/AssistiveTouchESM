package com.android.mirror.assisttouch.main;

import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.app.usage.UsageEvents;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import com.android.mirror.assisttouch.MyAdminReceiver;
import com.android.mirror.assisttouch.service.AssistiveTouchService;
import com.android.mirror.assisttouch.R;
import com.android.mirror.assisttouch.utils.SystemsUtils;


import com.awareframework.android.sensor.aware_appusage.AppusageSensor;
import com.awareframework.android.core.db.Engine;
import com.awareframework.android.sensor.aware_appusage.model.AppusageData;

public class MainActivity extends AppCompatActivity {

    private Button startBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("Mirror", SystemsUtils.isRooted() + " ");

        ComponentName componentName = new ComponentName(this, MyAdminReceiver.class);
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
        startActivityForResult(intent, 0);

        startBtn = (Button)findViewById(R.id.startBtn);
        startBtn.setOnClickListener(v -> {
            Intent intent1 = new Intent(MainActivity.this, AssistiveTouchService.class);
            if(isMyServiceRunning(AssistiveTouchService.class))
                stopService(intent1);
            else
                startService(intent1);
        });
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
