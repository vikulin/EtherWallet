package org.vikulin.etherwallet;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import org.json.JSONException;
import org.json.JSONObject;
import org.vikulin.etherwallet.adapter.TransactionListAdapter;
import org.vikulin.etherwallet.adapter.pojo.EtherscanTransaction;
import org.vikulin.etherwallet.adapter.pojo.EtherscanTransactionListResponse;
import org.vikulin.etherwallet.listener.HandleExceptionListener;
import org.web3j.utils.Convert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;

import static org.web3j.utils.Numeric.prependHexPrefix;

public class TransactionListActivity extends AddressListActivity {

    public static final String ETHERSCAN_API_KEY="YSZE17YIKHNPP8K26C93KEF8SP72Y38NA8";
    private TextView addressEditText;
    //private SharedPreferences preferences;
    private ListView transactionList;
    private int page = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_list);
        transactionList = (ListView) findViewById(R.id.txList);
        //preferences = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
        Set<String> savedKeys = preferences.getStringSet("keys", new HashSet<String>());
        String[] addresses = new String[savedKeys.size()];
        for(int i=0; i<savedKeys.size();i++){
            try {
                addresses[i]=new JSONObject(String.valueOf(savedKeys.toArray()[i])).getString("address");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        addressEditText = (TextView)findViewById(R.id.address);
        String address = preferences.getString(getAddressProperty(), null);
        if(address!=null) {
            int i = Arrays.asList(addresses).indexOf(address);
            if(i>=0){
                addressEditText.setText(address);
                setIcon(address);
            }
        } else {
            if(addresses.length>0){
                addressEditText.setText(addresses[0]);
                setIcon(addresses[0]);
            }
        }
        getPopupWindow(R.layout.spinner_address_item, addresses, addressEditText, getString(R.string.choose_address));
        if(addressEditText.getText()!=null && addressEditText.getText().toString().length()>0) {
            setTransactionList(prependHexPrefix(addressEditText.getText().toString()), page);
        }
        addressEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                page=1;
                setTransactionList(prependHexPrefix(addressEditText.getText().toString()), page);
            }
        });
        transactionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                EtherscanTransaction tx = (EtherscanTransaction)adapterView.getAdapter().getItem(i);
                view.setPressed(true);
                showTransactionDetail(tx);
            }
        });
    }

    protected String getAddressProperty() {
        return DEFAULT_SELL_ADDRESS;
    }


    private void setTransactionList(final String address, final int page){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    EtherscanTransactionListResponse etherscanTransactionListResponse = getTransactionList(address, page);
                    List<EtherscanTransaction> result = etherscanTransactionListResponse.getResult();
                    if(result==null){
                        result = new ArrayList();
                    }
                    final ListAdapter adapter = new TransactionListAdapter<>(TransactionListActivity.this, R.layout.list_transaction_item, result, address);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            transactionList.setAdapter(adapter);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    showAlertDialogOnUiThread("",e.getMessage());
                }
            }
        });
        thread.start();
    }

    private View showTransactionDetail(EtherscanTransaction tx){
        View view = LayoutInflater.from(this).inflate(R.layout.transaction_detail_layout,null);
        TextView address1 = (TextView)view.findViewById(R.id.address1);
        TextView address2 = (TextView)view.findViewById(R.id.address2);
        TextView value = (TextView)view.findViewById(R.id.value);
        TextView txHash = (TextView)view.findViewById(R.id.txHash);
        ImageView inOut = (ImageView) view.findViewById(R.id.inOut);
        value.setText(REAL_FORMATTER.format(Convert.fromWei(tx.getValue(), Convert.Unit.ETHER))+" ETH");
        txHash.setText(tx.getHash());
        switch (tx.getTransactionType()) {
            case IN:
                address1.setText(tx.getFrom());
                address2.setText(tx.getTo());
                inOut.setImageDrawable(this.getResources().getDrawable(R.drawable.in));
                break;
            case OUT:
                address1.setText(tx.getTo());
                address2.setText(tx.getFrom());
                inOut.setImageDrawable(this.getResources().getDrawable(R.drawable.out));
                break;
            case CONTRACT:
                address1.setText(tx.getFrom());
                address2.setText(tx.getContractAddress());
                LinearLayout LinearLayout = (LinearLayout)view.findViewById(R.id.contract);
                LinearLayout.setVisibility(View.VISIBLE);
                TextView inputData = (TextView)view.findViewById(R.id.input);
                inputData.setText(tx.getInput());
                inOut.setImageDrawable(this.getResources().getDrawable(R.drawable.contract));
                break;
        }

        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setView(view);
        ab.show();
        return view;
    }

    public void onClickCopyTxHash(View view) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Transaction hash", ((TextView)view).getText().toString());
        clipboard.setPrimaryClip(clip);
        showMessage(getString(R.string.address_copied));
    }

    private EtherscanTransactionListResponse getTransactionList(String address, int page) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet getRequest = new HttpGet("https://api.etherscan.io/api?module=account&action=txlist&address="+address+"&startblock=0&endblock=99999999&page="+page+"&offset=10&sort=desc&apikey="+ETHERSCAN_API_KEY);
        getRequest.addHeader("accept", "application/json");
        HttpResponse response = httpClient.execute(getRequest);
        int status = response.getStatusLine().getStatusCode();
        if (status != 200) {
            showAlertDialogOnUiThread("","Etherscan service is unavailable. Returned status:"+status, new HandleExceptionListener(this, "Etherscan service is unavailable. Returned status:"+status));
            return null;
        }
        BufferedReader br = new BufferedReader(
                new InputStreamReader((response.getEntity().getContent())));
        String content = read(br);
        Gson gson = new GsonBuilder().create();
        EtherscanTransactionListResponse etherscanTransactionListResponse = gson.fromJson(content, EtherscanTransactionListResponse.class);
        try {
            etherscanTransactionListResponse = gson.fromJson(content, EtherscanTransactionListResponse.class);
        } catch (JsonSyntaxException e){
            showAlertDialogOnUiThread("",e.getMessage()+"\n"+content);
        } finally {
            br.close();
            httpClient.close();
        }
        return etherscanTransactionListResponse;
    }

    @Override
    public TextView getWalletListView() {
        return addressEditText;
    }

    public void clickBack(View view) {
        if(page-1<1){
            return;
        }
        page--;
        if(addressEditText.getText()!=null && addressEditText.getText().toString().length()>0) {
            setTransactionList(prependHexPrefix(addressEditText.getText().toString()), page);
        }
    }

    public void clickForward(View view) {
        if(page+1>100){
            return;
        }
        page++;
        if(addressEditText.getText()!=null && addressEditText.getText().toString().length()>0) {
            setTransactionList(prependHexPrefix(addressEditText.getText().toString()), page);
        }
    }
}
