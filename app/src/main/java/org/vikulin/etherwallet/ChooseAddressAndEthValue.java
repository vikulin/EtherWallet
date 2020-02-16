package org.vikulin.etherwallet;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.content.FileProvider;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.CipherException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by vadym on 26.12.16.
 */

public class ChooseAddressAndEthValue extends AddressListActivity {

    public static final int CHOOSE_ADDRESS_AND_VALUE = 5000;
    private TextView addressEditText;
    private EditText value;
    private String keyContent;
    //private SharedPreferences preferences;
    public static final String KEY_CONTENT = "key_content";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_address_and_eth_value);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            keyContent = extras.getString(KEY_CONTENT);
        } else {
            finish();
        }
        value = (EditText) findViewById(R.id.enterValue);
        value.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
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
            }
        } else {
            if(addresses.length==1){
                addressEditText.setText(addresses[0]);
            }
        }

        getPopupWindow(R.layout.spinner_address_item, addresses, addressEditText, getString(R.string.choose_address));
        Button next = (Button) findViewById(R.id.next);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Send Ether
                if(!validateFields()){
                    return;
                }
                try {
                    process();
                } catch (JSONException e) {
                    showAlertDialog("",e.getMessage());
                    return;
                } catch (CipherException e) {
                    showAlertDialog("",e.getMessage());
                    return;
                } catch (IOException e) {
                    showAlertDialog("",e.getMessage());
                    return;
                }
            }
        });

        if(savedKeys==null || savedKeys.size()==0){
            importKey();
        }
    }

    protected void process() throws JSONException, CipherException, IOException {
        if(!validateFields()){
            return;
        }
        String paymentAddress = getWalletListView().getText().toString();
        preferences.edit().putString(getAddressProperty(), paymentAddress.toLowerCase()).commit();
        sendEther(Double.valueOf(value.getText().toString()));
    }

    private final void sendEther(double total) throws IOException, CipherException, JSONException {
        String addressTo = new JSONObject(keyContent).getString("address");
        //final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
        Set<String> savedKeys = preferences.getStringSet("keys", new HashSet<String>());
        for(int i=0; i<savedKeys.size();i++){
            String addressFrom = new JSONObject(String.valueOf(savedKeys.toArray()[i])).getString("address");
            if (addressFrom.equalsIgnoreCase(getWalletListView().getText().toString())){
                Intent intent = new Intent(this, SendEthOnlineActivity.class);
                intent.putExtra(SendEthOnlineActivity.ADDRESS_TO, addressTo);
                intent.putExtra(SendEthOnlineActivity.ADDRESS_FROM, addressFrom);
                intent.putExtra(SendEthOnlineActivity.VALUE, total);
                intent.putExtra(SendEthOnlineActivity.GAS_LIMIT, "21000");
                intent.putExtra(SendEthOnlineActivity.KEY_CONTENT_FROM, String.valueOf(savedKeys.toArray()[i]));
                startActivityForResult(intent, SendEthOnlineActivity.SEND_ETHER);
                break;
            }
        }
    }

    protected String getAddressProperty() {
        return DEFAULT_BUY_ADDRESS;
    }

    public TextView getWalletListView(){
        return addressEditText;
    }

    protected boolean validateFields() {
        if (value.getText()==null || value.getText().toString().trim().length()==0) {
            value.setError(getString(R.string.enter_price));
            value.requestFocus();
            return false;
        }
        try {
            if (Double.valueOf(value.getText().toString()) == 0) {
                value.setError(getString(R.string.enter_price));
                value.requestFocus();
                return false;
            }
        } catch (NumberFormatException e){
            value.setError(getString(R.string.enter_price));
            value.requestFocus();
            return false;
        }
        if (getWalletListView().getText()==null || getWalletListView().getText().toString().trim().length()!=40) {
            getWalletListView().setError(getString(R.string.choose_address));
            getWalletListView().requestFocus();
            return false;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==SendEthOnlineActivity.SEND_ETHER && resultCode==RESULT_OK){
            try {
                sendWalletKeyByEmail(keyContent);
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        if(requestCode==DrawerActivity.SEND_WALLET_KEY){
            setResult(resultCode);
            finish();
        }
    }

    public void sendWalletKeyByEmail(String decodedKeyFile) throws JSONException, IOException {
        JSONObject jObject = new JSONObject(decodedKeyFile);
        String addressTo = jObject.getString("address");
        String keyFileName = "ETH-"+addressTo+".key";
        File keyFile = new File(getExternalCacheDir(), keyFileName);
        writeFile(keyFile, decodedKeyFile);
        String text = "ETH address: "+ addressTo+"\n"+
                "1. Save the address. You can keep it to yourself or share it with others. That way, others can transfer ether to you.\n" +
                "2. Save versions of the private key. Do not share it with anyone else. Your private key is necessary when you want to access your Ether to send it!\n"+
                "3. Use your Code# to unlock your private key. Do not share it with anyone else.";
        String[] TO = {""};
        String[] CC = {""};
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("text/html");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
        emailIntent.putExtra(Intent.EXTRA_CC, CC);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "EtherWallet sent you: "+value.getText().toString()+"ETH");
        emailIntent.putExtra(Intent.EXTRA_TEXT, text);
        emailIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this,
                BuildConfig.APPLICATION_ID + ".provider",keyFile));
        emailIntent.setType("application/json");
        Intent result = Intent.createChooser(emailIntent,"");
        try {
            startActivityForResult(result, DrawerActivity.SEND_WALLET_KEY);
        } catch (android.content.ActivityNotFoundException ex) {
            showAlertDialog("","There is no email client installed.");
        }
    }
}
