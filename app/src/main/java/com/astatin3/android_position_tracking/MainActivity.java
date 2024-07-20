package com.astatin3.android_position_tracking;

import static android.util.Half.EPSILON;

import android.content.pm.ActivityInfo;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.content.Context;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    TextView textX, textY, textZ;
    Position o;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        textX = findViewById(R.id.textX);
        textY = findViewById(R.id.textY);
        textZ = findViewById(R.id.textZ);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        o = new Position(this);

//        o.startListening(ori_listener);
    }

    private Position.Listener ori_listener = new Position.Listener() {
        @Override
        public void onPositionChanged(float x, float y, float z) {
            textX.setText(String.valueOf((int)(x*100)));
            textY.setText(String.valueOf((int)(y*100)));
            textZ.setText(String.valueOf((int)(z*100)));
            System.out.println(x);
        }
    };

    public void onResume() {
        super.onResume();
        o.startListening(ori_listener);
//        sensorManager.registerListener(gyroListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }
    public void onStop() {
        super.onStop();
        o.stopListening();
//        sensorManager.unregisterListener(gyroListener);
    }
}