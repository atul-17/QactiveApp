package com.libre;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;

import com.cumulations.libreV2.activity.CTDeviceDiscoveryActivity;
import com.libre.Network.SacInstructionsActivity;
import com.libre.Network.SacSettingsPairShareScreen2;

public class SacSettingsPairShare extends CTDeviceDiscoveryActivity implements View.OnClickListener {
    Button yes,no;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sac_settings_pair_share);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        yes = (Button)findViewById(R.id.yes);
        no = (Button)findViewById(R.id.no);
        yes.setOnClickListener(this);
        no.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.yes:
                startActivity(new Intent(SacSettingsPairShare.this,SacSettingsPairShareScreen2.class));
                finish();
                break;
            case R.id.no:
                startActivity(new Intent(SacSettingsPairShare.this,SacInstructionsActivity.class));
                /* Fix this issue for killing the Activitiy When ever we are going to the seprate Screen*/
                finish();
                break;
            default:
                break;
        }
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(SacSettingsPairShare.this, SacSettingsOption.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
