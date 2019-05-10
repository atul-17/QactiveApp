package com.libre;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.libre.util.LibreLogger;

/**
 * Created by praveena on 3/28/16.
 */
public class LibreWebViewActivity extends Activity {

    WebView wv1 ;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.webviewactivity);



        String url=    getIntent().getStringExtra("url");
        String title = getIntent().getStringExtra("title");


        wv1 =(WebView)findViewById(R.id.webview);
        progressBar=(ProgressBar)findViewById(R.id.loader);

        wv1.setWebViewClient(new MyBrowser(progressBar));
        wv1.getSettings().setLoadsImagesAutomatically(true);
        wv1.getSettings().setJavaScriptEnabled(true);
        wv1.getSettings().setUserAgentString("Mozilla/5.0 (iPhone; U; CPU like Mac OS X; en) AppleWebKit/420+ (KHTML, like Gecko) Version/3.0 Mobile/1A543a Safari/419.3");
        wv1.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        wv1.canGoBack();
        wv1.canGoForward();


        wv1.loadUrl(url);



    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (wv1.canGoBack()) {
                        wv1.goBack();
                    } else {
                        finish();
                    }
                    return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    }



class MyBrowser extends WebViewClient {
    ProgressBar loader;

    MyBrowser(ProgressBar progressBar){
        loader=progressBar;
    }


    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        view.loadUrl(url);
        return true;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {

        LibreLogger.d(this,"page started");
        loader.setVisibility(View.VISIBLE);
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        LibreLogger.d(this,"page finished");
        loader.setVisibility(View.INVISIBLE);
        super.onPageFinished(view, url);
    }

}
