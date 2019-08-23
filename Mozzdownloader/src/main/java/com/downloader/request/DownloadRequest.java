

package com.downloader.request;

import android.content.Context;
import android.util.Log;

import com.downloader.Error;
import com.downloader.OnCancelListener;
import com.downloader.OnDownloadListener;
import com.downloader.OnPauseListener;
import com.downloader.OnProgressListener;
import com.downloader.OnStartOrResumeListener;
import com.downloader.Priority;
import com.downloader.Response;
import com.downloader.Status;
import com.downloader.core.Core;
import com.downloader.internal.ComponentHolder;
import com.downloader.internal.DownloadRequestQueue;
import com.downloader.internal.SynchronousCall;
import com.downloader.utils.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

import androidx.lifecycle.LifecycleOwner;
import androidx.work.WorkManager;



public class DownloadRequest {
    public UUID uuid;
    private Priority priority;
    private Object tag;
    private String url;
    private String dirPath;
    private String fileName;
    private int sequenceNumber;
    private Future future;
    private long downloadedBytes;
    private long totalBytes;
    private int readTimeout;
    private int connectTimeout;
    private String userAgent;
    private OnProgressListener onProgressListener;
    private OnDownloadListener onDownloadListener;
    private OnStartOrResumeListener onStartOrResumeListener;
    private OnPauseListener onPauseListener;
    private OnCancelListener onCancelListener;
    private int downloadId;
    private HashMap<String, List<String>> headerMap;
    private Status status;

    DownloadRequest(DownloadRequestBuilder builder) {
        this.url = builder.url;
        this.dirPath = builder.dirPath;
        this.fileName = builder.fileName;
        this.headerMap = builder.headerMap;
        this.priority = builder.priority;
        this.tag = builder.tag;
        this.readTimeout =
                builder.readTimeout != 0 ?
                        builder.readTimeout :
                        getReadTimeoutFromConfig();
        this.connectTimeout =
                builder.connectTimeout != 0 ?
                        builder.connectTimeout :
                        getConnectTimeoutFromConfig();
        this.userAgent = builder.userAgent;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Object getTag() {
        return tag;
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDirPath() {
        return dirPath;
    }

    public void setDirPath(String dirPath) {
        this.dirPath = dirPath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public HashMap<String, List<String>> getHeaders() {
        return headerMap;
    }

    public Future getFuture() {
        return future;
    }

    public void setFuture(Future future) {
        this.future = future;
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }

    public void setDownloadedBytes(long downloadedBytes) {
        this.downloadedBytes = downloadedBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public String getUserAgent() {
        if (userAgent == null) {
            userAgent = ComponentHolder.getInstance().getUserAgent();
        }
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public int getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(int downloadId) {
        this.downloadId = downloadId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public OnProgressListener getOnProgressListener() {
        return onProgressListener;
    }

    public DownloadRequest setOnStartOrResumeListener(OnStartOrResumeListener onStartOrResumeListener) {
        this.onStartOrResumeListener = onStartOrResumeListener;
        return this;
    }

    public DownloadRequest setOnProgressListener(OnProgressListener onProgressListener) {
        this.onProgressListener = onProgressListener;
        return this;
    }

    public DownloadRequest setOnPauseListener(OnPauseListener onPauseListener) {
        this.onPauseListener = onPauseListener;
        return this;
    }

    public DownloadRequest setOnCancelListener(OnCancelListener onCancelListener) {
        this.onCancelListener = onCancelListener;
        return this;
    }

    public int start(Context context, LifecycleOwner lifecycleOwner, OnDownloadListener onDownloadListener) {
        this.onDownloadListener = onDownloadListener;
        downloadId = Utils.getUniqueId(url, dirPath, fileName);
        DownloadRequestQueue.getInstance().addRequest(this,lifecycleOwner,context);
        return downloadId;
    }

    public Response executeSync() {
        downloadId = Utils.getUniqueId(url, dirPath, fileName);
        return new SynchronousCall(this).execute();
    }

    public void deliverError(final Error error) {
        if (status != Status.CANCELLED) {
            setStatus(Status.FAILED);
            Core.getInstance().getExecutorSupplier().forMainThreadTasks()
                    .execute(new Runnable() {
                        public void run() {
                            if (onDownloadListener != null) {
                                onDownloadListener.onError(error);
                            }
                            finish();
                        }
                    });
        }
    }

    public void deliverSuccess() {
        if (status != Status.CANCELLED) {
            setStatus(Status.COMPLETED);
            Core.getInstance().getExecutorSupplier().forMainThreadTasks()
                    .execute(new Runnable() {
                        public void run() {
                            if (onDownloadListener != null) {
                                onDownloadListener.onDownloadComplete();
                            }
                            finish();
                        }
                    });
        }
    }

    public void deliverStartEvent() {
        if (status != Status.CANCELLED) {
            Log.d("cvcv","ecc");
            //Core.getInstance().getExecutorSupplier().forMainThreadTasks().execute();
            //Core.getInstance().getExecutorSupplier().forBackgroundTasks().execute();
            Core.getInstance().getExecutorSupplier().forMainThreadTasks()
                    .execute(new Runnable() {
                        public void run() {
                            if (onStartOrResumeListener != null) {
                                Log.d("newboss","hoja");
                                onStartOrResumeListener.onStartOrResume();
                            }
                        }
                    });
        }
    }

    public void deliverPauseEvent() {
        if (status != Status.CANCELLED) {
            Core.getInstance().getExecutorSupplier().forMainThreadTasks()
                    .execute(new Runnable() {
                        public void run() {
                            if (onPauseListener != null) {
                                Log.d("mandys","bros");
                                onPauseListener.onPause();
                            }
                        }
                    });
        }
    }

    public void deliverCancelEvent() {
        Core.getInstance().getExecutorSupplier().forMainThreadTasks()
                .execute(new Runnable() {
                    public void run() {
                        if (onCancelListener != null) {
                            onCancelListener.onCancel();
                        }
                    }
                });
    }

    public void cancel() {
        Log.d("number1","times");
        status=Status.CANCELLED;
        this.deliverCancelEvent();
        Utils.deleteTempFileAndDatabaseEntryInBackground(Utils.getTempPath(this.getDirPath(), this.getFileName()), this.getDownloadId());
        WorkManager.getInstance().cancelWorkById(this.uuid);
        //Utils.deleteTempFileAndDatabaseEntryInBackground(Utils.getTempPath(dirPath, fileName), downloadId);
    }

    private void finish() {
        destroy();
        DownloadRequestQueue.getInstance().finish(this);
    }

    private void destroy() {
        this.onProgressListener = null;
        this.onDownloadListener = null;
        this.onStartOrResumeListener = null;
        this.onPauseListener = null;
        this.onCancelListener = null;
    }

    private int getReadTimeoutFromConfig() {
        return ComponentHolder.getInstance().getReadTimeout();
    }

    private int getConnectTimeoutFromConfig() {
        return ComponentHolder.getInstance().getConnectTimeout();
    }


    public int schedule(Context context, LifecycleOwner lifecycleOwner, int totaldiffminutes) {
        downloadId = Utils.getUniqueId(url, dirPath, fileName);
        DownloadRequestQueue.getInstance().addRequestaftermin(this,lifecycleOwner,context,totaldiffminutes);
        return downloadId;
    }
}
