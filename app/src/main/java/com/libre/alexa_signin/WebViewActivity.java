package com.libre.alexa_signin;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.libre.R;

/**
 * Created by amrit on 12/9/2016.
 */

public class WebViewActivity extends Activity implements View.OnClickListener {

    private WebView webView;
    private RelativeLayout rl_back;
    private ImageButton ib_back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.web_view_activity);

        inItWidgets();
        setEventListeners();
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("https://www.amazon.com/");

    }

    private void setEventListeners() {
        rl_back.setOnClickListener(this);
        ib_back.setOnClickListener(this);
    }

    private void inItWidgets() {
        webView = (WebView) findViewById(R.id.webView1);
        rl_back = (RelativeLayout) findViewById(R.id.Rl_back_button);
        ib_back =(ImageButton) findViewById(R.id.image_back);
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == rl_back.getId() || view.getId() == ib_back.getId()){
            onBackPressed();
        }
    }
}
