package com.libre.StaticInstructions;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.R;

public class spotifyInstructions extends CTDeviceDiscoveryActivity implements View.OnClickListener {
    private ImageButton m_back;
    private TextView learnMore , openSpotify;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spotify_instructions);
        m_back = (ImageButton) findViewById(R.id.back);
        m_back.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        openSpotify = (TextView)findViewById(R.id.openSpotify);
        learnMore = (TextView)findViewById(R.id.learnMore);
        openSpotify.setOnClickListener(this);
        learnMore.setText(Html.fromHtml("<a href= https://spotify.com/connect > Learn More </a>"));
        learnMore.setMovementMethod(LinkMovementMethod.getInstance());
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.openSpotify:
                String appPackageName = "com.spotify.music";
                launchTheApp(appPackageName);
                break;
        }
    }

    public void launchTheApp(String appPackageName) {

        Intent intent = getPackageManager().getLaunchIntentForPackage(appPackageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            redirectingToPlayStore(intent, appPackageName);
        }

    }


    public void redirectingToPlayStore(Intent intent, String appPackageName) {

        try {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("market://details?id=" + appPackageName));
            startActivity(intent);

        } catch (android.content.ActivityNotFoundException anfe) {

            intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName));
            startActivity(intent);

        }

    }
}
