
package com.downloader.internal;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.downloader.Constants;
import com.downloader.Progress;
import com.downloader.Response;
import com.downloader.Status;
import com.downloader.core.Core;
import com.downloader.handler.ProgressHandler;
import com.downloader.request.DownloadRequest;
import com.downloader.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;



public class DownloadRequestQueue {
    private ProgressHandler progressHandler;
    private static DownloadRequestQueue instance;
    private LiveDataHelper liveDataHelper;
    private final Map<Integer, DownloadRequest> currentRequestMap;
    private final AtomicInteger sequenceGenerator;
    Status previousstatus;
    private DownloadRequestQueue() {
        currentRequestMap = new ConcurrentHashMap<>();
        sequenceGenerator = new AtomicInteger();
        liveDataHelper=LiveDataHelper.getInstance();
    }

    public static void initialize() {
        getInstance();
    }

    public static DownloadRequestQueue getInstance() {
        if (instance == null) {
            synchronized (DownloadRequestQueue.class) {
                if (instance == null) {
                    instance = new DownloadRequestQueue();
                }
            }
        }
        return instance;
    }

    private int getSequenceNumber() {
        return sequenceGenerator.incrementAndGet();
    }

    public void pause(int downloadId) {
        DownloadRequest request = currentRequestMap.get(downloadId);
        if (request != null) {
            request.setStatus(Status.PAUSED);
        }
    }

    public void resume(final Context context, LifecycleOwner lifecycleOwner, int downloadId) {
        final DownloadRequest request = currentRequestMap.get(downloadId);
        Log.d("anu","shika");
        if (request==null){
            Log.d("anu","shika");
        }
        if (request != null) {
            request.setStatus(Status.QUEUED);
//            request.setFuture(Core.getInstance()
//                    .getExecutorSupplier()
//                    .forDownloadTasks()
//                    .submit(new DownloadRunnable(request)));
            final Response response = new Response();
            currentRequestMap.put(request.getDownloadId(), request);
            request.setStatus(Status.QUEUED);
            request.setSequenceNumber(getSequenceNumber());

            Log.d("blocks","qqw");

            //request object is serialised
            Gson gson=new GsonBuilder().serializeNulls().create();
            String json=gson.toJson(request);
            SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor prefsEditor = mPrefs.edit();
            prefsEditor.putString("SerializableObject", json);
            prefsEditor.commit();
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MyWorker.class).setConstraints(constraints).build();
            //OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MyWorker.class).build();
            request.uuid=workRequest.getId();
            WorkManager.getInstance().enqueue(workRequest);
            LiveDataHelper.getInstance().observeRequest()
                    .observe(lifecycleOwner, new Observer<DownloadRequest>() {
                        @Override
                        public void onChanged(DownloadRequest downloadRequest) {
                            Log.d("aamir",request.getStatus()+"");
//                        if (request.getStatus()==Status.CANCELLED){
//                            downloadRequest.setDownloadedBytes(0);
//                        }
                            if(request.getStatus()==Status.PAUSED){
                                Log.d("fissuveer","soupoveer");
                                request.deliverPauseEvent();

                                WorkManager.getInstance().cancelWorkById(request.uuid);

//                            Utils.deleteTempFileAndDatabaseEntryInBackground(Utils.getTempPath(request.getDirPath(), request.getFileName()), request.getDownloadId());
//                            //WorkManager.getInstance().cancelWorkById(request.uuid);
//                            request.deliverCancelEvent();
//                            //request.setDownloadedBytes(0);
//                            //request.setDownloadId(0);
                                return;
//
//
                            }
//                        if(request.getStatus()==Status.PAUSED){
//                            Log.d("stages","finale");
//                            WorkManager.getInstance().cancelWorkById(request.uuid);
//                            request.deliverPauseEvent();
//
//                        }
                            if(request.getDownloadedBytes()==0){
                                request.deliverStartEvent();
                            }
                            progressHandler
                                    .obtainMessage(Constants.UPDATE,
                                            new Progress(downloadRequest.getDownloadedBytes(),
                                                    downloadRequest.getTotalBytes())).sendToTarget();

                        }
                    });
            WorkManager.getInstance().getWorkInfoByIdLiveData(workRequest.getId()).observe(lifecycleOwner, new Observer<WorkInfo>() {
                @Override
                public void onChanged(@Nullable WorkInfo workInfo) {
                    Log.d("abd","see");

                    if(workInfo==null) {
                        Log.d("bug", request.getStatus()+"");
//                    previousstatus=request.getStatus();
                        //request.setStatus(Status.SCHEDULED);
                    }
                    if (workInfo != null) {
                        if(!workInfo.getState().isFinished()){
                            Log.d("bvbv","vbvb");
                            //request.setStatus(Status.SCHEDULED);
                        }

                        Log.d("fiss4.9",request.getStatus()+"");
                        if (request.getStatus()==Status.CANCELLED){
                            //request.deliverCancelEvent();
                            //request.setStatus(Status.UNKNOWN);
                            request.setOnProgressListener(null);
                            request.setDownloadedBytes(0);
                            liveDataHelper.updatePercentage(request);
                        }
                        if(request.getStatus()==Status.PAUSED){
                            Log.d("biss","toup");
                            //WorkManager.getInstance().cancelWorkById(request.uuid);



                        }
                        Log.d("boss2", "pls2");
                        if (workInfo.getState().isFinished()) {
//                        if (request.getStatus()==Status.SCHEDULED){
//                            request.setStatus(Status.QUEUED);
//                        }
                            Log.d("boss3", "pls3");
                            Data data = workInfo.getOutputData();
                            String output = data.getString("Response");
                            if(output!=null){
                                Log.d("boss4", output);
                                if (output.equals(Status.CANCELLED + "")) {
                                    response.setCancelled(true);
                                }
                                if (output.equals(Status.PAUSED + "")) {
                                    response.setPaused(true);
                                }
                                if (output.equals("succ")) {
                                    response.setSuccessful(true);
                                }
                                if (output.equals("abc")) {
                                    Error error = new Error();
                                    //error.setConnectionError(true);
                                    //error.setConnectionException(new Exception());
                                    //response.setError(error);
                                }
                                if (output.equals("def")) {
                                    Error error = new Error();
                                    //error.setServerError(true);
                                    //response.setError(error);
                                }

                                if (response.isSuccessful()) {
                                    request.deliverSuccess();
                                } else if (response.isPaused()) {
                                    request.deliverPauseEvent();
                                } else if (response.getError() != null) {
                                    //request.deliverError(response.getError());
                                } else if (!response.isCancelled()) {
                                    // request.deliverError(new Error());
                                }
                            }


                        }
                    }
                }
            });
        }
    }

    private void cancelAndRemoveFromMap(DownloadRequest request) {
        if (request != null) {
            request.cancel();
            currentRequestMap.remove(request.getDownloadId());
        }
    }

    public void cancel(int downloadId) {
        DownloadRequest request = currentRequestMap.get(downloadId);
        cancelAndRemoveFromMap(request);

    }

    public void cancel(Object tag) {
        for (Map.Entry<Integer, DownloadRequest> currentRequestMapEntry : currentRequestMap.entrySet()) {
            DownloadRequest request = currentRequestMapEntry.getValue();
            if (request.getTag() instanceof String && tag instanceof String) {
                final String tempRequestTag = (String) request.getTag();
                final String tempTag = (String) tag;
                if (tempRequestTag.equals(tempTag)) {
                    cancelAndRemoveFromMap(request);
                }
            } else if (request.getTag().equals(tag)) {
                cancelAndRemoveFromMap(request);
            }
        }
    }

    public void cancelAll() {
        for (Map.Entry<Integer, DownloadRequest> currentRequestMapEntry : currentRequestMap.entrySet()) {
            DownloadRequest request = currentRequestMapEntry.getValue();
            cancelAndRemoveFromMap(request);
        }
    }

    public Status getStatus(int downloadId) {
        DownloadRequest request = currentRequestMap.get(downloadId);
        Log.d("ddf",currentRequestMap.get(downloadId)+"");
        if (request != null) {
            return request.getStatus();
        }

        return Status.UNKNOWN;
    }

    public void addRequest(final DownloadRequest request, LifecycleOwner lifecycleOwner, final Context context) {
        Log.d("buggybug",request.getFileName()+"");
        final Response response = new Response();
        request.setStatus(Status.QUEUED);
        currentRequestMap.put(request.getDownloadId(), request);

        //instantiate the progress handler
        if (request.getOnProgressListener() != null) {
            progressHandler = new ProgressHandler(request.getOnProgressListener());
        }

        //Set the status and the sequence number
        request.setSequenceNumber(getSequenceNumber());

        //request object is serialised
        Gson gson=new GsonBuilder().serializeNulls().create();
        String json=gson.toJson(request);
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        prefsEditor.putString("SerializableObject", json);
        prefsEditor.commit();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MyWorker.class).setConstraints(constraints).build();
        request.uuid=workRequest.getId();
        WorkManager.getInstance().enqueue(workRequest);
        LiveDataHelper.getInstance().observeRequest()
                .observe(lifecycleOwner, new Observer<DownloadRequest>() {
                    @Override
                    public void onChanged(DownloadRequest downloadRequest) {
                        Log.d("aamir",request.getStatus()+"");
//                        if (request.getStatus()==Status.CANCELLED){
//                            downloadRequest.setDownloadedBytes(0);
//                        }
                        if(request.getStatus()==Status.PAUSED){
                            Log.d("fissuveer","soupoveer");
                            request.deliverPauseEvent();

                            WorkManager.getInstance().cancelWorkById(request.uuid);

//                            Utils.deleteTempFileAndDatabaseEntryInBackground(Utils.getTempPath(request.getDirPath(), request.getFileName()), request.getDownloadId());
//                            //WorkManager.getInstance().cancelWorkById(request.uuid);
//                            request.deliverCancelEvent();
//                            //request.setDownloadedBytes(0);
//                            //request.setDownloadId(0);
                            return;
//
//
                        }
//                        if(request.getStatus()==Status.PAUSED){
//                            Log.d("stages","finale");
//                            WorkManager.getInstance().cancelWorkById(request.uuid);
//                            request.deliverPauseEvent();
//
//                        }
                        if(request.getDownloadedBytes()==0){
                            Log.d("tttio","pppooo");
                            request.deliverStartEvent();
                        }
                        progressHandler
                                .obtainMessage(Constants.UPDATE,
                                        new Progress(downloadRequest.getDownloadedBytes(),
                                                downloadRequest.getTotalBytes())).sendToTarget();

                    }
                });
        WorkManager.getInstance().getWorkInfoByIdLiveData(workRequest.getId()).observe(lifecycleOwner, new Observer<WorkInfo>() {
            @Override
            public void onChanged(@Nullable WorkInfo workInfo) {
                Log.d("abd","see");

                if(workInfo==null) {
                    Log.d("bug", request.getStatus()+"");
//                    previousstatus=request.getStatus();
                    //request.setStatus(Status.SCHEDULED);
                }
                if (workInfo != null) {
                    if(!workInfo.getState().isFinished()){
                        Log.d("bvbv","vbvb");
                        //request.setStatus(Status.SCHEDULED);
                    }

                    Log.d("fiss4.9",request.getStatus()+"");
                    if (request.getStatus()==Status.CANCELLED){
                        //request.deliverCancelEvent();
                        //request.setStatus(Status.UNKNOWN);
                        request.setOnProgressListener(null);
                        request.setDownloadedBytes(0);
                        liveDataHelper.updatePercentage(request);
                    }
                    if(request.getStatus()==Status.PAUSED){
                        Log.d("biss","toup");
                        //WorkManager.getInstance().cancelWorkById(request.uuid);



                    }
                    Log.d("boss2", "pls2");
                    if (workInfo.getState().isFinished()) {
//                        if (request.getStatus()==Status.SCHEDULED){
//                            request.setStatus(Status.QUEUED);
//                        }
                        Log.d("boss3", "pls3");
                        Data data = workInfo.getOutputData();
                        String output = data.getString("Response");
                        if(output!=null){
                            Log.d("boss4", output);
                            if (output.equals(Status.CANCELLED + "")) {
                                response.setCancelled(true);
                            }
                            if (output.equals(Status.PAUSED + "")) {
                                response.setPaused(true);
                            }
                            if (output.equals("succ")) {
                                response.setSuccessful(true);
                            }
                            if (output.equals("abc")) {
                                Error error = new Error();
                                //error.setConnectionError(true);
                                //error.setConnectionException(new Exception());
                                //response.setError(error);
                            }
                            if (output.equals("def")) {
                                Error error = new Error();
                                //error.setServerError(true);
                                //response.setError(error);
                            }

                            if (response.isSuccessful()) {
                                request.deliverSuccess();
                            } else if (response.isPaused()) {
                                request.deliverPauseEvent();
                            } else if (response.getError() != null) {
                                //request.deliverError(response.getError());
                            } else if (!response.isCancelled()) {
                                // request.deliverError(new Error());
                            }
                        }


                    }
                }
            }
        });
//        request.setFuture(Core.getInstance()
//                .getExecutorSupplier()
//                .forDownloadTasks()
//                .submit(new DownloadRunnable(request)));
    }

    public void finish(DownloadRequest request) {
        currentRequestMap.remove(request.getDownloadId());
    }

    public void addRequestaftermin(final DownloadRequest request, LifecycleOwner lifecycleOwner, Context context, int totaldiffminutes) {
        if (request.getOnProgressListener() != null) {
            Log.d("toll","lol");
            progressHandler = new ProgressHandler(request.getOnProgressListener());
        }

        final Response response = new Response();
        //
        request.setStatus(Status.SCHEDULED);
        currentRequestMap.put(request.getDownloadId(), request);

        request.setSequenceNumber(getSequenceNumber());

        Log.d("shijus",request.getOnProgressListener()+"");

        //request object is serialised
        Gson gson=new GsonBuilder().serializeNulls().create();
        String json=gson.toJson(request);
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        prefsEditor.putString("SerializableObject", json);
        prefsEditor.commit();
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MyWorker.class).setConstraints(constraints).setInitialDelay(totaldiffminutes,TimeUnit.MINUTES).build();
        request.uuid=workRequest.getId();
        WorkManager.getInstance().enqueue(workRequest);
        request.setStatus(Status.SCHEDULED);
        LiveDataHelper.getInstance().observeRequest()
                .observe(lifecycleOwner, new Observer<DownloadRequest>() {
                    @Override
                    public void onChanged(DownloadRequest downloadrequest) {
//                        if (request.getStatus()==Status.CANCELLED){
//                            Log.d("uuli","were");
//                            WorkManager.getInstance().cancelWorkById(request.uuid);
////                            Gson gson=new GsonBuilder().serializeNulls().create();
////                            String json=gson.toJson(request.getStatus());
////                            SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
////                            SharedPreferences.Editor prefsEditor = mPrefs.edit();
////                            prefsEditor.putString("SerializableObject3", json);
////                            prefsEditor.commit();
//                            Utils.deleteTempFileAndDatabaseEntryInBackground(Utils.getTempPath(request.getDirPath(), request.getFileName()), request.getDownloadId());
//                            request.deliverCancelEvent();
//                            return;
//
//                        }
                        if(request.getStatus()==Status.PAUSED){
                            Log.d("stages","finale");
                            request.deliverPauseEvent();
                            WorkManager.getInstance().cancelWorkById(request.uuid);
                            return;
                        }
                        Log.d("poiuy",downloadrequest.getDownloadedBytes()+"");
                        if(downloadrequest.getDownloadedBytes()!=0){
                            Log.d("danny",request.getStatus()+"");
                            if (request.getStatus()==Status.SCHEDULED){
                                Log.d("kinny","binyy");
                                request.setStatus(Status.QUEUED);
                            }
                        }
                        if (request.getDownloadedBytes()==0){
                            request.deliverStartEvent();
                        }
                        progressHandler
                                .obtainMessage(Constants.UPDATE,
                                        new Progress(downloadrequest.getDownloadedBytes(),
                                                downloadrequest.getTotalBytes())).sendToTarget();

                    }
                });
        WorkManager.getInstance().getWorkInfoByIdLiveData(workRequest.getId()).observe(lifecycleOwner, new Observer<WorkInfo>() {
            @Override
            public void onChanged(@Nullable WorkInfo workInfo) {
                Log.d("abd","see");
                Log.d("ffq",workInfo+"");
                if(!workInfo.getState().isFinished()){
                    Log.d("bugsingh", request.getStatus()+"");
                    //previousstatus=request.getStatus();
                    //request.setStatus(Status.SCHEDULED);
                    //currentRequestMap.put(request.getDownloadId(), request);
                    Log.d("rrt",request.getStatus()+"");

                }

                if(workInfo==null) {
                    Log.d("bug", Status.CANCELLED+"");
                }
                if (workInfo != null) {
                    if(request.getStatus()==Status.CANCELLED){
                        Log.d("fiss5","soup");
                       // WorkManager.getInstance().cancelWorkById(request.uuid);
//                        Gson gson=new GsonBuilder().serializeNulls().create();
//                        String json=gson.toJson(request.getStatus());
//                        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
//                        SharedPreferences.Editor prefsEditor = mPrefs.edit();
//                        prefsEditor.putString("SerializableObject3", json);
//                        prefsEditor.commit();
                       // Utils.deleteTempFileAndDatabaseEntryInBackground(Utils.getTempPath(request.getDirPath(), request.getFileName()), request.getDownloadId());
                        //WorkManager.getInstance().cancelWorkById(request.uuid);
                       // request.setDownloadedBytes(0);
                        //request.setDownloadId(0);
                        //return;


                    }
                    if(request.getStatus()==Status.PAUSED){
                        Log.d("biss","toup");
                        //WorkManager.getInstance().cancelWorkById(request.uuid);
                        //return;


                    }
                    Log.d("boss2", "pls2");
                    if (workInfo.getState().isFinished()) {
                        if (workInfo.getState().isFinished()) {
//                            if (request.getStatus() == Status.SCHEDULED) {
//                                request.setStatus(Status.QUEUED);
//                            }
                        }
                        Log.d("boss3", "pls3");
                        Data data = workInfo.getOutputData();
                        String output = data.getString("Response");
                        if(output!=null){
                            Log.d("boss4", output);
                            if (output.equals(Status.CANCELLED + "")) {
                                response.setCancelled(true);
                            }
                            if (output.equals(Status.PAUSED + "")) {
                                response.setPaused(true);
                            }
                            if (output.equals("succ")) {
                                response.setSuccessful(true);
                            }
                            if (output.equals("abc")) {
                                Error error = new Error();
                                //error.setConnectionError(true);
                                //error.setConnectionException(new Exception());
                                //response.setError(error);
                            }
                            if (output.equals("def")) {
                                Error error = new Error();
                                //error.setServerError(true);
                                //response.setError(error);
                            }

                            if (response.isSuccessful()) {
                                request.deliverSuccess();
                            } else if (response.isPaused()) {
                                request.deliverPauseEvent();
                            } else if (response.getError() != null) {
                                //request.deliverError(response.getError());
                            } else if (!response.isCancelled()) {
                                // request.deliverError(new Error());
                            }
                        }


                    }
                }
            }
        });

    }


}
