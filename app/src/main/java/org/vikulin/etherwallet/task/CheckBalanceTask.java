package org.vikulin.etherwallet.task;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import java.math.BigInteger;
import java.util.concurrent.ExecutionException;

import static org.web3j.utils.Numeric.decodeQuantity;
import static org.web3j.utils.Numeric.prependHexPrefix;

/**
 * Created by vadym on 21.01.17.
 */

public final class CheckBalanceTask extends AsyncTask<Void, Void, Exception> {

    private final TextView balanceText;
    private final String symbol;
    private BigInteger balance;
    private Activity activity;
    private String address;
    private Web3j web3j;
    private int errorNumber = 0;

    public CheckBalanceTask(Activity activity, String address, TextView balanceText, String symbol) {
        this.activity = activity;
        this.address = address;
        this.balanceText = balanceText;
        this.symbol = symbol;
    }

    @Override
    protected Exception doInBackground(Void... voids) {
        try {
            web3j = Web3jFactory.build(new HttpService("https://mainnet.infura.io/erbkhNQe0QE11SJcEi1B"));
        } catch (RuntimeException e){
            e.printStackTrace();
            return e;
        }
        String balanceResult = null;
        if(address==null){
            return null;
        }
        try {
            balanceResult = web3j.ethGetBalance(prependHexPrefix(address), DefaultBlockParameterName.LATEST).sendAsync().get().getResult();
        } catch (InterruptedException e) {
            e.printStackTrace();
            showAlertDialogOnUiThread("",e.getMessage());
        } catch (ExecutionException e) {
            e.printStackTrace();
            if(e.getMessage()!=null && e.getMessage().contains("Unrecognized token")){
                errorNumber++;
                if(errorNumber>5){
                    showAlertDialogOnUiThread("Execution Error", "Code:5, "+e.getMessage());
                } else {
                    doInBackground(voids);
                }
            } else {
                showAlertDialogOnUiThread("Execution Exception", e.getMessage());
            }
        }
        if(balanceResult!=null) {
            balance = decodeQuantity(balanceResult);
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    balanceText.setText(Convert.fromWei(balance.toString(), Convert.Unit.ETHER).toString()+" "+symbol);
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
                    new AlertDialog.Builder(balanceText.getContext())
                            .setTitle(title)
                            .setMessage(message)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
            });
        }
    }

    protected void showAlertDialog(String title, String message){
        new AlertDialog.Builder(balanceText.getContext())
                .setTitle(title)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}