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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * 上传管理器
 *
 * @author wangjian
 * @date 2016/5/13 .
 */
public class UploadManager {

    private static Context mContext;

    private static SQLiteDatabase db;
    private OkHttpClient mClient;

    private int mPoolSize = 20;
    // 将执行结果保存在future变量中
    private Map<String, Future> mFutureMap;
    private ExecutorService mExecutor;
    private Map<String, UploadTask> mCurrentTaskList;

    static UploadManager manager;

    /**
     * 方法加锁，防止多线程操作时出现多个实例
     */
    private static synchronized void init() {
        if (manager == null) {
            manager = new UploadManager();
        }
    }

    /**
     * 获得当前对象实例
     *
     * @return 当前实例对象
     */
    public final static UploadManager getInstance() {
        if (manager == null) {
            init();
        }
        return manager;
    }

    /**
     * 管理器初始化，建议在application中调用
     *
     * @param context
     */
    public static void init(Context context, SQLiteDatabase db1) {
        mContext = context;
        db = db1;
        getInstance();
    }

    public UploadManager() {
        initOkhttpClient();

        // 初始化线程池
        mExecutor = Executors.newFixedThreadPool(mPoolSize);
        mFutureMap = new HashMap<>();
        mCurrentTaskList = new HashMap<>();
    }

    /**
     * 初始化okhttp
     */
    private void initOkhttpClient() {
        OkHttpClient.Builder okBuilder = new OkHttpClient.Builder();
        okBuilder.connectTimeout(1000, TimeUnit.SECONDS);
        okBuilder.readTimeout(1000, TimeUnit.SECONDS);
        okBuilder.writeTimeout(1000, TimeUnit.SECONDS);
        mClient = okBuilder.build();
    }

    /**
     * 添加上传任务
     *
     * @param uploadTask
     */
    public void addUploadTask(UploadTask uploadTask) {
        if (uploadTask != null && !isUploading(uploadTask)) {
            uploadTask.setClient(mClient);
            uploadTask.setUploadStatus(UploadStatus.UPLOAD_STATUS_INIT);
            // 保存上传task列表
            mCurrentTaskList.put(uploadTask.getId(), uploadTask);
            Future future = mExecutor.submit(uploadTask);
            mFutureMap.put(uploadTask.getId(), future);
        }
    }

    private boolean isUploading(UploadTask task) {
        if (task != null) {
            if (task.getUploadStatus() == UploadStatus.UPLOAD_STATUS_UPLOADING) {
                return true;
            }
        }
        return false;
    }

    /**
     * 暂停上传任务
     *
     * @param id 任务id
     */
    public void pause(String id) {
        UploadTask task = getUploadTask(id);
        if (task != null) {
            task.setUploadStatus(UploadStatus.UPLOAD_STATUS_PAUSE);
        }
    }

    /**
     * 重新开始已经暂停的上传任务
     *
     * @param id 任务id
     */
    public void resume(String id, UploadTaskListener listener) {
        UploadTask task = getUploadTask(id);
        if (task != null) {
            addUploadTask(task);
        }
    }

/*    *//**
     * 取消上传任务(同时会删除已经上传的文件，和清空数据库缓存)
     *
     * @param id       任务id
     * @param listener
     *//*
    public void cancel(String id, UploadTaskListener listener) {
        UploadTask task = getUploadTask(id);
        if (task != null) {
            mCurrentTaskList.remove(id);
            mFutureMap.remove(id);
            task.setmListener(listener);
            task.cancel();
            task.setDownloadStatus(UploadStatus.DOWNLOAD_STATUS_CANCEL);
        }
    }*/

    /**
     * 实时更新manager中的task信息
     *
     * @param task
     */
    public void updateUploadTask(UploadTask task) {
        if (task != null) {
            UploadTask currTask = getUploadTask(task.getId());
            if (currTask != null) {
                mCurrentTaskList.put(task.getId(), task);
            }
        }
    }

    /**
     * 获得指定的task
     *
     * @param id task id
     * @return
     */
    public UploadTask getUploadTask(String id) {
        UploadTask currTask = mCurrentTaskList.get(id);
        if (currTask == null) {
                currTask = parseEntity2Task(new UploadTask.Builder().build());
                // 放入task list中
                mCurrentTaskList.put(id, currTask);
        }

        return currTask;
    }


    private UploadTask parseEntity2Task(UploadTask currTask) {

        UploadTask.Builder builder = new UploadTask.Builder()//
                .setUploadStatus(currTask.getUploadStatus())
                .setFileName(currTask.getFileName())//
                .setUrl(currTask.getUrl())
                .setId(currTask.getId());

            currTask.setBuilder(builder);

        return currTask;
    }
}
