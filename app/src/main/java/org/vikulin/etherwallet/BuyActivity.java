package org.vikulin.etherwallet;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.CipherException;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.vikulin.etherwallet.CodeScannerActivity.QR_CODE_DATA;
import static org.vikulin.etherwallet.SignTransactionActivity.SIGN_TRANSACTION;

public class BuyActivity extends BuySellActivity {

    private String addressTo;
    private String transactionId;

    @Override
    protected String getAddressProperty() {
        return DEFAULT_BUY_ADDRESS;
    }

    @Override
    protected boolean validateFields() {
        if (getWalletListView().getText()==null || getWalletListView().getText().toString().trim().length()!=40) {
            getWalletListView().setError("Choose address");
            getWalletListView().requestFocus();
            return false;
        }
        return true;
    }

    @Override
    protected void process() throws JSONException, CipherException, IOException {
        double total = getAdapter().getTotal();
        signTransaction(total);
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_buy;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String rawString = extras.getString(QR_CODE_DATA);
            //Toast.makeText(this, "Сканирование завершено!", Toast.LENGTH_LONG).show();
            JSONObject json;
            try {
                json = new JSONObject(rawString);
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(BuyActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            try {
                Double total = json.getDouble("t");
                addressTo = json.getString("a");
                transactionId = json.getString("id");
                if(!addressTo.startsWith("0x")){
                    addressTo="0x"+addressTo;
                }
                JSONArray items = json.getJSONArray("i");
                getAdapter().setSellItems(items);
                getAdapter().notifyDataSetChanged();
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(BuyActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        }

    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        //Web3ClientVersion web3ClientVersion = web3.web3ClientVersion().sendAsync().get();
        //System.out.println(web3ClientVersion.getWeb3ClientVersion());
        String address = "0x6dcfe076f96a3dc4ea32aaebd8ddefab39c8e1ae";

        BigInteger startBlock = BigInteger.valueOf(2765548);

    }

    private final void signTransaction(double total) throws IOException, CipherException, JSONException {
        //final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
        Set<String> savedKeys = preferences.getStringSet("keys", new HashSet<String>());
        for(int i=0; i<savedKeys.size();i++){
            String addressFrom = new JSONObject(String.valueOf(savedKeys.toArray()[i])).getString("address");
            if (addressFrom.equalsIgnoreCase(getWalletListView().getText().toString())){
                Intent intent = new Intent(this, SignTransactionActivity.class);
                intent.putExtra(SignTransactionActivity.ADDRESS_TO, addressTo);
                intent.putExtra(SignTransactionActivity.ADDRESS_FROM, addressFrom);
                intent.putExtra(SignTransactionActivity.VALUE, total);
                intent.putExtra(SignTransactionActivity.TRANSACTION_ID, transactionId);

                intent.putExtra(SignTransactionActivity.KEY_CONTENT, String.valueOf(savedKeys.toArray()[i]));
                startActivityForResult(intent, SIGN_TRANSACTION);
            }
        }
    }
}
