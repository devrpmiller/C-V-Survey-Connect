package net.cvc_inc.cvsurveyconnect;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

public class LetterJobsActivity extends AppCompatActivity {

    private ListView letterJobsList;
    private Toolbar myToolbar;
    private TextView jobNotFoundView;
    private CustomArrayAdapter adapter;
    private NtlmPasswordAuthentication userCreds;
    private String myPath, currentLetter;
    private SharedPreferences sharedPref;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.letter_jobs);
        if (getIntent().hasExtra("EXTRA_USER_AUTH")) {
            userCreds = (NtlmPasswordAuthentication) getIntent().getSerializableExtra("EXTRA_USER_AUTH");
        }
        if (getIntent().hasExtra("EXTRA_LETTER")) {
            currentLetter = getIntent().getStringExtra("EXTRA_LETTER");
        }
        myToolbar = findViewById(R.id.myToolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Select a Project");
        getSupportActionBar().setHomeButtonEnabled(true);
        letterJobsList = findViewById(R.id.letterJobsListView);
        myPath = Constants.getProjectsUrl() + currentLetter + "/";
        new getLetterJobs().execute();
        letterJobsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(view.getContext(), ActionSelectActivity.class);
                intent.putExtra("EXTRA_CURRENT_JOB", adapterView.getItemAtPosition(i).toString());
                intent.putExtra("EXTRA_FULL_PATH", myPath);
                intent.putExtra("EXTRA_USER_AUTH", userCreds);
                startActivity(intent);
            }
        });
        jobNotFoundView = findViewById(R.id.emptyView);
        letterJobsList.setEmptyView(jobNotFoundView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.job_search);
        SearchView mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setQueryHint("Search");
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return true;
            }
        });

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

    private class getLetterJobs extends AsyncTask<Void, Void, Void> {
        List<String> letterJobList = new ArrayList<>();
        ProgressDialog pd = new ProgressDialog(LetterJobsActivity.this);

        @Override
        protected void onPreExecute() {
            jcifs.Config.setProperty("jcifs.resolveOrder", "DNS");
            pd.setCancelable(false);
            pd.setTitle("Loading project list");
            pd.setMessage("Please wait...");
            pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pd.show();
        }

        protected Void doInBackground(Void... v) {
            try {
                SmbFile smbFile = new SmbFile(Constants.getProjectsUrl() + currentLetter + "/", userCreds);
                SmbFile[] filesByLetter = smbFile.listFiles();
                for (int i = 0; i < filesByLetter.length; i++) {
                    if (filesByLetter[i].isDirectory()) {
                        letterJobList.add(filesByLetter[i].getName().substring(0, filesByLetter[i].getName().length() - 1));
                    }
                }
            } catch (Exception e) {
                Log.d("LETTER_JOBS", "An error occurred in " + new Object() {
                }.getClass().getEnclosingMethod().getName() + ": " + e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            adapter = new CustomArrayAdapter<String>(LetterJobsActivity.this, android.R.layout.simple_list_item_1, letterJobList);
            letterJobsList.setAdapter(adapter);
            if (pd.isShowing()) {
                pd.dismiss();
            }
        }
    }
}