package com.lp.double_b.view.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.alibaba.fastjson.util.IOUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.lp.double_b.R;
import com.lp.double_b.view.activity.BookDetailActivity;
import com.lp.double_b.view.activity.MainActivity;
import com.lp.double_b.view.activity.SearchActivity;
import com.lp.double_b.view.adapter.BookListAdapter;
import com.lp.double_b.view.data.BookInfoBean;
import com.lp.double_b.view.util.FileUtils;
import com.lp.double_b.view.util.LogUtils;
import com.lp.double_b.view.util.ThreadPoolProxyFactory;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BookCityFragment extends Fragment implements AdapterView.OnItemClickListener {
    public static final String ARGUMENT = "argument";
    private static final String TAG ="BookCityFragment";
    private LinearLayout searchLayout;
    BookListAdapter mAdapter;
    private ListView listView;
    public List<BookInfoBean> _listData;
    private LoadDataTask mLoadDataTask;

    Handler mHandler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.obj != null) {
                _listData = (List<BookInfoBean>) msg.obj;
            }


            return false;
        }
    });
    private String titles;
    private Activity mActivity;

    public BookCityFragment() {
        // Required empty public constructor
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_book_city, container, false);
        searchLayout = (LinearLayout) view.findViewById(R.id.search_layout);
        searchLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SearchActivity.startActivity(getActivity());
            }
        });

        listView=(ListView)view.findViewById(R.id.listView);
        listView.setOnItemClickListener(this);
//        getData();

        mAdapter = new BookListAdapter(getActivity(), null);
        listView.setAdapter(mAdapter);
        //异步加载
        mLoadDataTask = new LoadDataTask();
        //new Thread(mLoadDataTask).start();
        ThreadPoolProxyFactory.createNormalThreadPoolProxy().submit(mLoadDataTask);
        return view;
    }
    
    /**
     * 加载数据
     * 1.磁盘,磁盘有返回,存内存
     * 2.网络,网络有返回,存磁盘,存内存
     * @return
     * @throws Exception
     */
    public List<BookInfoBean> loadData(){

        List<BookInfoBean> result = null;


        /*--------------- 1.磁盘,磁盘有返回,存内存 ---------------*/
        //先本地,有返回
        result = loadDataFromLocal();
        if (result != null) {
            LogUtils.i(TAG, "从本地加载了数据-->" + getCacheFile().getAbsolutePath());
            return result;
        }
        /*--------------- 2.网络,网络有返回,存磁盘,存内存 ---------------*/
        //在网络,存本地
        return loadDataFromNet();
    }

    /**
     * 从本地加载数据
     *
     * @return
     */
    private List<BookInfoBean> loadDataFromLocal() {
        BufferedReader reader = null;
        try {
            File cacheFile = getCacheFile();
            if (cacheFile.exists()) {//有缓存
                //判断缓存是否过期-->读取缓存的生成时间

                reader = new BufferedReader(new FileReader(cacheFile));
                String insertTimeStr = reader.readLine();
                Long insertTime = Long.parseLong(insertTimeStr);

                if ((System.currentTimeMillis() - insertTime) < 5*60*1000) {
                    //有效的缓存-->读取缓存内容
                    String diskCacheJsonString = reader.readLine();

                    //解析返回
                    List<BookInfoBean> result = parseJsonString(diskCacheJsonString);
                    return result;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.close(reader);
        }

        return null;
    }

    @NonNull
    private File getCacheFile() {
        String dir = FileUtils.getDir("json");//sdcard/Android/data/包目录/json
        String fileName = getInterfaceKey();
        return new File(dir, fileName);
    }

    @Override
    public void onResume() {
        Log.e("onResume","onResume == enter...");
        //titles = ((MainActivity) mActivity).getTitles();
        Log.e("onResume","titles=="+ titles );
        //new Thread(mLoadDataTask).start();
        super.onResume();
    }

    //   / Fragment中的onAttach方法
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
        titles = ((MainActivity) mActivity).getTitles();
        Log.e("onAttach","titles=="+ titles );
        //onResume();
    }
    //通过强转成宿主activity，就可以获取到传递过来的数据
    public String getInterfaceKey() {
//        Bundle arguments = getArguments();
////        Log.e("argument","argument==null ?"+(arguments==null));
//        String title= arguments.getString(titles);
//        Log.e("titles","titles=  ?"+ title );
//        Log.e("argument","argument==null ?"+  (arguments == null) );
        String t;
        switch (titles ) {
            case "男生":
                return "male";
//                break;
            case "女生":
                return "female";
//                break;
            default:
                return "recommend";
//                break;
        }
//        return t;
//        Log.e("t","t=  ?"+ t);

       //return "recommend";
    }
    /**
     * 从网络获取数据
     *
     * @return
     * @throws IOException
     */
    private List<BookInfoBean> loadDataFromNet() {
        //1.创建okHttpClient对象
        OkHttpClient okHttpClient = new OkHttpClient();
        //2.创建请求对象
        //http://localhost:8080/Double_B_Reader/home/female.json
        String url = "http://10.0.3.2:8080/Double_B_Reader/home/" + getInterfaceKey();

        Log.e("loadDataFromNet","getInterfaceKey == " + getInterfaceKey());
        LogUtils.s(url);

        Request request = new Request.Builder().get().url(url).build();

        //3.发起请求-->同步
        Response response = null;
        try {
            response = okHttpClient.newCall(request).execute();

            if (response.isSuccessful()) {//有响应
                String responseJsonString = response.body().string();

                Log.w(TAG,"responseJsonString = " + responseJsonString);

                //完成json的解析
                List<BookInfoBean> t = parseJsonString(responseJsonString);
                return t;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //onResume();
        return null;
    }


    public List<BookInfoBean> parseJsonString(String resultJsonString) {
        List<BookInfoBean> result = new ArrayList<>();
        Gson gson = new Gson();

        //Json的解析类对象
        JsonParser parser = new JsonParser();
        //将JSON的String 转成一个JsonArray对象
        JsonArray jsonArray = parser.parse(resultJsonString).getAsJsonArray();

        for (JsonElement jsonElement : jsonArray) {
            BookInfoBean bookInfoBean = gson.fromJson(jsonElement, BookInfoBean.class);
            Log.w(TAG,"BookInfoBean == " +bookInfoBean.toString());
            result.add(bookInfoBean);
        }
        //onResume();
        return result;
    }

    class LoadDataTask implements Runnable {
        @Override
        public void run() {
            //真正在子线程中开始加载具体的数据了
            final List<BookInfoBean> mBookInfoBeen = loadDataFromNet();
            Log.w(TAG,"bookInfoBeen.size() ++++++++++++++++++++++++++++++++++++ ");
            if (null != mBookInfoBeen)
            {Log.w(TAG,"bookInfoBeen.size() ============================================================== ");
                Log.w(TAG,"bookInfoBeen.size() = " + mBookInfoBeen.size());}

            Message message = Message.obtain();
            message.obj = mBookInfoBeen;
            mHandler.sendMessage(message);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //mAdapter = new BookListAdapter(getActivity(), mBookInfoBeen);
                    //listView.setAdapter(mAdapter);
                    mAdapter.updateData(mBookInfoBeen);
                }
            });
            //run方法走到最后,置空任务
            mLoadDataTask = null;
            //onResume();
        }
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (_listData != null) {

            BookInfoBean bookInfoBean = _listData.get(position);
            BookDetailActivity.startActivity(getActivity(),bookInfoBean);
        }
        //onResume();
    }

    public void reloadData() {
        //ThreadPoolProxyFactory.createNormalThreadPoolProxy().submit(mLoadDataTask);
        Log.e("reloadData", "reloadData()  = " );
        titles = ((MainActivity) mActivity).getTitles();
        Log.e("reloadData","titles=="+ titles );
        _listData.clear();
        LoadDataTask task = new LoadDataTask();
        new Thread(task).start();
    }

}
