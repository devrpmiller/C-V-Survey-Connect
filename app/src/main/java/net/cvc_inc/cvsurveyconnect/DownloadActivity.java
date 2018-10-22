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

public class DownloadActivity extends AppCompatActivity {

    private Toolbar myToolbar;
    private NtlmPasswordAuthentication userCreds;
    private String fullPath, currentJob;
    private ImageView fieldButton, cutsheetsButton, stakeoutButton;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download);
        myToolbar = findViewById(R.id.myToolbar);
        if (getIntent().hasExtra("EXTRA_USER_AUTH")) {
            userCreds = (NtlmPasswordAuthentication) getIntent().getSerializableExtra("EXTRA_USER_AUTH");
        }
        if (getIntent().hasExtra("EXTRA_FULL_PATH")) {
            fullPath = getIntent().getStringExtra("EXTRA_FULL_PATH");
        }
        if (getIntent().hasExtra("EXTRA_CURRENT_JOB")) {
            currentJob = getIntent().getStringExtra("EXTRA_CURRENT_JOB");
        }
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(currentJob);
        getSupportActionBar().setSubtitle("Download");

        fieldButton = findViewById(R.id.fieldDownImage);
        fieldButton.setImageBitmap(ResizeImage.decodeSampledBitmapFromResource(getResources(), R.drawable.field, 800, 800));
        fieldButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), DirectoryListActivity.class);
                intent.putExtra("EXTRA_USER_AUTH", userCreds);
                intent.putExtra("EXTRA_FULL_PATH", fullPath + "/Survey/Field/");
                intent.putExtra("EXTRA_CURRENT_JOB", currentJob);
                intent.putExtra("EXTRA_DIR_LIST_TYPE", DirectoryListActivity.FIELD);
                startActivity(intent);
            }
        });
        cutsheetsButton = findViewById(R.id.cutsheetsDownImage);
        cutsheetsButton.setImageBitmap(ResizeImage.decodeSampledBitmapFromResource(getResources(), R.drawable.cutsheets, 800, 800));
        cutsheetsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), DirectoryListActivity.class);
                intent.putExtra("EXTRA_USER_AUTH", userCreds);
                intent.putExtra("EXTRA_FULL_PATH", fullPath + "/Survey/Cutsheets/");
                intent.putExtra("EXTRA_CURRENT_JOB", currentJob);
                intent.putExtra("EXTRA_DIR_LIST_TYPE", DirectoryListActivity.CUTSHEET);
                startActivity(intent);
            }
        });
        stakeoutButton = findViewById(R.id.stakeoutDownImage);
        stakeoutButton.setImageBitmap(ResizeImage.decodeSampledBitmapFromResource(getResources(), R.drawable.stakeout, 800, 800));
        stakeoutButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), DirectoryListActivity.class);
                intent.putExtra("EXTRA_USER_AUTH", userCreds);
                intent.putExtra("EXTRA_FULL_PATH", fullPath + "/Survey/Stakeout/");
                intent.putExtra("EXTRA_CURRENT_JOB", currentJob);
                intent.putExtra("EXTRA_DIR_LIST_TYPE", DirectoryListActivity.STAKEOUT);
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
