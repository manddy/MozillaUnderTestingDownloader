package com.sample;

import android.app.Notification;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


import com.downloader.Error;
import com.downloader.MozzDownloader;
import com.downloader.OnCancelListener;
import com.downloader.OnDownloadListener;
import com.downloader.OnPauseListener;
import com.downloader.OnProgressListener;
import com.downloader.OnStartOrResumeListener;
import com.downloader.Progress;
import com.downloader.Status;
import com.sample.utils.Utils;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;

import static com.sample.NotificationHelper.channel1ID;

public class SampleService extends LifecycleService {
    final static String MY_ACTION = "MY_ACTION";

    final String URL1 = "http://mattmahoney.net/dc/enwik8.pmd";
    private static String dirPath;
    int downloadIdOne;
    NotificationHelper mNotificationHelper;
    MainActivity mainActivity;

    @Override
    public void onCreate() {
        super.onCreate();
        mainActivity=new MainActivity();
        mNotificationHelper = new NotificationHelper(this);
        dirPath = Utils.getRootDirPath(getApplicationContext());

    }

    private void sendOnchannel1(String title, String message) {
       // NotificationCompat.Builder nb = mNotificationHelper.getChannel1Notification(title, message);
      //  mNotificationHelper.getManager().notify(1, nb.build());

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("nnull","unn");
        String downloadstatus = intent.getStringExtra("downloadstatus");
        String schedulestatus = intent.getStringExtra("schedulestatus");
        String removeschedulling = intent.getStringExtra("scheduleoption");
        if(removeschedulling!=null){
            if(removeschedulling.equals("removeschedulling")){
                MozzDownloader.cancel(downloadIdOne);
                Notification notification=new NotificationCompat.Builder(getApplicationContext(),channel1ID)
                        .setContentTitle("mandy.pmd").setContentText("Schedule Terminated").setSmallIcon(R.drawable.ic_launcher_background).build();
                startForeground(1,notification);
                return START_STICKY;
            }
        }
        if(schedulestatus!=null)
        {
            if(schedulestatus.equals("scheduled")){

                Notification notification=new NotificationCompat.Builder(getApplicationContext(),channel1ID)
                        .setContentTitle("mandy.pmd").setContentText("Download Schedulled").setSmallIcon(R.drawable.ic_launcher_background).build();
                startForeground(1,notification);
                if(downloadstatus!=null){
                    if(downloadstatus.equals("cancelled")){
                        MozzDownloader.cancel(downloadIdOne);
                        return START_STICKY;
                    }
                    if (Status.QUEUED == MozzDownloader.getStatus(downloadIdOne)) {
                        MozzDownloader.pause(downloadIdOne);
                        return START_STICKY;
                }

                }
                if (Status.PAUSED == MozzDownloader.getStatus(downloadIdOne)) {
                    MozzDownloader.resume(getApplicationContext(), this, downloadIdOne);
                    return START_STICKY;
                }
                int totaldiffminutes = intent.getIntExtra("scheduletime",0);
                downloadIdOne = MozzDownloader.download(URL1, dirPath, "mandys.pmd")
                .build()
                .setOnStartOrResumeListener(new OnStartOrResumeListener() {
                    @Override
                    public void onStartOrResume() {
                        Notification notification=new NotificationCompat.Builder(getApplicationContext(),channel1ID)
                                .setContentTitle("mandy.pmd").setContentText("Download Running").setSmallIcon(R.drawable.ic_launcher_background).build();
                        startForeground(1,notification);
                        Intent intent = new Intent();
                        intent.setAction(MY_ACTION);
                        intent.putExtra("Statusofdownload","started");
                        sendBroadcast(intent);
                        sendOnchannel1("mandy.pmd", "Download Running");
                    }
                })
                .setOnPauseListener(new OnPauseListener() {
                    @Override
                    public void onPause() {
                        Notification notification=new NotificationCompat.Builder(getApplicationContext(),channel1ID)
                                .setContentTitle("mandy.pmd").setContentText("Download  Paused").setSmallIcon(R.drawable.ic_launcher_background).build();
                        startForeground(1,notification);

                        sendOnchannel1("mandy.pmd", "Download Paused");
                        Intent intent = new Intent();
                        intent.setAction(MY_ACTION);

                        intent.putExtra("Statusofdownload","paused");
                        sendBroadcast(intent);

                    }
                })
                .setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel() {
                        Notification notification=new NotificationCompat.Builder(getApplicationContext(),channel1ID)
                                .setContentTitle("mandy.pmd").setContentText("Download Cancelled").setSmallIcon(R.drawable.ic_launcher_background).build();
                        startForeground(1,notification);
                        Intent intent = new Intent();
                        intent.setAction(MY_ACTION);
                        intent.putExtra("Statusofdownload","cancelled");
                        sendBroadcast(intent);
                    }
                })
                .setOnProgressListener(new OnProgressListener() {
                    @Override
                    public void onProgress(Progress progress) {
                        if(progress.totalBytes==0 && progress.currentBytes==0){
                            //progressBarOne.setProgress(0);
                            return;
                        }
                        long progressPercent = progress.currentBytes * 100 / progress.totalBytes;
                        Log.d("rrhy",progressPercent+"");
                        Intent intent = new Intent();
                        intent.setAction(MY_ACTION);

                        intent.putExtra("DATAPASSED",progressPercent);

                        sendBroadcast(intent);
                    }
                }).schedule(getApplicationContext(), this, totaldiffminutes);
                Log.d("toasti","dosti");
            }
        }
        else{
            if(downloadstatus!=null){
                if(downloadstatus.equals("cancelled")){
                    MozzDownloader.cancel(downloadIdOne);
                    return START_STICKY;
                }
            }
            if (Status.QUEUED == MozzDownloader.getStatus(downloadIdOne)) {
                MozzDownloader.pause(downloadIdOne);
                return START_STICKY;
            }
            if (Status.PAUSED == MozzDownloader.getStatus(downloadIdOne)) {
                MozzDownloader.resume(getApplicationContext(), this, downloadIdOne);
                return START_STICKY;
            }

            downloadIdOne = MozzDownloader.download(URL1, dirPath, "mandys.pmd")
                    .build()
                    .setOnStartOrResumeListener(new OnStartOrResumeListener() {
                        @Override
                        public void onStartOrResume() {
                            Notification notification=new NotificationCompat.Builder(getApplicationContext(),channel1ID)
                                    .setContentTitle("mandy.pmd").setContentText("Download Running").setSmallIcon(R.drawable.ic_launcher_background).build();
                            startForeground(1,notification);
                            Intent intent = new Intent();
                            intent.setAction(MY_ACTION);
                            intent.putExtra("Statusofdownload","started");
                            sendBroadcast(intent);
                            sendOnchannel1("mandy.pmd", "Download Running");
                        }
                    })
                    .setOnPauseListener(new OnPauseListener() {
                        @Override
                        public void onPause() {
                            Notification notification=new NotificationCompat.Builder(getApplicationContext(),channel1ID)
                                    .setContentTitle("mandy.pmd").setContentText("Download  Paused").setSmallIcon(R.drawable.ic_launcher_background).build();
                            startForeground(1,notification);

                            sendOnchannel1("mandy.pmd", "Download Paused");
                            Intent intent = new Intent();
                            intent.setAction(MY_ACTION);

                            intent.putExtra("Statusofdownload","paused");
                            sendBroadcast(intent);

                        }
                    })
                    .setOnCancelListener(new OnCancelListener() {
                        @Override
                        public void onCancel() {
                            Notification notification=new NotificationCompat.Builder(getApplicationContext(),channel1ID)
                                    .setContentTitle("mandy.pmd").setContentText("Download Cancelled").setSmallIcon(R.drawable.ic_launcher_background).build();
                            startForeground(1,notification);
                            Intent intent = new Intent();
                            intent.setAction(MY_ACTION);
                            intent.putExtra("Statusofdownload","cancelled");
                            sendBroadcast(intent);
                        }
                    })
                    .setOnProgressListener(new OnProgressListener() {
                        @Override
                        public void onProgress(Progress progress) {
                            if(progress.totalBytes==0 && progress.currentBytes==0){
                                //progressBarOne.setProgress(0);
                                return;
                            }
                            long progressPercent = progress.currentBytes * 100 / progress.totalBytes;
                            Log.d("rrhy",progressPercent+"");
                            Intent intent = new Intent();
                            intent.setAction(MY_ACTION);

                            intent.putExtra("DATAPASSED",progressPercent);

                            sendBroadcast(intent);
                        }
                    })
                    .start(getApplicationContext(),this, new OnDownloadListener() {
                        @Override
                        public void onDownloadComplete() {
                            sendOnchannel1("mandy.pmd", "Download Completed");
                        }

                        @Override
                        public void onError(Error error) {
                            downloadIdOne = 0;
                            sendOnchannel1("mandy.pmd", "Download Error");
                        }
                    });
        }

        return super.onStartCommand(intent, flags, startId);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return null;
    }
}
