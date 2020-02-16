package org.vikulin.etherwallet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.vikulin.etherwallet.MainActivity.ZXING_WEB_VIEW_RESULT_FOR_SCAN_ADDRESS;

public class ContractActivity extends AddressListActivity {

    //private SharedPreferences preferences;
    private TextView addressFrom;
    private EditText gasLimit;
    private EditText byteCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contract);
        gasLimit = (EditText) findViewById(R.id.gasLimit);
        byteCode = (EditText) findViewById(R.id.byteCodeText);
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
        String address = preferences.getString(getAddressProperty(), null);
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
    }

    @Override
    public TextView getWalletListView() {
        return addressFrom;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == CodeScannerActivity.SCAN_BYTE_CODE && resultCode == RESULT_OK) {
            Bundle extras = intent.getExtras();
            String qrCodeData = (String) extras.get(CodeScannerActivity.QR_CODE_DATA);
            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
            EditText byteCode = (EditText) findViewById(R.id.byteCodeText);
            byteCode.setText(qrCodeData);
            return;
        }
    }

    public void onClickScanQRCode(View view) {
        launchActivityForResult(CodeScannerActivity.class);
    }

    public void launchActivityForResult(Class<?> clss) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, ZXING_WEB_VIEW_RESULT_FOR_SCAN_ADDRESS);
        } else {
            Intent intent = new Intent(this, clss);
            intent.putExtra(CodeScannerActivity.BARCODE_TYPE, CodeScannerActivity.QR_CODE);
            startActivityForResult(intent, CodeScannerActivity.SCAN_BYTE_CODE);
        }
    }

    protected String getAddressProperty() {
        return DEFAULT_SELL_ADDRESS;
    }

    public void onClickGenerateTransaction(View view) {
        if(!validateFields()){
            return;
        }
        String paymentAddress = getWalletListView().getText().toString();
        preferences.edit().putString(getAddressProperty(), paymentAddress.toLowerCase()).commit();
        try {
            Long.parseLong(gasLimit.getText().toString());
            deployContract(byteCode.getText().toString(), gasLimit.getText().toString());
        } catch (JSONException e) {
            e.printStackTrace();
            showAlertDialog("",e.getMessage());
        } catch (NumberFormatException e){
            gasLimit.setError(getString(R.string.enter_value));
        }
    }

    private final void deployContract(String byteCode, String gasLimit) throws JSONException {
        //final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
        Set<String> savedKeys = preferences.getStringSet("keys", new HashSet<String>());
        boolean found = false;
        for(int i=0; i<savedKeys.size();i++){
            String addressFrom = new JSONObject(String.valueOf(savedKeys.toArray()[i])).getString("address");
            if (addressFrom.equalsIgnoreCase(getWalletListView().getText().toString())){
                found = true;
                Intent intent = new Intent(this, DeployContractActivity.class);
                intent.putExtra(DeployContractActivity.ADDRESS_FROM, addressFrom);
                intent.putExtra(DeployContractActivity.BYTE_CODE, byteCode);
                intent.putExtra(DeployContractActivity.GAS_LIMIT, gasLimit);
                intent.putExtra(DeployContractActivity.KEY_CONTENT_FROM, String.valueOf(savedKeys.toArray()[i]));
                startActivityForResult(intent, DeployContractActivity.DEPLOY_CONTRACT);
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
        try {
            long gas_limit = Long.valueOf(gasLimit.getText().toString());
            if (gasLimit.getText().toString().trim().toString().length()==0 || gas_limit<21000) {
                gasLimit.setError(getString(R.string.enter_value));
                gasLimit.requestFocus();
                return false;
            }
        } catch (NumberFormatException e){
            gasLimit.setError(getString(R.string.enter_value));
            gasLimit.requestFocus();
            return false;
        }

        return true;
    }
}
