package com.dx.anonymousmessenger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class SetupInProcess extends AppCompatActivity {

    private BroadcastReceiver mMyBroadcastReceiver;
    private TextView statusText;
    private Button gotoContact,restartTorButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_setup_in_process);
        new Thread(()->{
            if(((DxApplication) getApplication()).isServerReady()){
                Intent intent = new Intent(this, AppActivity.class);
                startActivity(intent);
                finish();
            }
        }).start();
        statusText = findViewById(R.id.status_text);
        gotoContact = findViewById(R.id.btn_goto_contacts);
        restartTorButton = findViewById(R.id.btn_restart_tor);
        if(!getIntent().getBooleanExtra("first_time",true)){
            gotoContact.setVisibility(View.VISIBLE);
            gotoContact.setOnClickListener(v -> {
                ((DxApplication)getApplication()).setExitingHoldup(true);
                Intent intent = new Intent(this, AppActivity.class);
                intent.putExtra("force_app",true);
                startActivity(intent);
                finish();
            });
            restartTorButton.setVisibility(View.VISIBLE);
            restartTorButton.setOnClickListener(v -> ((DxApplication)getApplication()).restartTor());
        }
//        try {
//            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMyBroadcastReceiver,new IntentFilter("tor_status"));
//        } catch (Exception e){
//            e.printStackTrace();
//        }
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    public void onResume() {
        super.onResume();
        if(((DxApplication) getApplication()).isServerReady()){
            Intent intent = new Intent(this, AppActivity.class);
            startActivity(intent);
            finish();
        }
        if(mMyBroadcastReceiver!=null){
            return;
        }
        mMyBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                new Thread(()->{
                    try{
                        updateUi(intent.getStringExtra("tor_status"));
                    }catch (Exception ignored) {}
                }).start();
            }
        };
        try {
            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMyBroadcastReceiver,new IntentFilter("tor_status"));
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMyBroadcastReceiver);
            mMyBroadcastReceiver = null;
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void updateUi(String torStatus){
        if (torStatus==null){
            return;
        }
        if(torStatus.contains("ALL GOOD") || torStatus.contains("message") || torStatus.contains("status")){
            runOnUiThread(()->{
                try {
                    statusText.setText(torStatus);
                }catch (Exception ignored) {}
                if(((DxApplication)getApplication()).isExitingHoldup()){
                    return;
                }
                ((DxApplication)getApplication()).setExitingHoldup(true);
                Intent intent = new Intent(this, AppActivity.class);
                startActivity(intent);
                finish();
            });
            new Thread(()->{
                LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMyBroadcastReceiver);
                if(getIntent().getBooleanExtra("first_time",true)){
                    ((DxApplication) this.getApplication()).sendNotification("Ready to chat securely!",
                            "You got all you need to chat securely with your friends!",false);
                }
            }).start();
        }else{
            try {
                runOnUiThread(()-> statusText.setText(torStatus));
            }catch (Exception ignored){}
        }
    }
}