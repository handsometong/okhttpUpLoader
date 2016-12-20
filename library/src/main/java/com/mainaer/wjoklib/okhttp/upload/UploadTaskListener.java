package com.mainaer.wjoklib.okhttp.upload;


import java.io.File;

/**
 * Created by wangjian on 15/11/21.
 */
public interface UploadTaskListener {

    /**
     * 上传中
     *
     * @param percent
     * @param uploadTask
     */
    void onUploading(UploadTask uploadTask, String percent, int position);


    /**
     * 上传成功
     *
     * @param file
     * @param uploadTask
     */
    void onUploadSuccess(UploadTask uploadTask, File file);

    /**
     * 上传失败
     *
     * @param uploadTask
     * @param errorCode    {@link UploadStatus}
     */
    void onError(UploadTask uploadTask, int errorCode, int position);
    /**
     * 上传暂停
     *
     * @param uploadTask
     *
     */
    void onPause(UploadTask uploadTask);

}
