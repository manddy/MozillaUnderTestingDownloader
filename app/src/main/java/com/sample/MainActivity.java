/*
 *    Copyright (C) 2017 MINDORKS NEXTGEN PRIVATE LIMITED
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.sample;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.sample.utils.Utils;

import java.util.Calendar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

public class MainActivity extends AppCompatActivity implements TimePickerDialog.OnTimeSetListener {


    final String URL1 = "http://mattmahoney.net/dc/enwik8.pmd";
    final String URL2 = "http://mattmahoney.net/dc/enwik9.pmd";
    final String URL3 = "http://www.appsapk.com/downloading/latest/WeChat-6.5.7.apk";
    final String URL4 = "http://www.appsapk.com/downloading/latest/Emoji%20Flashlight%20-%20Brightest%20Flashlight%202018-2.0.1.apk";
    final String URL5 = "http://www.appsapk.com/downloading/latest/Screen%20Recorder-7.7.apk";
    final String URL6 = "http://www.appsapk.com/downloading/latest/Call%20Recorder%20-%20Automatic%20Call%20Recorder-1.6.0.apk";
    final String URL7 = "http://www.appsapk.com/downloading/latest/Sound%20Profile%20(+%20volume%20scheduler)-5.25.apk";
    final String URL8 = "http://www.appsapk.com/downloading/latest/Evernote%20-%20stay%20organized.-7.9.7.apk";
    final String URL9 = "http://www.appsapk.com/downloading/latest/UC-Browser.apk";
    final String URL10 = "http://www.appsapk.com/downloading/latest/Barcode%20Scanner-1.2.apk";
    final String URL11 = "http://download.blender.org/peach/bigbuckbunny_movies/BigBuckBunny_640x360.m4v";
    final String URL12 = "http://www2.sdfi.edu.cn/netclass/jiaoan/englit/download/Harry%20Potter%20and%20the%20Sorcerer's%20Stone.pdf";
    final String URL13 = "https://media.giphy.com/media/Bk0CW5frw4qfS/giphy.gif";
    final String URL14 = "http://techslides.com/demos/sample-videos/small.mp4";
    final String URL15 = "http://www.sample-videos.com/video/mp4/720/big_buck_bunny_720p_10mb.mp4";

    Button buttonOne;
    Button buttonCancelOne;
    Button buttonScheduleOne;




    TextView textViewProgressOne,textViewProgressTwo,textViewProgressThree, textViewProgressFour, textViewProgressFive, textViewProgressSix,
            textViewProgressSeven, textViewProgressEight, textViewProgressNine,
            textViewProgressTen, textViewProgressEleven, textViewProgressTwelve,
            textViewProgressThirteen, textViewProgressFourteen, textViewProgressFifteen;

    ProgressBar progressBarOne, progressBarTwo, progressBarThree,
            progressBarFour, progressBarFive, progressBarSix,
            progressBarSeven, progressBarEight, progressBarNine,
            progressBarTen, progressBarEleven, progressBarTwelve,
            progressBarThirteen, progressBarFourteen, progressBarFifteen;

    MainActivity mainActivity;
    private int STORAGE_PERMISSIONS_CODE = 1;
    private static String dirPath;
    private  NotificationHelper mNotificationHelper;
    Long lasttimestamp;
    MyReceiver myReceiver;
    String schedulesession;

    private class MyReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            // TODO Auto-generated method stub

            long datapassed = arg1.getLongExtra("DATAPASSED", 0);
            progressBarOne.setProgress((int)datapassed);
            String statusofdownload=arg1.getStringExtra("Statusofdownload");

            if (statusofdownload!=null){
                if(statusofdownload.equals("started")){
                    progressBarOne.setIndeterminate(false);
                    buttonOne.setEnabled(true);
                    buttonOne.setText(R.string.pause);
                    buttonCancelOne.setEnabled(true);
                    buttonScheduleOne.setEnabled(false);
                    progressBarOne.setProgress(0);
                    textViewProgressOne.setText("");
                }
                else if (statusofdownload.equals("paused")){
                    buttonOne.setText(R.string.resume);
                }

                else if (statusofdownload.equals("cancelled")){
                    buttonOne.setText(R.string.start);
                    buttonCancelOne.setEnabled(false);
                    progressBarOne.setProgress(0);
                    textViewProgressOne.setText("");
                    progressBarOne.setIndeterminate(false);
                    schedulesession="false";
                    buttonScheduleOne.setEnabled(true);
                }

            }
        }

    }
    public void startService(View v){
        Intent serviceintent=new Intent(this,SampleService.class);
        startService(serviceintent);

    }
    @Override
    protected void onStop() {
        unregisterReceiver(myReceiver);
        super.onStop();
    }
    public void stopService(View v){
        Intent serviceintent=new Intent(this,SampleService.class);
        stopService(serviceintent);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myReceiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SampleService.MY_ACTION);
        registerReceiver(myReceiver, intentFilter);
        mNotificationHelper=new NotificationHelper(this);
        mainActivity = this;
        lasttimestamp=System.currentTimeMillis();
        dirPath = Utils.getRootDirPath(getApplicationContext());
        /**Getting the external storage permissions**/
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        {
            Toast.makeText(this, "You have already been granted the permissions", Toast.LENGTH_SHORT).show();
        }
        else
        {
            requeststoragepermission();
        }
        //Code to autostart
        //        if (Build.BRAND.equalsIgnoreCase("xiaomi")) {
        //            Intent intent = new Intent();
        //            intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
        //            startActivity(intent);
        //        }
        init();
        onClickListenerOne();

    }

    private void requeststoragepermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(this).setTitle("Permission Needed").setPositiveButton("ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSIONS_CODE);
                }
            }).setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).create().show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSIONS_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (STORAGE_PERMISSIONS_CODE == requestCode) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "PERMISSION GRANTED", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "PERMISSION DENIES", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void init() {
        buttonOne = findViewById(R.id.buttonOne);
        buttonCancelOne = findViewById(R.id.buttonCancelOne);
        buttonScheduleOne = findViewById(R.id.buttonscheduleOne);
        textViewProgressOne = findViewById(R.id.textViewProgressOne);
        progressBarOne = findViewById(R.id.progressBarOne);
    }



    public void onClickListenerOne() {
        buttonOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick( View view) {
                Intent serviceintent = new Intent(MainActivity.this, SampleService.class);
                startService(serviceintent);
            }
        });

        buttonCancelOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent serviceIntent = new Intent(MainActivity.this,SampleService.class);
                serviceIntent.putExtra("downloadstatus", "cancelled");
                startService(serviceIntent);
            }
        });
        buttonScheduleOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (schedulesession!=null && schedulesession.equals("true")) {
                    Toast.makeText(getApplicationContext(), "alreadyscheduled", Toast.LENGTH_SHORT).show();
                    AlertDialog.Builder builder=new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("CHOOSE ONE OF THE OPTION").setPositiveButton("Remove Shedulling", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent serviceIntent = new Intent(MainActivity.this,SampleService.class);
                            serviceIntent.putExtra("scheduleoption", "removeschedulling");
                            startService(serviceIntent);
                            buttonOne.setEnabled(true);
                        }
                    }).setNegativeButton("Reschedule", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent serviceIntent = new Intent(MainActivity.this,SampleService.class);
                            serviceIntent.putExtra("scheduleoption", "removeschedulling");
                            startService(serviceIntent);
                            DialogFragment timepicker = new TimePickerFragment();
                            timepicker.show(getSupportFragmentManager(), "time picker");
                        }
                    });

                    AlertDialog alert=builder.create();
                    alert.show();
                    return;
                }

                DialogFragment timepicker = new TimePickerFragment();
                timepicker.show(getSupportFragmentManager(), "time picker");

            }
        });
    }

    @Override
    public void onTimeSet(TimePicker view, int scheduledhours, int minute) {

        Calendar rightNow = Calendar.getInstance();
        int currentHourIn24Format = rightNow.get(Calendar.HOUR_OF_DAY);
        if (currentHourIn24Format == 0) {
            currentHourIn24Format = 24;
        }
        int diffhours = 0;

        int currentminutes = rightNow.get(Calendar.MINUTE);

        if (scheduledhours > currentHourIn24Format) {
            diffhours = scheduledhours - currentHourIn24Format;
        } else if (scheduledhours == currentHourIn24Format) {
            diffhours = 0;
        }
        else {
            diffhours = (24 - currentHourIn24Format) + scheduledhours;
        }
        int diffminutes = Math.abs(minute - currentminutes);
        final int totaldiffminutes = diffhours * 60 + diffminutes;

        buttonOne.setEnabled(false);
        Intent serviceIntent = new Intent(MainActivity.this,SampleService.class);
        serviceIntent.putExtra("scheduletime", totaldiffminutes);
        serviceIntent.putExtra("schedulestatus", "scheduled");
        schedulesession="true";
        startService(serviceIntent);
    }
}

