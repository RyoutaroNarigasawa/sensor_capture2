package com.research.pengin.sensorcapture2;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    EditText tester;
    Spinner hand,course,threshold,number;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button capturebtn = (Button) findViewById(R.id.capture);//マスタデータ登録フォーム遷移用ボタン

        hand      = (Spinner)findViewById(R.id.hand);
        tester    = (EditText)findViewById(R.id.tester);
        number    = (Spinner)findViewById(R.id.number);

        capturebtn.setOnClickListener(new OnClickListener() {//マスタデータ登録フォーム遷移用ボタン押下時の動作
            public void onClick(View v) {
                if(tester.getText().toString().equals("")) {
                    Toast.makeText(MainActivity.this,"名前が入力されていません",Toast.LENGTH_SHORT).show();
                }else{

                    Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                    intent.putExtra("tester",tester.getText().toString());
                    intent.putExtra("hand", (String) hand.getSelectedItem());
                    intent.putExtra("number",(String)number.getSelectedItem());
                    startActivity(intent);
                }
            }
        });

    }
}
