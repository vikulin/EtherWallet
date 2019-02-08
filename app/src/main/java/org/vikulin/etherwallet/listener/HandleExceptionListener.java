package org.vikulin.etherwallet.listener;

import android.app.Activity;
import android.content.DialogInterface;

import org.acra.ACRA;

/**
 * Created by vadym on 11.02.17.
 */

public class HandleExceptionListener implements DialogInterface.OnClickListener{

    private Activity activity;
    private String message;

    public HandleExceptionListener(Activity activity, String message){
        this.activity = activity;
        this.message = message;
    }
    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        ACRA.getErrorReporter().handleSilentException(new Exception(message));
        activity.finish();
    }
}