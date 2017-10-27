package io.github.black_square.chargetimecalculator;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "MyPrefsFile";
    public static final String BatteryTargetLevel = "BatteryTargetLevel";
    public static final String BatteryChargeSpeed = "BatteryChargeSpeed";

    private Timer mUpdateTimer;
    private TextView mResultTime;
    private TextView mDuration;
    private TextInputEditText mBatteryTargetLevel;
    private TextInputEditText mBatteryChargeSpeed;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mResultTime = findViewById(R.id.ResultTime);
        mDuration = findViewById(R.id.Duration);
        mBatteryTargetLevel = findViewById(R.id.BateryTargetLevel);
        mBatteryChargeSpeed = findViewById(R.id.BateryChargeSpeed);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        mBatteryTargetLevel.setText( settings.getString(BatteryTargetLevel, "70" ));
        mBatteryChargeSpeed.setText( settings.getString(BatteryChargeSpeed, "30" ));

        mBatteryTargetLevel.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                update();
            }
        });

        mBatteryChargeSpeed.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                update();
            }
        });

        mUpdateTimer = new Timer();
        mUpdateTimer.schedule(new TimerTask() {
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        update();
                    }
                });
            }
        }, 0, 1 * 60 * 1000);
    }
    @Override
    protected void onStop(){
        super.onStop();

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(BatteryTargetLevel, mBatteryTargetLevel.getText().toString());
        editor.putString(BatteryChargeSpeed, mBatteryChargeSpeed.getText().toString());

        editor.commit();
    }


    private float currentBatteryLevel() {
        //https://stackoverflow.com/a/15746919/3415353
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        return level / (float)scale * 100;
    }

    public String formatDifference(Date startDate, Date endDate) {
        //milliseconds
        long different = endDate.getTime() - startDate.getTime();

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = different / daysInMilli;
        different = different % daysInMilli;

        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;

        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;

        long elapsedSeconds = different / secondsInMilli;

        return String.format(
                "%d days, %d hours, %d minutes, %d seconds",
                elapsedDays, elapsedHours, elapsedMinutes, elapsedSeconds);
    }
    private void update() {

        try {
            float targetLevel = Float.parseFloat(mBatteryTargetLevel.getText().toString());
            float chargeSpeed = Float.parseFloat(mBatteryChargeSpeed.getText().toString());
            float curLevel = currentBatteryLevel();

            if (curLevel >= targetLevel) {
                throw new Exception("Charged");
            }

            float chargeTime = (targetLevel - curLevel) / chargeSpeed * 60 * 60;

            Calendar c = Calendar.getInstance();

            Date curTime = c.getTime();
            c.add(Calendar.SECOND, (int) chargeTime);

            SimpleDateFormat df = new SimpleDateFormat("h:mm a");
            String formattedDate = df.format(c.getTime());

            mResultTime.setText(formattedDate);
            mDuration.setText(formatDifference(curTime, c.getTime()));
        }
        catch(Exception e)
        {
            mResultTime.setText("-----");
            mDuration.setText(e.getMessage());
        }
    }
}
