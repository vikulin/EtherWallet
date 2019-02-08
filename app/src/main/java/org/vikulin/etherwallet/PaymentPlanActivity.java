package org.vikulin.etherwallet;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.vikulin.etherwallet.task.CheckBalanceTask;
import org.web3j.utils.Numeric;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.web3j.utils.Numeric.prependHexPrefix;

/**
 * Created by vadym on 07.01.17.
 */

public class PaymentPlanActivity extends AddressListActivity {

    public final static String ADDRESS_TO="address_to";
    public final static String PAYMENT_VALUE="payment_value";
    public final static String DOMAIN="domain";
    public final static String LINKED_ADDRESS="linked_address";
    public static final int DOMAIN_PAYMENT = 3141;
    public static final int CHANGER_PAYMENT = 3142;

    //private SharedPreferences preferences;
    private TextView balanceText;
    private TextView addressFrom;
    private EditText addressTo;
    private EditText gasValue;
    private EditText value;
    private String domain;
    private String linkedAddress;

    @Override
    public TextView getWalletListView() {
        return addressFrom;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_plan);
        addressFrom = (TextView) findViewById(R.id.address);
        addressTo = (EditText) findViewById(R.id.addressTo);
        value = (EditText) findViewById(R.id.value);
        final ImageView idIcon = (ImageView) findViewById(R.id.idIcon);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String addressTo = extras.getString(ADDRESS_TO);
            String paymentValue = extras.getString(PAYMENT_VALUE);
            domain = extras.getString(DOMAIN);
            linkedAddress = extras.getString(LINKED_ADDRESS);
            this.addressTo.setText(addressTo);
            idIcon.setVisibility(View.VISIBLE);
            setIcon(addressTo, idIcon, 6);
            value.setText(paymentValue);
        }
        gasValue = (EditText) findViewById(R.id.gasLimit);
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
        addressFrom = (TextView)findViewById(R.id.address);
        final String address = preferences.getString(getAddressProperty(), null);
        if(address!=null) {
            int i = Arrays.asList(addresses).indexOf(address);
            if(i>=0){
                addressFrom.setText(address);
                setIcon(address);
            }
        } else {
            if(addresses.length==1){
                addressFrom.setText(addresses[0]);
                setIcon(addresses[0]);
            }
        }
        getPopupWindow(R.layout.spinner_address_item, addresses, addressFrom, getString(R.string.choose_address));
        balanceText = (TextView) findViewById(R.id.balance);
        checkBalance(address);
        addressFrom.addTextChangedListener(new TextWatcher(){

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                checkBalance(editable.toString());
            }
        });
    }

    private void checkBalance(String address){
        mBalanceTask = new CheckBalanceTask(this, address, balanceText, "ETH");
        mBalanceTask.execute((Void) null);
    }

    protected String getAddressProperty() {
        return DEFAULT_SELL_ADDRESS;
    }

    private CheckBalanceTask mBalanceTask = null;

    public void onClickGenerateTransaction(View view) {
        if(!validateFields()){
            return;
        }
        String paymentAddress = getWalletListView().getText().toString();
        preferences.edit().putString(getAddressProperty(), paymentAddress.toLowerCase()).commit();
        try {
            sendEther(Double.valueOf(value.getText().toString()));
        } catch (JSONException e) {
            e.printStackTrace();
            showAlertDialog("",e.getMessage());
        }
    }

    private final void sendEther(double total) throws JSONException {
        //final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
        Set<String> savedKeys = preferences.getStringSet("keys", new HashSet<String>());
        boolean found = false;
        for(int i=0; i<savedKeys.size();i++){
            String addressFrom = new JSONObject(String.valueOf(savedKeys.toArray()[i])).getString("address");
            if (addressFrom.equalsIgnoreCase(getWalletListView().getText().toString())){
                found = true;
                Intent intent = new Intent(this, SendEthOnlineActivity.class);
                intent.putExtra(SendEthOnlineActivity.ADDRESS_TO, addressTo.getText().toString());
                intent.putExtra(SendEthOnlineActivity.ADDRESS_FROM, addressFrom);
                intent.putExtra(SendEthOnlineActivity.VALUE, total);
                if(domain!=null) {
                    String paymentData = Numeric.toHexString((domain + "|" + prependHexPrefix(linkedAddress)).getBytes());
                    intent.putExtra(SendEthOnlineActivity.PAYMENT_DATA, paymentData);
                }
                intent.putExtra(SendEthOnlineActivity.GAS_LIMIT, gasValue.getText().toString());
                intent.putExtra(SendEthOnlineActivity.KEY_CONTENT_FROM, String.valueOf(savedKeys.toArray()[i]));
                startActivityForResult(intent, SendEthOnlineActivity.SEND_ETHER);
            }
        }
        if(!found){
            showAlertDialog("","Address not found in key store!");
        }
    }

    protected boolean validateFields() {
        if (getWalletListView().getText()==null || getWalletListView().getText().toString().trim().length()!=40) {
            getWalletListView().setError(getString(R.string.choose_address));
            getWalletListView().requestFocus();
            return false;
        }
        if (addressTo.getText()==null || addressTo.getText().toString().trim().length()!=42) {
            addressTo.setError(getString(R.string.choose_address));
            addressTo.requestFocus();
            return false;
        }
        if (value.getText()==null || value.getText().toString().trim().length()==0) {
            value.setError(getString(R.string.enter_value));
            value.requestFocus();
            return false;
        }
        try {
            if (Double.valueOf(value.getText().toString()) == 0) {
                value.setError(getString(R.string.enter_value));
                value.requestFocus();
                return false;
            }
        }catch (NumberFormatException e){
            value.setError(getString(R.string.enter_value));
            value.requestFocus();
            return false;
        }
        try{
            if (Double.valueOf(gasValue.getText().toString())<21000) {
                gasValue.setError(getString(R.string.enter_value));
                gasValue.requestFocus();
                return false;
            }
        }catch (NumberFormatException e) {
            gasValue.setError(getString(R.string.enter_value));
            gasValue.requestFocus();
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(requestCode==SendEthOnlineActivity.SEND_ETHER && resultCode==RESULT_OK){
            Intent result = new Intent();
            result.putExtra(DOMAIN, domain);
            result.putExtra(LINKED_ADDRESS, linkedAddress);
            setResult(RESULT_OK, result);
            finish();
        }
    }
}
