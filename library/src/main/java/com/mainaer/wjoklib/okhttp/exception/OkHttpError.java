/*
 * Copyright 2014-2015 ieclipse.cn.
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
package com.mainaer.wjoklib.okhttp.exception;

import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * 对异常进行统一处理，更多异常状态请如下自行处理
 *
 * @author wangjian
 * @date 2015年11月9日
 */
public class OkHttpError extends Exception {
    public static final int TYPE_NO_CONNECTION = 0x01;//网络错误
    public static final int TYPE_TIMEOUT = 0x02;//连接超时
    public static final int TYPE_PARSE = 0x03;// 数据解析错误
    public static final int TYPE_ERROR = 0x04;// 其他原因数据解析失败


    public int mErrorType;

    public OkHttpError(Exception error) {
        super(error);
        initType();
    }
    
    private void initType() {
        Throwable error = getCause();
        if (error == null) {
            return;
        }
        if (error instanceof UnknownHostException) {
            mErrorType = TYPE_NO_CONNECTION;
        }
        else if (error instanceof SocketTimeoutException) {
            mErrorType = TYPE_TIMEOUT;
        }
        else if (error instanceof JsonSyntaxException) {
            mErrorType = TYPE_PARSE;
        }
        else if (error instanceof IOException) {
            mErrorType = TYPE_PARSE;
        }
        else if (error instanceof Exception) {
            mErrorType = TYPE_ERROR;
        }
    }
    
    public int getType() {
        return mErrorType;
    }
}
