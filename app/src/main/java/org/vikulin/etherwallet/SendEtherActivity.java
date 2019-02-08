package org.vikulin.etherwallet;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import org.json.JSONException;
import org.json.JSONObject;
import org.vikulin.ethername.Domain;
import org.vikulin.etherwallet.adapter.TokenAdapter;
import org.vikulin.etherwallet.adapter.pojo.EthplorerResponse;
import org.vikulin.etherwallet.adapter.pojo.Token;
import org.vikulin.etherwallet.adapter.pojo.TokenInfo;
import org.vikulin.etherwallet.listener.HandleExceptionListener;
import org.vikulin.etherwallet.task.CheckBalanceTask;
import org.vikulin.etherwallet.task.CheckContractCodeTask;
import org.vikulin.etherwallet.task.CheckTransactionFeeTask;
import org.xbill.DNS.TextParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;

import static org.web3j.utils.Numeric.prependHexPrefix;

/**
 * Created by vadym on 07.01.17.
 */

public class SendEtherActivity extends AddressListActivity {

    private TextView balanceText;
    private TextView addressFrom;
    private TextView token;
    private AutoCompleteTextView addressTo;
    private EditText gasValue;
    private CheckTransactionFeeTask transactionFeeTask;
    private int gasPrice;

    public EditText getGasValue(){
        return gasValue;
    }

    private EditText value;

    @Override
    public TextView getWalletListView() {
        return addressFrom;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_ethereum);
        addressFrom = (TextView) findViewById(R.id.address);
        token = (TextView) findViewById(R.id.token);
        addressTo = (AutoCompleteTextView) findViewById(R.id.addressTo);
        final ImageView idIcon = (ImageView) findViewById(R.id.idIcon);
        addressTo.addTextChangedListener(new TextWatcher(){

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(final Editable editable) {
                if(editable==null || editable.toString().length()==0){
                    idIcon.setImageDrawable(SendEtherActivity.this.getResources().getDrawable(R.drawable.icon_cycle));
                    return;
                }
                setIcon(editable.toString(), idIcon, 8);
                if(!editable.toString().startsWith("0x")){
                    addressTo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                    Thread threadAddress = new Thread(new Runnable() {
                        String address;
                        public void run() {
                            try {
                                String value = editable.toString();
                                if(value!=null) {
                                    String v = Domain.resolve(value).getResolved();
                                    //showMessageOnUiThread(v);
                                    if(v!=null) {
                                        address = v.replaceAll("\"", "");
                                        if(address.startsWith("0x")) {
                                            final ArrayAdapter<String> adapter = new ArrayAdapter<>(SendEtherActivity.this,
                                                    android.R.layout.simple_dropdown_item_1line, new String[]{address});
                                            SendEtherActivity.this.runOnUiThread(new Runnable() {
                                                public void run() {
                                                    addressTo.setAdapter(adapter);
                                                    addressTo.showDropDown();
                                                }
                                            });
                                        }
                                    }
                                }
                            } catch (TextParseException e) {
                                e.printStackTrace();
                            } catch (final UnknownHostException e) {
                                e.printStackTrace();
                                SendEtherActivity.this.runOnUiThread(new Runnable() {
                                    public void run() {
                                        addressTo.setError(e.getMessage());
                                    }
                                });
                            }
                        }
                    });
                    threadAddress.start();
                } else {
                    addressTo.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
                    String address = editable.toString();
                    if(address!=null) {
                        mContractCodeTask = new CheckContractCodeTask(SendEtherActivity.this, address, gasValue);
                        mContractCodeTask.execute((Void) null);
                    }
                }
            }
        });
        gasValue = (EditText) findViewById(R.id.gasLimit);
        value = (EditText) findViewById(R.id.value);
        Set<String> savedKeys = preferences.getStringSet("keys", new HashSet<String>());
        String[] addresses = new String[savedKeys.size()];
        for(int i=0; i<savedKeys.size();i++){
            try {
                addresses[i]=new JSONObject(String.valueOf(savedKeys.toArray()[i])).getString("address");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        String address = preferences.getString(getAddressProperty(), null);
        //final String address = "201dfe03703dd030f53236fde9cb93f0a60fc6a0";
        //getTokenInfo(address);

        if(address!=null) {
            int i = Arrays.asList(addresses).indexOf(address);
            if(i>=0){
                addressFrom.setText(address);
                setIcon(address);
                getTokenInfo(address);
            }
        } else {
            if(addresses.length==1){
                address=addresses[0];
                addressFrom.setText(address);
                setIcon(address);
                getTokenInfo(address);
            }
        }
        //Test
        //addressFrom.setText("201dfe03703dd030f53236fde9cb93f0a60fc6a0");
        //setIcon("201dfe03703dd030f53236fde9cb93f0a60fc6a0");
        //Test
        getPopupWindow(R.layout.spinner_address_item, addresses, addressFrom, getString(R.string.choose_address));
        getTokenPopupWindow(R.layout.spinner_address_item, new ArrayList<Token>(), token, "");

        token.addTextChangedListener(new TextWatcher(){

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                //TODO set balance and GAS_LIMIT
                if(editable.toString().equalsIgnoreCase("ETH")) {
                    checkBalance(addressFrom.getText().toString());
                    getGasValue().setText("21000");
                } else {
                    int tokenIndex = Integer.parseInt(token.getTag().toString());
                    setTokenBalance(balanceText, tokenIndex);
                    getGasValue().setText("120000");
                }
            }
        });

        balanceText = (TextView) findViewById(R.id.balance);
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
                getTokenInfo(editable.toString());
                token.setText("ETH");
                gasValue.setText("21000");
            }
        });

        SeekBar gasPrice = (SeekBar) findViewById(R.id.gasPrice);
        final TextView transactionFee = (TextView) findViewById(R.id.transactionFee);
        transactionFeeTask = new CheckTransactionFeeTask(this, gasPrice, transactionFee, gasValue);
        transactionFeeTask.execute((Void) null);

        gasValue.addTextChangedListener(new TextWatcher(){

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    Long.parseLong(editable.toString());
                } catch (NumberFormatException e){
                    gasValue.setError(getString(R.string.enter_value));
                    return;
                }
                transactionFeeTask.calculate();
            }
        });
        checkBalance(address);
    }

    private void getTokenInfo(final String address) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final EthplorerResponse ethplorerResponse = getTokenBalanceList(prependHexPrefix(address));
                    if(ethplorerResponse==null){
                        return;
                    }
                    final List<Token> tokens = ethplorerResponse.getTokens();
                    Collections.sort(tokens);
                    if(tokens!=null && tokens.size()>0) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Token eth = new Token();
                                eth.setBalance(ethplorerResponse.getEth().getBalance());
                                TokenInfo tokenInfo = new TokenInfo();
                                tokenInfo.setName("Ether");
                                tokenInfo.setSymbol("ETH");
                                eth.setTokenInfo(tokenInfo);
                                tokens.add(0, eth);
                                tokenAdapter.clear();
                                tokenAdapter.addAll(tokens);
                                tokenAdapter.notifyDataSetChanged();
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                tokenAdapter.clear();
                                tokenAdapter.notifyDataSetChanged();
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        });
        thread.start();
    }

    private void setTokenBalance(TextView balanceText, int tokenIndex) {
        Token token = tokenAdapter.getData().get(tokenIndex);
        String balance = REAL_FORMATTER.format(token.getBalance()/Math.pow(10.0d,token.getTokenInfo().getDecimals()));
        balanceText.setText(balance+" "+token.getTokenInfo().getSymbol());
    }

    private void checkBalance(String address){
        mBalanceTask = new CheckBalanceTask(this, address, balanceText, "ETH");
        mBalanceTask.execute((Void) null);
    }

    protected String getAddressProperty() {
        return DEFAULT_SELL_ADDRESS;
    }

    private CheckBalanceTask mBalanceTask = null;
    private CheckContractCodeTask mContractCodeTask = null;

    public void onClickScanQRCode(View view) {
        launchActivityForResult(CodeScannerActivity.class);
    }

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
        Set<String> savedKeys = preferences.getStringSet("keys", new HashSet<String>());
        boolean found = false;
        for(int i=0; i<savedKeys.size();i++){
            String addressFrom = new JSONObject(String.valueOf(savedKeys.toArray()[i])).getString("address");
            if (addressFrom.equalsIgnoreCase(getWalletListView().getText().toString())){
                found = true;
                Intent intent = new Intent(this, SendEthOnlineActivity.class);
                intent.putExtra(SendEthOnlineActivity.ADDRESS_TO, addressTo.getText().toString());
                intent.putExtra(SendEthOnlineActivity.ADDRESS_FROM, addressFrom);
                intent.putExtra(SendEthOnlineActivity.GAS_LIMIT, gasValue.getText().toString());
                intent.putExtra(SendEthOnlineActivity.GAS_PRICE, String.valueOf(gasPrice));

                intent.putExtra(SendEthOnlineActivity.KEY_CONTENT_FROM, String.valueOf(savedKeys.toArray()[i]));
                if(!token.getText().toString().equalsIgnoreCase("ETH")) {
                    int tokenIndex = Integer.parseInt(this.token.getTag().toString());
                    Token token = tokenAdapter.getData().get(tokenIndex);
                    intent.putExtra(SendEthOnlineActivity.TOKEN_CONTRACT, token.getTokenInfo().getAddress());
                    intent.putExtra(SendEthOnlineActivity.TOKEN_VALUE, new BigDecimal(BigInteger.TEN.pow(token.getTokenInfo().getDecimals().intValue())).multiply(BigDecimal.valueOf(total)).toBigInteger().toString(10));

                } else {
                    intent.putExtra(SendEthOnlineActivity.VALUE, total);
                }

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
        } catch (NumberFormatException e){
            value.setError(getString(R.string.enter_value));
            value.requestFocus();
            return false;
        }
        try {
            if (Double.valueOf(gasValue.getText().toString()) < 2100) {
                gasValue.setError(getString(R.string.enter_value));
                gasValue.requestFocus();
                return false;
            }
        } catch (NumberFormatException e){
            gasValue.setError(getString(R.string.enter_value));
            gasValue.requestFocus();
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,  String permissions[], int[] grantResults) {
        switch (requestCode) {
            case ZXING_CAMERA_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(mClss != null) {
                        Intent intent = new Intent(this, mClss);
                        startActivity(intent);
                    }
                } else {
                    Toast.makeText(this, "Please grant camera permission to use the QR Scanner", Toast.LENGTH_SHORT).show();
                }
                return;
            case ZXING_WEB_VIEW_RESULT_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(mClss != null) {
                        Intent intent = new Intent(this, mClss);
                        intent.putExtra(CodeScannerActivity.BARCODE_TYPE, CodeScannerActivity.QR_CODE);
                        startActivityForResult(intent, CodeScannerActivity.SCAN_ADDRESS);
                    }
                } else {
                    Toast.makeText(this, "Please grant camera permission to use the QR Scanner", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }

    public void launchActivityForResult(Class<?> clss) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            mClss = clss;
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.CAMERA }, ZXING_WEB_VIEW_RESULT_PERMISSION);
        } else {
            Intent intent = new Intent(this, clss);
            intent.putExtra(CodeScannerActivity.BARCODE_TYPE, CodeScannerActivity.QR_CODE);
            startActivityForResult(intent, CodeScannerActivity.SCAN_ADDRESS);
        }
    }

    private static final int ZXING_WEB_VIEW_RESULT_PERMISSION = 4;
    private static final int ZXING_CAMERA_PERMISSION = 3;
    private Class<?> mClss;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(requestCode == CodeScannerActivity.SCAN_ADDRESS && resultCode == RESULT_OK) {
            Bundle extras = intent.getExtras();
            String qrCodeData = (String) extras.get(CodeScannerActivity.QR_CODE_DATA);
            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
            EditText addressTo = (EditText) findViewById(R.id.addressTo);
            int q = qrCodeData.indexOf('?');
            qrCodeData = qrCodeData.toLowerCase();
            if(qrCodeData.startsWith("ethereum:") && q>10 && qrCodeData.contains("amount=")){
                String address = qrCodeData.substring(9,q);
                addressTo.setText(address);
                EditText value = (EditText) findViewById(R.id.value);
                int e = qrCodeData.indexOf("=")+1;
                String amount = qrCodeData.substring(e);
                try{
                    double a = Double.parseDouble(amount);
                    value.setText(String.valueOf(a));
                } catch (NumberFormatException e1){
                    showAlertDialog("",e1.getMessage());
                }
            } else
            if(qrCodeData.startsWith("ethereum:")){
                try {
                    String address = qrCodeData.substring(9, 51);
                    addressTo.setText(address);
                } catch (StringIndexOutOfBoundsException e){
                    addressTo.setError(getString(R.string.choose_address));
                    addressTo.requestFocus();
                }

            } else
            if(qrCodeData.startsWith("address:") && q>10 && qrCodeData.contains("amount=")){
                String address = qrCodeData.substring(8,q);
                addressTo.setText(address);
                EditText value = (EditText) findViewById(R.id.value);
                int e = qrCodeData.indexOf("=")+1;
                String amount = qrCodeData.substring(e);
                try{
                    double a = Double.parseDouble(amount);
                    value.setText(String.valueOf(a));
                } catch (NumberFormatException e1){
                    showAlertDialog("",e1.getMessage());
                }
            } else {
                addressTo.setText(qrCodeData);
            }
            return;
        }
        if(requestCode==SendEthOnlineActivity.SEND_ETHER && resultCode==RESULT_OK){
            checkBalance(addressFrom.getText().toString());
            return;
        }
    }


    public PopupWindow getTokenPopupWindow(int textViewResourceId, List<Token> objects, TextView editText, String hint) {
        // initialize a pop up window type
        PopupWindow popupWindow = new PopupWindow(this);
        // the drop down list is a list view
        ListView listView = new ListView(this);
        listView.setDividerHeight(0);
        View header = getLayoutInflater().inflate(R.layout.address_header, null);
        TextView headerTitle = (TextView)header.findViewById(R.id.headerTitle);
        headerTitle.setText(hint);
        listView.addHeaderView(header);
        // set our adapter and pass our pop up window contents
        tokenAdapter = new TokenAdapter(this, textViewResourceId, objects, popupWindow , editText);
        listView.setAdapter(tokenAdapter);
        // set the item click listener
        listView.setOnItemClickListener(tokenAdapter);
        // some other visual settings
        popupWindow.setFocusable(true);
        //popupWindow.setWidth(400);
        Display display = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        popupWindow.setWidth(display.getWidth()-30);
        popupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        // set the list view as pop up window content
        popupWindow.setContentView(listView);
        this.popupToken = popupWindow;
        return popupWindow;
    }

    public Adapter getTokenAdapter(){
        return tokenAdapter;
    }

    private TokenAdapter tokenAdapter;

    private PopupWindow popupToken;


    public void onClickTokenList(View v){
        int height = -1 * v.getHeight();
        getTokenListPopup().showAsDropDown(v, -2, height);
        //String value = getWalletListView().getText().toString();
        //if(height<0 && value!=null && !value.startsWith("0x")){
        //    resolveAddress(value);
        //}
    }

    public PopupWindow getTokenListPopup(){
        return popupToken;
    }

    public void setGasPrice(int gasPrice) {
        this.gasPrice = gasPrice;
    }
}
