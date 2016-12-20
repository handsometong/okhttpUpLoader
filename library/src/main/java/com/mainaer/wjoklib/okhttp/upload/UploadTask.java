/*
 * Copyright 2014-2016 wjokhttp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mainaer.wjoklib.okhttp.upload;

import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 上传线程
 *
 * @author hst
 * @date 2016/9/6 .
 */
    public class UploadTask implements Runnable {

    private static String FILE_MODE = "rwd";
    private OkHttpClient mClient;
    private SQLiteDatabase db;
    private UploadTaskListener mListener;

    private Builder mBuilder;
    private String id;// task id
    private String url;// file url
    private String fileName; // File name when saving
    private int uploadStatus;
    private int chunck, chuncks;//流块
    private int position;

    private int errorCode;
    static String BOUNDARY = "----------" + System.currentTimeMillis();
    public static final MediaType MEDIA_TYPE_MARKDOWN = MediaType.parse("multipart/form-data;boundary=" + BOUNDARY);

    private UploadTask(Builder builder) {
        mBuilder = builder;
        mClient = new OkHttpClient();
        this.id = mBuilder.id;
        this.url = mBuilder.url;
        this.fileName = mBuilder.fileName;
        this.uploadStatus = mBuilder.uploadStatus;
        this.chunck = mBuilder.chunck;
        this.setmListener(mBuilder.listener);
        // 以kb为计算单位
    }

    @Override
    public void run() {
        try {
            int blockLength = 1024 * 1024;
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator +fileName);
            if (file.length() % blockLength == 0) {
                chuncks = (int) file.length() / blockLength;
            } else {
                chuncks = (int) file.length() / blockLength + 1;

            }
            while (chunck <= chuncks&&uploadStatus!= UploadStatus.UPLOAD_STATUS_PAUSE&&uploadStatus!= UploadStatus.UPLOAD_STATUS_ERROR)
            {

                uploadStatus = UploadStatus.UPLOAD_STATUS_UPLOADING;
                Map<String, String> params = new HashMap<String, String>();
                params.put("name", fileName);
                params.put("chunks", chuncks + "");
                params.put("chunk", chunck + "");
                final byte[] mBlock = FileUtils.getBlock((chunck - 1) * blockLength, file, blockLength);
                MultipartBody.Builder builder = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM);
                addParams(builder, params);
                RequestBody requestBody = RequestBody.create(MEDIA_TYPE_MARKDOWN, mBlock);
                builder.addFormDataPart("mFile", fileName, requestBody);
                Request request = new Request.Builder()
                        .url(url+ "uploaderWithContinuinglyTransferring")
                        .post(builder.build())
                        .build();
                Response response = null;
                response = mClient.newCall(request).execute();
                if (response.isSuccessful()) {
                    onCallBack();
                    chunck++;
                   /* if (chunck <= chuncks) {
                         run();
                    }*/
                }
                else
                {
                    uploadStatus = UploadStatus.UPLOAD_STATUS_ERROR;
                    onCallBack();
                }

            }
        } catch (IOException e) {
            uploadStatus = UploadStatus.UPLOAD_STATUS_ERROR;
            onCallBack();
            e.printStackTrace();
        }
    }


/*    *//**
     * 删除数据库文件和已经上传的文件
     *//*
    public void cancel() {
        if (mListener != null)
            mListener.onCancel(UploadTask.this);
    }*/

    /**
     * 分发回调事件到ui层
     */
    private void onCallBack() {
        mHandler.sendEmptyMessage(uploadStatus);
        // 同步manager中的task信息
        //UploadManager.getInstance().updateUploadTask(this);
    }

    Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            int code = msg.what;
            switch (code) {
                // 上传失败
                case UploadStatus.UPLOAD_STATUS_ERROR:
                    mListener.onError(UploadTask.this, errorCode,position);
                    break;
                // 正在上传
                case UploadStatus.UPLOAD_STATUS_UPLOADING:
                    mListener.onUploading(UploadTask.this, getDownLoadPercent(), position);
                 // 暂停上传
                    break;
                case UploadStatus.UPLOAD_STATUS_PAUSE:
                    mListener.onPause(UploadTask.this);
                    break;

            }
        }
    };

    private String getDownLoadPercent() {
        String baifenbi = "0";// 接受百分比的值
        if (chunck >= chuncks) {
            return "100";
        }
        double baiy = chunck * 1.0;
        double baiz = chuncks * 1.0;
        // 防止分母为0出现NoN
        if (baiz > 0) {
            double fen = (baiy / baiz) * 100;
            //NumberFormat nf = NumberFormat.getPercentInstance();
            //nf.setMinimumFractionDigits(2); //保留到小数点后几位
            // 百分比格式，后面不足2位的用0补齐
            //baifenbi = nf.format(fen);
            //注释掉的也是一种方法
            DecimalFormat df1 = new DecimalFormat("0");//0.00
            baifenbi = df1.format(fen);
        }
        return baifenbi;
    }


    private String getFileNameFromUrl(String url) {
        if (!TextUtils.isEmpty(url)) {
            return url.substring(url.lastIndexOf("/") + 1);
        }
        return System.currentTimeMillis() + "";
    }

    private void close(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void setClient(OkHttpClient mClient) {
        this.mClient = mClient;
    }

    public Builder getBuilder() {
        return mBuilder;
    }

    public void setBuilder(Builder builder) {
        this.mBuilder = builder;
    }

    public String getId() {
        if (!TextUtils.isEmpty(id)) {
        } else {
            id = url;
        }
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getFileName() {
        return fileName;
    }


    public void setUploadStatus(int uploadStatus) {
        this.uploadStatus = uploadStatus;
    }

    public int getUploadStatus() {
        return uploadStatus;
    }


    public void setmListener(UploadTaskListener mListener) {
        this.mListener = mListener;
    }

    public static class Builder {
        private String id;// task id
        private String url;// file url
        private String fileName; // File name when saving
        private int uploadStatus = UploadStatus.UPLOAD_STATUS_INIT;
        private int chunck;//第几块
        private UploadTaskListener listener;

        /**
         * 作为上传task开始、删除、停止的key值，如果为空则默认是url
         *
         * @param id
         * @return
         */
        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        /**
         * 上传url（not null）
         *
         * @param url
         * @return
         */
        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        /**
         * 设置上传状态
         *
         * @param uploadStatus
         * @return
         */
        public Builder setUploadStatus(int uploadStatus) {
            this.uploadStatus = uploadStatus;
            return this;
        }

        /**
         * 第几块
         *
         * @param chunck
         * @return
         */
        public Builder setChunck(int chunck) {
            this.chunck = chunck;
            return this;
        }


        /**
         * 设置文件名
         *
         * @param fileName
         * @return
         */
        public Builder setFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        /**
         * 设置上传回调
         *
         * @param listener
         * @return
         */
        public Builder setListener(UploadTaskListener listener) {
            this.listener = listener;
            return this;
        }

        public UploadTask build() {
            return new UploadTask(this);
        }
    }

    private void addParams(MultipartBody.Builder builder, Map<String, String> params) {
        if (params != null && !params.isEmpty()) {
            for (String key : params.keySet()) {
                builder.addPart(Headers.of("Content-Disposition", "form-data; name=\"" + key + "\""),
                        RequestBody.create(null, params.get(key)));
            }
        }
    }

}
