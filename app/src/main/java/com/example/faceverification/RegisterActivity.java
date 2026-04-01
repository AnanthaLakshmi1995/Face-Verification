package com.example.faceverification;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class RegisterActivity extends AppCompatActivity {

    EditText name, age, emailid;
    Button register, camera;
    ImageView imageFace, qrImageView;
    Bitmap capturedFace;
    DataBase db;
    Bitmap generatedQrBitmap;
    String savedEmail;

    ActivityResultLauncher<Intent> cameraLauncher;

    private static final String secretKey = "1234567890123456"; // 16 chars AES

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        TextView faceStatus = findViewById(R.id.faceStatus);
        //faceStatus.setText("✓ Enrolled");
        //faceStatus.setTextColor(Color.GREEN);
        //faceStatus.setText("✗ Not Matched");
        //faceStatus.setTextColor(Color.RED);
        name = findViewById(R.id.Name);
        age = findViewById(R.id.Age);
        emailid = findViewById(R.id.Emailid);
        register = findViewById(R.id.Register);
        camera = findViewById(R.id.Camera);
        //sendQRcode = findViewById(R.id.sendQRcode);
        imageFace = findViewById(R.id.imageFace);
        qrImageView = findViewById(R.id.qrImageView);

        db = new DataBase(this);

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                        photo = rotateBitmap(photo, -90);
                        capturedFace = photo;
                        imageFace.setImageBitmap(photo);
                        faceStatus.setText("✓ Enrolled");
                        faceStatus.setTextColor(Color.GREEN);
                    }
                });

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        }

        camera.setOnClickListener(v -> openCamera());
        register.setOnClickListener(v -> saveData());

        //sendQRcode.setOnClickListener(v -> {
            //if (savedEmail != null && generatedQrBitmap != null) {
               // File file = qrBitmapToFile(generatedQrBitmap);
               // sendEmailWithQr(savedEmail, file);
           // } else {
               // Toast.makeText(this, "Register first!", Toast.LENGTH_SHORT).show();
           // }
       // });
    }

    private Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(
                source, 0, 0, source.getWidth(), source.getHeight(), matrix, true
        );
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra("android.intent.extras.CAMERA_FACING", 1);
        intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1);
        intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
        cameraLauncher.launch(intent);
        Toast.makeText(this, " Your face data has been securely captured", Toast.LENGTH_SHORT).show();

    }

    private void saveData() {

        String nameText = name.getText().toString().trim();
        String ageText = age.getText().toString().trim();
        String emailText = emailid.getText().toString().trim();

        if (nameText.isEmpty()) {
            name.setError("Enter name");
            return;
        }
        if (ageText.isEmpty()) {
            age.setError("Enter age");
            return;
        }
        if (!ageText.matches("\\d+")) {
            age.setError("Numbers only");
            return;
        }
        if (emailText.isEmpty()) {
            emailid.setError("Enter email");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(emailText).matches()) {
            emailid.setError("Invalid email");
            return;
        }
        if (capturedFace == null) {
            Toast.makeText(this, "Capture face first!", Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] imageBytes = imageToByte(capturedFace);

        SQLiteDatabase database = db.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("username", nameText);
        cv.put("age", Integer.parseInt(ageText));
        cv.put("emailid", emailText);
        cv.put("image", imageBytes);

        long result = database.insert("users", null, cv);

        if (result != -1) {
            Toast.makeText(this, "Registered Successfully", Toast.LENGTH_SHORT).show();
            savedEmail = emailText;
            String faceBase64 = bitmapToBase64(capturedFace);


            try {

                JSONObject obj = new JSONObject();

                obj.put("name", name.getText().toString());
                obj.put("email", emailid.getText().toString());
                obj.put("age", age.getText().toString());
                obj.put("face", faceBase64);
                String encryptedText = encrypt(obj.toString(), secretKey);
                generatedQrBitmap = generateQRCode(encryptedText);

                qrImageView.setImageBitmap(generatedQrBitmap);

                File qrcodeFile = qrBitmapToFile(generatedQrBitmap);
                sendEmailWithQr(savedEmail, qrcodeFile);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "QR/Encryption Error!", Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show();
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 50, 50, true); // smaller
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 30, baos); // lower quality
        byte[] bytes = baos.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }
    private byte[] imageToByte(Bitmap bitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 200, 200, true);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 30, stream);
        return stream.toByteArray();
    }
    private String buildEncryptedQR(String name, String email, String age, String faceBase64, String secretKey) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("email", email);
            obj.put("age", age);
            obj.put("face", faceBase64);

            String jsonString = obj.toString();
            return encrypt(jsonString, secretKey);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    private String encrypt(String data, String key) throws Exception {
        byte[] keyBytes = Arrays.copyOf(key.getBytes("UTF-8"), 16);
        SecretKeySpec skey = new SecretKeySpec(keyBytes, "AES");

        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, skey);

        byte[] encrypted = cipher.doFinal(data.getBytes("UTF-8"));
        return Base64.encodeToString(encrypted, Base64.NO_WRAP);
    }

    private Bitmap generateQRCode(String data) {
        try {
            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
            BitMatrix bitMatrix = multiFormatWriter.encode(data, BarcodeFormat.QR_CODE, 500, 500);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            return barcodeEncoder.createBitmap(bitMatrix);
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }



    private File qrBitmapToFile(Bitmap bitmap) {
        try {
            File file = new File(getExternalCacheDir(), "qrcode.png");
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sendEmailWithQr(String email, File qrFile) {
        new Thread(() -> {
            try {
                if (qrFile == null || !qrFile.exists()) {
                    runOnUiThread(() -> Toast.makeText(this, "QR file not created!", Toast.LENGTH_SHORT).show());
                    return;
                }
                String senderEmail = "anisatya2019@gmail.com";
                String senderPassword = "xjvv uyfw firw iies";

                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");
                props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

                Session session = Session.getInstance(props, new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(senderEmail, senderPassword);
                    }
                });

                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(senderEmail));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
                message.setSubject("Your Registration QR Code");

                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText("Here is your encrypted QR Code including your face for verification.");

                MimeBodyPart attachPart = new MimeBodyPart();
                attachPart.attachFile(qrFile);

                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(textPart);
                multipart.addBodyPart(attachPart);

                message.setContent(multipart);
                Transport.send(message);

                runOnUiThread(() ->
                {
                    Toast.makeText(this, "QR Code Sent to your emailid", Toast.LENGTH_LONG).show();
                    finish();
                });
                Intent intent=new Intent(RegisterActivity.this, DashBoard.class);
                startActivity(intent);
                finish();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to send email!", Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
