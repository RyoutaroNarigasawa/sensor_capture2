package com.research.pengin.sensorcapture2;

/**
 * Created by pengin on 2017/06/02.
 */

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class CaptureActivity extends Activity implements Runnable,SensorEventListener{
    //加速度センサとジャイロセンサのセンサマネージャ
    SensorManager sm_a, sm_g;

    //フラグ
    int state = 0;//記録状態
    int savestate = -1; //ファイル記録結果

    //SDカードのマウント先記録
    String sdCardState = Environment.getExternalStorageState();

    //センサデータ記録用可変長配列
    ArrayList<Float> Accel_X = new ArrayList<Float>(200);
    ArrayList<Float> Accel_Y = new ArrayList<Float>(200);
    ArrayList<Float> Accel_Z = new ArrayList<Float>(200);
    ArrayList<Float> Gyro_X  = new ArrayList<Float>(200);
    ArrayList<Float> Gyro_Y  = new ArrayList<Float>(200);
    ArrayList<Float> Gyro_Z  = new ArrayList<Float>(200);

    //端末が実際に取得した加速度値。重力加速度も含まれる。This values include gravity force.
    private float[] currentOrientationValues = {0.0f, 0.0f, 0.0f};

    //ローパス、ハイパスフィルタ後の加速度値 Values after low pass and high pass filter
    private float[] currentAccelerationValues = {0.0f, 0.0f, 0.0f};

    //端末が実際に取得した角速度値
    private float[] currentGyroscropValues = {0.0f,0.0f,0.0f};

    //previous data 1つ前の値
    private float old_x = 0.0f;
    private float old_y = 0.0f;
    private float old_z = 0.0f;

    //ノイズ対策
    boolean noiseflg_A = true;

    //Spinner number;
    Button startbtn,stopbtn,masterbtn;

    View decor;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);
       decor = this.getWindow().getDecorView();

        Intent intent = getIntent();
        //被験者の各種データ記録用
        final String tester = intent.getStringExtra("tester");
        final String hand   = intent.getStringExtra("hand");
        final String number = intent.getStringExtra("number");

        //number    = (Spinner)findViewById(R.id.number);
        startbtn  = (Button)findViewById(R.id.start);//記録開始用ボタン

        startbtn.setOnClickListener(new View.OnClickListener() {//記録開始処理
            @Override
            public void onClick(View v) {
                state = 1;
                long time = System.currentTimeMillis();
                decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                //startbtn.setFocusable(false);
                startbtn.setEnabled(false);
                Toast.makeText(CaptureActivity.this, "記録を開始します", Toast.LENGTH_SHORT).show();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (sdCardState.equals(Environment.MEDIA_MOUNTED)) {
                            state = 0;
                            //startbtn.setClickable(true);
                            //startbtn.setFocusableInTouchMode(true);
                            startbtn.setEnabled(true);
                            WriteCSV wcsv = new WriteCSV(getExternalFilesDirs(null)[1]+"/"+tester+"-"+hand+"-wait-"+number+".csv");
                            savestate = wcsv.csv(Accel_X,Accel_Y,Accel_Z,Gyro_X,Gyro_Y,Gyro_Z);
                            if(savestate == 0){
                                Toast.makeText(CaptureActivity.this,"記録が正常に行われました",Toast.LENGTH_SHORT).show();
                            }else if(savestate == 1){
                                Toast.makeText(CaptureActivity.this,"ファイルが正常に作成されませんでした(error code 1)",Toast.LENGTH_SHORT).show();
                            }else if(savestate == -1){
                                Toast.makeText(CaptureActivity.this,"クローズ処理が正常におこなわれませんでした(error code -1)",Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(CaptureActivity.this,"想定外の異常が発生しました",Toast.LENGTH_SHORT).show();
                            }
                        } else if (sdCardState.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
                            Toast.makeText(CaptureActivity.this, "このSDカードは読み取り専用です", Toast.LENGTH_SHORT).show();
                        } else if (sdCardState.equals(Environment.MEDIA_REMOVED)) {
                            Toast.makeText(CaptureActivity.this, "SDカードが挿入されていません", Toast.LENGTH_SHORT).show();
                        } else if (sdCardState.equals(Environment.MEDIA_UNMOUNTED)) {
                            Toast.makeText(CaptureActivity.this, "SDカードがマウントされていません", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(CaptureActivity.this, "再度SDカードを確認してください", Toast.LENGTH_SHORT).show();
                        }
                    }
                },10000);
            }
        });

    }


    @Override
    public void run() {

    }

    protected void onResume(){
        super.onResume();
        sm_a = (SensorManager) getSystemService(SENSOR_SERVICE);
        sm_g = (SensorManager) getSystemService(SENSOR_SERVICE);

        List<Sensor> sensors_a = sm_a.getSensorList(Sensor.TYPE_ACCELEROMETER);
        List<Sensor> sensors_g = sm_g.getSensorList(Sensor.TYPE_GYROSCOPE);
        if (0 < sensors_a.size()) {
            sm_a.registerListener(this, sensors_a.get(0), SensorManager.SENSOR_DELAY_FASTEST);
        }
        if(0 < sensors_g.size()){
            sm_g.registerListener(this, sensors_g.get(0), SensorManager.SENSOR_DELAY_FASTEST);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        sm_a.unregisterListener(this);
        sm_g.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //h.removeCallbacks(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // 取得

            // ローパスフィルタで重力値を抽出
            currentOrientationValues[0] = event.values[0] * 0.1f + currentOrientationValues[0] * (1.0f - 0.1f);
            currentOrientationValues[1] = event.values[1] * 0.1f + currentOrientationValues[1] * (1.0f - 0.1f);
            currentOrientationValues[2] = event.values[2] * 0.1f + currentOrientationValues[2] * (1.0f - 0.1f);

            // 重力の値を省く
            currentAccelerationValues[0] = event.values[0] - currentOrientationValues[0];
            currentAccelerationValues[1] = event.values[1] - currentOrientationValues[1];
            currentAccelerationValues[2] = event.values[2] - currentOrientationValues[2];

            // 状態更新
            //vectorSize_old = vectorSize;
            old_x = currentAccelerationValues[0];
            old_y = currentAccelerationValues[1];
            old_z = currentAccelerationValues[2];
        }
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            currentGyroscropValues[0] = event.values[0];
            currentGyroscropValues[1] = event.values[1];
            currentGyroscropValues[2] = event.values[2];

        }
        // 一回目はノイズになるから省く
        if (noiseflg_A == true) {
            noiseflg_A = false;
        } else {
            if (state == 1) {
                try {
                    Accel_X.add(currentAccelerationValues[0]);
                    Accel_Y.add(currentAccelerationValues[1]);
                    Accel_Z.add(currentAccelerationValues[2]);
                    Gyro_X.add(currentGyroscropValues[0]);
                    Gyro_Y.add(currentGyroscropValues[1]);
                    Gyro_Z.add(currentGyroscropValues[2]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged (Sensor sensor,int accuracy){

    }

}
