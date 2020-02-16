package org.vikulin.etherwallet.task;

import android.app.Activity;
import android.os.AsyncTask;
import androidx.appcompat.app.AlertDialog;
import android.widget.TextView;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.vikulin.etherwallet.AddressListActivity.REAL_FORMATTER;
import static org.web3j.utils.Numeric.decodeQuantity;
import static org.web3j.utils.Numeric.prependHexPrefix;

/**
 * Created by vadym on 21.01.17.
 */

public final class CheckTotalBalanceTask extends AsyncTask<Void, Void, Exception> {

    private final TextView balanceText;
    private final String symbol;
    private BigInteger balance;
    private Activity activity;
    private List<String> addresses;
    private Web3j web3j;

    public CheckTotalBalanceTask(Activity activity, List<String> addresses, TextView balanceText, String symbol) {
        this.activity = activity;
        this.addresses = addresses;
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
        if(addresses==null){
            return null;
        }
        BigDecimal totalBalance = BigDecimal.ZERO;
        for(String address: addresses) {
            try {
                balanceResult = web3j.ethGetBalance(prependHexPrefix(address), DefaultBlockParameterName.LATEST).sendAsync().get().getResult();
            } catch (InterruptedException e) {
                e.printStackTrace();
                //showAlertDialogOnUiThread("", e.getMessage());
            } catch (ExecutionException e) {
                e.printStackTrace();
                //showAlertDialogOnUiThread("", e.getMessage());
            }
            if(balanceResult!=null) {
                balance = decodeQuantity(balanceResult);
                totalBalance = totalBalance.add(Convert.fromWei(balance.toString(), Convert.Unit.ETHER));
            }
        }

        DecimalFormat formatter = (DecimalFormat) REAL_FORMATTER.clone();
        formatter.setMaximumFractionDigits(2);
        final String finalTotalBalance = formatter.format(totalBalance);
        activity.runOnUiThread(new Runnable() {
            public void run() {
                balanceText.setText(finalTotalBalance.toString() + " " + symbol);
            }
        });

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