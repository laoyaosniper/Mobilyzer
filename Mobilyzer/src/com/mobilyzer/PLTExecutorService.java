package com.mobilyzer;



import com.mobilyzer.util.AndroidWebView;
import com.mobilyzer.util.Logger;
import com.mobilyzer.util.AndroidWebView.WebViewProtocol;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class PLTExecutorService extends Service {
  boolean spdyTest;

  @Override
  public void onCreate() {
    super.onCreate();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    boolean spdy = intent.getBooleanExtra(UpdateIntent.PLT_TASK_PAYLOAD_TEST_TYPE, false);
    spdyTest = spdy;
    String url = intent.getStringExtra(UpdateIntent.PLT_TASK_PAYLOAD_URL);
    long startTimeFilter = intent.getLongExtra(UpdateIntent.PLT_TASK_PAYLOAD_STARTTIME, 0);
    Logger.d("ashkan_plt: PLTExecutorService is started " + spdy + " " + url);
    if (spdy) {

      AndroidWebView webView =
          new AndroidWebView(this, true, WebViewProtocol.HTTP, startTimeFilter, url);
      webView.loadUrl();

    } else {
      AndroidWebView webView =
          new AndroidWebView(this, false, WebViewProtocol.HTTP, startTimeFilter, url);
      webView.loadUrl();
    }

    return Service.START_NOT_STICKY;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }


  @Override
  public void onDestroy() {
    Logger.d("ashkan_plt: PLTExecutorService: onDestroy");
    super.onDestroy();
  }
}
