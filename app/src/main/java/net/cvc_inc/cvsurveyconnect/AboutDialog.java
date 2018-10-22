package net.cvc_inc.cvsurveyconnect;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class AboutDialog {
    public void AboutDialog(final Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View aboutDialog = inflater.inflate(R.layout.about, null);
        final Button ok = aboutDialog.findViewById(R.id.aboutButton);
        final TextView version = aboutDialog.findViewById(R.id.aboutVersion2);
        version.setText(BuildConfig.VERSION_NAME);

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(aboutDialog);
        final AlertDialog dialog = builder.show();
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
    }
}
