package org.vikulin.etherchange;

import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import org.acra.ACRA;
import org.spongycastle.util.encoders.Hex;
import org.vikulin.etherchange.changelly.GenerateAddressPair;
import org.vikulin.etherchange.changelly.GenerateAddressRequest;
import org.vikulin.etherchange.changelly.GenerateAddressResponse;
import org.vikulin.etherchange.changelly.GetCurrenciesRequest;
import org.vikulin.etherchange.changelly.GetCurrenciesResponse;
import org.vikulin.etherchange.changelly.GetExchangeAmountPair;
import org.vikulin.etherchange.changelly.GetExchangeAmountRequest;
import org.vikulin.etherchange.changelly.GetExchangeAmountResponse;
import org.vikulin.etherchange.changelly.GetMinAmountRequest;
import org.vikulin.etherchange.changelly.GetMinAmountResponse;
import org.vikulin.etherchange.changelly.Pair;
import org.vikulin.etherchange.changer.Batch;
import org.vikulin.etherchange.changer.Exchange;
import org.vikulin.etherchange.changer.LimitPair;
import org.vikulin.etherchange.changer.Limits;
import org.vikulin.etherchange.changer.QueryString;
import org.vikulin.etherchange.changer.Rate;
import org.vikulin.etherwallet.AddressListActivity;
import org.vikulin.etherwallet.R;
import org.vikulin.etherwallet.listener.HandleExceptionListener;
import org.vikulin.etherwallet.listener.InterruptableTextWatcher;
import org.vikulin.etherwallet.view.UploadWebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.entity.ByteArrayEntity;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;
import cz.msebera.android.httpclient.message.BasicNameValuePair;

import static org.web3j.utils.Numeric.prependHexPrefix;

/**
 * Created by DDD on 25.03.2017.
 */

public abstract class ChangerActivity extends AddressListActivity {
    private static final String STATE_EXCHANGE = "exchange";
    private static final String STATE_ADDRESS_RESPONSE = "address_response";
    protected static final String STATE_FROM = "from";
    protected static final String STATE_TO = "to";

    protected Exchange exchange;
    protected Currency from;
    protected Currency to;
    protected GenerateAddressResponse addressResponse;

    abstract AlertDialog showRateDialog();

    abstract boolean validateAddress(TextView addressTo);

    private Rate rate;
    private Limits limits;
    private DecimalFormatSymbols ds;
    protected TextView addressEditText;
    //private SharedPreferences preferences;
    protected UploadWebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // landscape
            setContentView(R.layout.activity_changer_h);
        } else {
            // portrait
            setContentView(R.layout.activity_changer_w);
        }
        ds = new DecimalFormatSymbols(this.getResources().getConfiguration().locale);
        ds.setDecimalSeparator('.');
        //ds.setGroupingSeparator(',');
        //preferences = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
        mWebView = (UploadWebView) findViewById(R.id.exchangeWebview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        Object exchange = null;
        Object addressResponse = null;
        Object from = null;
        Object to = null;
        if(savedInstanceState!=null) {
            exchange = savedInstanceState.getSerializable(STATE_EXCHANGE);
            addressResponse = savedInstanceState.getSerializable(STATE_ADDRESS_RESPONSE);
            from = savedInstanceState.getSerializable(STATE_FROM);
            to = savedInstanceState.getSerializable(STATE_TO);
        }
        if(exchange!=null) {
            this.exchange = (Exchange) exchange;
            this.from = (Currency) from;
            this.to = (Currency) to;
            this.exchangeDialog = showExchangeDialog(this.exchange);
        } else
        if(addressResponse!=null) {
            this.addressResponse = (GenerateAddressResponse) addressResponse;
            this.from = (Currency) from;
            this.to = (Currency) to;
            this.exchangeDialog = showExchangeDialog(this.addressResponse.getSendAmount().toString(), this.addressResponse.getReceiveAmount().toString(), this.addressResponse.getReceiverId());
        }
    }

    @Override
    public TextView getWalletListView() {
        return addressEditText;
    }

    private static final String API_KEY_CHANGER = "c4b83ecb-ee7a-4e65-b52c-e354653314f1";
    private static final String API_SECURE_CHANGER = "94b700dd4c5927686ab49190ba9ede049ff6dd64c7e6aebe9a206e28324e9cdb";
    public static final String REFID_CHANGER = "101857";
    private static final String API_KEY_CHANGELLY = "2d61e32121c840f296ca8bc6bbd6cef3";
    private static final String API_SECURE_CHANGELLY = "53c56802fd8102b661ab59007e830d2f93b1cc6fc7b6a94429d27c659dd667b7";

    private Rate getRateChanger(Currency from, Currency to) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet getRequest = new HttpGet("https://www.changer.com/api/v2/rates/"+from.name()+"/"+to.name());
        getRequest.addHeader("accept", "application/json");
        HttpResponse response = httpClient.execute(getRequest);
        final int status = response.getStatusLine().getStatusCode();
        final String content = read(new BufferedReader(new InputStreamReader((response.getEntity().getContent()))));
        httpClient.close();
        if (status != 200) {
            this.runOnUiThread(new Runnable() {
                public void run() {
                    showAlertDialog("", "Changer error:" + status + " \n" + content, new HandleExceptionListener(ChangerActivity.this, "Changer error:" + status + " \n" + content));
                }});
            return null;
        }
        Gson gson = new GsonBuilder().create();
        Rate rate = null;
        try {
            rate = gson.fromJson(content, Rate.class);
        } catch (JsonSyntaxException e){
            ACRA.getErrorReporter().handleSilentException(new Exception("Changer error:\n" + content));
        }
        return rate;
    }

    private GetExchangeAmountResponse getExchangeAmount(Currency from, Currency to, String amount) throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost postRequest = new HttpPost("https://api.changelly.com/");
        postRequest.addHeader("Content-Type", "application/json");
        GetExchangeAmountPair pair = new GetExchangeAmountPair(from.name(), to.name(), amount);
        GetExchangeAmountRequest exchangeAmount = new GetExchangeAmountRequest(new Date().getTime(), pair);
        Gson gson = new GsonBuilder().create();
        final String value = gson.toJson(exchangeAmount, GetExchangeAmountRequest.class);
        final String sign = hmacSha512(value, API_SECURE_CHANGELLY);
        postRequest.addHeader("api-key", API_KEY_CHANGELLY);
        postRequest.addHeader("sign", sign);
        HttpEntity body = new ByteArrayEntity(value.getBytes("UTF-8"));
        postRequest.setEntity(body);
        HttpResponse response = httpClient.execute(postRequest);
        final int status = response.getStatusLine().getStatusCode();
        final String content = read(new BufferedReader(new InputStreamReader((response.getEntity().getContent()))));
        httpClient.close();
        if (status != 200) {
            this.runOnUiThread(new Runnable() {
                public void run() {
                    showAlertDialog("", "Changelly error:" + status + " \n" + content+"\n request body:"+value+"\n sign:"+sign, new HandleExceptionListener(ChangerActivity.this, "Changelly error:" + status + " \n" + content+"\n request body:"+value+"\n sign:"+sign));
                }});
            return null;
        }
        GetExchangeAmountResponse exchangeAmountResponse = null;
        try {
            exchangeAmountResponse = gson.fromJson(content, GetExchangeAmountResponse.class);
        } catch (JsonSyntaxException e){
            ACRA.getErrorReporter().handleSilentException(new Exception("Changelly error:\n" + content));
        }
        if(exchangeAmountResponse.getError()!=null){
            final GetExchangeAmountResponse finalExchangeAmountResponse = exchangeAmountResponse;
            this.runOnUiThread(new Runnable() {
                public void run() {
                    showAlertDialog("", "Changelly error: code=" + finalExchangeAmountResponse.getError().getCode() + "\n message:"+finalExchangeAmountResponse.getError().getMessage(), new HandleExceptionListener(ChangerActivity.this, "Changelly error: code=" + finalExchangeAmountResponse.getError().getCode() + "\n message:"+finalExchangeAmountResponse.getError().getMessage()));
                }});
            return null;
        }
        return exchangeAmountResponse;
    }

    protected GetCurrenciesResponse getCurrencies() throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost postRequest = new HttpPost("https://api.changelly.com/");
        postRequest.addHeader("Content-Type", "application/json");
        GetCurrenciesRequest getCurrencies = new GetCurrenciesRequest(1l, "getCurrencies");
        Gson gson = new GsonBuilder().create();
        final String value = gson.toJson(getCurrencies, GetCurrenciesRequest.class);
        final String sign = hmacSha512(value, API_SECURE_CHANGELLY);
        postRequest.addHeader("api-key", API_KEY_CHANGELLY);
        postRequest.addHeader("sign", sign);
        HttpEntity body = new ByteArrayEntity(value.getBytes("UTF-8"));
        postRequest.setEntity(body);
        HttpResponse response = httpClient.execute(postRequest);
        final int status = response.getStatusLine().getStatusCode();
        final String content = read(new BufferedReader(new InputStreamReader((response.getEntity().getContent()))));
        httpClient.close();
        if (status != 200) {
            this.runOnUiThread(new Runnable() {
                public void run() {
                    showAlertDialog("", "Changelly error:" + status + " \n" + content+"\n request body:"+value+"\n sign:"+sign, new HandleExceptionListener(ChangerActivity.this, "Changelly error:" + status + " \n" + content+"\n request body:"+value+"\n sign:"+sign));
                }});
            return null;
        }
        GetCurrenciesResponse currenciesResponse = null;
        try {
            currenciesResponse = gson.fromJson(content, GetCurrenciesResponse.class);
        } catch (JsonSyntaxException e){
            ACRA.getErrorReporter().handleSilentException(new Exception("Changelly error:\n" + content));
        }
        if(currenciesResponse.getError()!=null){
            final GetCurrenciesResponse finalCurrenciesResponse = currenciesResponse;
            this.runOnUiThread(new Runnable() {
                public void run() {
                    showAlertDialog("", "Changelly error: code=" + finalCurrenciesResponse.getError().getCode() + "\n message:"+finalCurrenciesResponse.getError().getMessage(), new HandleExceptionListener(ChangerActivity.this, "Changelly error: code=" + finalCurrenciesResponse.getError().getCode() + "\n message:"+finalCurrenciesResponse.getError().getMessage()));
                }});
            return null;
        }
        return currenciesResponse;
    }

    protected GenerateAddressResponse generateAddress(Currency from, Currency to, String address) throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost postRequest = new HttpPost("https://api.changelly.com/");
        postRequest.addHeader("Content-Type", "application/json");
        GenerateAddressPair pair = new GenerateAddressPair(from.name(), to.name(), address);
        GenerateAddressRequest addressRequest = new GenerateAddressRequest(new Date().getTime(), pair);
        Gson gson = new GsonBuilder().create();
        final String value = gson.toJson(addressRequest, GenerateAddressRequest.class);
        final String sign = hmacSha512(value, API_SECURE_CHANGELLY);
        postRequest.addHeader("api-key", API_KEY_CHANGELLY);
        postRequest.addHeader("sign", sign);
        HttpEntity body = new ByteArrayEntity(value.getBytes("UTF-8"));
        postRequest.setEntity(body);
        HttpResponse response = httpClient.execute(postRequest);
        final int status = response.getStatusLine().getStatusCode();
        final String content = read(new BufferedReader(new InputStreamReader((response.getEntity().getContent()))));
        httpClient.close();
        if (status != 200) {
            this.runOnUiThread(new Runnable() {
                public void run() {
                    showAlertDialog("", "Changelly error:" + status + " \n" + content+"\n request body:"+value+"\n sign:"+sign, new HandleExceptionListener(ChangerActivity.this, "Changelly error:" + status + " \n" + content+"\n request body:"+value+"\n sign:"+sign));
                }});
            return null;
        }
        GenerateAddressResponse addressResponse = null;
        try {
            addressResponse = gson.fromJson(content, GenerateAddressResponse.class);
        } catch (JsonSyntaxException e){
            ACRA.getErrorReporter().handleSilentException(new Exception("Changelly error:\n" + content));
        }
        if(addressResponse.getError()!=null){
            final GenerateAddressResponse finalAddressResponse = addressResponse;
            this.runOnUiThread(new Runnable() {
                public void run() {
                    showAlertDialog("", "Changelly error: code=" + finalAddressResponse.getError().getCode() + "\n message:"+finalAddressResponse.getError().getMessage(), new HandleExceptionListener(ChangerActivity.this, "Changelly error: code=" + finalAddressResponse.getError().getCode() + "\n message:"+finalAddressResponse.getError().getMessage()));
                }});
            return null;
        }
        return addressResponse;
    }

    private Limits getLimitsChanger(Currency from, Currency to) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet getRequest = new HttpGet("https://www.changer.com/api/v2/limits/"+from.name()+"/"+to.name());
        getRequest.addHeader("accept", "application/json");
        HttpResponse response = httpClient.execute(getRequest);
        final int status = response.getStatusLine().getStatusCode();
        final String content = read(new BufferedReader(new InputStreamReader((response.getEntity().getContent()))));
        httpClient.close();
        if (response.getStatusLine().getStatusCode() != 200) {
            this.runOnUiThread(new Runnable() {
                public void run() {
                    showAlertDialog("", "Changer error:" + status + " \n" + content, new HandleExceptionListener(ChangerActivity.this, "Changer error:" + status + " \n" + content));
                }});
            return null;
        }
        Gson gson = new GsonBuilder().create();
        Limits limits = null;
        try {
            limits = gson.fromJson(content, Limits.class);
        } catch (JsonSyntaxException e){
            ACRA.getErrorReporter().handleSilentException(new Exception("Changer error:\n" + content));
        }
        return limits;
    }

    private GetMinAmountResponse getMinAmount(Currency from, Currency to) throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost postRequest = new HttpPost("https://api.changelly.com/");
        postRequest.addHeader("Content-Type", "application/json");
        Pair pair = new Pair(from.name(), to.name());
        GetMinAmountRequest minAmount = new GetMinAmountRequest(new Date().getTime(), pair);
        Gson gson = new GsonBuilder().create();
        final String value = gson.toJson(minAmount, GetMinAmountRequest.class);
        final String sign = hmacSha512(value, API_SECURE_CHANGELLY);
        postRequest.addHeader("api-key", API_KEY_CHANGELLY);
        postRequest.addHeader("sign", sign);
        HttpEntity body = new ByteArrayEntity(value.getBytes("UTF-8"));
        postRequest.setEntity(body);
        HttpResponse response = httpClient.execute(postRequest);
        final int status = response.getStatusLine().getStatusCode();
        final String content = read(new BufferedReader(new InputStreamReader((response.getEntity().getContent()))));
        httpClient.close();
        if (status != 200) {
            this.runOnUiThread(new Runnable() {
                public void run() {
                    showAlertDialog("", "Changelly error:" + status + " \n" + content+"\n request body:"+value+"\n sign:"+sign, new HandleExceptionListener(ChangerActivity.this, "Changelly error:" + status + " \n" + content+"\n request body:"+value+"\n sign:"+sign));
                }});
            return null;
        }
        GetMinAmountResponse minAmountResponse = null;
        try {
            minAmountResponse = gson.fromJson(content, GetMinAmountResponse.class);
        } catch (JsonSyntaxException e){
            ACRA.getErrorReporter().handleSilentException(new Exception("Changelly error:\n" + content));
        }
        if(minAmountResponse.getError()!=null){
            final GetMinAmountResponse finalMinAmountResponse = minAmountResponse;
            this.runOnUiThread(new Runnable() {
                public void run() {
                    showAlertDialog("", "Changelly error: code=" + finalMinAmountResponse.getError().getCode() + "\n message:"+finalMinAmountResponse.getError().getMessage()+"\n body:"+value,
                            new HandleExceptionListener(ChangerActivity.this, "Changelly error: code=" + finalMinAmountResponse.getError().getCode() + "\n message:"+finalMinAmountResponse.getError().getMessage()+"\n body:"+value));
                }});
            return null;
        }
        return minAmountResponse;
    }

    private static String hmacSha256(String value, String key) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        String type = "HmacSHA256";
        SecretKeySpec secret = new SecretKeySpec(key.getBytes(), type);
        Mac mac = Mac.getInstance(type);
        mac.init(secret);
        byte[] bytes = mac.doFinal(value.getBytes());
        return Hex.toHexString(bytes);
    }

    private static String hmacSha512(String value, String key) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        String type = "HmacSHA512";
        SecretKeySpec secret = new SecretKeySpec(key.getBytes(), type);
        Mac mac = Mac.getInstance(type);
        mac.init(secret);
        byte[] bytes = mac.doFinal(value.getBytes("UTF-8"));
        return Hex.toHexString(bytes);
    }

    private static String httpBuildQuery(List<? extends NameValuePair> parameters) {
        NameValuePair firstPair = parameters.remove(0);
        QueryString qs = new QueryString(firstPair.getName(), firstPair.getValue());
        for (NameValuePair pair : parameters) {
            qs.add(pair.getName(), pair.getValue());
        }
        return qs.toString();
    }

    private static HttpPost getAuthRequestChanger(String method, List<? extends NameValuePair> parameters) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        HttpPost request = new HttpPost("https://www.changer.com"+method);
        request.setEntity(new UrlEncodedFormEntity(parameters));
        String postData = httpBuildQuery(parameters);
        String timestamp = Long.toString(System.currentTimeMillis()/1000);
        String API_SIGN = hmacSha256(method+":"+postData+":"+timestamp, API_SECURE_CHANGER);
        request.addHeader("accept", "application/json");
        request.addHeader("Api-Key", API_KEY_CHANGER);
        request.addHeader("Api-Sign", API_SIGN);
        request.addHeader("Api-Timestamp", timestamp);
        return request;
    }

    protected Exchange makeExchangeChanger(String refid, Currency from, Currency to, double amount, String receiver_id) throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("refid", refid));
        params.add(new BasicNameValuePair("send", from.name().toString()));
        params.add(new BasicNameValuePair("receive", to.name().toString()));
        params.add(new BasicNameValuePair("amount", Double.toString(amount)));
        params.add(new BasicNameValuePair("receiver_id", receiver_id));
        params.add(new BasicNameValuePair("html", "1"));
        HttpPost request = getAuthRequestChanger("/api/v2/exchange", params);
        HttpResponse response = httpClient.execute(request);
        final int status = response.getStatusLine().getStatusCode();
        final BufferedReader br = new BufferedReader(
                new InputStreamReader((response.getEntity().getContent())));
        String message = "";
        for (String line; (line = br.readLine()) != null; message += line);
        if (response.getStatusLine().getStatusCode() != 200) {
            showAlertDialogOnUiThread("", "Changer error:" + status + " \n" + message, new HandleExceptionListener(ChangerActivity.this, "Changer error:" + status + " \n" + message));
            br.close();
            return null;
        }
        Gson gson = new GsonBuilder().create();
        Exchange exchange = gson.fromJson(message, Exchange.class);
        br.close();
        return exchange;
    }

    protected AlertDialog showExchangeDialog(Exchange exchange){
        View view = LayoutInflater.from(this).inflate(R.layout.currency_list_layout,null);
        TextView from = (TextView)view.findViewById(R.id.from);
        TextView to = (TextView)view.findViewById(R.id.to);
        TextView address = (TextView)view.findViewById(R.id.address);
        from.setText(exchange.getSendAmount().toString());
        to.setText(exchange.getReceiveAmount().toString());
        address.setText(exchange.getReceiverId());
        ImageView fromIcon = (ImageView)view.findViewById(R.id.imageViewFrom);
        fromIcon.setImageDrawable(getResources().getDrawable(getCurrencyIconChanger(this.from)));
        ImageView toIcon = (ImageView)view.findViewById(R.id.imageViewTo);
        toIcon.setImageDrawable(getResources().getDrawable(getCurrencyIconChanger(this.to)));
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setView(view);
        return ab.show();
    }

    protected AlertDialog showExchangeDialog(String sendAmount, String receiveAmount, String receiverId) {
        View view = LayoutInflater.from(this).inflate(R.layout.currency_list_layout,null);
        TextView from = (TextView)view.findViewById(R.id.from);
        TextView to = (TextView)view.findViewById(R.id.to);
        TextView address = (TextView)view.findViewById(R.id.address);
        from.setText(sendAmount);
        to.setText(receiveAmount);
        address.setText(receiverId);
        ImageView fromIcon = (ImageView)view.findViewById(R.id.imageViewFrom);
        fromIcon.setImageDrawable(getResources().getDrawable(getCurrencyIconChanger(this.from)));
        ImageView toIcon = (ImageView)view.findViewById(R.id.imageViewTo);
        toIcon.setImageDrawable(getResources().getDrawable(getCurrencyIconChanger(this.to)));
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setView(view);
        return ab.show();
    }

    protected int getCurrencyIconChanger(Currency from){
        if(from != null) {
            switch (from) {
                //Changer icons
                case pm_USD:
                    return R.drawable.pm;
                //case okpay_USD:
                //    return R.drawable.okpay;
                //case payeer_USD:
                //    return R.drawable.payeer;
                case advcash_USD:
                    return R.drawable.advcash;
                case bitcoin_BTC:
                    return R.drawable.btc;
                case ethereum_ETH:
                    return R.drawable.ether;
                case dash_DASH:
                    return R.drawable.dash;
                case ethereumclassic_ETC:
                    return R.drawable.etc;
                case zcash_ZEC:
                    return R.drawable.zcash;
                case litecoin_LTC:
                    return R.drawable.litecoin;
                case augur_REP:
                    return R.drawable.augur;

                //Changelly icons
                case btc:
                    return R.drawable.btc;
                case bch:
                    return R.drawable.bch;
                case eth:
                    return R.drawable.ether;
                case dash:
                    return R.drawable.dash;
                case etc:
                    return R.drawable.etc;
                case zec:
                    return R.drawable.zcash;
                case ltc:
                    return R.drawable.litecoin;
                case rep:
                    return R.drawable.augur;
                case gnt:
                    return R.drawable.golem;
                default:
                    return R.drawable.lock;
            }
        }
        return R.drawable.lock;
    }

    protected String getCurrencyNameChanger(Currency from){
        switch (from) {
            //Changer currencies
            case pm_USD: return "Perfect Money";
            //case okpay_USD: return "OKPAY";
            //case payeer_USD: return "Payeer";
            case advcash_USD: return "Advcash";
            //case btce_USD: return "BTCE";
            case bitcoin_BTC: return "Bitcoin";
            case bitcoincash_BCH: return "Bitcoin Cash";
            case ethereum_ETH: return "Ethereum";
            case dash_DASH: return "Dash";
            case ethereumclassic_ETC: return "Ether Classic";
            case zcash_ZEC: return "Zcash";
            case litecoin_LTC: return "Litecoin";
            case augur_REP: return "Augur";

            //Changelly currencies
            case btc: return "Bitcoin";
            case bch: return "Bitcoin Cash";
            case eth: return "Ethereum";
            case dash: return "Dash";
            case etc: return "Ether Classic";
            case zec: return "Zcash";
            case ltc: return "Litecoin";
            case rep: return "Augur";
            case gnt: return "Golem";
            default: return "";
        }
    }

    protected Exchanger getExchanger(Currency from, Currency to){
        switch (from) {
            //case okpay_USD: return Exchanger.Changer;
            case advcash_USD: return Exchanger.Changer;
            //case btce_USD: return Exchanger.Changer;
            case pm_USD: return Exchanger.Changer;
        }
        switch (to) {
            //case okpay_USD: return Exchanger.Changer;
            case advcash_USD: return Exchanger.Changer;
            //case btce_USD: return Exchanger.Changer;
            case pm_USD: return Exchanger.Changer;
        }
        return Exchanger.Changelly;
    }

    protected Exchanger getExchanger(Currency currency){
        switch (currency) {
            //case okpay_USD: return Exchanger.Changer;
            case advcash_USD: return Exchanger.Changer;
            //case btce_USD: return Exchanger.Changer;
            case pm_USD: return Exchanger.Changer;
        }
        return Exchanger.Changelly;
    }

    protected volatile AlertDialog rateDialog;
    protected volatile AlertDialog exchangeDialog;
    protected volatile AlertDialog btceDialog;
    protected volatile AlertDialog sendToDialog;
    private Rate usdRate;

    protected void showRateChanger(){
        Exchanger exchanger = getExchanger(from, to);
        final Timer timer = new Timer();
        if(rateDialog!=null && rateDialog.isShowing()){
            rateDialog.dismiss();
        }
        rateDialog = showRateDialog();
        rateDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                timer.cancel();
            }
        });
        rateDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                timer.cancel();
            }
        });
        final EditText fromField = (EditText) rateDialog.getWindow().findViewById(R.id.from);
        final EditText toField = (EditText) rateDialog.getWindow().findViewById(R.id.to);
        final InterruptableTextWatcher twFrom = new InterruptableTextWatcher() {
            @Override
            public void run(Editable s) {
                refreshRateDialogTo(rate, s);
            }
        };
        final InterruptableTextWatcher twTo = new InterruptableTextWatcher() {
            @Override
            public void run(Editable s) {
                refreshRateDialogFrom(rate, s);
            }
        };
        twFrom.setInterruptableTextWatcher(twTo);
        twTo.setInterruptableTextWatcher(twFrom);
        fromField.addTextChangedListener(twFrom);
        toField.addTextChangedListener(twTo);

        if(exchanger.equals(Exchanger.Changer)) {
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        rate = getRateChanger(from, to);
                        usdRate = getRateChanger(from, Currency.advcash_USD);

                        limits = getLimitsChanger(from, to);
                        ChangerActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                refreshRateDialog(rate, twFrom, twTo);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                        showMessageOnUiThread(e.getMessage());
                    }
                }
            };
            timer.schedule(timerTask, 0, 10000);
        } else {
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        Double amountFrom = 1.0;
                        GetExchangeAmountResponse response = getExchangeAmount(from, to, amountFrom.toString());
                        if(response==null){
                            return;
                        }
                        Double amountTo = Double.parseDouble(response.getResult());

                        rate = new Rate(amountTo/amountFrom);
                        GetMinAmountResponse minAmountResponse = getMinAmount(from, to);
                        if(minAmountResponse==null){
                            return;
                        }

                        Double minAmount = Double.parseDouble(minAmountResponse.getResult());
                        limits = new Limits(new LimitPair(minAmount, 1000000000000.0));
                        ChangerActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                refreshRateDialog(rate, twFrom, twTo);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                        showMessageOnUiThread(e.getMessage());
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                        showMessageOnUiThread(e.getMessage());
                    } catch (InvalidKeyException e) {
                        e.printStackTrace();
                        showMessageOnUiThread(e.getMessage());
                    }
                }
            };
            timer.schedule(timerTask, 0, 10000);
        }
        
        modifyAddress();
    }

    protected abstract void modifyAddress();

    private void refreshRateDialogFrom(Rate rate, Editable to) {
        EditText from = (EditText) rateDialog.getWindow().findViewById(R.id.from);
        if(to!=null && to.length()>0 && rate!=null){
            DecimalFormat REAL_FORMATTER = new DecimalFormat(Const.AdvCash.DECIMAL_FORMAT, ds);
            String fromValue = REAL_FORMATTER.format(Double.parseDouble(to.toString())/rate.getRate());
            from.setText(fromValue);
        } else {
            from.setText(null);
        }
    }

    private void refreshRateDialogTo(Rate rate, Editable from) {
        EditText to = (EditText) rateDialog.getWindow().findViewById(R.id.to);
        if(from!=null && from.length()>0 && rate!=null){
            String toValue = String.valueOf(Double.parseDouble(from.toString())*rate.getRate());
            to.setText(toValue);
            to.setSelection(0);
        } else {
            to.setText(null);
        }
    }

    private void refreshRateDialog(Rate rate, InterruptableTextWatcher twFrom, InterruptableTextWatcher twTo) {
        EditText from = (EditText) rateDialog.getWindow().findViewById(R.id.from);
        EditText to = (EditText) rateDialog.getWindow().findViewById(R.id.to);
        DecimalFormat REAL_FORMATTER = new DecimalFormat(Const.AdvCash.DECIMAL_FORMAT, ds);
        if(rate!=null) {
            if (to.getText() != null && to.getText().length() > 0) {
                String fromValue = REAL_FORMATTER.format(Double.parseDouble(to.getText().toString()) / rate.getRate());
                twFrom.stop();
                from.setText(fromValue);
                twFrom.resume();
            } else {
                if (from.getText() != null && from.getText().length() > 0) {
                    String toValue = String.valueOf(Double.parseDouble(from.getText().toString()) * rate.getRate());
                    twTo.stop();
                    to.setText(toValue);
                    to.setSelection(0);
                    twTo.resume();
                }
            }
        }
    }

    protected String getAddressProperty() {
        return DEFAULT_BUY_ADDRESS;
    }

    protected boolean validateRedeemCode(TextView redeemCode) {
        if (redeemCode.getText() == null || redeemCode.getText().toString().trim().length() ==0) {
            redeemCode.setError(getString(R.string.enter_value));
            redeemCode.requestFocus();
            return false;
        }
        return true;
    }

    protected boolean validateChangerLimits(EditText from, EditText to) {
        if (from.getText() == null || from.getText().toString().trim().length() == 0) {
            from.setError(getString(R.string.enter_value));
            from.requestFocus();
            return false;
        }
        try {
            Double send = Double.parseDouble(from.getText().toString());
            //if (limits==null){
            //    from.setError(getString(R.string.in_progress));
            //    return false;
            //}
            //if (!(send*usdRate.getRate()>limits.getLimits().getMinAmount() && send*usdRate.getRate()<limits.getLimits().getMaxAmount())) {
            //    from.setError(getString(R.string.enter_value));
            //    from.requestFocus();
            //    from.setTextColor(Color.RED);
            //    return false;
            //}
        } catch (NumberFormatException e){
            from.setError(getString(R.string.enter_value));
            from.requestFocus();
            from.setTextColor(Color.RED);
            return false;
        }
        from.setTextColor(getResources().getColor(R.color.primary_text));
        try {
            Double receive = Double.parseDouble(to.getText().toString());
        } catch (NumberFormatException e){
            to.setError(getString(R.string.enter_value));
            to.requestFocus();
            to.setTextColor(Color.RED);
            return false;
        }
        to.setTextColor(getResources().getColor(R.color.primary_text));
        return true;
    }

    protected boolean validateChangellyLimits(EditText from, EditText to) {
        if (from.getText() == null || from.getText().toString().trim().length() == 0) {
            from.setError(getString(R.string.enter_value));
            from.requestFocus();
            return false;
        }
        try {
            Double send = Double.parseDouble(from.getText().toString());
            if (limits==null){
                from.setError(getString(R.string.in_progress));
                return false;
            }
            if (send<limits.getLimits().getMinAmount() || send>limits.getLimits().getMaxAmount()) {
                from.setError(getString(R.string.enter_value));
                from.requestFocus();
                from.setTextColor(Color.RED);
                return false;
            }
        } catch (NumberFormatException e){
            from.setError(getString(R.string.enter_value));
            from.requestFocus();
            from.setTextColor(Color.RED);
            return false;
        }
        from.setTextColor(getResources().getColor(R.color.primary_text));
        try {
            Double receive = Double.parseDouble(to.getText().toString());
        } catch (NumberFormatException e){
            to.setError(getString(R.string.enter_value));
            to.requestFocus();
            to.setTextColor(Color.RED);
            return false;
        }
        to.setTextColor(getResources().getColor(R.color.primary_text));
        return true;
    }

    @Override
    public void onBackPressed() {
        if (findViewById(R.id.exchangeLayout).getVisibility() == View.VISIBLE) {
            findViewById(R.id.exchangeLayout).setVisibility(View.GONE);
            findViewById(R.id.changer_menu).setVisibility(View.VISIBLE);
            mWebView.loadUrl("about:blank");
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        // Make sure to call the super method so that the states of our views are saved
        super.onSaveInstanceState(savedInstanceState);
        // Save our own state now
        savedInstanceState.putSerializable(STATE_FROM, from);
        savedInstanceState.putSerializable(STATE_TO, to);
        if(exchangeDialog!=null && exchangeDialog.isShowing()) {
            savedInstanceState.putSerializable(STATE_EXCHANGE, exchange);
            savedInstanceState.putSerializable(STATE_ADDRESS_RESPONSE, addressResponse);
        }
    }

    protected Batch completePayment(String exchangeId, String transactionId) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost("https://www.changer.com/api/v2/exchange/"+exchangeId);
        request.addHeader("accept", "application/json");
        List<NameValuePair> parameters = new ArrayList<>();
        parameters.add(new BasicNameValuePair("batch", transactionId));
        request.setEntity(new UrlEncodedFormEntity(parameters));
        HttpResponse response = httpClient.execute(request);
        final int status = response.getStatusLine().getStatusCode();
        final BufferedReader br = new BufferedReader(
                new InputStreamReader((response.getEntity().getContent())));
        if (response.getStatusLine().getStatusCode() != 200) {
            this.runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        showAlertDialog("", "Changer error:" + status + " \n" + br.readLine(), new HandleExceptionListener(ChangerActivity.this, "Changer error:" + status + " \n" + br.readLine()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }});
            br.close();
            return null;
        } else {
            this.runOnUiThread(new Runnable() {
                public void run() {
                    findViewById(R.id.exchangeLayout).setVisibility(View.GONE);
                    findViewById(R.id.changer_menu).setVisibility(View.VISIBLE);
                    showAlertDialog("", getString(R.string.payment_successful));
                }});
        }
        Gson gson = new GsonBuilder().create();
        Batch batch = gson.fromJson(br, Batch.class);
        br.close();
        return batch;
    }

    public void onClickExchange(View view) {
        Exchanger exchanger = getExchanger(from, to);
        final EditText fromAmount = (EditText)view.getRootView().findViewById(R.id.from);
        final EditText toAmount = (EditText)view.getRootView().findViewById(R.id.to);
        final TextView destinationAddress = (TextView)view.getRootView().findViewById(R.id.address);
        if(!validateAddress(destinationAddress)){
            return;
        }
        if(exchanger.equals(Exchanger.Changer)) {
            if (!validateChangerLimits(fromAmount, toAmount)) {
                return;
            }
        } else {
            if (!validateChangellyLimits(fromAmount, toAmount)) {
                return;
            }
        }
        final String address;
        if(to.equals(Currency.eth) || to.equals(Currency.etc) || to.equals(Currency.ethereum_ETH)) {
            address = prependHexPrefix(destinationAddress.getText().toString());
        } else {
            address = destinationAddress.getText().toString();
        }

        final double sendAmount = Double.parseDouble(fromAmount.getText().toString());
        final double receiveAmount = Double.parseDouble(toAmount.getText().toString());

        rateDialog.dismiss();

        if(exchanger.equals(Exchanger.Changer)) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final Exchange exchange = makeExchangeChanger(REFID_CHANGER, from, to, sendAmount, address);
                        if (exchange == null) {
                            return;
                        }
                        ChangerActivity.this.exchange = exchange;
                        ChangerActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                exchangeDialog = showExchangeDialog(exchange);
                                //showInfoDialogOnUiThread("", exchange);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InvalidKeyException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        } else {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final GenerateAddressResponse addressResponse = generateAddress(from, to, address);
                        if (addressResponse == null) {
                            return;
                        }
                        addressResponse.setSendAmount(sendAmount);
                        addressResponse.setReceiveAmount(receiveAmount);
                        addressResponse.setReceiverId(address);
                        ChangerActivity.this.addressResponse = addressResponse;
                        ChangerActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                exchangeDialog = showExchangeDialog(fromAmount.getText().toString(), toAmount.getText().toString(), address);
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InvalidKeyException e) {
                        e.printStackTrace();
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
    }
}
