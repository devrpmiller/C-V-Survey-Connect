package net.cvc_inc.cvsurveyconnect;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        long startTime = System.currentTimeMillis();
        super.onCreate(savedInstanceState);
        long endTime = System.currentTimeMillis();
        if (endTime - startTime < 1500) {
            try {
                Thread.sleep(1500 - (endTime - startTime));
            } catch (Exception e) {

            }
        }

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

}
