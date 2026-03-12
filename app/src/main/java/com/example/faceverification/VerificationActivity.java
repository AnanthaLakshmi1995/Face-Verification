package com.example.faceverification;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
public class VerificationActivity extends AppCompatActivity {
    Button camera, verify;
    ImageView imageVerify;
    Bitmap capturedFace;
    DataBase db;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);
        camera = findViewById(R.id.Camera);
        verify = findViewById(R.id.Verify);
        imageVerify = findViewById(R.id.imageVerify);
        db = new DataBase(this);
        camera.setOnClickListener(v -> openCamera());
        verify.setOnClickListener(v -> checkFace());
    }
    private void openCamera()
    {

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent,2);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data) {

        super.onActivityResult(requestCode,resultCode,data);

        if(requestCode==2 && resultCode==RESULT_OK && data!=null  && data.getExtras()!=null){

            Bitmap photo = (Bitmap) data.getExtras().get("data");

            imageVerify.setImageBitmap(photo);

            capturedFace = Bitmap.createScaledBitmap(photo,200,200,true);
        }
    }

    public void checkFace()
    {
        if(capturedFace == null)
        {
            Toast.makeText(this,"Capture face first",Toast.LENGTH_SHORT).show();
            return;
        }

        SQLiteDatabase database = db.getReadableDatabase();

        Cursor c = database.rawQuery("SELECT username,age,emailid,image FROM users", null);

        if(c.getCount() == 0)
        {
            Toast.makeText(this,"No data in database",Toast.LENGTH_SHORT).show();
            c.close();
            return;
        }

        while(c.moveToNext())
        {
            String name = c.getString(0);
            String age = c.getString(1);
            String emailid = c.getString(2);
            byte[] imageBytes = c.getBlob(3);
            Bitmap storedFace = Bitmap.createScaledBitmap(byteToImage(imageBytes),200,200,true);

            if(compareImages(storedFace,capturedFace))
            {
                Toast.makeText(this, "User Found Name: "+name+ "Age: "+age+ "Email: "+emailid, Toast.LENGTH_LONG).show();

                c.close();
                return;
            }
        }

        Toast.makeText(this,"Face Not Match",Toast.LENGTH_SHORT).show();
        c.close();
    }

    public boolean compareImages(Bitmap img1, Bitmap img2)
    {
        img1 = Bitmap.createScaledBitmap(img1,200,200,true);
        img2 = Bitmap.createScaledBitmap(img2,200,200,true);
        int width = img1.getWidth();
        int height = img1.getHeight();
        int matchedPixels = 0;
        int totalPixels = width * height;
        for(int x=0;x<width;x++)
        {
            for(int y=0;y<height;y++)
            {
                int p1 = img1.getPixel(x,y);
                int p2 = img2.getPixel(x,y);
                int r1 = (p1 >> 16) & 0xff;
                int g1 = (p1 >> 8) & 0xff;
                int b1 = p1 & 0xff;
                int r2 = (p2 >> 16) & 0xff;
                int g2 = (p2 >> 8) & 0xff;
                int b2 = p2 & 0xff;
                int diff = Math.abs(r1-r2) + Math.abs(g1-g2) + Math.abs(b1-b2);

                if(diff < 50)
                {
                    matchedPixels++;
                }
            }
        }

        double similarity = (double)matchedPixels / totalPixels;

        return similarity > 0.30;
    }
    public Bitmap byteToImage(byte[] imageBytes)
    {

        return BitmapFactory.decodeByteArray(imageBytes,0,imageBytes.length);
    }
}