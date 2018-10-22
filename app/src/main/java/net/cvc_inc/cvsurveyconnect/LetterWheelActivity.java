package net.cvc_inc.cvsurveyconnect;

import android.app.ProgressDialog;
import android.content.Context;
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
import android.widget.NumberPicker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;

import static android.support.v7.widget.SearchView.INVISIBLE;
import static android.support.v7.widget.SearchView.VISIBLE;
import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.makeText;

public class LetterWheelActivity extends AppCompatActivity {

    private ListView allJobsView;
    private Toolbar myToolbar;
    private NtlmPasswordAuthentication userCreds;
    private CustomArrayAdapter adapter;
    private NumberPicker wheel;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.letter_wheel);
        if (getIntent().hasExtra("EXTRA_USER_AUTH")) {
            userCreds = (NtlmPasswordAuthentication) getIntent().getSerializableExtra("EXTRA_USER_AUTH");
        }
        myToolbar = findViewById(R.id.myToolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Select a Project");
        getSupportActionBar().setHomeButtonEnabled(true);
        allJobsView = findViewById(R.id.allJobsViewList);
        new getAllJobs().execute();
        allJobsView.setAdapter(new CustomArrayAdapter<>(LetterWheelActivity.this, android.R.layout.simple_list_item_1, new String[0]));
        allJobsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(view.getContext(), ActionSelectActivity.class);
                Bundle extras = new Bundle();
                extras.putString("EXTRA_CURRENT_JOB", adapterView.getItemAtPosition(i).toString());
                intent.putExtra("EXTRA_CURRENT_JOB", adapterView.getItemAtPosition(i).toString());
                intent.putExtra("EXTRA_FULL_PATH", Constants.getProjectsUrl() + adapterView.getItemAtPosition(i).toString().substring(0, 1) + "/");
                intent.putExtra("EXTRA_USER_AUTH", userCreds);
                startActivity(intent);
            }
        });
        wheel = findViewById(R.id.wheel_picker);
        //set min value zero
        wheel.setMinValue(0);
        //set max value from length array string reduced 1
        wheel.setMaxValue(Constants.getAtoZ().length - 1);
        //implement array string to number picker
        wheel.setDisplayedValues(Constants.getAtoZ());
        setDividerColor(wheel);
        //disable soft keyboard
        wheel.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        //set wrap true or false, try it you will know the difference
        wheel.setWrapSelectorWheel(true);
        wheel.setEnabled(true);
        wheel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent openJobListActivity = new Intent(view.getContext(), LetterJobsActivity.class);
                openJobListActivity.putExtra("EXTRA_LETTER", Constants.getAtoZ()[wheel.getValue()]);
                openJobListActivity.putExtra("EXTRA_USER_AUTH", userCreds);
                startActivity(openJobListActivity);
            }
        });
    }

    private void setDividerColor(NumberPicker picker) {
        java.lang.reflect.Field[] pickerFields = NumberPicker.class.getDeclaredFields();
        for (java.lang.reflect.Field pf : pickerFields) {
            if (pf.getName().equals("mSelectionDivider")) {
                pf.setAccessible(true);
                try {
                    pf.set(picker, getResources().getDrawable(android.R.color.transparent));
                } catch (Exception e) {
                    Log.d("LETTER_WHEEL", "An error occurred in " + new Object() {
                    }.getClass().getEnclosingMethod().getName() + ": " + e.toString());
                    makeText(this, e.toString(), LENGTH_LONG);
                }
                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        final MenuItem searchMenuItem = menu.findItem(R.id.job_search);
        final SearchView searchView = (SearchView) searchMenuItem.getActionView();
        searchView.setQueryHint("Search");
        searchView.onActionViewCollapsed();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    wheel.setVisibility(VISIBLE);
                    allJobsView.setAdapter(new CustomArrayAdapter<>(LetterWheelActivity.this, android.R.layout.simple_list_item_1, new String[0]));
                    return true;
                } else {
                    wheel.setVisibility(INVISIBLE);
                    adapter.getFilter().filter(newText);
                    allJobsView.setAdapter(adapter);
                    return false;
                }
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

    private class getAllJobs extends AsyncTask<Void, Void, Void> {
        ProgressDialog pd = new ProgressDialog(LetterWheelActivity.this);
        List<String> jobList = new ArrayList();
        List<String> savedJobList = new ArrayList();
        Set<String> savedJobSet;

        @Override
        protected void onPreExecute() {
            jcifs.Config.setProperty("jcifs.resolveOrder", "DNS");
            sharedPref = getPreferences(Context.MODE_PRIVATE);
            savedJobSet = sharedPref.getStringSet(getString(R.string.SharedJobList), null);
            if (savedJobSet == null) {
                pd.setCancelable(false);
                pd.setTitle("Loading project list");
                pd.setMessage("Please wait...");
                pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                pd.show();
            } else {
                savedJobList = new ArrayList(savedJobSet);
                adapter = new CustomArrayAdapter<String>(LetterWheelActivity.this, android.R.layout.simple_dropdown_item_1line, savedJobList);
            }
        }

        @Override
        protected Void doInBackground(Void... v) {
            SmbFile[] smbFileArray = new SmbFile[Constants.getAtoZ().length];
            SmbFile[] allFilesByLetter;
            try {
                for (int i = 0; i < Constants.getAtoZ().length; i++) {
                    smbFileArray[i] = new SmbFile(Constants.getProjectsUrl() + Constants.getAtoZ()[i] + "/", userCreds);
                }
                for (int i = 0; i < Constants.getAtoZ().length; i++) {
                    allFilesByLetter = smbFileArray[i].listFiles();
                    for (int j = 0; j < allFilesByLetter.length; j++) {
                        if (allFilesByLetter[j].isDirectory()) {
                            jobList.add(allFilesByLetter[j].getName().substring(0, allFilesByLetter[j].getName().length() - 1));
                        }
                    }
                }
            } catch (Exception e) {
                Log.d("LETTER_WHEEL", "An error occurred in " + new Object() {
                }.getClass().getEnclosingMethod().getName() + ": " + e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!savedJobList.equals(jobList)) {
                adapter = new CustomArrayAdapter<String>(LetterWheelActivity.this, android.R.layout.simple_dropdown_item_1line, jobList);
                if (pd.isShowing()) {
                    pd.dismiss();
                }
                editor = sharedPref.edit();
                editor.putStringSet(getString(R.string.SharedJobList), new HashSet(jobList));
                editor.commit();
            }
        }
    }
}