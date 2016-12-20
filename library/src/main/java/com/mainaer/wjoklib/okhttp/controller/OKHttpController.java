/*
 * Copyright (C) 20015 MaiNaEr All rights reserved
 */
package com.mainaer.wjoklib.okhttp.controller;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mainaer.wjoklib.okhttp.IUrl;
import com.mainaer.wjoklib.okhttp.OKBaseResponse;
import com.mainaer.wjoklib.okhttp.OKHttpManager;
import com.mainaer.wjoklib.okhttp.exception.OkHttpError;
import com.mainaer.wjoklib.okhttp.utils.LoggerUtil;
import com.mainaer.wjoklib.okhttp.utils.OkStringUtils;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * okhttp加载基类，子类需继承之实现自定义功能
 *
 * @author wangjian
 * @date 2016/3/23.
 */
public abstract class OKHttpController<Listener> {

    public static final int SUCCESS_CODE = 0x01;
    public static final int ERROR_CODE = 0x02;
    protected Listener mListener;
    private Call mCall = null;

    public OKHttpController() {
    }

    public OKHttpController(Listener l) {
        super();
        setListener(l);
    }

    public void setListener(Listener l) {
        this.mListener = l;
    }

    protected abstract class LoadTask<Input, Output> implements Callback {

        private OkHttpClient mClient = OKHttpManager.getOkHttpClient();
        private MediaType MEDIA_TYPE_PLAIN = MediaType.parse("text/plain;charset=utf-8");
        protected Class<Output> mDataClazz;
        protected Class<?> mDataItemClass;
        private Gson mGson = new Gson();
        protected Input input;

        /**
         * 获取url
         *
         * @return
         */
        protected abstract IUrl getUrl();

        /**
         * 加载成功回调
         *
         * @param output
         */
        protected abstract void onSuccess(Output output);

        /**
         *
         * @param error
         */
        protected abstract void onError(OkHttpError error);

        /**
         * Intercept 'data' json parser
         *
         * @param response the whole response object ({@link OKBaseResponse} instance)
         * @return true if you want to skip convert 'data' json to object.
         */
        protected boolean onInterceptor(OKBaseResponse response) {
            return false;
        }

        /**
         * 加载数据解析成list
         *
         * @param input 请求参数
         * @param itemClass 相应的item类型
         */
        public void load2List(Input input, Class<?> itemClass) {
            this.mDataItemClass = itemClass;
            load(input, null);
        }

        /**
         * 加载数据
         *
         * @param input 输入数据
         * @param clazz 响应类型
         */
        public void load(Input input, Class<Output> clazz) {
            this.input = input;
            this.mDataClazz = clazz;

            IUrl url = getUrl();
            LoggerUtil.i("request url = " + url.getUrl());
            // 获取请求方法
            int method = url.getMethod();
            Request.Builder builder = null;
            RequestBody requestBody =   null;
            builder = new Request.Builder();
            switch (method) {
                case Method.GET:// 默认请求方式
                    // 获取请求体
                    bindBody(url, builder).get();
                    break;
                case Method.POST:
                    requestBody = postBody(input);
                    builder.url(url.getUrl()).post(requestBody);
                    break;
                case Method.DELETE:
                    bindBody(url, builder).delete();
                    break;
                case Method.PUT:
                    // 待测
                    requestBody = RequestBody.create(MEDIA_TYPE_PLAIN, getBody(input));
                    bindBody(url, builder).put(requestBody);
                    break;
                case Method.HEAD:
                    // 待测
                    bindBody(url, builder).head();
                    break;
                case Method.PATCH:
                    // 待测
                    requestBody = RequestBody.create(MEDIA_TYPE_PLAIN, getBody(input));
                    builder.url(url.getUrl()).patch(requestBody);
                    break;
            }

            // 构建tag
            String tag = getClass().getName();
            // 封装请求
            Request request = builder.tag(tag).build();
            // 执行请求
            mCall = mClient.newCall(request);
            mCall.enqueue(this);
        }

        // 拼接get请求参数
        private Request.Builder bindBody(IUrl url, Request.Builder builder) {
            String body = getBody(input);
            url.setQuery(body);
            builder.url(url.getUrl());
            LoggerUtil.i("get request url = " + url.getUrl());
            return builder;
        }

        @Override
        public final void onFailure(Call call, IOException e) {
            sendMessage(new OkHttpError(e), ERROR_CODE);
        }

        @Override
        public final void onResponse(Call call, Response response) {
            if (response != null && response.isSuccessful() && response.code() == 200) {
                convertData(response);
            }
            else {
                throw new NullPointerException("base response is null, please check your http response.");
            }
        }

        /**
         * 解析请求成功的数据
         *
         * @param response
         */
        protected void convertData(Response response) {
            Output out = null;
            String body = null;
            try {
                body = response.body().string();
                // 解析成OKBaseResponse
                OKBaseResponse baseResponse = mGson.fromJson(body, getBaseResponseClass());
                if (!onInterceptor(baseResponse)) {
                    // mDataClazz是否是BaseResponse
                    if (mDataClazz != null) {
                        boolean baseOutput = isOKBaseResponse(mDataClazz);
                        if (baseOutput) {
                            out = (Output) baseResponse;
                            sendMessage(out, SUCCESS_CODE);
                            return;
                        }
                    }
                    // 解析成BaseResponse中的data
                    String data = baseResponse.getData();
                    if (!TextUtils.isEmpty(data)) {
                        LoggerUtil.i("response = " + body);
                        if (mDataItemClass != null && mDataClazz == null) {
                            out = mGson.fromJson(data, type(List.class, mDataItemClass));
                            if (out == null) {
                                out = (Output) new ArrayList<>(0);
                            }
                        }
                        else {
                            out = mGson.fromJson(data, mDataClazz);
                        }
                        sendMessage(out, SUCCESS_CODE);
                    }
                }
            } catch (IOException e) {
                sendMessage(new OkHttpError(e), ERROR_CODE);
                return;
            } catch (JsonSyntaxException e) {
                sendMessage(new OkHttpError(e), ERROR_CODE);
                return;
            } catch (Exception e) {
                sendMessage(new OkHttpError(e), ERROR_CODE);
                return;
            }
        }

        /**
         * 数据加载完成或失败
         *
         * @param obj  加载到的数据或失败信息
         * @param what
         */
        protected void sendMessage(Object obj, int what) {
            Message msg = Message.obtain();
            msg.obj = obj;
            msg.what = what;
            handler.sendMessage(msg);
        }

        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == SUCCESS_CODE) {
                    onSuccess((Output) msg.obj);
                }
                else if (msg.what == ERROR_CODE) {
                    onError((OkHttpError) msg.obj);
                }
            }
        };

        /**
         * Get parameter encoding
         *
         * @return request parameter encoding
         */

        public String getParamsEncoding() {
            return "UTF-8";
        }

        /**
         * 获取post请求body
         *
         * @param input
         * @return
         */
        protected RequestBody postBody(Input input) {
            HashMap<String, ?> map = OkStringUtils.postRequestParam(input, getParamsEncoding());
            FormBody.Builder formBuilder = new FormBody.Builder();
            if (map != null) {
                StringBuffer buffer = new StringBuffer();

                for (String key : map.keySet()) {
                    buffer.append(key).append("=").append(map.get(key)).append(" ,");
                    formBuilder.add(key, OkStringUtils.getRequestParamValue(map.get(key), getParamsEncoding()));
                }
                LoggerUtil.i("request body: " + buffer.deleteCharAt(buffer.toString().length() - 1));
            }
            return formBuilder.build();
        }

        /**
         * Get request body.
         *
         * @param input request entity
         * @return encoded body string
         * @see #getParamsEncoding()
         */
        protected String getBody(Input input) {
            String body = null;
            if (input == null) {
                body = null;
            }
            else if (input instanceof Map) {
                Map map = (Map) input;
                StringBuilder sb = new StringBuilder();
                for (Object key : map.keySet()) {
                    sb.append(key);
                    sb.append('=');
                    sb.append(OkStringUtils.getRequestParamValue(map.get(key), getParamsEncoding()));
                    sb.append('&');
                }
                if (sb.length() > 1) {
                    sb.deleteCharAt(sb.length() - 1);
                }
                body = sb.toString();
            }
            else {
                body = OkStringUtils.getRequestParam(input, getParamsEncoding());
            }
            LoggerUtil.i("request body: " + body);
            return body;
        }
    }

    /**
     * 在不需要的时候，取消请求
     */
    public void onDestroy() {
        if (mCall != null && !mCall.isExecuted()) {
            try {
                mCall.cancel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Class<? extends OKBaseResponse> getBaseResponseClass() {
        return OKHttpManager.getConfig().getBaseResponseClass();
    }

    private static boolean isOKBaseResponse(Class<?> clazz) {
        List<Class<?>> list = getSuperType(clazz);
        return list.contains(OKBaseResponse.class);
    }

    public static List<Class<?>> getSuperType(Class<?> clazz) {
        List<Class<?>> set = new ArrayList<>();
        getSuperType(clazz, set);
        return set;
    }

    private static void getSuperType(Class<?> clazz, List<Class<?>> set) {
        set.add(clazz);
        if (clazz.getSuperclass() != null) {
            getSuperType(clazz.getSuperclass(), set);
        }
        Class<?>[] interfaces = clazz.getInterfaces();
        for (Class<?> c : interfaces) {
            getSuperType(c, set);
        }
    }

    public static ParameterizedType type(final Class raw, final Type... args) {
        return new ParameterizedType() {
            public Type getRawType() {
                return raw;
            }

            public Type[] getActualTypeArguments() {
                return args;
            }

            public Type getOwnerType() {
                return null;
            }
        };
    }

    public interface Method {
        int GET = 0;
        int POST = 1;
        int PUT = 2;
        int DELETE = 3;
        int HEAD = 4;
        int PATCH = 5;
    }
}
