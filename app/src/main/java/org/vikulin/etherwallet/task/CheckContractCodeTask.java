package org.vikulin.etherwallet.task;

import android.app.Activity;
import android.os.AsyncTask;
import androidx.appcompat.app.AlertDialog;
import android.widget.TextView;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;
import java.util.concurrent.ExecutionException;

import static org.web3j.utils.Numeric.prependHexPrefix;

/**
 * Created by vadym on 21.01.17.
 */

public final class CheckContractCodeTask extends AsyncTask<Void, Void, Exception> {

    private final TextView gasValue;
    private Activity activity;
    private String address;
    private Web3j web3j;

    public CheckContractCodeTask(Activity activity, String address, TextView gasValue) {
        this.activity = activity;
        this.address = address;
        this.gasValue = gasValue;
    }

    @Override
    protected Exception doInBackground(Void... voids) {
        try {
            web3j = Web3jFactory.build(new HttpService("https://mainnet.infura.io/v3/fe943ac1a7ef432f9f41d61417ebb350"));
        } catch (RuntimeException e){
            e.printStackTrace();
            return e;
        }
        String codeResult = null;
        if(address==null){
            return null;
        }
        try {
            codeResult = web3j.ethGetCode(prependHexPrefix(address), DefaultBlockParameterName.LATEST).sendAsync().get().getResult();
        } catch (InterruptedException e) {
            e.printStackTrace();
            //Skip notification
            //showAlertDialogOnUiThread("",e.getMessage());
        } catch (ExecutionException e) {
            e.printStackTrace();
            //Skip notification
            //showAlertDialogOnUiThread("",e.getMessage());
        }
        if(codeResult!=null && codeResult.length()>2) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    gasValue.setText("120000");////
                }
            });
        } else {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    gasValue.setText("21000");
                }
            });
        }
        return null;
    }

    @Override
    protected void onPostExecute(final Exception exception) {
        if (exception!=null && !activity.isFinishing()) {
            showAlertDialog("",exception.getMessage());
        }
    }

    protected void showAlertDialogOnUiThread(final String title, final String message){
        if(!activity.isFinishing()) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    new AlertDialog.Builder(gasValue.getContext())
                            .setTitle(title)
                            .setMessage(message)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
            });
        }
    }

    protected void showAlertDialog(String title, String message){
        new AlertDialog.Builder(gasValue.getContext())
                .setTitle(title)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}