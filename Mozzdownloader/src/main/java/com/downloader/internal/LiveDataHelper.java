package com.downloader.internal;

import com.downloader.request.DownloadRequest;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

public class LiveDataHelper {
    private MediatorLiveData<DownloadRequest> _DownloadRequest = new MediatorLiveData<>();
    private LiveDataHelper() {
    }
    private static LiveDataHelper liveDataHelper;
    synchronized public static LiveDataHelper getInstance() {
        if (liveDataHelper == null)
            liveDataHelper = new LiveDataHelper();
        return liveDataHelper;
    }
    void updatePercentage(DownloadRequest dr) {
        _DownloadRequest.postValue(dr);
    }
    LiveData<DownloadRequest> observeRequest() {
        return _DownloadRequest;
    }
}