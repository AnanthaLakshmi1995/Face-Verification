package com.example.faceverification;
import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Properties;
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
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class RegisterActivity extends AppCompatActivity
{

    EditText name, age, emailid;
    Button register, camera,sendqrcode;
    ImageView imageFace, qrImageView;
    Bitmap capturedFace;
    DataBase db;
    Bitmap generatedQrBitmap;
    String savedEmail;
    ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        name = findViewById(R.id.Name);
        age = findViewById(R.id.Age);
        emailid = findViewById(R.id.Emailid);
        register = findViewById(R.id.Register);
        camera = findViewById(R.id.Camera);
        sendqrcode=findViewById(R.id.sendQRcode);
        imageFace = findViewById(R.id.imageFace);
        qrImageView = findViewById(R.id.qrImageView);
        db = new DataBase(this);
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {

                        Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                        photo = rotateBitmap(photo, -90);
                        imageFace.setImageBitmap(photo);
                        capturedFace = photo;
                    }
                });

        camera.setOnClickListener(v -> openCamera());
        register.setOnClickListener(v -> saveData());

        sendqrcode.setOnClickListener(v -> {
            if (savedEmail != null && generatedQrBitmap != null) {
                sendEmailWithQr(savedEmail, generatedQrBitmap);
            } else {
                Toast.makeText(this, "Register first to generate QR!", Toast.LENGTH_SHORT).show();
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);

        }
    }
    private Bitmap rotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0,
                source.getWidth(), source.getHeight(), matrix, true);
    }

    private void openCamera()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra("android.intent.extras.CAMERA_FACING", 1);
        intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1);
        intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true);
        cameraLauncher.launch(intent);
    }

    private void saveData()
    {

        String nameText = name.getText().toString();
        String ageText = age.getText().toString().trim();
        String emailText = emailid.getText().toString().trim();
        if (nameText.isEmpty())
        {
            name.setError("Enter name");
            return;
        }
        if (ageText.isEmpty())
        { age.setError("Enter age");
            return;
        }
        if (!ageText.matches("\\d+"))
        { age.setError("Enter numbers only");
            return;
        }
        if (emailText.isEmpty())
        {
            emailid.setError("Enter email");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(emailText).matches())
        {
            emailid.setError("Enter valid email"); return;
        }
        if (capturedFace == null)
        {
            Toast.makeText(this, "Capture image first", Toast.LENGTH_SHORT).show();
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
        if (result != -1)
        {
            Toast.makeText(this, "Registered Successfully", Toast.LENGTH_SHORT).show();
            savedEmail = emailText;
            String userData = "Name: " + nameText + "\nEmail: " + emailText + "\nAge: " + ageText;
            generatedQrBitmap = generateQrCode(userData);
            qrImageView.setImageBitmap(generatedQrBitmap);
            sendEmailWithQr(savedEmail, generatedQrBitmap);
        }
        else
        {
            Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] imageToByte(Bitmap bitmap)
    {
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 200, 200, true);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 30, stream);
        return stream.toByteArray();
    }

    private Bitmap generateQrCode(String text)
    {
        try
        {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 500, 500);
            BarcodeEncoder encoder = new BarcodeEncoder();
            return encoder.createBitmap(matrix);

        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private File bitmapToFile(Bitmap bitmap)
    {
        File file = new File(getExternalFilesDir(null), "qr_image.png");

        try
        {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return file;
    }
    private void sendEmailWithQr(String receiverEmail, Bitmap qrBitmap)
    {
        new Thread(() ->
        {
            try
            {
                String senderEmail = "anisatya2019@gmail.com";
                String senderPassword = "xjvv uyfw firw iies";
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");
                Session session = Session.getInstance(props, new Authenticator()
                        {
                            protected PasswordAuthentication getPasswordAuthentication()
                            {
                                return new PasswordAuthentication(senderEmail, senderPassword);
                            }
                        });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(senderEmail));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(receiverEmail));
                message.setSubject("Your Registration QR Code");
                Multipart multipart = new MimeMultipart();
                MimeBodyPart textBody = new MimeBodyPart();
                textBody.setText("Your registration is successful.\nQR Code sent.");
                multipart.addBodyPart(textBody);
                MimeBodyPart attachment = new MimeBodyPart();
                File qrFile = bitmapToFile(qrBitmap);
                attachment.attachFile(qrFile);
                multipart.addBodyPart(attachment);
                message.setContent(multipart);
                Transport.send(message);
                session.setDebug(true);
                runOnUiThread(() -> {
                    Toast.makeText(this, "QR Code sent successfully", Toast.LENGTH_LONG).show();

                });
                startActivity(new Intent(RegisterActivity.this, VerificationActivity.class));
                finish();
            }
            catch (Exception e)
            {
                runOnUiThread(() ->
                {
                    Toast.makeText(this, "Failed to send email: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
                e.printStackTrace();
            }

        }).start();
    }
}