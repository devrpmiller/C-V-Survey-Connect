package net.cvc_inc.cvsurveyconnect;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import jcifs.UniAddress;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbSession;

public class LoginActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 112;
    private NtlmPasswordAuthentication userCred;
    private long ReauthDate;
    private ImageView candvImage;
    private EditText pwordET;
    private SecurePreferences securePref;
    private SecurePreferences.Editor securePrefEditor;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // Request permissions if not already granted
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // Request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        } else {
            // Permissions already granted
            ReauthDate = new SecurePreferences(getApplicationContext()).getLong(getString(R.string.SecureReauthDate), 0);
            if (System.currentTimeMillis() < ReauthDate) {
                userCred = new NtlmPasswordAuthentication(Constants.getDomain(),
                        new SecurePreferences(getApplicationContext()).getString(getString(R.string.SecureUsername), ""),
                        new SecurePreferences(getApplicationContext()).getString(getString(R.string.SecurePassword), ""));
                try {
                    new LoginActivity.validateUser(userCred).execute();
                } catch (Exception e) {
                    Log.d("LOGIN", "An error occurred in " + new Object() {
                    }.getClass().getEnclosingMethod().getName() + ": " + e.toString());
                }
            }
        }

        candvImage = findViewById(R.id.candvImage);
        candvImage.setImageBitmap(ResizeImage.decodeSampledBitmapFromResource(getResources(), R.drawable.candvsurveyconnect, 800, 470));

        // Login button
        final Button loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                userCred = new NtlmPasswordAuthentication(Constants.getDomain(),
                        ((EditText) findViewById(R.id.usernameText)).getText().toString(),
                        ((EditText) findViewById(R.id.passwordText)).getText().toString());
                try {
                    new validateUser(userCred).execute();
                } catch (Exception e) {
                    Log.d("LOGIN", "An error occurred in " + new Object() {
                    }.getClass().getEnclosingMethod().getName() + ": " + e.toString());
                }
            }
        });

        pwordET = findViewById(R.id.passwordText);
        pwordET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (keyEvent != null && (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) || (i == EditorInfo.IME_ACTION_DONE)) {
                    loginButton.performClick();
                }
                return false;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private class validateUser extends AsyncTask<NtlmPasswordAuthentication, String, Boolean> {
        ProgressDialog pd = new ProgressDialog(LoginActivity.this);
        boolean validBool = false;
        NtlmPasswordAuthentication auth;

        public validateUser(NtlmPasswordAuthentication userCreds) {
            auth = userCreds;
        }

        @Override
        protected void onPreExecute() {
            jcifs.Config.setProperty("jcifs.resolveOrder", "DNS");
            pd.setCancelable(false);
            pd.setTitle("Logging in");
            pd.setMessage("Please wait...");
            pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pd.show();
        }

        @Override
        protected Boolean doInBackground(NtlmPasswordAuthentication... ntlmPasswordAuthentications) {
            try {
                SmbSession.logon(UniAddress.getByName(Constants.getIntIp()), auth);
                validBool = true;
            } catch (Exception e) {
                Log.d("LOGIN", "An error occurred in " + new Object() {
                }.getClass().getEnclosingMethod().getName() + ": " + e.toString());
                validBool = false;
            }
            return validBool;
        }

        @Override
        protected void onPostExecute(Boolean results) {
            if (validBool) {
                securePref = new SecurePreferences(getApplicationContext());
                securePrefEditor = securePref.edit();
                securePrefEditor.putLong(getString(R.string.SecureReauthDate), (System.currentTimeMillis() + 3600000));
                securePrefEditor.putString(getString(R.string.SecureUsername), auth.getUsername());
                securePrefEditor.putString(getString(R.string.SecurePassword), auth.getPassword());
                securePrefEditor.commit();
                Intent intent = new Intent(LoginActivity.this, LetterWheelActivity.class);
                intent.putExtra("EXTRA_USER_AUTH", userCred);
                startActivity(intent);
                finish();
                pd.dismiss();
            } else {
                Toast.makeText(LoginActivity.this, R.string.login_message, Toast.LENGTH_LONG).show();
                pd.dismiss();
            }
        }
    }
}