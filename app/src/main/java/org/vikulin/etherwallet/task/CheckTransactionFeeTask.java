package org.vikulin.etherwallet.task;

import android.app.Activity;
import android.os.AsyncTask;
import androidx.appcompat.app.AlertDialog;
import android.widget.SeekBar;
import android.widget.TextView;

import org.vikulin.etherwallet.SendEtherActivity;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.http.HttpService;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.concurrent.ExecutionException;

/**
 * Created by vadym on 21.01.17.
 */

public final class CheckTransactionFeeTask extends AsyncTask<Void, Void, BigInteger> {

    private final TextView transactionFee;
    private final TextView gasLimit;
    private final SeekBar gasPriceSeekBar;
    private Activity activity;
    private Integer gasPrice;
    private Web3j web3j;
    private DecimalFormat precision = new DecimalFormat("0.000000");

    public CheckTransactionFeeTask(Activity activity, SeekBar gasPriceSeekBar, TextView transactionFee, TextView gasLimit) {
        this.activity = activity;
        this.gasPriceSeekBar = gasPriceSeekBar;
        this.transactionFee = transactionFee;
        this.gasLimit = gasLimit;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        this.precision.setDecimalFormatSymbols(symbols);
    }

    public void setGasPrice(int gasPrice){
        ((SendEtherActivity)activity).setGasPrice(gasPrice);
    }

    @Override
    protected BigInteger doInBackground(Void... voids) {
        try {
            web3j = Web3jFactory.build(new HttpService("https://mainnet.infura.io/erbkhNQe0QE11SJcEi1B"));
        } catch (RuntimeException e){
            e.printStackTrace();
        }
        try {
            final BigInteger gasPrice = web3j.ethGasPrice().sendAsync().get().getGasPrice();
            return gasPrice;
        } catch (InterruptedException e) {
            e.printStackTrace();
            //Skip notification
            //showAlertDialogOnUiThread("",e.getMessage());
        } catch (ExecutionException e) {
            e.printStackTrace();
            //Skip notification
            //showAlertDialogOnUiThread("",e.getMessage());
        }
        return null;
    }

    @Override
    protected void onPostExecute(final BigInteger middleGasPrice) {
        if (middleGasPrice!=null && !activity.isFinishing()) {

            final int step = 1;
            int middleValue = middleGasPrice.divide(BigInteger.valueOf(1000_000_000l)).intValue();
            int max = 2*middleValue;
            final int min = 1;
            // Ex :
            // If you want values from 3 to 5 with a step of 0.1 (3, 3.1, 3.2, ..., 5)
            // this means that you have 21 possible values in the seekbar.
            // So the range of the seek bar will be [0 ; (5-3)/0.1 = 20].
            gasPriceSeekBar.setMax( (max - min) / step );
            gasPriceSeekBar.setOnSeekBarChangeListener(
                    new SeekBar.OnSeekBarChangeListener()
                    {
                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {}

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {}

                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress,
                                                      boolean fromUser)
                        {
                            // Ex :
                            // And finally when you want to retrieve the value in the range you
                            // wanted in the first place -> [3-5]
                            //
                            // if progress = 13 -> value = 3 + (13 * 0.1) = 4.3
                            int value = min + (progress * step);
                            gasPrice = value;
                            calculate();
                        }
                    }
            );
            gasPriceSeekBar.setProgress(middleValue);
        }
    }

    protected void showAlertDialogOnUiThread(final String title, final String message){
        if(!activity.isFinishing()) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    new AlertDialog.Builder(gasPriceSeekBar.getContext())
                            .setTitle(title)
                            .setMessage(message)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
            });
        }
    }

    protected void showAlertDialog(String title, String message){
        new AlertDialog.Builder(gasPriceSeekBar.getContext())
                .setTitle(title)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public void calculate() {
        if(gasPrice!=null) {
            setGasPrice(gasPrice);
            CharSequence gasLimit = this.gasLimit.getText();
            long gasLimitValue = Long.parseLong(gasLimit.toString());
            transactionFee.setText(precision.format((gasPrice * gasLimitValue) / 1000_000_000.0) + " ETH");
        }
    }
}