package net.cvc_inc.cvsurveyconnect;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import jcifs.smb.NtlmPasswordAuthentication;

public class ActionSelectActivity extends AppCompatActivity {

    private Toolbar myToolbar;
    private NtlmPasswordAuthentication auth;
    private String fullPath, jobPath, currentJob;
    private ImageView upButton, downButton, scannedButton;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.action_select);
        if (getIntent().hasExtra("EXTRA_USER_AUTH")) {
            auth = (NtlmPasswordAuthentication) getIntent().getSerializableExtra("EXTRA_USER_AUTH");
        }
        if (getIntent().hasExtra("EXTRA_CURRENT_JOB")) {
            currentJob = getIntent().getStringExtra("EXTRA_CURRENT_JOB");
        }
        if (getIntent().hasExtra("EXTRA_JOB_PATH")) {
            jobPath = getIntent().getStringExtra("EXTRA_JOB_PATH");
        }
        if (getIntent().hasExtra("EXTRA_FULL_PATH")) {
            fullPath = getIntent().getStringExtra("EXTRA_FULL_PATH");
        }
        myToolbar = findViewById(R.id.myToolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(currentJob);

        upButton = findViewById(R.id.upImage);
        upButton.setImageBitmap(ResizeImage.decodeSampledBitmapFromResource(getResources(), R.drawable.up_arrow, 800, 800));
        upButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), UploadActivity.class);
                intent.putExtra("EXTRA_UP", "UP");
                intent.putExtra("EXTRA_USER_AUTH", auth);
                intent.putExtra("EXTRA_FULL_PATH", fullPath + currentJob);
                intent.putExtra("EXTRA_CURRENT_JOB", currentJob);
                startActivity(intent);
            }
        });
        downButton = findViewById(R.id.downImage);
        downButton.setImageBitmap(ResizeImage.decodeSampledBitmapFromResource(getResources(), R.drawable.down_arrow, 800, 800));
        downButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), DownloadActivity.class);
                intent.putExtra("EXTRA_DOWN", "DOWN");
                intent.putExtra("EXTRA_USER_AUTH", auth);
                intent.putExtra("EXTRA_FULL_PATH", fullPath + currentJob);
                intent.putExtra("EXTRA_CURRENT_JOB", currentJob);
                startActivity(intent);
            }
        });
        scannedButton = findViewById(R.id.scannedImage);
        scannedButton.setImageBitmap(ResizeImage.decodeSampledBitmapFromResource(getResources(), R.drawable.scanned, 800, 800));
        scannedButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), DirectoryListActivity.class);
                intent.putExtra("EXTRA_SCANNED", "SCANNED");
                intent.putExtra("EXTRA_USER_AUTH", auth);
                intent.putExtra("EXTRA_FULL_PATH", fullPath + currentJob + "/Scanned Plans/");
                intent.putExtra("EXTRA_CURRENT_JOB", currentJob);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.standard_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
//            case R.id.settings:
//                startActivity(new Intent(this, SettingsActivity.class)
//                        .putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.SettingsFragment.class.getName())
//                        .putExtra(SettingsActivity.EXTRA_NO_HEADERS, true));
//                return true;
            case R.id.about:
                new AboutDialog().AboutDialog(this);
                return true;
            case R.id.exit:
                new ExitDialog(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
