package org.vikulin.etherwallet;

import android.app.Activity;
import android.app.NotificationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * Created by vadym on 25.04.17.
 */

public class CancelActivity extends Activity {

    public static final String NOTIFICATION_ID = "NOTIFICATION_ID";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(getIntent().getIntExtra(NOTIFICATION_ID, -1));
        finish();
    }
}
