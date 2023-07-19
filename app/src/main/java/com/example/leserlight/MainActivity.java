package com.example.leserlight;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.Manifest.permission_group.CAMERA;
import static android.os.Build.VERSION.SDK_INT;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.rtugeek.android.colorseekbar.ColorSeekBar;
import com.rtugeek.android.colorseekbar.OnColorChangeListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PictureCallback {

    private ImageView img_leser, img_leserlight, press_flash, press_vibrate, press_volume, press_camera;
    private String[] neededPermissions = new String[]{CAMERA, WRITE_EXTERNAL_STORAGE};
    private boolean isFlashlightOn = false;
    private Camera.Parameters parameters;
    private SurfaceHolder surfaceHolder;
    private SensorManager sensorManager;
    private CameraManager cameraManager;
    private ColorSeekBar colorSeekBar;
    private SurfaceView surfaceView;
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    boolean isVibrate = true;
    private String cameraId;
    boolean isCamera = true;
    boolean isVolume = true;
    boolean isCheck = true;
    private Camera camera;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        img_leser = findViewById(R.id.img_leser);
        press_flash = findViewById(R.id.press_flash);
        press_camera = findViewById(R.id.press_camera);
        press_volume = findViewById(R.id.press_volume);
        surfaceView = findViewById(R.id.surfaceView);
        press_vibrate = findViewById(R.id.press_vibrate);
        colorSeekBar = findViewById(R.id.color_seek_bar);
        img_leserlight = findViewById(R.id.img_leserlight);

        mediaPlayer = MediaPlayer.create(this, R.raw.blue_laser);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        press_camera.setOnClickListener(view -> {
            if (isCamera) {
                press_camera.setSelected(true);
                checkPermission();
                surfaceView.setVisibility(View.VISIBLE);
                if (isCamera) {
                    isCamera = true;
                }
                isCamera = false;
            } else {
                press_camera.setSelected(false);
                surfaceView.setVisibility(View.GONE);
                if (isCamera) {
                    isCamera = false;
                }
                isCamera = true;
            }
        });

        press_volume.setOnClickListener(view -> {
            if (isVolume) {
                press_volume.setSelected(true);
                SharedPreferences sharedPreferences = getSharedPreferences("MyShared", MODE_PRIVATE);
                SharedPreferences.Editor myEdit = sharedPreferences.edit();
                myEdit.putBoolean("j2", true);
                myEdit.apply();
                isVolume = false;
            } else {
                press_volume.setSelected(false);
                SharedPreferences sharedPreferences = getSharedPreferences("MyShared", MODE_PRIVATE);
                SharedPreferences.Editor myEdit = sharedPreferences.edit();
                myEdit.putBoolean("j2", false);
                myEdit.apply();
                mediaPlayer.stop();
                isVolume = true;
            }
        });

        press_flash.setOnClickListener(v -> {
            if (isCheck) {
                press_flash.setSelected(false);
                turnFlashlightOn();
                isCheck = false;
            } else {
                press_flash.setSelected(true);
                isCheck = true;
                turnFlashlightOff();
            }
            isFlashlightOn = !isFlashlightOn;
        });

        press_vibrate.setOnClickListener(view -> {
            press_vibrate.setSelected(!press_vibrate.isSelected());
            if (press_vibrate.isSelected()) {
                SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE);
                SharedPreferences.Editor myEdit = sharedPreferences.edit();
                myEdit.putBoolean("s1", true);
                myEdit.apply();
                isVibrate = true;
                enableVibration();
            } else {
                SharedPreferences sharedPreferences = getSharedPreferences("MySharedPref", MODE_PRIVATE);
                SharedPreferences.Editor myEdit = sharedPreferences.edit();
                myEdit.putBoolean("s1", false);
                myEdit.apply();
                isVibrate = false;
                disableVibration();
            }
        });

        img_leser.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    img_leserlight.setVisibility(View.VISIBLE);
                    if (isVolume) {
                        isVolume = true;
                    } else {
                        stopAndPlay(R.raw.blue_laser, mediaPlayer);
                        mediaPlayer.start();
                        isVolume = false;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    img_leserlight.setVisibility(View.INVISIBLE);
                    mediaPlayer.stop();
                    break;
            }
            if (press_vibrate.isSelected()) {
                enableVibration();
            }
            return true;
        });

        colorSeekBar.setOnColorChangeListener((progress, color) -> img_leserlight.setColorFilter(color));

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(listener);
        }
    }

    private final SensorEventListener listener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float xValue = Math.abs(event.values[0]);
            float yValue = Math.abs(event.values[1]);
            float zValue = Math.abs(event.values[2]);
            if (xValue > 15 || yValue > 15 || zValue > 15) {
                if (isCheck) {
                    img_leserlight.setVisibility(View.VISIBLE);
                    if (!isVolume) {
                        stopAndPlay(R.raw.blue_laser, mediaPlayer);
                        mediaPlayer.start();
                    }
//                    turnFlashlightOn();
                    isCheck = false;
                } else {
                    img_leserlight.setVisibility(View.INVISIBLE);
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.stop();
                    }
//                    turnFlashlightOff();
                    isCheck = true;
                }
                if (press_vibrate.isSelected()) {
                    enableVibration();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    private void enableVibration() {
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(100);
        }
    }

    private void disableVibration() {
        vibrator.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sh = getSharedPreferences("MySharedPref", MODE_PRIVATE);
        boolean isVibrate = sh.getBoolean("s1", false);
        press_vibrate.setSelected(isVibrate);

        SharedPreferences sharedPref = getSharedPreferences("MyShared", MODE_PRIVATE);
        boolean isVolume = sharedPref.getBoolean("j2", false);
        press_volume.setSelected(isVolume);
    }

    private void turnFlashlightOn() {
        if (press_flash.isSelected()) {
            try {
                if (SDK_INT >= Build.VERSION_CODES.M) {
                    cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                    cameraId = cameraManager.getCameraIdList()[0];
                    cameraManager.setTorchMode(cameraId, true);
                } else {
                    camera = Camera.open();
                    parameters = camera.getParameters();
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    camera.setParameters(parameters);
                    camera.startPreview();
                }
            } catch (CameraAccessException | RuntimeException e) {
                e.printStackTrace();
            }
        }
        if (isCamera) {
            camera = Camera.open();
            parameters = camera.getParameters();
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            camera.setParameters(parameters);
            camera.startPreview();
            isCamera = true;
        } else {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(parameters);
            camera.stopPreview();
            camera.release();
            isCamera = false;
        }
    }

    private void turnFlashlightOff() {
        try {
            if (SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager.setTorchMode(cameraId, false);
            } else {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                camera.setParameters(parameters);
                camera.stopPreview();
                camera.release();
            }
        } catch (CameraAccessException | RuntimeException e) {
        }
    }

    private void stopAndPlay(int rawId, @NonNull MediaPlayer mediaPlayer) {
        mediaPlayer.reset();
        AssetFileDescriptor afd = this.getResources().openRawResourceFd(rawId);
        try {
            mediaPlayer.setDataSource((afd).getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.start();
    }

    public void checkPermission() {
        List<String> permissionList = new ArrayList<>();

        if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionList.add(Permission.CAMERA);
        } else {
            permissionList.add(Permission.CAMERA);
        }

        XXPermissions.with(this).permission(permissionList).interceptor(new PermissionAllow()).request((permissions, allGranted) -> {
            if (allGranted) {
                setupSurfaceHolder();
            }
        });
    }

    private void setViewVisibility(int id, int visibility) {
        View view = findViewById(id);
        if (view != null) {
            view.setVisibility(visibility);
        }
    }

    private void setupSurfaceHolder() {
        setViewVisibility(R.id.press_camera, View.VISIBLE);
        setViewVisibility(R.id.surfaceView, View.VISIBLE);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
    }

    @Override
    public void onPictureTaken(byte[] bytes, Camera camera) {
        resetCamera();
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        startCamera();
    }

    private void startCamera() {
        camera = Camera.open();
        camera.setDisplayOrientation(90);
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        resetCamera();
    }

    public void resetCamera() {
        if (surfaceHolder.getSurface() == null) {
            return;
        }

        if (camera != null) {
            camera.stopPreview();
            try {
                camera.setPreviewDisplay(surfaceHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            camera.startPreview();
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        releaseCamera();
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }
}