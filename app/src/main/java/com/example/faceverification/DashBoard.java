package com.example.faceverification;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class  DashBoard extends AppCompatActivity {
 Button Register,Verification;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dash_board);
        Register=findViewById(R.id.Register);
        Verification=findViewById(R.id.Verification);
        Register.setOnClickListener(new  View.OnClickListener()
        {
            public void onClick(View view)
            {
               Intent intent=new Intent(DashBoard.this,RegisterActivity.class) ;
               startActivity(intent);
            }
        });
        Verification.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
               Intent intent=new Intent(DashBoard.this,VerificationActivity.class);
               startActivity(intent);
            }
       });

    }
}