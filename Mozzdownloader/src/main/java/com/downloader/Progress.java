
package com.downloader;

import java.io.Serializable;

public class Progress implements Serializable {

    public long currentBytes;
    public long totalBytes;
    public  int prog;

//    public Progress2(long currentBytes, long totalBytes) {
//        this.currentBytes = currentBytes;
//        this.totalBytes = totalBytes;
//    }

//    public Progress(int ggh) {
//        this.prog=ggh;
//    }

    public Progress(long downloadedBytes, long totalBytes) {
        this.currentBytes=downloadedBytes;
        this.totalBytes=totalBytes;
    }

    @Override
    public String toString() {
        return "Progress{" +
                "currentBytes=" + currentBytes +
                ", totalBytes=" + totalBytes +
                '}';
    }
}
