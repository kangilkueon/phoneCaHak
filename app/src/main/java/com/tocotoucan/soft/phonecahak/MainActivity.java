package com.tocotoucan.soft.phonecahak;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {
    LinearLayout intro_layout;
    ImageView intro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        intro_layout = (LinearLayout) findViewById(R.id.introLayout);
        intro = (ImageView) findViewById(R.id.introImage);

        boolean isRunIntro = getIntent().getBooleanExtra("intro", true);
        if (isRunIntro) {
            beforeIntro();
        } else {
            afterIntro(savedInstanceState);
        }
    }

    // 인트로 화면
    private void beforeIntro() {
        getWindow().getDecorView().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("intro", false);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        }, 1500);
    }

    // 인트로 화면 이후.
    private void afterIntro(Bundle savedInstanceState) {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
        finish();
    }
}