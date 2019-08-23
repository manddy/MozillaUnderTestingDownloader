package com.downloader.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.downloader.Constants;
import com.downloader.Progress;
import com.downloader.Response;
import com.downloader.Status;
import com.downloader.database.DownloadModel;
import com.downloader.handler.ProgressHandler;
import com.downloader.httpclient.HttpClient;
import com.downloader.internal.stream.FileDownloadOutputStream;
import com.downloader.internal.stream.FileDownloadRandomAccessFile;
import com.downloader.request.DownloadRequest;
import com.downloader.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class MyWorker extends Worker   {
    //public final Priority priority;
    public int sequence;
    SharedPreferences mPrefs ;
    private LiveDataHelper liveDataHelper;
    private static final int BUFFER_SIZE = 1024 * 4;
    private static final long TIME_GAP_FOR_SYNC = 2000;
    private static final long MIN_BYTES_FOR_SYNC = 65536;
    private Map<Integer, DownloadRequest> currentRequestMap;
    private  DownloadRequest request;
    private ProgressHandler progressHandler;
    private long lastSyncTime;
    private long lastSyncBytes;
    private InputStream inputStream;
    private FileDownloadOutputStream outputStream;
    private HttpClient httpClient;
    private long totalBytes;
    private int responseCode;
    private String eTag;
    private boolean isResumeSupported;
    private String tempPath;

    public MyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        currentRequestMap=new ConcurrentHashMap<>();
        mPrefs=PreferenceManager.getDefaultSharedPreferences(context);
        liveDataHelper=LiveDataHelper.getInstance();
//        Log.d("con2",context+"");
//        int a = getInputData().getInt("ID",2);
//        String statu = getInputData().getString("status");
//        Gson gson = new Gson();
//        String json = mPrefs.getString("SerializableObject", "");
//        if(json.equals("")){
//            Log.d("noii","nn");
//        }
//        request = gson.fromJson(json, DownloadRequest.class);
//        Log.d("goi",request.getUserAgent()+"");
        //priority=request.getPriority();
//         sequence=request.getSequenceNumber();
    }
    @Override
    public void onStopped(){
        super.onStopped();


    }

    @NonNull
    @Override
    public Result doWork() {

        //pass input data
//        int a = getInputData().getInt("ID",2);
//        String statu = getInputData().getString("status");
        Gson gson=new GsonBuilder().serializeNulls().create();
        String json = mPrefs.getString("SerializableObject", "");

        request= gson.fromJson( json, DownloadRequest.class);
        sequence=request.getSequenceNumber();
        if(request!=null)
        {
            Log.d("soii",request.getStatus()+"");
        }
        else
            {
            Log.d("loii","toy");
        }
        Response response = new Response();
//        if (request.getStatus() == Status.CANCELLED) {
//            response.setCancelled(true);
//            Data output = new Data.Builder()
//                    .putString("Response",response+"")
//                    .build();
//            return Result.failure(output);
//        } else if (request.getStatus() == Status.PAUSED) {
//            response.setPaused(true);
//            Data output = new Data.Builder()
//                    .putString("Response",response+"")
//                    .build();
//            return Result.failure(output);
//        }

        try {
            Log.d("koko","gogo");
            if(request.getOnProgressListener()==null){
                Log.d("dope",request.getUrl()+"");


                //progressHandler = new ProgressHandler(request.getOnProgressListener());
            }
//            if (request.getOnProgressListener() != null) {
//                Log.d("toll","lol");
//                progressHandler = new ProgressHandler(request.getOnProgressListener());
//            }

            Log.d("suppp","ppp");
            tempPath = Utils.getTempPath(request.getDirPath(), request.getFileName());

            File file = new File(tempPath);

            DownloadModel model = getDownloadModelIfAlreadyPresentInDatabase();
            Log.d("dupp","nupp");
            if (model != null) {
                Log.d("love","no");
                if (file.exists()) {
                    request.setTotalBytes(model.getTotalBytes());
                    request.setDownloadedBytes(model.getDownloadedBytes());
                } else {
                    removeNoMoreNeededModelFromDatabase();
                    request.setDownloadedBytes(0);
                    request.setTotalBytes(0);
                    model = null;
                }
            }
            Log.d("ab","devel");
            httpClient = ComponentHolder.getInstance().getHttpClient();
            //Log.d("ggs","iipu");
            httpClient.connect(request);

            if (request.getStatus() == Status.CANCELLED) {
                response.setCancelled(true);
                Data output = new Data.Builder()
                        .putString("Response",response+"")
                        .build();
                return Result.failure(output);
            } else if (request.getStatus() == Status.PAUSED) {
                response.setPaused(true);
                Data output = new Data.Builder()
                        .putString("Response",response+"")
                        .build();
                return Result.failure(output);
            }

            httpClient = Utils.getRedirectedConnectionIfAny(httpClient, request);
            Log.d("kk",httpClient.getContentLength()+"");
            responseCode = httpClient.getResponseCode();

            eTag = httpClient.getResponseHeader(Constants.ETAG);
            Log.d("sipi","kipi");
            if (checkIfFreshStartRequiredAndStart(model)) {
                model = null;
                Log.d("lova","nova");
            }

            if (!isSuccessful()) {
                Log.d("lvv","nppp");
                Error error = new Error();
                //error.setServerError(true);
                //error.setServerErrorMessage(convertStreamToString(httpClient.getErrorStream()));
                //error.setHeaderFields(httpClient.getHeaderFields());
                //error.setResponseCode(responseCode);
                //response.setError(error);
                Data output = new Data.Builder()
                        .putString("Response","abc")
                        .build();
                return Result.failure(output);
            }

            setResumeSupportedOrNot();
            Log.d("coder","npppp");
            totalBytes = request.getTotalBytes();

            if (!isResumeSupported) {
                deleteTempFile();
            }

            if (totalBytes == 0) {
                totalBytes = httpClient.getContentLength();
                request.setTotalBytes(totalBytes);
            }

            if (isResumeSupported && model == null) {
                createAndInsertNewModel();
            }

            if (request.getStatus() == Status.CANCELLED) {
                response.setCancelled(true);
                Data output = new Data.Builder()
                        .putString("Response",response+"")
                        .build();
                return Result.failure(output);
            } else if (request.getStatus() == Status.PAUSED) {
                response.setPaused(true);
                Data output = new Data.Builder()
                        .putString("Response",response+"")
                        .build();
                return Result.failure(output);
            }

            Log.d("dodo","soso");

            //request.deliverStartEvent();
            Log.d("coco","momo");
            inputStream = httpClient.getInputStream();

            byte[] buff = new byte[BUFFER_SIZE];

            if (!file.exists()) {
                if (file.getParentFile() != null && !file.getParentFile().exists()) {
                    if (file.getParentFile().mkdirs()) {
                        //noinspection ResultOfMethodCallIgnored
                        file.createNewFile();
                    }
                } else {
                    //noinspection ResultOfMethodCallIgnored
                    file.createNewFile();
                }
            }

            this.outputStream = FileDownloadRandomAccessFile.create(file);

            if (isResumeSupported && request.getDownloadedBytes() != 0) {
                outputStream.seek(request.getDownloadedBytes());
            }

            Log.d("miggy","siggy");
            if (request.getStatus() == Status.CANCELLED) {
                response.setCancelled(true);
                Data output = new Data.Builder()
                        .putString("Response",response+"")
                        .build();
                return Result.failure(output);
            } else if (request.getStatus() == Status.PAUSED) {
                response.setPaused(true);
                Data output = new Data.Builder()
                        .putString("Response",response+"")
                        .build();
                return Result.failure(output);
            }


            do {


                Log.d("left","some");
                final int byteCount = inputStream.read(buff, 0, BUFFER_SIZE);

                if (byteCount == -1) {
                    break;
                }

                outputStream.write(buff, 0, byteCount);

                request.setDownloadedBytes(request.getDownloadedBytes() + byteCount);

                int percent = (int) ((request.getDownloadedBytes() * 100) /totalBytes);
                Log.d("per",percent+"");

                if(isStopped()){

                        //request.setDownloadedBytes(0);
                        //liveDataHelper.updatePercentage(request);
                        return Result.failure();


                }
                liveDataHelper.updatePercentage(request);

                //sendProgress();
                Log.d("now2","one2");
                syncIfRequired(outputStream);
                Log.d("common","yessin");

                if (request.getStatus() == Status.CANCELLED) {
                    response.setCancelled(true);
                    Data output = new Data.Builder()
                            .putString("Response",response+"")
                            .build();
                    Log.d("main","reason");
;                    return Result.failure(output);
                } else if (request.getStatus() == Status.PAUSED) {
                    sync(outputStream);
                    response.setPaused(true);
                    Data output = new Data.Builder()
                            .putString("Response",response+"")
                            .build();
                    return Result.failure(output);
                }

            } while (true );
//            if(isStopped()){
//
//                return Result.failure();
//            }
            final String path = Utils.getPath(request.getDirPath(), request.getFileName());
            //File file1 = new File(request.getDirPath() ,request.getFileName() );
            //file1.createNewFile();
            Log.d("beck",path);
//            File from = new File(Environment.getExternalStorageDirectory(),tempPath);
//            File to = new File(Environment.getExternalStorageDirectory(),path);
//                File oldfile=new File(tempPath);
//                File newfile=new File(path);
//                oldfile.renameTo(newfile);
            final File oldFile = new File(tempPath);
            try {

                final File newFile = new File(path);
                Log.d("initial",oldFile.getName()+newFile.getName()+"");
                if (newFile.exists()) {
                    Log.d("initial1",oldFile.getName()+newFile+"");

                    if (!newFile.delete()) {
                        Log.d("initial2",oldFile.getName()+newFile+"");
                        throw new IOException("Deletion Failed");
                    }
                }

                Boolean result=oldFile.renameTo(newFile);
                Log.d("rru",result+"");
                oldFile.renameTo(newFile);
                if (!oldFile.renameTo(newFile)) {
                    throw new IOException("Rename Failed");
                }

            } finally {
                Log.d("horse","corse");
                if (oldFile.exists()) {
                    Log.d("horse1","corse");
                    //noinspection ResultOfMethodCallIgnored
                    oldFile.delete();
                }
            }
           // Utils.renameFileName(tempPath, path);


            Log.d("dovuee1",tempPath);
            response.setSuccessful(true);
            Log.d("yup","dove");

            if (isResumeSupported) {
                removeNoMoreNeededModelFromDatabase();
            }

        } catch (IOException | IllegalAccessException e) {
            if (!isResumeSupported) {
                deleteTempFile();
            }
            Error error = new Error();
            //error.setConnectionError(true);
            //error.setConnectionException(e);
            //response.setError(error);
            Data output = new Data.Builder()
                    .putString("Response","def")
                    .build();
            return Result.retry();
        } finally {
            closeAllSafely(outputStream);
        }

        Log.d("final","done");
        Data output = new Data.Builder()
                .putString("Response","succ")
                .build();
        return Result.success(output);
    }
    private void deleteTempFile() {
        File file = new File(tempPath);
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    private boolean isSuccessful() {
        return responseCode >= HttpURLConnection.HTTP_OK
                && responseCode < HttpURLConnection.HTTP_MULT_CHOICE;
    }

    private void setResumeSupportedOrNot() {
        isResumeSupported = (responseCode == HttpURLConnection.HTTP_PARTIAL);
    }

    private boolean checkIfFreshStartRequiredAndStart(DownloadModel model) throws IOException,
            IllegalAccessException {
        if (responseCode == Constants.HTTP_RANGE_NOT_SATISFIABLE || isETagChanged(model)) {
            if (model != null) {
                removeNoMoreNeededModelFromDatabase();
            }
            deleteTempFile();
            request.setDownloadedBytes(0);
            request.setTotalBytes(0);
            httpClient = ComponentHolder.getInstance().getHttpClient();
            httpClient.connect(request);
            httpClient = Utils.getRedirectedConnectionIfAny(httpClient, request);
            responseCode = httpClient.getResponseCode();
            return true;
        }
        return false;
    }

    private boolean isETagChanged(DownloadModel model) {
        return !(eTag == null || model == null || model.getETag() == null)
                && !model.getETag().equals(eTag);
    }

    private DownloadModel getDownloadModelIfAlreadyPresentInDatabase() {
        return ComponentHolder.getInstance().getDbHelper().find(request.getDownloadId());
    }

    private void createAndInsertNewModel() {
        DownloadModel model = new DownloadModel();
        model.setId(request.getDownloadId());
        model.setUrl(request.getUrl());
        model.setETag(eTag);
        model.setDirPath(request.getDirPath());
        model.setFileName(request.getFileName());
        model.setDownloadedBytes(request.getDownloadedBytes());
        model.setTotalBytes(totalBytes);
        model.setLastModifiedAt(System.currentTimeMillis());
        ComponentHolder.getInstance().getDbHelper().insert(model);
    }

    private void removeNoMoreNeededModelFromDatabase() {
        ComponentHolder.getInstance().getDbHelper().remove(request.getDownloadId());
    }

//    private void sendProgress() {
//        if (request.getStatus() != Status.CANCELLED) {
//            Log.d("ttyl","ggyl");
////            if (progressHandler != null) {
//                Log.d("mozz","illa");
//
//
//            //}
////            else{
////                Log.d("maybe","into");
////            }
//        }
//    }

    private void syncIfRequired(FileDownloadOutputStream outputStream) {
        final long currentBytes = request.getDownloadedBytes();
        final long currentTime = System.currentTimeMillis();
        final long bytesDelta = currentBytes - lastSyncBytes;
        final long timeDelta = currentTime - lastSyncTime;
        if (bytesDelta > MIN_BYTES_FOR_SYNC && timeDelta > TIME_GAP_FOR_SYNC) {
            sync(outputStream);
            lastSyncBytes = currentBytes;
            lastSyncTime = currentTime;
        }
    }

    private void sync(FileDownloadOutputStream outputStream) {
        boolean success;
        try {
            outputStream.flushAndSync();
            success = true;
        } catch (IOException e) {
            success = false;
            e.printStackTrace();
        }
        if (success && isResumeSupported) {
            ComponentHolder.getInstance().getDbHelper()
                    .updateProgress(request.getDownloadId(),
                            request.getDownloadedBytes(),
                            System.currentTimeMillis());
        }

    }

    private void closeAllSafely(FileDownloadOutputStream outputStream) {
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            if (outputStream != null) {
                try {
                    sync(outputStream);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            if (outputStream != null)
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    private String convertStreamToString(InputStream stream) {
        StringBuilder stringBuilder = new StringBuilder();
        if (stream != null) {
            String line;
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(stream));
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
            } catch (IOException ignored) {

            } finally {
                try {
                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                } catch (NullPointerException | IOException ignored) {

                }
            }
        }
        return stringBuilder.toString();
    }
}
