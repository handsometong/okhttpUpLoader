/*
 * Copyright (C) 20015 MaiNaEr All rights reserved
 */
package com.mainaer.wjoklib.okhttp.controller;

import java.io.File;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.HashMap;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * 文件图片上传基类
 *
 * @author wangjian
 * @date 2016/3/25.
 */
public abstract class OKUploadController<Listener> extends OKHttpController<Listener> {


    public OKUploadController() {
        super();
    }

    public OKUploadController(Listener l) {
        super();
        setListener(l);
    }

    protected abstract class BaseUpLoadTask<Output> extends LoadTask<File, Output> {

        @Override
        protected RequestBody postBody(File file) {
            // 设置请求体
            MultipartBody.Builder builder = new MultipartBody.Builder();
            builder.setType(MultipartBody.FORM);
            // 封装文件请求体
            if (file != null && file.exists()) {
                String filename = file.getName();
                MediaType mediaType = MediaType.parse(guessMimeType(filename));
                RequestBody fileBody = RequestBody.create(mediaType, file);
                builder.addFormDataPart("file", filename, fileBody);
            }
            // 封装请求参数
            HashMap<String, String> params = new HashMap<>();
            addParams(params);
            if (params != null && !params.isEmpty()) {
                for (String key : params.keySet()) {
                    builder.addPart(Headers.of("Content-Disposition", "form-data; name=\"" + key + "\""),
                        RequestBody.create(null, params.get(key)));
                }
            }

            return builder.build();
        }

        private String guessMimeType(String filename) {
            FileNameMap fileNameMap = URLConnection.getFileNameMap();
            String contentTypeFor = fileNameMap.getContentTypeFor(filename);
            if (contentTypeFor == null) {
                contentTypeFor = "application/octet-stream";
            }
            return contentTypeFor;
        }

        /**
         * 添加请求键值对
         */
        protected abstract void addParams(HashMap<String, String> params);
    }
}
