package net.cvc_inc.cvsurveyconnect;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicBoolean;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

public class DownloadFile {

    final static int OPEN_FILE = 0, SAVE_FILE = 1, UPLOAD_DOC = 2, NOTIF_PROGRESS = 100, NOTIF_COMPLETE = 101;

    private Context parentContext;
    private NtlmPasswordAuthentication userCreds;
    private String serverFileUri, fileName, currentJob, deviceFilePath, deviceWorkingDirectory;
    private long fileSize;
    private int downloadType;


    public DownloadFile(String name, String serverDirectoryUri, String job, NtlmPasswordAuthentication creds, Context context) {
        parentContext = context;
        fileName = name;
        serverFileUri = serverDirectoryUri + fileName;
        currentJob = job;
        userCreds = creds;
    }

    private static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public void showDownloadDialog() {
        LayoutInflater inflater = LayoutInflater.from(parentContext);
        View downloadDialog = inflater.inflate(R.layout.three_button_dialog, null);

        final Button open, save, cancel;
        final TextView title, message;
        cancel = downloadDialog.findViewById(R.id.Button1);
        cancel.setText(R.string.cancel_button);
        open = downloadDialog.findViewById(R.id.Button2);
        open.setText(R.string.open_button);
        save = downloadDialog.findViewById(R.id.Button3);
        save.setText(R.string.save_button);
        title = downloadDialog.findViewById(R.id.dialogTitle);
        title.setText(R.string.download_dialog_title);
        message = downloadDialog.findViewById(R.id.dialogMessage);
        message.setText(R.string.download_message);
        final AlertDialog.Builder builder = new AlertDialog.Builder(parentContext);
        builder.setView(downloadDialog);
        final AlertDialog dialog = builder.show();
        open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadType = OPEN_FILE;
                deviceWorkingDirectory = parentContext.getCacheDir().toString() + "/";
                deviceFilePath = deviceWorkingDirectory + fileName;
                if (fileExists(Constants.getDeviceProjectsFolder() + currentJob + "/" + fileName)) {
                    fileExistsDialog();
                } else if (fileExists(deviceFilePath)) {
                    fileExistsDialog();
                } else {
                    File deviceDirectory = new File(deviceWorkingDirectory);
                    if (!deviceDirectory.exists()) {
                        deviceDirectory.mkdirs();
                    }
                    new saveFile().execute();
                }
                dialog.dismiss();
            }
        });
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadType = SAVE_FILE;
                deviceWorkingDirectory = Constants.getDeviceProjectsFolder() + currentJob + "/";
                deviceFilePath = deviceWorkingDirectory + fileName;
                if (fileExists(deviceFilePath)) {
                    fileExistsDialog();
                } else if (fileExists(deviceFilePath)) {
                    fileExistsDialog();
                } else {
                    File deviceDirectory = new File(deviceWorkingDirectory);
                    if (!deviceDirectory.exists()) {
                        deviceDirectory.mkdirs();
                    }
                    new saveFile().execute();
                }
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

    public void downloadDocument(String saveFileName) {
        downloadType = UPLOAD_DOC;
        deviceWorkingDirectory = Constants.getDeviceProjectsFolder() + currentJob + "/Cutsheets/";
        fileName = saveFileName + ".xlsx";
        deviceFilePath = deviceWorkingDirectory + fileName;
        if (fileExists(deviceFilePath)) {
            fileExistsDialog();
        } else {
            File deviceDirectory = new File(deviceWorkingDirectory);
            if (!deviceDirectory.exists()) {
                deviceDirectory.mkdirs();
            }
            new saveFile().execute();
        }
    }

    public void openFile() {
        Uri uri = FileProvider.getUriForFile(parentContext, "net.cvc_inc.cvsurveyconnect.fileprovider", new File(deviceFilePath));
        MimeTypeMap map = MimeTypeMap.getSingleton();
        String ext = MimeTypeMap.getFileExtensionFromUrl(fileName);
        int i = fileName.lastIndexOf('.');
        if (ext == "" && i > 0) {
            ext = fileName.substring(i + 1);
        }
        String type = map.getMimeTypeFromExtension(ext);
        try {
            parentContext.startActivity(new Intent()
                    .setAction(Intent.ACTION_VIEW)
                    .setDataAndType(uri, type)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
        } catch (Exception e) {
            Log.d("DOWNLOAD_FILE", "An error occurred in " + new Object() {
            }.getClass().getEnclosingMethod().getName() + ": " + e.toString());
        }
    }

    private boolean fileExists(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return true;
        } else {
            return false;
        }
    }

    private void fileExistsDialog() {
        new AlertDialog.Builder(parentContext)
                .setTitle("File Exists")
                .setMessage("A file named \"" + fileName + "\" already exists. Would you like to open the existing file or download a new copy and replace?")
                .setPositiveButton("Download New", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        new saveFile().execute();
                        dialogInterface.dismiss();
                    }
                })
                .setNegativeButton("Open", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        openFile();
                        dialogInterface.dismiss();
                    }
                }).show();
    }

    private class saveFile extends AsyncTask<Void, Long, Long> {
        final int PROGRESS_MAX = 100;
        int progressCurrent = 0;
        AtomicBoolean cancelled = new AtomicBoolean(false);
        SmbFileInputStream in;
        FileOutputStream out;
        SmbFile smbFile;
        byte[] buffer = new byte[16 * 1024];
        ProgressDialog progressDialog;
        AlertDialog.Builder dialog;
        NotificationManager notifManager;
        NotificationCompat.Builder notifProgress, notifComplete;
        NotificationCompat.BigTextStyle notifBigTextStyle;

        private saveFile() {
            progressDialog = new ProgressDialog(parentContext);
            progressDialog.setCancelable(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMax(PROGRESS_MAX);

            dialog = new AlertDialog.Builder(parentContext);


            notifProgress = new NotificationCompat.Builder(parentContext, "downloads");
            notifProgress.setSmallIcon(R.drawable.ic_stat_survey_small_icon);
            notifProgress.setOnlyAlertOnce(true);
            notifProgress.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND);
            notifProgress.setVibrate(new long[]{0L});
            notifProgress.setPriority(NotificationCompat.PRIORITY_DEFAULT);

            notifComplete = new NotificationCompat.Builder(parentContext, "downloads");
            notifComplete.setSmallIcon(R.drawable.ic_stat_survey_small_icon);
            notifComplete.setOnlyAlertOnce(true);
            notifComplete.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND);
            notifComplete.setVibrate(new long[]{0L});
            notifComplete.setPriority(NotificationCompat.PRIORITY_DEFAULT);

            notifBigTextStyle = new NotificationCompat.BigTextStyle();

            notifManager = (NotificationManager) parentContext.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel notificationChannel = new NotificationChannel("downloads", "Downloads", NotificationManager.IMPORTANCE_LOW);
                notificationChannel.setDescription("Download File");
                notifManager.createNotificationChannel(notificationChannel);
            }
        }

        @Override
        protected void onPreExecute() {
            jcifs.Config.setProperty("jcifs.resolveOrder", "DNS");
            if (downloadType == SAVE_FILE || downloadType == OPEN_FILE) {
                notifProgress.setContentTitle("Download");
                progressDialog.setTitle("Download");
                progressDialog.setMessage("Preparing to download file.");
                progressDialog.setProgress(progressCurrent);

//                progressDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Send to background", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        progressDialog.dismiss();
//                    }
//                });
                progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        cancel();
                        progressDialog.cancel();
                    }
                });
            } else if (downloadType == UPLOAD_DOC) {
                progressDialog.setTitle("Getting Document");
                progressDialog.setMessage("Preparing file, please wait...");
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            }
            progressDialog.show();
            notifProgress.setStyle(notifBigTextStyle.bigText("Download will begin shortly"));
            notifProgress.setProgress(PROGRESS_MAX, progressCurrent, false);
            notifManager.notify(NOTIF_PROGRESS, notifProgress.build());
        }

        protected Long doInBackground(Void... v) {
            int bytesRead;
            long progress = 0;
            try {
                if (downloadType == SAVE_FILE || downloadType == OPEN_FILE) {
                    smbFile = new SmbFile(serverFileUri, userCreds);
                } else if (downloadType == UPLOAD_DOC) {
                    smbFile = new SmbFile(Constants.getCutsheetDocURL(), userCreds);
                }
                fileSize = smbFile.length();
                in = new SmbFileInputStream(smbFile);
                out = new FileOutputStream(deviceFilePath);
                while ((bytesRead = in.read(buffer)) > 0 && !cancelled.get()) {
                    out.write(buffer, 0, bytesRead);
                    progress += bytesRead;
                    publishProgress(progress);
                }
                in.close();
                out.close();
            } catch (Exception e) {
                Log.d("DOWNLOAD_FILE", "An error occurred in " + new Object() {
                }.getClass().getEnclosingMethod().getName() + ": " + e.toString());
            }
            if (cancelled.get()) {
                progress = 0;
            }
            return progress;
        }

        @Override
        protected void onProgressUpdate(Long... prog) {
            if (downloadType == OPEN_FILE || downloadType == SAVE_FILE) {
                progressCurrent = (int) (prog[0] * 100 / fileSize);
                progressDialog.setProgress(progressCurrent);
                progressDialog.setTitle(R.string.downloading_dialog_title);
                progressDialog.setMessage("File: " + fileName
                        + "\nSize: " + readableFileSize(fileSize)
                        + "\nDownloaded: " + readableFileSize(prog[0]));
                notifProgress.setContentTitle("Downloading");
                notifProgress.setStyle(notifBigTextStyle
                        .bigText("File: " + fileName
                                + "\nSize: " + readableFileSize(fileSize)
                                + "\nDownloaded: " + readableFileSize(prog[0])));
                notifProgress.setProgress(PROGRESS_MAX, progressCurrent, false);
                notifManager.notify(NOTIF_PROGRESS, notifProgress.build());
            }
        }

        @Override
        protected void onPostExecute(Long result) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            notifManager.cancelAll();
            if (result > 0) {
                notifComplete.setContentTitle("Download Complete");
                notifComplete.setStyle(notifBigTextStyle.bigText(fileName + " successfully downloaded. (" + readableFileSize(result) + ")"));
                if (downloadType == SAVE_FILE) {
                    dialog.setTitle("Open File");
                    dialog.setMessage("Successfully downloaded \"" + fileName + "\". Would you like to open the file?");
                    dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            openFile();
                        }
                    });
                    dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    dialog.show();
                } else if (downloadType == OPEN_FILE) {
                    openFile();
                } else if (downloadType == UPLOAD_DOC) {
                    MicrosoftExcel.launchApplication(parentContext);
                }
            } else {
                notifComplete.setContentTitle("Download Cancelled");
                notifComplete.setStyle(notifBigTextStyle.bigText(fileName + " not downloaded."));
                dialog.setTitle("Download Cancelled");
                dialog.setMessage(fileName + " not downloaded.");
                dialog.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                dialog.show();
            }
            notifManager.notify(NOTIF_COMPLETE, notifComplete.build());

        }

        protected void cancel() {
            cancelled.set(true);
        }
    }
}