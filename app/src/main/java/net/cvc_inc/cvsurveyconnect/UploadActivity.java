package net.cvc_inc.cvsurveyconnect;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import jcifs.smb.NtlmPasswordAuthentication;

public class UploadActivity extends AppCompatActivity {

    private Toolbar myToolbar;
    private NtlmPasswordAuthentication userCreds;
    private String myPath, currentJob, subFolder = null;
    private ImageView imagesButton, cutsheetsButton, pointsButton;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upload);
        myToolbar = findViewById(R.id.myToolbar);
        if (getIntent().hasExtra("EXTRA_USER_AUTH")) {
            userCreds = (NtlmPasswordAuthentication) getIntent().getSerializableExtra("EXTRA_USER_AUTH");
        }
        if (getIntent().hasExtra("EXTRA_FULL_PATH")) {
            myPath = getIntent().getStringExtra("EXTRA_FULL_PATH");
        }
        if (getIntent().hasExtra("EXTRA_CURRENT_JOB")) {
            currentJob = getIntent().getStringExtra("EXTRA_CURRENT_JOB");
        }
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(currentJob);
        getSupportActionBar().setSubtitle("Upload");

        final Intent target = new Intent(Intent.ACTION_GET_CONTENT);
        target.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        imagesButton = findViewById(R.id.imagesUpImage);
        imagesButton.setImageBitmap(ResizeImage.decodeSampledBitmapFromResource(getResources(), R.drawable.images, 800, 800));
        imagesButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                target.setType("image/*");
                try {
                    startActivityForResult(Intent.createChooser(target, "Choose File"), UploadFiles.IMAGE_FILE);
                } catch (Exception e) {
                    Log.d("UPLOAD", "An error occurred in " + new Object() {
                    }.getClass().getEnclosingMethod().getName() + ": " + e.toString());
                }
            }
        });
        cutsheetsButton = findViewById(R.id.cutsheetsUpmage);
        cutsheetsButton.setImageBitmap(ResizeImage.decodeSampledBitmapFromResource(getResources(), R.drawable.cutsheets, 800, 800));
        cutsheetsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                try {
                    LayoutInflater inflater = LayoutInflater.from(UploadActivity.this);
                    View cutsheetDialog = inflater.inflate(R.layout.three_button_dialog, null);

                    final Button uploadButton, resumeButton, newButon;
                    final TextView title, message;
                    newButon = cutsheetDialog.findViewById(R.id.Button1);
                    newButon.setText(R.string.new_button);
                    resumeButton = cutsheetDialog.findViewById(R.id.Button2);
                    resumeButton.setText(R.string.resume_button);
                    uploadButton = cutsheetDialog.findViewById(R.id.Button3);
                    uploadButton.setText(R.string.upload_button);
                    title = cutsheetDialog.findViewById(R.id.dialogTitle);
                    title.setText(R.string.cs_dialog_title);
                    message = cutsheetDialog.findViewById(R.id.dialogMessage);
                    message.setText(R.string.cutsheet_message);
                    final AlertDialog.Builder builder = new AlertDialog.Builder(UploadActivity.this);
                    builder.setView(cutsheetDialog);
                    final AlertDialog dialog = builder.show();
                    newButon.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            renameFileDialog();
                            dialog.dismiss();
                        }
                    });
                    resumeButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            MicrosoftExcel.launchApplication(UploadActivity.this);
                            dialog.dismiss();
                        }
                    });
                    uploadButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            target.setType("*/*");
                            target.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/pdf"});
                            startActivityForResult(Intent.createChooser(target, "Choose file to upload"), UploadFiles.CUTSHEET_FILE);
                            dialog.dismiss();
                        }
                    });
                } catch (Exception e) {
                    Log.d("UPLOAD", "An error occurred in " + new Object() {
                    }.getClass().getEnclosingMethod().getName() + ": " + e.toString());
                }
            }
        });
        pointsButton = findViewById(R.id.pointsUpImage);
        pointsButton.setImageBitmap(ResizeImage.decodeSampledBitmapFromResource(getResources(), R.drawable.points, 800, 800));
        pointsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                try {
                    createFolderDialog();
                } catch (Exception e) {
                    Log.d("UPLOAD", "An error occurred in " + new Object() {
                    }.getClass().getEnclosingMethod().getName() + ": " + e.toString());
                }
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent fileChooser) {
        super.onActivityResult(requestCode, resultCode, fileChooser);
        int count = 0;
        String fileType;
        Uri[] fileUri;

        try {
            if (resultCode == RESULT_OK) {
                if (fileChooser.getClipData() != null) {
                    count = fileChooser.getClipData().getItemCount();
                    fileUri = new Uri[count];

                    for (int i = 0; i < count; i++) {
                        fileUri[i] = fileChooser.getClipData().getItemAt(i).getUri();
                    }
                } else {
                    count = 1;
                    fileUri = new Uri[]{fileChooser.getData()};
                }
                fileType = getTypeString(requestCode, count);
                new UploadFiles(fileUri, myPath, subFolder, requestCode, userCreds, UploadActivity.this).pushFiles(fileType);
            } else if (resultCode == RESULT_CANCELED) {
                fileType = getTypeString(requestCode, count);
                new AlertDialog.Builder(UploadActivity.this)
                        .setTitle("Upload Cancelled")
                        .setMessage("No " + fileType + " were selected.")
                        .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        }).show();
            }
        } catch (Exception e) {
            Log.d("UPLOAD", "An error occurred in " + new Object() {
            }.getClass().getEnclosingMethod().getName() + ": " + e.toString());
        }
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

    private String getTypeString(int typeInt, int count) {
        String typeStr;
        switch (typeInt) {
            case UploadFiles.CUTSHEET_FILE:
                if (count == 1) {
                    typeStr = "cutsheet";
                } else {
                    typeStr = "cutsheets";
                }
                break;
            case UploadFiles.IMAGE_FILE:
                if (count == 1) {
                    typeStr = "image";
                } else {
                    typeStr = "images";
                }
                break;
            case UploadFiles.POINT_FILE:
                if (count == 1) {
                    typeStr = "point file";
                } else {
                    typeStr = "point files";
                }
                break;
            default:
                if (count == 1) {
                    typeStr = "file";
                } else {
                    typeStr = "files";
                }
                break;
        }
        return typeStr;
    }

    public void renameFileDialog() {
        LayoutInflater inflater = LayoutInflater.from(UploadActivity.this);
        View renameDialog = inflater.inflate(R.layout.rename_dialog, null);

        final Button enter, cancel;
        final TextView title;
        final EditText newName;
        cancel = renameDialog.findViewById(R.id.Button1);
        cancel.setText(R.string.cancel_button);
        enter = renameDialog.findViewById(R.id.Button2);
        enter.setText(R.string.enter_button);
        title = renameDialog.findViewById(R.id.dialogTitle);
        title.setText(R.string.rename_dialog_title);
        newName = renameDialog.findViewById(R.id.dialogNewName);
        newName.setText(new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + "-");
        newName.setSelection(newName.getText().length());
        final AlertDialog.Builder builder = new AlertDialog.Builder(UploadActivity.this);
        builder.setView(renameDialog);
        final AlertDialog dialog = builder.show();
        enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DownloadFile(Constants.getCutsheetDoc(), myPath, currentJob, userCreds, UploadActivity.this).downloadDocument(newName.getText().toString());
                dialog.dismiss();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
    }

    public void createFolderDialog() {
        LayoutInflater inflater = LayoutInflater.from(UploadActivity.this);
        View renameDialog = inflater.inflate(R.layout.rename_dialog, null);

        final Intent target = new Intent(Intent.ACTION_GET_CONTENT);
        final Button enter, cancel;
        final TextView title;
        final EditText newName;
        cancel = renameDialog.findViewById(R.id.Button1);
        cancel.setText(R.string.cancel_button);
        enter = renameDialog.findViewById(R.id.Button2);
        enter.setText(R.string.enter_button);
        title = renameDialog.findViewById(R.id.dialogTitle);
        title.setText(R.string.point_subfolder_dialog_title);
        newName = renameDialog.findViewById(R.id.dialogNewName);
        newName.setText(new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + "-");
        newName.setSelection(newName.getText().length());

        final AlertDialog.Builder builder = new AlertDialog.Builder(UploadActivity.this);
        builder.setView(renameDialog);
        final AlertDialog dialog = builder.show();
        enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                target.setType("*/*");
                target.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                subFolder = newName.getText().toString();
                startActivityForResult(Intent.createChooser(target, "Choose File"), UploadFiles.POINT_FILE);
                dialog.dismiss();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
    }
}