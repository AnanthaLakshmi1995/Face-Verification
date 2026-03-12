package com.example.faceverification;

import static java.nio.file.StandardOpenOption.CREATE;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DataBase extends SQLiteOpenHelper {
    public static final String DB_Name="Face.Db";
    private static  final int DB_version=2;
    public DataBase(Context context)
    {
        super(context,DB_Name,null,DB_version);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL("CREATE TABLE users(" + "id INTEGER PRIMARY KEY AUTOINCREMENT," + "username TEXT," + "age TEXT," + "emailid TEXT," + "image BLOB)");
    }
public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion)
{
    db.execSQL("Drop table if exists users");
    onCreate(db);

}
public void insertUser(String name,String age,String emailid,byte[] image)
{
    SQLiteDatabase db=this.getWritableDatabase();
    ContentValues cv=new ContentValues();
    cv.put("username", name);
    cv.put("age", age);
    cv.put("emailid",emailid);
    cv.put("image",image);
    db.insert ("users",null,cv);

}
}
