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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.io.ByteArrayOutputStream;

public class RegisterActivity extends AppCompatActivity {

    EditText name, age, emailid;
    Button register, camera;
    ImageView imageFace;
    Bitmap capturedFace;
    DataBase db;
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
        imageFace = findViewById(R.id.imageFace);
        db = new DataBase(this);
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null)
                    {

                        Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                        photo = rotateBitmap(photo, -90);
                        imageFace.setImageBitmap(photo);
                        capturedFace = photo;
                    }
                });

        camera.setOnClickListener(v -> openCamera());

        register.setOnClickListener(v -> saveData());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED)
        {

            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        }
    }

    private Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
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
        if (name.getText().toString().isEmpty())
        {
            name.setError("Enter name");
            return;
        }
        String ageText = age.getText().toString().trim();
        if (ageText.isEmpty())
        {
            age.setError("Enter age");
            return;
        }
        if (!ageText.matches("\\d+"))
        {
            age.setError("Enter numbers only");
            return;
        }

        if (emailid.getText().toString().isEmpty())
        {
            emailid.setError("Enter email");
            return;
        }

        if (capturedFace == null)
        {
            Toast.makeText(this, "Capture image first", Toast.LENGTH_SHORT).show();
            return;
        }
        byte[] imageBytes = imageToByte(capturedFace);
        SQLiteDatabase database = db.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("username", name.getText().toString());
        cv.put("age", Integer.parseInt(ageText));
        cv.put("emailid", emailid.getText().toString());
        cv.put("image", imageBytes);
        long result = database.insert("users", null, cv);
        if (result != -1)
        {

            Toast.makeText(this, "Registered Successfully", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, VerificationActivity.class));
            finish();

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
}