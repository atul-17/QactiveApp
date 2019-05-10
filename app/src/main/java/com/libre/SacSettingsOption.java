package com.libre;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.Network.NewSacActivity;
import com.libre.Network.SacActivityScreenForLaunchWifiSettings;

public class SacSettingsOption extends CTDeviceDiscoveryActivity implements View.OnClickListener {

    Button sacConfigure,pairShareConfigure;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sac_settings_option);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pairShareConfigure = (Button)findViewById(R.id.pairShareConfigure);
        sacConfigure = (Button)findViewById(R.id.sacConfigure);
        pairShareConfigure.setVisibility(View.GONE);
       pairShareConfigure.setOnClickListener(this);
        sacConfigure.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.pairShareConfigure:
                 startActivity(new Intent(SacSettingsOption.this,SacSettingsPairShare.class));
                finish();
                break;
            case R.id.sacConfigure:
                startActivity(new Intent(SacSettingsOption.this, SacActivityScreenForLaunchWifiSettings.class));
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(SacSettingsOption.this, NewSacActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
