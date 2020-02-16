package org.vikulin.etherwallet.task;

import android.app.Activity;
import android.os.AsyncTask;
import androidx.appcompat.app.AlertDialog;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import org.vikulin.etherwallet.adapter.pojo.EtherscanBalanceListResponse;
import org.vikulin.etherwallet.listener.HandleExceptionListener;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;

import static org.vikulin.etherwallet.TransactionListActivity.ETHERSCAN_API_KEY;
import static org.web3j.utils.Numeric.decodeQuantity;
import static org.web3j.utils.Numeric.prependHexPrefix;

/**
 * Created by vadym on 21.01.17.
 */

public final class CheckBalanceTask extends AsyncTask<Void, Void, Exception> {

    private final TextView balanceText;
    private final String symbol;
    private BigDecimal balance;
    private Activity activity;
    private String address;

    public CheckBalanceTask(Activity activity, String address, TextView balanceText, String symbol) {
        this.activity = activity;
        this.address = address;
        this.balanceText = balanceText;
        this.symbol = symbol;
    }

    @Override
    protected Exception doInBackground(Void... voids) {
        EtherscanBalanceListResponse balanceResult = null;
        if(address==null){
            return null;
        }
        try {
            balanceResult = getBalance(prependHexPrefix(address));
        } catch (IOException e) {
            e.printStackTrace();
            showAlertDialogOnUiThread("",e.getMessage());
        }
        if(balanceResult!=null) {
            balance = Convert.fromWei(new BigInteger(balanceResult.getResult().get(0).getBalance()).toString(10), Convert.Unit.ETHER);
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    balanceText.setText(balance.toString()+" "+symbol);
                }
            });
        }
        return null;
    }

    private EtherscanBalanceListResponse getBalance(String address) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        String url = "https://api.etherscan.io/api?module=account&action=balancemulti&address=";
        url += address;
        url += "&tag=latest&apikey="+ETHERSCAN_API_KEY; // remove last , AND add token
        HttpGet getRequest = new HttpGet(url);
        getRequest.addHeader("accept", "application/json");
        HttpResponse response = httpClient.execute(getRequest);
        int status = response.getStatusLine().getStatusCode();
        if (status != 200) {
            showAlertDialogOnUiThread("","Etherscan service is unavailable. Returned status:"+status);
            return null;
        }
        BufferedReader br = new BufferedReader(
                new InputStreamReader((response.getEntity().getContent())));
        String content = read(br);
        Gson gson = new GsonBuilder().create();
        EtherscanBalanceListResponse etherscanBalanceListResponse = null;
        try {
            etherscanBalanceListResponse = gson.fromJson(content, EtherscanBalanceListResponse.class);
        } catch (JsonSyntaxException e){
            showAlertDialogOnUiThread("",e.getMessage()+"\n"+content+"\n"+url);
        } finally {
            br.close();
            httpClient.close();
        }
        return etherscanBalanceListResponse;
    }

    public String read(BufferedReader reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        String aux = "";
        while ((aux = reader.readLine()) != null) {
            builder.append(aux);
        }
        String text = builder.toString();
        reader.close();
        return text;
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