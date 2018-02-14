package com.fogoa.networkapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.fogoa.networkapplication.extensions.BaseActivity;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //force login to access this activity
        isLoginReq = true;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



    }
}


