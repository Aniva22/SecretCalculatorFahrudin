package com.example.secretcalculatorfahrudin;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private EditText etNumber1, etNumber2;
    private TextView tvResult;
    private Button btnAdd, btnSubtract, btnMultiply, btnDivide, btnCalculate;
    private ImageView ivImage;
    private String operation = "";
    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope;
    private FusedLocationProviderClient fusedLocationClient;
    private final int[] nimDigits = {0, 2, 9, 0, 9}; // 5 digit terakhir NIM
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<String> requestLocationPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inisialisasi UI
        etNumber1 = findViewById(R.id.etNumber1);
        etNumber2 = findViewById(R.id.etNumber2);
        tvResult = findViewById(R.id.tvResult);
        btnAdd = findViewById(R.id.btnAdd);
        btnSubtract = findViewById(R.id.btnSubtract);
        btnMultiply = findViewById(R.id.btnMultiply);
        btnDivide = findViewById(R.id.btnDivide);
        btnCalculate = findViewById(R.id.btnCalculate);
        ivImage = findViewById(R.id.ivImage);

        // Inisialisasi sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // Inisialisasi lokasi
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Inisialisasi permission launcher
        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        openCamera();
                    } else {
                        Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show();
                    }
                });

        requestLocationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        showLocation();
                    } else {
                        Toast.makeText(this, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show();
                    }
                });

        // Set listener untuk tombol operasi
        btnAdd.setOnClickListener(v -> operation = "+");
        btnSubtract.setOnClickListener(v -> operation = "-");
        btnMultiply.setOnClickListener(v -> operation = "*");
        btnDivide.setOnClickListener(v -> operation = "/");
        btnCalculate.setOnClickListener(v -> performCalculation());
    }

    private void performCalculation() {
        String num1Str = etNumber1.getText().toString();
        String num2Str = etNumber2.getText().toString();

        if (num1Str.isEmpty() || num2Str.isEmpty() || operation.isEmpty()) {
            Toast.makeText(this, "Masukkan angka dan pilih operasi", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double num1 = Double.parseDouble(num1Str);
            double num2 = Double.parseDouble(num2Str);
            double result;

            switch (operation) {
                case "+":
                    result = num1 + num2;
                    break;
                case "-":
                    result = num1 - num2;
                    break;
                case "*":
                    result = num1 * num2;
                    break;
                case "/":
                    if (num2 == 0) {
                        Toast.makeText(this, "Tidak dapat membagi dengan nol", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    result = num1 / num2;
                    break;
                default:
                    Toast.makeText(this, "Operasi tidak valid", Toast.LENGTH_SHORT).show();
                    return;
            }

            tvResult.setText("Hasil: " + result);
            checkResult((int) Math.floor(result));

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Masukkan angka yang valid", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkResult(int result) {
        ivImage.setVisibility(View.GONE); // Reset gambar

        // Karena 0 dan 9 muncul dua kali, kita gunakan flag untuk melacak fitur yang sudah dipicu
        boolean cameraTriggered = false;
        boolean gpsTriggered = false;
        boolean sensorTriggered = false;

        for (int i = 0; i < nimDigits.length; i++) {
            if (result == nimDigits[i]) {
                switch (i) {
                    case 0: // Digit 1: 0 (Kamera)
                        if (!cameraTriggered) {
                            openCamera();
                            cameraTriggered = true;
                        }
                        return;
                    case 1: // Digit 2: 2 (Audio)
                        playAudio();
                        return;
                    case 2: // Digit 3: 9 (Gambar)
                        showImage();
                        return;
                    case 3: // Digit 4: 0 (GPS)
                        if (!gpsTriggered && cameraTriggered) {
                            showLocation();
                            gpsTriggered = true;
                        }
                        return;
                    case 4: // Digit 5: 9 (Sensor)
                        if (!sensorTriggered) {
                            showSensors();
                            sensorTriggered = true;
                        }
                        return;
                }
            }
        }
        Toast.makeText(this, "Hasil tidak cocok dengan digit NIM", Toast.LENGTH_SHORT).show();
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            Toast.makeText(this, "Perangkat tidak mendukung kamera", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                Log.d("SecretCalculator", "Membuka aplikasi kamera");
                startActivityForResult(cameraIntent, 1);
            } else {
                Toast.makeText(this, "Tidak ada aplikasi kamera yang tersedia. Silakan instal aplikasi kamera.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e("SecretCalculator", "Gagal membuka kamera: " + e.getMessage());
            Toast.makeText(this, "Gagal membuka kamera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void playAudio() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.sample_audio);
        if (mediaPlayer != null) {
            mediaPlayer.start();
            mediaPlayer.setOnCompletionListener(mp -> mp.release());
        } else {
            Toast.makeText(this, "Gagal memutar audio", Toast.LENGTH_SHORT).show();
        }
    }

    private void showImage() {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sample_image);
        if (bitmap != null) {
            ivImage.setImageBitmap(bitmap);
            ivImage.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show();
        }
    }

    private void showLocation() {
        if (fusedLocationClient == null) {
            Toast.makeText(this, "Layanan lokasi tidak tersedia", Toast.LENGTH_SHORT).show();
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "GPS tidak aktif. Silakan aktifkan GPS.", Toast.LENGTH_LONG).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            String loc = "Latitude: " + location.getLatitude() +
                                    ", Longitude: " + location.getLongitude();
                            Toast.makeText(this, loc, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Lokasi tidak tersedia", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Gagal mendapatkan lokasi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void showSensors() {
        if (accelerometer != null && gyroscope != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
            Toast.makeText(this, "Sensor diaktifkan", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Sensor tidak tersedia", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            String data = "Accelerometer: X=" + event.values[0] + ", Y=" + event.values[1] +
                    ", Z=" + event.values[2];
            Toast.makeText(this, data, Toast.LENGTH_SHORT).show();
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            String data = "Gyroscope: X=" + event.values[0] + ", Y=" + event.values[1] +
                    ", Z=" + event.values[2];
            Toast.makeText(this, data, Toast.LENGTH_SHORT).show();
        }
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Tidak diperlukan
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }
}