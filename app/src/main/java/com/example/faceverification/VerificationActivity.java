package com.example.faceverification;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONObject;

import java.util.Arrays;

public class VerificationActivity extends AppCompatActivity {

    Button camera, verifyBtn, scanQR;
    ImageView imageVerify;
    TextView faceStatus;
    Bitmap capturedFace;
    Bitmap qrFaceBitmap;

    String qrName = "", qrEmail = "", qrAge = "";
    String secretKey = "1234567890123456";
    boolean isRealFaceDetected = false;


    ActivityResultLauncher<ScanOptions> qrScannerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);
        camera = findViewById(R.id.Camera);
        verifyBtn = findViewById(R.id.Verify);
        scanQR = findViewById(R.id.scanQR);
        imageVerify = findViewById(R.id.imageFace);
        faceStatus = findViewById(R.id.faceStatus);
        camera.setOnClickListener(v -> openCamera());
        scanQR.setOnClickListener(v -> startQrScanner());
        verifyBtn.setOnClickListener(v -> verifyFaces());
        qrScannerLauncher = registerForActivityResult(new ScanContract(), result -> {

            if (result.getContents() == null) {
                Toast.makeText(this, "No QR scanned", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                String encryptedText = result.getContents();
                Log.d("QR_DEBUG", "Encrypted QR content: " + encryptedText);
                Log.d("QR_DEBUG", "QR Raw Data: " + result.getContents());
                String decryptedJson = decrypt(encryptedText, secretKey);
                Log.d("QR_DEBUG", "Decrypted JSON: " + decryptedJson);
                Log.d("QR_DEBUG", "DECRYPTED JSON = " + decryptedJson);

                JSONObject obj = new JSONObject(decryptedJson);

                qrName = obj.getString("name");
                qrEmail = obj.getString("email");
                qrAge = obj.getString("age");

                byte[] decoded = Base64.decode(obj.getString("face"), Base64.DEFAULT);
                qrFaceBitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);

                // 🔥 FIX: restore captured face after scanning QR
                if (capturedFace != null) {
                    imageVerify.setImageBitmap(capturedFace);
                }

                Toast.makeText(this, "QR Code Scanned, Now click  on Verify", Toast.LENGTH_LONG).show();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "QR decrypt error!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, 200);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 200 && resultCode == RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            photo = rotateBitmap(photo, -90);

            capturedFace = Bitmap.createScaledBitmap(photo, 200, 200, true);
            imageVerify.setImageBitmap(capturedFace);
            detectFace(photo);
            //Toast.makeText(this, "Face Captured", Toast.LENGTH_SHORT).show();
        }

    }
    private void startQrScanner() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Scan QR Code");
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setOrientationLocked(true);
        qrScannerLauncher.launch(options);


    }
    private Bitmap rotateBitmap(Bitmap source, float angle) {
        if (source == null) return null;
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
    private void detectFace(Bitmap bitmap)
    {

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        FaceDetectorOptions options = new FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setMinFaceSize(0.15f)
                .build();
        FaceDetector detector = FaceDetection.getClient(options);
        detector.process(image).addOnSuccessListener(faces ->
                {

                    if (!faces.isEmpty())
                    {
                        isRealFaceDetected = true;
                        Toast.makeText(this, "Face Detected ✓", Toast.LENGTH_SHORT).show();
                        TextView faceStatus = findViewById(R.id.faceStatus);
                       faceStatus.setText("✓ Face Detected");
                    faceStatus.setTextColor(Color.GREEN);

                    }
                    else
                    {

                        isRealFaceDetected = false;
                        Toast.makeText(this, "No Face Detected", Toast.LENGTH_SHORT).show();
                        TextView faceStatus = findViewById(R.id.faceStatus);
                      faceStatus.setText("✗ No Face Found");
                      faceStatus.setTextColor(Color.RED);
                    }
                })
                .addOnFailureListener(e ->
                {
                    isRealFaceDetected = false;
                    Toast.makeText(this, "Face Detection Error!", Toast.LENGTH_SHORT).show();
                    Log.e("FACE", e.getMessage());
                });
    }
    private void verifyFaces() {

        if (capturedFace == null) {
            Toast.makeText(this, "Capture face first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (qrFaceBitmap == null) {
            Toast.makeText(this, "Scan QR first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (compareFaces(capturedFace, qrFaceBitmap)) {
            Toast.makeText(this, "Face Matched", Toast.LENGTH_LONG).show();
            showUserDetails(qrName, qrAge, qrEmail);
        } else {
            Toast.makeText(this, "Face Not Matched!", Toast.LENGTH_LONG).show();
        }
    }
    private boolean compareFaces(Bitmap b1, Bitmap b2) {
        // Resize
        Bitmap r1 = Bitmap.createScaledBitmap(b1, 100, 100, true);
        Bitmap r2 = Bitmap.createScaledBitmap(b2, 100, 100, true);

        // Convert to grayscale
        int width = 100, height = 100;
        long diff = 0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                int p1 = r1.getPixel(x, y);
                int p2 = r2.getPixel(x, y);

                int g1 = (Color.red(p1) + Color.green(p1) + Color.blue(p1)) / 3;
                int g2 = (Color.red(p2) + Color.green(p2) + Color.blue(p2)) / 3;

                diff += Math.abs(g1 - g2);
            }
        }
        double avgDiff = diff / (width * height);
        return avgDiff < 45;
    }

    private void showUserDetails(String name, String age, String email) {
        TableLayout table = findViewById(R.id.userTable);
        TableRow row = new TableRow(this);
        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setPadding(8,8,8,8);
        TextView tvAge = new TextView(this);
        tvAge.setText(age);
        tvAge.setPadding(8,8,8,8);
        TextView tvEmail = new TextView(this);
        tvEmail.setText(email);
        tvEmail.setPadding(8,8,8,8);
        row.addView(tvName);
        row.addView(tvAge);
        row.addView(tvEmail);
        table.addView(row);
        //Toast.makeText(this, "User Found Name: " + name + "Age: " + age + " Emailid:" + emailid, Toast.LENGTH_LONG).show();
        return;

    }

    private String decrypt(String encrypted, String key) throws Exception {
        byte[] keyBytes = Arrays.copyOf(key.getBytes("UTF-8"), 16);
        SecretKeySpec skey = new SecretKeySpec(keyBytes, "AES");
        encrypted = encrypted.replace(" ", "+")
                .replace("\n", "")
                .replace("\r", "")
                .trim();
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, skey);
        byte[] decoded = Base64.decode(encrypted, Base64.DEFAULT);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, "UTF-8");
    }

}
