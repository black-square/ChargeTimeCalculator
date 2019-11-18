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
    public static final String BatteryCurrentLevel2 = "BatteryCurrentLevel2";
    public static final String BatteryTargetLevel2 = "BatteryTargetLevel2";
    public static final String BatteryChargeSpeed2 = "BatteryChargeSpeed2";

    private Timer mUpdateTimer;
    private TextView mResultTime;
    private TextView mDuration;
    private TextInputEditText mBatteryTargetLevel;
    private TextInputEditText mBatteryChargeSpeed;

    private TextView mResultTime2;
    private TextView mDuration2;
    private TextInputEditText mBatteryCurrentLevel2;
    private TextInputEditText mBatteryTargetLevel2;
    private TextInputEditText mBatteryChargeSpeed2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mResultTime = findViewById(R.id.ResultTime);
        mDuration = findViewById(R.id.Duration);
        mBatteryTargetLevel = findViewById(R.id.BatteryTargetLevel);
        mBatteryChargeSpeed = findViewById(R.id.BatteryChargeSpeed);

        mResultTime2 = findViewById(R.id.ResultTime2);
        mDuration2 = findViewById(R.id.Duration2);
        mBatteryCurrentLevel2 = findViewById(R.id.BatteryCurrentLevel2);
        mBatteryTargetLevel2 = findViewById(R.id.BatteryTargetLevel2);
        mBatteryChargeSpeed2 = findViewById(R.id.BatteryChargeSpeed2);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        mBatteryTargetLevel.setText( settings.getString(BatteryTargetLevel, "70" ));
        mBatteryChargeSpeed.setText( settings.getString(BatteryChargeSpeed, "30" ));
        mBatteryCurrentLevel2.setText( settings.getString(BatteryCurrentLevel2, "42" ));
        mBatteryTargetLevel2.setText( settings.getString(BatteryTargetLevel2, "70" ));
        mBatteryChargeSpeed2.setText( settings.getString(BatteryChargeSpeed2, "30" ));

        TextWatcher textWatcher = new TextWatcher() {
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
        };

        mBatteryTargetLevel.addTextChangedListener(textWatcher);
        mBatteryChargeSpeed.addTextChangedListener(textWatcher);
        mBatteryCurrentLevel2.addTextChangedListener(textWatcher);
        mBatteryTargetLevel2.addTextChangedListener(textWatcher);
        mBatteryChargeSpeed2.addTextChangedListener(textWatcher);

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
        editor.putString(BatteryCurrentLevel2, mBatteryCurrentLevel2.getText().toString());
        editor.putString(BatteryTargetLevel2, mBatteryTargetLevel2.getText().toString());
        editor.putString(BatteryChargeSpeed2, mBatteryChargeSpeed2.getText().toString());

        editor.commit();
    }

    private float currentBatteryLevel() {
        //https://stackoverflow.com/a/15746919/3415353
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, intentFilter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        return level / (float)scale * 100;
    }

    private static String formatDifference(Date startDate, Date endDate) {
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

    private interface FloatSupplier {
        float getAsFloat();
    }

    private static void updateImpl(FloatSupplier fsCurrentLevel, TextView tvTargetLevel,
                                   TextView tvChargeSpeed, TextView tvResultTime,
                                   TextView tvDuration)
    {
        try {
            float curLevel = fsCurrentLevel.getAsFloat();
            float targetLevel = Float.parseFloat(tvTargetLevel.getText().toString());
            float chargeSpeed = Float.parseFloat(tvChargeSpeed.getText().toString());

            if (curLevel >= targetLevel) {
                throw new Exception("Charged");
            }

            float chargeTime = (targetLevel - curLevel) / chargeSpeed * 60 * 60;

            Calendar c = Calendar.getInstance();

            Date curTime = c.getTime();
            c.add(Calendar.SECOND, (int) chargeTime);

            SimpleDateFormat df = new SimpleDateFormat("h:mm a");
            String formattedDate = df.format(c.getTime());

            tvResultTime.setText(formattedDate);
            tvDuration.setText(formatDifference(curTime, c.getTime()));
        }
        catch(Exception e)
        {
            tvResultTime.setText("-----");
            tvDuration.setText(e.getMessage());
        }
    }

    private void update() {
        updateImpl(() -> currentBatteryLevel(), mBatteryTargetLevel,
                mBatteryChargeSpeed, mResultTime, mDuration);

        updateImpl(() -> Float.parseFloat(mBatteryCurrentLevel2.getText().toString()),
                mBatteryTargetLevel2, mBatteryChargeSpeed2, mResultTime2, mDuration2);
    }
}
