package com.astatin3.android_position_tracking;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.WindowManager;

import androidx.annotation.Nullable;

public class Position implements SensorEventListener {
    public interface Listener {
        void onPositionChanged(float x, float y, float z);
    }
    private Orientation o;
    float[] cur_rotation;
    private final SensorManager mSensorManager;
    private static final int SENSOR_DELAY_MICROS = 16 * 1000; // 16ms
    @Nullable
    private final Sensor mAccelSensor;

    private int mLastAccuracy;
    private Listener mListener;

//    private float[] acceleration = new float[3]; // Accelerometer values
//    private float[] rotation = new float[4]; // Gyroscope values
//    private float[] globalAcceleration = new float[3]; // Global accelerometer values
//    private float[] position = new float[3]; // Device position

    public Position(Activity activity){
//        mWindowManager = activity.getWindow().getWindowManager();
        mSensorManager = (SensorManager) activity.getSystemService(Activity.SENSOR_SERVICE);

        // Can be null if the sensor hardware is not available
        mAccelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        o = new Orientation(activity);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (mListener == null) {
            return;
        }
        if (mLastAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return;
        }
        if (event.sensor == mAccelSensor) {
            updateAccel(event.values);
        }
    }

    public void stopListening() {
        mSensorManager.unregisterListener(this);
        mListener = null;
        o.stopListening();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (mLastAccuracy != accuracy) {
            mLastAccuracy = accuracy;
        }
    }

    public void startListening(Position.Listener listener) {
        if (mListener == listener) {
            return;
        }
        mListener = listener;
        if (mAccelSensor == null) {
            System.out.println("Rotation vector sensor not available; will not provide orientation data.");
            return;
        }

        o.startListening(new Orientation.Listener() {
            @Override
            public void onOrientationChanged(float[] rotation) {
                cur_rotation = rotation;
            }
        });

        mSensorManager.registerListener(this, mAccelSensor, SENSOR_DELAY_MICROS);
    }

    private float[] position = {0, 0, 0};
    private float[] velocity = {0, 0, 0};
    private float lastUpdateTime = 0;
    private float[] lastAccel = {0, 0, 0};
    private static final float ALPHA = 0.1f; // Low-pass filter factor
    private static final float NOISE_THRESHOLD = 0.1f; // Noise threshold

    public void updateAccel(float[] accelVector) {
        float currentTime = System.nanoTime() / 1e9f; // Convert to seconds
        float deltaTime = currentTime - lastUpdateTime;

        if (lastUpdateTime == 0) {
            lastUpdateTime = currentTime;
            System.arraycopy(accelVector, 0, lastAccel, 0, 3);
            return;
        }

        // Apply low-pass filter
        for (int i = 0; i < 3; i++) {
            accelVector[i] = lowPassFilter(lastAccel[i], accelVector[i]);
        }

        // Apply threshold-based noise reduction
        for (int i = 0; i < 3; i++) {
            if (Math.abs(accelVector[i]) < NOISE_THRESHOLD) {
                accelVector[i] = 0;
            }
        }

        accelVector = rotatePosition(accelVector, cur_rotation);

        for (int i = 0; i < 3; i++) {
            // Update velocity: v = v0 + a * t
            velocity[i] += accelVector[i] * deltaTime;

            // Update position: x = x0 + v * t + (1/2) * a * t^2
            position[i] += velocity[i] * deltaTime + 0.5f * accelVector[i] * deltaTime * deltaTime;
        }

        lastUpdateTime = currentTime;
        System.arraycopy(accelVector, 0, lastAccel, 0, 3);

        mListener.onPositionChanged(position[0], position[1], position[2]);
    }

    private float lowPassFilter(float lastValue, float currentValue) {
        return ALPHA * lastValue + (1 - ALPHA) * currentValue;
    }

    public static float[] rotatePosition(float[] position, float[] rotation) {
        if (position.length != 3 || rotation.length != 3) {
            throw new IllegalArgumentException("Both position and rotation must have 3 components");
        }

        // Convert degrees to radians
        float pitch = (float) Math.toRadians(rotation[0]);
        float yaw = (float) Math.toRadians(rotation[1]);
        float roll = (float) Math.toRadians(rotation[2]);

        // Precompute trigonometric functions
        float cp = (float) Math.cos(pitch);
        float sp = (float) Math.sin(pitch);
        float cy = (float) Math.cos(yaw);
        float sy = (float) Math.sin(yaw);
        float cr = (float) Math.cos(roll);
        float sr = (float) Math.sin(roll);

        // Create rotation matrix
        float[][] rotMatrix = new float[3][3];
        rotMatrix[0][0] = cy * cr + sy * sp * sr;
        rotMatrix[0][1] = -cy * sr + sy * sp * cr;
        rotMatrix[0][2] = sy * cp;
        rotMatrix[1][0] = sr * cp;
        rotMatrix[1][1] = cr * cp;
        rotMatrix[1][2] = -sp;
        rotMatrix[2][0] = -sy * cr + cy * sp * sr;
        rotMatrix[2][1] = sy * sr + cy * sp * cr;
        rotMatrix[2][2] = cy * cp;

        // Apply rotation
        float[] rotatedPosition = new float[3];
        for (int i = 0; i < 3; i++) {
            rotatedPosition[i] =
                    rotMatrix[i][0] * position[0] +
                            rotMatrix[i][1] * position[1] +
                            rotMatrix[i][2] * position[2];
        }

        return rotatedPosition;
    }
    // Add a calibration method to remove initial offset
//    public void calibrate(int numSamples) {
//        float[] sumAccel = {0, 0, 0};
//        for (int i = 0; i < numSamples; i++) {
//            float[] sample = getSensorReading(); // Implement this method to get raw sensor data
//            for (int j = 0; j < 3; j++) {
//                sumAccel[j] += sample[j];
//            }
//        }
//        for (int i = 0; i < 3; i++) {
//            sumAccel[i] /= numSamples;
//        }
//        // Store this average as the zero offset and subtract it from future readings
//    }
}
