package uploader.com.okhttpuploader;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.Callback;
import com.zhy.http.okhttp.callback.FileCallBack;
import com.zhy.http.okhttp.callback.StringCallback;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Request;


/**
 * Created by zhy on 15/icon_tmp/17.
 */
public class OkHttpRequestUtils {
    public static OkHttpRequestUtils MyHttpRequestUtil;
    public static Context m_context;
    private String errorinfo;
    private ProgressBar mProgressBar;

  //  public final static String BaseUrl = new SPUttils(m_context).getURL();

    public OkHttpRequestUtils(Context context) {
        m_context = context;
    }

    public static synchronized OkHttpRequestUtils getInstance(Context context) {
        if (MyHttpRequestUtil == null) {
            MyHttpRequestUtil = new OkHttpRequestUtils(context);
        }
        return MyHttpRequestUtil;
    }
    public class MyStringCallback extends StringCallback
    {
        @Override
        public void onBefore(Request request)
        {
            super.onBefore(request);
        }

        @Override
        public void onAfter()
        {
            super.onAfter();
        }

        @Override
        public void onError(Call call, Exception e)
        {
            e.printStackTrace();
            Log.e("TAG",e.getMessage());
        }

        @Override
        public void onResponse(String response)
        {
            Log.e("TAG",response);
        }

        @Override
        public void inProgress(float progress)
        {
           // mProgressBar.setProgress((int) (100 * progress));
        }
    }

    /**
     * 获取对应的String数据 使用post方法
     *
     * @param Url      方法地址
     * @param params   参数信息
     * @param callback 返回句柄
     */
    public synchronized void OkHttpGetStringRequest(String Url, final Map<String, String> params, final Callback callback) {
       // String urlAll = BaseUrl + Url;
        //每次請求都加入token

        OkHttpUtils//
                .post()//
                .url(Url)
                .params(params)
                .build()//
                .connTimeOut(20000)
                .readTimeOut(20000)
                .writeTimeOut(20000)
                .execute(new StringCallback()//
                {

                    @Override
                    public void onError(Call call, Exception e) {

                        //Toast.makeText(m_context, "errorinfo:" + errorinfo + "ex:" + ex.getMessage(), Toast.LENGTH_SHORT).show();
                        Toast.makeText(m_context, "网络出错！", Toast.LENGTH_SHORT).show();
                        callback.onError(call, e);
                    }

                    @Override
                    public void onResponse(String response) {
                        Log.i("OkHttpRequestUtil", "success:" + response);
                        callback.onResponse(response);

                    }
                });
    }



    /**
     * 上传文件 使用post方法
     *
     * @param Url    方法地址
     * @param file   文件
     */
    public synchronized void OkHttpUploadFileRequest(String Url, File file,String fileName,final Callback callback)
    {
       // String url = BaseUrl + Url;
        Map<String, String> params = new HashMap<String, String>();
        params.put("name", "1.rmvb");
        params.put("chunks", "3");
        params.put("chunk", "1");

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("APP-Key", "APP-Secret222");
        headers.put("APP-Secret", "APP-Secret111");
        headers.put("Content-Type","multipart/form-data");


        OkHttpUtils.post()//
                .addFile("mFile", fileName, file)//
                .url(Url)//
                .params(params)//
                .headers(headers)//
                .build()//
                .connTimeOut(1800000)
                .readTimeOut(1800000)
                .writeTimeOut(1800000)
                .execute(new StringCallback(){

                    @Override
                    public void onBefore(Request request)
                    {
                        super.onBefore(request);
                    }

                    @Override
                    public void onAfter()
                    {
                        super.onAfter();
                    }
                    @Override
                    public void onError(Call call, Exception e) {
                        Log.d("OkHttpUploadFileRequest", "onError: "+e.getMessage());
                        callback.onError(call, e);
                    }

                    @Override
                    public void onResponse(String response) {
                        Log.d("OkHttpUploadFileRequest", response);
                        callback.onResponse(response);
                    }

                    @Override
                    public void inProgress(float progress)
                    {
                        callback.inProgress(progress);
                        // mProgressBar.setProgress((int) (100 * progress));
                    }
                });

    }




    /**
     * 下载文件 使用get方法
     *
     * @param Url    方法地址
     * @param file   文件
     * @param fileName  文件名称
     */
    public synchronized void OkHttpDownLoadFileRequest(String Url, File file,String fileName) {
        String urlAll = Url;
        OkHttpUtils//
                .get()//
                .url(urlAll)//
                .build()//
                .connTimeOut(20000)
                .readTimeOut(20000)
                .writeTimeOut(20000)
                .execute(new FileCallBack(Environment.getExternalStorageDirectory().getAbsolutePath(), fileName)//
                {

                    @Override
                    public void onBefore(Request request)
                    {
                        super.onBefore(request);
                    }

                    @Override
                    public void inProgress(float progress, long total)
                    {
                        mProgressBar.setProgress((int) (100 * progress));
                    }

                    @Override
                    public void onError(Call call, Exception e)
                    {
                        Log.e("DownLoadFile", "onError :" + e.getMessage());
                    }

                    @Override
                    public void onResponse(File file)
                    {
                        Log.e("DownLoadFile", "onResponse :" + file.getAbsolutePath());
                    }
                });

    }


}






