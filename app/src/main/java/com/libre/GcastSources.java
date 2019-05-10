package com.libre;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.Scanning.Constants;
import com.libre.luci.LSSDPNodeDB;
import com.libre.luci.LSSDPNodes;
import com.libre.luci.LUCIControl;

import java.util.ArrayList;

/**
 * Created by khajan on 30/3/16.
 */
public class GcastSources extends CTDeviceDiscoveryActivity {

    LSSDPNodes lssdpNode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gcast_sources_layout);

        LSSDPNodeDB lssdpNodeDB=LSSDPNodeDB.getInstance();
        ArrayList<LSSDPNodes> lssdpNodes=lssdpNodeDB.GetDB();
        if(lssdpNodes.size()==0)
            return;


        lssdpNode=lssdpNodes.get(0);


        TextView gCastTuneinView= (TextView) findViewById(R.id.gcast_tunein);
        gCastTuneinView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (googleTosCheckIsSuccess())
                    launchTheApp("tunein.player");
            }
        });
        TextView gCastDeezerinView= (TextView) findViewById(R.id.gcast_deezer);
        gCastDeezerinView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (googleTosCheckIsSuccess())
                    launchTheApp("deezer.android.app");
            }
        });

        TextView gCastGooglePlay= (TextView) findViewById(R.id.gcast_google_play);
        gCastGooglePlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (googleTosCheckIsSuccess())
                    launchTheApp("com.google.android.music");
            }
        });


        TextView gCastPandoraView= (TextView) findViewById(R.id.gcast_pandora);
        gCastPandoraView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (googleTosCheckIsSuccess())
                    launchTheApp("com.pandora.android");

            }
        });

        TextView gcastSpotify= (TextView) findViewById(R.id.gcast_spotify);
        gcastSpotify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (googleTosCheckIsSuccess())
                    launchTheApp("com.spotify.music");

            }
        });


        TextView gmoreApps= (TextView) findViewById(R.id.moreapps);
        SpannableString content = new SpannableString("Google Cast-enabled apps");
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        gmoreApps.setText(content);

        gmoreApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (googleTosCheckIsSuccess()) {

                    Intent intent = new Intent(GcastSources.this,LibreWebViewActivity.class);
                    intent.putExtra("url","http://g.co/cast/audioapps");
                    intent.putExtra("title", "Sources");
                    startActivity(intent);

                }
            }
        });

        LinearLayout discoverAllCastDevices = (LinearLayout)findViewById(R.id.discoverAllCastDevices);
        discoverAllCastDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // deep linking
                if (googleTosCheckIsSuccess())
                    launchTheApp("com.google.android.apps.chromecast.app","DEVICES");

            }
        });
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
    public void launchTheApp(String appPackageName,String deepLinkExtension) {

        Intent intent = getPackageManager().getLaunchIntentForPackage(appPackageName);
        if (intent != null) {
          /*  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);*/
            intent = new Intent(appPackageName+"."+deepLinkExtension);
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

    private boolean googleTosCheckIsSuccess() {
        SharedPreferences sharedPreferences = getApplicationContext()
                .getSharedPreferences(Constants.SHOWN_GOOGLE_TOS, Context.MODE_PRIVATE);
        String shownGoogle = sharedPreferences.getString(Constants.SHOWN_GOOGLE_TOS, null);
        if (shownGoogle==null){

            showGoogleTosDialog(this);
            return false;
        }
        return true;
    }


    private SpannableString clickableString(String sampleString) {
        SpannableString spannableString = new SpannableString(sampleString);
        ClickableSpan googleTermsClicked = new ClickableSpan() {
            @Override
            public void onClick(View textView) {

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("http://www.google.com/intl/en/policies/privacy/"));
                startActivity(intent);


            }
        };


        ClickableSpan termsClicked = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("https://www.google.com/intl/en/policies/terms/"));
                startActivity(intent);
            }
        };

        ClickableSpan privacyClicked = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("https://www.google.com/intl/en/policies/privacy/"));
                startActivity(intent);
            }
        };

        spannableString.setSpan(termsClicked, 4, 25, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(googleTermsClicked, 395, 418, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(privacyClicked, 422, 444, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.brand_orange)), 4, 25,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.brand_orange)), 395, 418,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.brand_orange)), 422, 444,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }


    public void showGoogleTosDialog(Context context){

        LayoutInflater factory = LayoutInflater.from(this);
        final View gcastDialog = factory.inflate(
                R.layout.gcast_alert_dialog, null);
        TextView textView= (TextView)gcastDialog. findViewById(R.id.google_text);
        textView.setText(clickableString(getString((R.string.google_tos))));
        textView.setAutoLinkMask(RESULT_OK);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setGravity(Gravity.LEFT);
        Linkify.addLinks(clickableString(getString((R.string.google_tos))), Linkify.WEB_URLS);

        final AlertDialog.Builder builder = new AlertDialog.Builder(GcastSources.this);
        builder.setTitle(getString(R.string.googleTerms))
                .setCancelable(false)
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {



                    }
                })
                .setPositiveButton(getString(R.string.accept), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                                SharedPreferences sharedpreferences = getApplicationContext()
                                        .getSharedPreferences(Constants.SHOWN_GOOGLE_TOS, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedpreferences.edit();
                                editor.putString(Constants.SHOWN_GOOGLE_TOS, getString(R.string.yes));
                                editor.commit();

                                LUCIControl control=new LUCIControl("");
                                control.sendGoogleTOSAcceptanceIfRequired(GcastSources.this,"true");

;
                            }
                        }).start();

                        Toast.makeText(GcastSources.this, getString(R.string.canUseGcastOptions), Toast.LENGTH_LONG).show();

                    }

                });


        AlertDialog googleTosAlert = builder.create();
        googleTosAlert.setView(gcastDialog);
        googleTosAlert.show();


    }




}