package com.example.faceverification;
import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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
import java.io.InputStream;
public class RegisterActivity extends AppCompatActivity {

    EditText name, age, emailid;
    Button register, verify, camera;
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
        verify = findViewById(R.id.Verify);
        camera = findViewById(R.id.Camera);
        imageFace = findViewById(R.id.imageFace);
        db = new DataBase(this);
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result ->
                {
                    if (result.getResultCode() == RESULT_OK && result.getData()!=null )
                    {
                        Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                        imageFace.setImageBitmap(photo);
                        capturedFace = photo;
                    }
                });

        camera.setOnClickListener(v -> openCamera());

        register.setOnClickListener(v -> saveData());

        verify.setOnClickListener(v ->
                startActivity(new Intent(this, VerificationActivity.class)));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED)
        {

            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        }
    }

    private void openCamera()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }
    private void saveData()
    {
        if (name.getText().toString().isEmpty())
        {
            name.setError("Enter name");
            return;
        }

        if (age.getText().toString().isEmpty())
        {
            age.setError("Enter age");
            return;
        }

        if (emailid.getText().toString().isEmpty())
        {
            emailid.setError("Enter emailid");
            return;
        }

        if (capturedFace == null) {
            Toast.makeText(this,"Capture image first",Toast.LENGTH_SHORT).show();
            return;
        }
        byte[] imageBytes = imageToByte(capturedFace);
        SQLiteDatabase database = db.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("username", name.getText().toString());
        cv.put("age", age.getText().toString());
        cv.put("emailid", emailid.getText().toString());
        cv.put("image", imageBytes);
        long result = database.insert("users", null, cv);
        if (result != -1)
        {
            Toast.makeText(this,"Registered Successfully",Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, VerificationActivity.class));
            finish();

        }
        else
        {

            Toast.makeText(this,"Registration Failed",Toast.LENGTH_SHORT).show();
        }
    }
    private byte[] imageToByte(Bitmap bitmap)
    {

        Bitmap resized = Bitmap.createScaledBitmap(bitmap,200,200,true);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG,30,stream);
        return stream.toByteArray();
    }
}