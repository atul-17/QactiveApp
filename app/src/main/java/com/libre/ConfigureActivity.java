package com.libre;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.ManageDevice.CreateNewScene;
import com.libre.Network.NewSacActivity;
import com.libre.Scanning.Constants;
import com.libre.Scanning.ScanningHandler;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;
import com.libre.netty.LibreDeviceInteractionListner;
import com.libre.netty.NettyData;


public class ConfigureActivity extends CTDeviceDiscoveryActivity implements View.OnClickListener,LibreDeviceInteractionListner {
    private ScanningHandler mScanHandler = ScanningHandler.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        findViewById(R.id.config_button).setOnClickListener(this);
    }

    @SuppressLint("HandlerLeak")
    Handler handler=new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (ScanningHandler.getInstance().centralSceneObjectRepo.size()>0)
            {
                    Intent newIntent = new Intent(ConfigureActivity.this, ActiveScenesListActivity.class);
                    startActivity(newIntent);
                    finish();
            }
            handler.sendEmptyMessageDelayed(1,3000);
        }
    };




    @Override
    protected void onResume() {
        super.onResume();
        registerForDeviceEvents(this);
        setTheFooterIcons();

        if (handler!=null)
        handler.sendEmptyMessageDelayed(1,3000);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegisterForDeviceEvents();
    }
    @Override
    public void onBackPressed() {
        ScanningHandler mScanHandler = ScanningHandler.getInstance();
        if (mScanHandler.centralSceneObjectRepo.size()<1)
        {
            Intent intentx = new Intent(Intent.ACTION_MAIN);
            intentx.addCategory(Intent.CATEGORY_HOME);
            intentx.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//***Change Here***
            startActivity(intentx);
            finish();
            System.exit(0);

        }else {
            super.onBackPressed();
            finish();
        }
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.favorites_icon:

                if (LSSDPNodeDB.isLS9ExistsInTheNetwork()){

                    SharedPreferences sharedPreferences = getApplicationContext()
                            .getSharedPreferences(Constants.SHOWN_GOOGLE_TOS, Context.MODE_PRIVATE);
                    String shownGoogle = sharedPreferences.getString(Constants.SHOWN_GOOGLE_TOS, null);
                    if (shownGoogle == null) {
                        showGoogleTosDialog(ConfigureActivity.this);
                    }
                    else {
                        Intent intent=new Intent(ConfigureActivity.this,GcastSources.class);
                        startActivity(intent);
                    }
                }
                else {
                    Toast.makeText(this,getString(R.string.favourits_not_implemented), Toast.LENGTH_LONG).show();
                }

                break;
            case R.id.playnew_icon:
   //             Toast.makeText(this, getString(R.string.playnew_button), Toast.LENGTH_SHORT).show();
                Intent newIntent = new Intent(ConfigureActivity.this, CreateNewScene.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(newIntent);
                finish();
                break;

            case R.id.config_item:
                //Toast.makeText(this, "Config", Toast.LENGTH_SHORT).show();
				Intent configIntent = new Intent (ConfigureActivity.this, NewSacActivity.class);
                startActivity(configIntent);
                finish();
                break;

            case R.id.config_button:
             //   Toast.makeText(this, "Config", Toast.LENGTH_SHORT).show();
				 Intent configIntent1 = new Intent (ConfigureActivity.this, NewSacActivity.class);
                startActivity(configIntent1);
                finish();
                break;


        }
    }
    public void showGoogleTosDialog(final Context context) {
        LayoutInflater factory = LayoutInflater.from(this);
        final View gcastDialog = factory.inflate(
                R.layout.gcast_alert_dialog, null);
        TextView textView= (TextView)gcastDialog. findViewById(R.id.google_text);
        textView.setText(clickableString(getString((R.string.google_tos))));
        textView.setAutoLinkMask(RESULT_OK);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setGravity(Gravity.LEFT);
        Linkify.addLinks(clickableString(getString((R.string.google_tos))), Linkify.WEB_URLS);



        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(getString(R.string.googleTerms))
                .setCancelable(false)
                        //                .setMessage(clickableString(getString((R.string.google_tos))))
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setPositiveButton(getString(R.string.accept), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        SharedPreferences sharedpreferences = getApplicationContext()
                                .getSharedPreferences(Constants.SHOWN_GOOGLE_TOS, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedpreferences.edit();
                        editor.putString(Constants.SHOWN_GOOGLE_TOS, getString(R.string.yes));
                        editor.commit();

                        LUCIControl control=new LUCIControl("");
                        control.sendGoogleTOSAcceptanceIfRequired(ConfigureActivity.this, "true");



                        Intent intent = new Intent(context, GcastSources.class);
                        startActivity(intent);


                    }

                });

        AlertDialog  googAlert = builder.create();
        googAlert.setView(gcastDialog);
        googAlert.show();
    }

    private SpannableString clickableString(String sampleString) {
        SpannableString spannableString = new SpannableString(sampleString);
        ClickableSpan googleTermsClicked = new ClickableSpan() {
            @Override
            public void onClick(View textView) {

                Intent intent=new Intent(getApplicationContext(),LibreWebViewActivity.class);
                intent.putExtra("url", "http://www.google.com/intl/en/policies/privacy/");
                startActivity(intent);
/*
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("http://www.google.com/intl/en/policies/privacy/"));
                startActivity(intent);
*/





            }
        };


        ClickableSpan termsClicked = new ClickableSpan() {
            @Override
            public void onClick(View textView) {

                Intent intent=new Intent(getApplicationContext(),LibreWebViewActivity.class);
                intent.putExtra("url","https://www.google.com/intl/en/policies/terms/");
                startActivity(intent);

/*
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("https://www.google.com/intl/en/policies/terms/"));
                startActivity(intent);
*/
            }
        };

        ClickableSpan privacyClicked = new ClickableSpan() {
            @Override
            public void onClick(View textView) {

                Intent intent=new Intent(getApplicationContext(),LibreWebViewActivity.class);
                intent.putExtra("url","http://www.google.com/intl/en/policies/privacy/");
                startActivity(intent);

/*
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("https://www.google.com/intl/en/policies/privacy/"));
                startActivity(intent);
*/
            }
        };

        spannableString.setSpan(googleTermsClicked, 4, 25, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(termsClicked, 395, 418, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(privacyClicked, 422, 444, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.brand_orange)), 4, 25,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.brand_orange)), 395, 418,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.brand_orange)), 422, 444,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return spannableString;
    }

    public void setTheFooterIcons() {

     /*   if( LSSDPNodeDB.isLS9ExistsInTheNetwork()) {

            TextView icon = (TextView) findViewById(R.id.favorites_icon);
            Drawable top = getResources().getDrawable(R.mipmap.ic_cast_black_24dp);
            icon.setCompoundDrawablesWithIntrinsicBounds(null, top, null, null);
            icon.setText(R.string.favorite_button_playcast);
        }else{
            TextView icon = (TextView) findViewById(R.id.favorites_icon);
            Drawable top = getResources().getDrawable(R.mipmap.ic_favorites);
            icon.setCompoundDrawablesWithIntrinsicBounds(null, top, null, null);
            icon.setText(R.string.favorite_button);
        }*/
    }

    @Override
    public void deviceDiscoveryAfterClearingTheCacheStarted() {

    }

    @Override
    public void newDeviceFound(LSSDPNodes node) {

        if (handler!=null)
        handler.removeMessages(1);

       Intent newIntent;
       /* if (node.getDeviceState().contains("M"))*/ {
            if (!mScanHandler.isIpAvailableInCentralSceneRepo(node.getIP())) {
                SceneObject sceneObjec = new SceneObject(" ", node.getFriendlyname(), 0, node.getIP());
                mScanHandler.putSceneObjectToCentralRepo(node.getIP(), sceneObjec);
                newIntent = new Intent(ConfigureActivity.this, ActiveScenesListActivity.class);
                startActivity(newIntent);
                finish();
            }
            updateSceneObjectAndLaunchActiveScene(node.getIP());
        }
    }
    /*this method will get called when we are getting master 103*/
    private void updateSceneObjectAndLaunchActiveScene(String ipaddress) {

        LSSDPNodes mMasterNode = mScanHandler.getLSSDPNodeFromCentralDB(ipaddress);

        if (mMasterNode!=null) {

            if (!mScanHandler.isIpAvailableInCentralSceneRepo(ipaddress)) {
                SceneObject mMastersceneObjec = new SceneObject(" ", mMasterNode.getFriendlyname(), 0, mMasterNode.getIP());
                mScanHandler.putSceneObjectToCentralRepo(ipaddress, mMastersceneObjec);
            }
            if(!LibreApplication.mCleanUpIsDoneButNotRestarted) {
                Intent intent = new Intent(ConfigureActivity.this, ActiveScenesListActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        }

    }
    @Override
    public void deviceGotRemoved(String ipaddress) {

    }

    @Override
    public void messageRecieved(NettyData packet) {

    }


    @Override
    protected void onStop() {
        super.onStop();
        if (handler!=null)
            handler.removeMessages(1);
    }
}
