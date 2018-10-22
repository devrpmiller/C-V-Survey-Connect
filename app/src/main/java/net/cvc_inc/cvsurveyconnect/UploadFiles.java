package net.cvc_inc.cvsurveyconnect;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicBoolean;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;

public class UploadFiles {

    final static int CUTSHEET_FILE = 0, IMAGE_FILE = 1, POINT_FILE = 2, NOTIF_PROGRESS = 100, NOTIF_COMPLETE = 101;

    private Context parentContext;
    private NtlmPasswordAuthentication userCreds;
    private String serverWorkingDirectory, fileTypeStr;
    private String[] serverFileName, serverParentDirectory;
    private Calendar[] deviceFileDate;
    private Uri[] deviceFileUri;
    private long totalUploadSize = 0;
    private int fileCount, fileTypeInt, filesRemaining;

    public UploadFiles(Uri[] uri, String smbPath, @Nullable String subFolder, int requestCode, NtlmPasswordAuthentication creds, Context context) {
        parentContext = context;
        fileCount = uri.length;
        filesRemaining = fileCount;
        fileTypeInt = requestCode;
        userCreds = creds;
        deviceFileUri = uri;
        serverFileName = new String[fileCount];
        serverParentDirectory = new String[fileCount];
        deviceFileDate = new Calendar[fileCount];

        if (fileTypeInt == IMAGE_FILE) {
            serverWorkingDirectory = smbPath + Constants.getImagessUploadFolder();
        } else if (fileTypeInt == CUTSHEET_FILE) {
            serverWorkingDirectory = smbPath + Constants.getCutsheetsUploadFolder();
        } else if (fileTypeInt == POINT_FILE) {
            serverWorkingDirectory = smbPath + Constants.getPointsUploadFolder() + subFolder + "/";
        }

        for (int i = 0; i < fileCount; i++) {
            String extension = null;
            Cursor returnCursor = parentContext.getContentResolver().query(deviceFileUri[i], null, null, null, null);
            int size = returnCursor.getColumnIndex(OpenableColumns.SIZE);
            int name = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            int date = returnCursor.getColumnIndex("last_modified");
            returnCursor.moveToFirst();
            deviceFileDate[i] = GregorianCalendar.getInstance();
            deviceFileDate[i].setTimeInMillis(returnCursor.getLong(date));
            if (returnCursor.getString(name).contains(".")) {
                extension = returnCursor.getString(name).substring(returnCursor.getString(name).lastIndexOf("."));
            }
            if (fileTypeInt == IMAGE_FILE) {
                serverParentDirectory[i] = serverWorkingDirectory + deviceFileDate[i].get(Calendar.YEAR) + "-" + String.format("%02d", deviceFileDate[i].get(Calendar.MONTH) + 1) + "-" + String.format("%02d", deviceFileDate[i].get(Calendar.DAY_OF_MONTH)) + "/";
                serverFileName[i] = String.format("%02d", deviceFileDate[i].get(Calendar.HOUR_OF_DAY)) + "-" + String.format("%02d", deviceFileDate[i].get(Calendar.MINUTE)) + String.format("%02d", deviceFileDate[i].get(Calendar.SECOND)) + extension;
            } else if (fileTypeInt == CUTSHEET_FILE || fileTypeInt == POINT_FILE) {
                serverParentDirectory[i] = serverWorkingDirectory;
                serverFileName[i] = returnCursor.getString(name);
            }
            totalUploadSize += returnCursor.getLong(size);
            returnCursor.close();
        }
    }

    private static String readableFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public void pushFiles(String fileType) {
        new pushFiles(fileType).execute();
    }

    private class pushFiles extends AsyncTask<Void, Long, Long> {
        final int PROGRESS_MAX = 100;
        int progressCurrent = 0;
        String currentFileName;
        AtomicBoolean cancelled = new AtomicBoolean(false);
        InputStream in;
        OutputStream out;
        SmbFile smbFile, smbParentDir;
        byte[] buffer = new byte[16 * 1024];
        ProgressDialog progressDialog;
        AlertDialog.Builder dialog;
        NotificationManager notifManager;
        NotificationCompat.Builder notifProgress, notifComplete;
        NotificationCompat.BigTextStyle notifBigTextStyle;

        private pushFiles(String fileType) {
            fileTypeStr = fileType;

            progressDialog = new ProgressDialog(parentContext);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setCancelable(false);
            progressDialog.setMax(PROGRESS_MAX);

            dialog = new AlertDialog.Builder(parentContext);

            notifProgress = new NotificationCompat.Builder(parentContext, "uploads");
            notifProgress.setSmallIcon(R.drawable.ic_stat_survey_small_icon);
            notifProgress.setOnlyAlertOnce(true);
            notifProgress.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND);
            notifProgress.setVibrate(new long[]{0L});
            notifProgress.setPriority(NotificationCompat.PRIORITY_DEFAULT);

            notifComplete = new NotificationCompat.Builder(parentContext, "uploads");
            notifComplete.setSmallIcon(R.drawable.ic_stat_survey_small_icon);
            notifComplete.setOnlyAlertOnce(true);
            notifComplete.setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND);
            notifComplete.setVibrate(new long[]{0L});
            notifComplete.setPriority(NotificationCompat.PRIORITY_DEFAULT);

            notifBigTextStyle = new NotificationCompat.BigTextStyle();

            notifManager = (NotificationManager) parentContext.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationChannel notificationChannel = new NotificationChannel("uploads", "Uploads", NotificationManager.IMPORTANCE_LOW);
                notificationChannel.setDescription("Upload Files");
                notifManager.createNotificationChannel(notificationChannel);
            }
        }

        @TargetApi(Build.VERSION_CODES.O)
        @Override
        protected void onPreExecute() {
            jcifs.Config.setProperty("jcifs.resolveOrder", "DNS");
            if (fileCount > 0) {
                notifProgress.setContentTitle("Upload");
                progressDialog.setTitle("Upload");
                progressDialog.setMessage("Preparing to upload");
                progressDialog.setProgress(progressCurrent);

//                progressDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Send to background", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        progressDialog.hide();
//                    }
//                });
                progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        cancel();
                        progressDialog.cancel();
                    }
                });
                progressDialog.show();
                notifProgress.setStyle(notifBigTextStyle.bigText("Upload will begin shortly"));
                notifProgress.setProgress(PROGRESS_MAX, progressCurrent, false);
                notifManager.notify(NOTIF_PROGRESS, notifProgress.build());
            }
        }

        protected Long doInBackground(Void... v) {
            int bytesRead;
            long progress = 0;
            try {
                for (int i = 0; i < fileCount; i++) {
                    smbParentDir = new SmbFile(serverParentDirectory[i], userCreds);
                    if (!smbParentDir.isDirectory()) {
                        smbParentDir.mkdirs();
                    }
                    smbFile = new SmbFile(serverParentDirectory[i] + serverFileName[i], userCreds);
                    currentFileName = serverFileName[i];
                    in = parentContext.getContentResolver().openInputStream(deviceFileUri[i]);
                    out = new SmbFileOutputStream(smbFile);
                    while ((bytesRead = in.read(buffer)) > 0 && !cancelled.get()) {
                        out.write(buffer, 0, bytesRead);
                        progress += bytesRead;
                        publishProgress(progress);
                    }
                    in.close();
                    out.close();
                    if (!cancelled.get()) {
                        filesRemaining--;
                    }
                }

            } catch (Exception e) {
                Log.d("UPLOAD_FILE", "An error occurred in " + new Object() {
                }.getClass().getEnclosingMethod().getName() + ": " + e.toString());
            }
            if (cancelled.get()) {
                progress = 0;
            }
            return progress;
        }

        @TargetApi(Build.VERSION_CODES.O)
        @Override
        protected void onProgressUpdate(Long... prog) {
            progressCurrent = (int) (prog[0] * 100 / totalUploadSize);
            progressDialog.setProgress(progressCurrent);
            progressDialog.setTitle(R.string.uploading_dialog_title);
            progressDialog.setMessage("File: " + currentFileName
                    + "\nSize: " + readableFileSize(totalUploadSize)
                    + "\nUploaded: " + readableFileSize(prog[0]));
            notifProgress.setContentTitle("Uploading");
            notifProgress.setStyle(notifBigTextStyle
                    .bigText("File: " + currentFileName
                            + "\nSize: " + readableFileSize(totalUploadSize)
                            + "\nUploaded: " + readableFileSize(prog[0])));
            notifProgress.setProgress(PROGRESS_MAX, progressCurrent, false);
            notifManager.notify(NOTIF_PROGRESS, notifProgress.build());
        }

        @TargetApi(Build.VERSION_CODES.O)
        @Override
        protected void onPostExecute(Long result) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            notifManager.cancelAll();

            if (result > 0) {
                notifComplete.setContentTitle("Upload Complete");
                notifComplete.setStyle(notifBigTextStyle.bigText(fileCount + " " + fileTypeStr + " successfully uploaded. (" + readableFileSize(result) + ")"));
                dialog.setTitle("Upload Complete");
                dialog.setMessage(fileCount + " " + fileTypeStr + " successfully uploaded. (" + readableFileSize(result) + ")");
                dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).show();
            } else {
                dialog.setTitle("Upload Cancelled");
                notifComplete.setContentTitle("Upload Cancelled");
                if (fileCount == 1) {
                    dialog.setMessage(fileCount + " " + fileTypeStr + " not uploaded.");
                    notifComplete.setStyle(notifBigTextStyle.bigText(fileCount + " " + fileTypeStr + " not uploaded."));
                } else if (fileCount > 1) {
                    dialog.setMessage(filesRemaining + " of " + fileCount + " " + fileTypeStr + " not uploaded.");
                    notifComplete.setStyle(notifBigTextStyle.bigText(filesRemaining + " of " + fileCount + " " + fileTypeStr + " not uploaded."));
                }
                dialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
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
