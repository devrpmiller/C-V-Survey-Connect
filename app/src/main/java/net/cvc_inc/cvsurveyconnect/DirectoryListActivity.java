package net.cvc_inc.cvsurveyconnect;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

public class DirectoryListActivity extends AppCompatActivity {

    final static int SCANNED_PLANS = 0, FIELD = 1, CUTSHEET = 2, STAKEOUT = 3;

    private ListView directoryList;
    private Toolbar myToolbar;
    private TextView emptyView, currentPathView;
    private NtlmPasswordAuthentication userCreds;
    private CustomArrayAdapter adapter;
    private String myPath, currentJob;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private boolean reversSort;
    private int dirListType;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.directory_list);
        myToolbar = findViewById(R.id.myToolbar);
        currentPathView = findViewById(R.id.pathTextView);
        emptyView = findViewById(R.id.emptyView);
        if (getIntent().hasExtra("EXTRA_USER_AUTH")) {
            userCreds = (NtlmPasswordAuthentication) getIntent().getSerializableExtra("EXTRA_USER_AUTH");
        }
        if (getIntent().hasExtra("EXTRA_FULL_PATH")) {
            myPath = getIntent().getStringExtra("EXTRA_FULL_PATH");
        }
        if (getIntent().hasExtra("EXTRA_CURRENT_JOB")) {
            currentJob = getIntent().getStringExtra("EXTRA_CURRENT_JOB");
        }
        if (getIntent().hasExtra("EXTRA_DIR_LIST_TYPE")) {
            dirListType = getIntent().getIntExtra("EXTRA_DIR_LIST_TYPE", 0);
        }
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(currentJob);

        sharedPref = getPreferences(Context.MODE_PRIVATE);
        reversSort = sharedPref.getBoolean(getString(R.string.ReverseSort), false);

        switch (dirListType) {
            case SCANNED_PLANS:
                getSupportActionBar().setSubtitle("Scanned Plans");
                break;
            case FIELD:
                getSupportActionBar().setSubtitle("Download - Field");
                break;
            case CUTSHEET:
                getSupportActionBar().setSubtitle("Download - Cutsheet");
                break;
            case STAKEOUT:
                getSupportActionBar().setSubtitle("Download - Stakeout");
                break;
        }

        currentPathView.setText(myPath.substring(18, myPath.length()));
        currentPathView.setSingleLine(true);
        currentPathView.setEllipsize(TextUtils.TruncateAt.START);
        directoryList = findViewById(R.id.direcotryListView);
        new getSubDirectory(myPath).execute();
        directoryList.setEmptyView(emptyView);
        directoryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (isDirectory(myPath + adapterView.getItemAtPosition(i).toString())) {
                    Intent intent = new Intent(view.getContext(), DirectoryListActivity.class);
                    intent.putExtra("EXTRA_USER_AUTH", userCreds);
                    intent.putExtra("EXTRA_FULL_PATH", myPath + adapterView.getItemAtPosition(i).toString());
                    intent.putExtra("EXTRA_CURRENT_JOB", currentJob);
                    intent.putExtra("EXTRA_DIR_LIST_TYPE", dirListType);
                    startActivity(intent);
                } else {
                    new DownloadFile(adapterView.getItemAtPosition(i).toString(), myPath, currentJob, userCreds, DirectoryListActivity.this).showDownloadDialog();
                }
            }
        });
    }

    private boolean isDirectory(String path) {
        final boolean[] dirBool = new boolean[1];
        try {
            final SmbFile myDir = new SmbFile(path, userCreds);
            Thread bgTask = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        dirBool[0] = myDir.isDirectory();
                    } catch (Exception e) {
                        Log.d("DIRECTORY_LIST", "An error occurred in " + new Object() {
                        }.getClass().getEnclosingMethod().getName() + ": " + e.toString());
                    }
                }
            });
            bgTask.start();
            bgTask.join();
        } catch (Exception e) {
            Log.d("DIRECTORY_LIST", "An error occurred in " + new Object() {
            }.getClass().getEnclosingMethod().getName() + ": " + e.toString());
            dirBool[0] = false;
        }
        return dirBool[0];
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.directory_list_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.sort:
                editor = sharedPref.edit();
                editor.putBoolean(getString(R.string.ReverseSort), !reversSort);
                editor.commit();
                finish();
                startActivity(getIntent());
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

    private class getSubDirectory extends AsyncTask<Void, Void, Void> {
        final ArrayList<String> dirList = new ArrayList<>();
        final ArrayList<String> fileList = new ArrayList<>();
        ProgressDialog dialog = new ProgressDialog(DirectoryListActivity.this);

        String smbDir;

        public getSubDirectory(String dir) {
            smbDir = dir;
        }

        @Override
        protected void onPreExecute() {
            jcifs.Config.setProperty("jcifs.resolveOrder", "DNS");
            dialog.setCancelable(false);
            dialog.setTitle("Loading folder list");
            dialog.setMessage("Please wait...");
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... v) {
            try {
                SmbFile smbFile = new SmbFile(smbDir, userCreds);
                SmbFile[] allItems = smbFile.listFiles();
                for (int i = 0; i < allItems.length; i++) {
                    if (allItems[i].isDirectory()) {
                        dirList.add(allItems[i].getName());
                    } else {
                        fileList.add(allItems[i].getName());
                    }
                }
                if (!dirList.isEmpty()) {
                    Collections.sort(dirList, Collator.getInstance(Locale.US));
                    if(reversSort) {
                        Collections.reverse(dirList);
                    }
                }
                if (!fileList.isEmpty()) {
                    Collections.sort(fileList, Collator.getInstance(Locale.US));
                    if(reversSort) {
                        Collections.reverse(fileList);
                    }
                }
                dirList.addAll(fileList);
            } catch (Exception e) {
                Log.d("DIRECTORY_LIST", "An error occurred in " + new Object() {
                }.getClass().getEnclosingMethod().getName() + ": " + e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            adapter = new CustomArrayAdapter<String>(DirectoryListActivity.this, android.R.layout.simple_list_item_1, dirList);
            directoryList.setAdapter(adapter);
            dialog.dismiss();
        }
    }
}