package net.cvc_inc.cvsurveyconnect;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class MicrosoftExcel {
    public static void launchApplication(Context context) {
        try {
            context.startActivity(context.getPackageManager().getLaunchIntentForPackage("com.microsoft.office.excel")
                    .addCategory(Intent.CATEGORY_LAUNCHER));
        } catch (Exception e) {
            Toast.makeText(context, "Please install Microsoft Excel to continue.", Toast.LENGTH_LONG).show();
            Log.d("MICROSOFT_EXCEL", "An error occurred in " + new Object() {
            }.getClass().getEnclosingMethod().getName() + ": " + e.toString());
        }
    }
}
