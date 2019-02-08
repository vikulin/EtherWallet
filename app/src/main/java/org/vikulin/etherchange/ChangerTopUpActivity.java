package org.vikulin.etherchange;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.WriterException;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.vikulin.etherchange.changelly.GenerateAddressResponse;
import org.vikulin.etherchange.changer.Exchange;
import org.vikulin.etherchange.changer.QueryString;
import org.vikulin.etherwallet.R;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.utils.URLEncodedUtils;
import cz.msebera.android.httpclient.util.EncodingUtils;

import static org.vikulin.etherwallet.ShowCodeActivity.encodeAsBitmap;
import static org.vikulin.etherwallet.ShowCodeActivity.getResizedBitmap;

/**
 * Created by vadym on 11.04.17.
 */

public class ChangerTopUpActivity extends ChangerActivity {

    private static final String STATE_SEND_TO_CHANGER = "send_to_changer";
    private static final String STATE_SEND_TO_CHANGELLY = "send_to_changelly";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        to = Currency.eth;
        super.onCreate(savedInstanceState);
        if(savedInstanceState!=null) {
            from = (Currency) savedInstanceState.getSerializable(STATE_FROM);
            to = (Currency) savedInstanceState.getSerializable(STATE_TO);
            if(from==null){
                showAlertDialog("","Currency from is null!");
                return;
            }
            Exchanger exchanger = getExchanger(from);
            if(exchanger.equals(Exchanger.Changer)){
                to = Currency.ethereum_ETH;
                Object exchange = savedInstanceState.getSerializable(STATE_SEND_TO_CHANGER);
                if(exchange!=null) {
                    this.exchange = (Exchange) exchange;
                    this.sendToDialog = showSendToDialog(this.exchange);
                }
            } else
            if(exchanger.equals(Exchanger.Changelly)){
                to = Currency.eth;
                Object addressResponse = savedInstanceState.getSerializable(STATE_SEND_TO_CHANGELLY);
                if(addressResponse!=null) {
                    this.addressResponse = (GenerateAddressResponse) addressResponse;
                    this.sendToDialog = showSendToDialog(this.addressResponse);
                }
            }
        }
    }

    @Override
    protected void modifyAddress() {
        Set<String> savedKeys = preferences.getStringSet("keys", new HashSet<String>());
        String[] addresses = new String[savedKeys.size()];
        for(int i=0; i<savedKeys.size();i++){
            try {
                addresses[i]=new JSONObject(String.valueOf(savedKeys.toArray()[i])).getString("address");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        addressEditText = (TextView)rateDialog.getWindow().findViewById(R.id.address);
        String address = preferences.getString(getAddressProperty(), null);
        if(address!=null) {
            int i = Arrays.asList(addresses).indexOf(address);
            if(i>=0){
                addressEditText.setText(address);
                setIcon(address);
            }
        } else {
            if(addresses.length==1){
                addressEditText.setText(addresses[0]);
                setIcon(addresses[0]);
            }
        }
        getPopupWindow(R.layout.spinner_address_item, addresses, addressEditText, getString(R.string.choose_address));
    }


    public void onClick(View view) {
        from = Currency.valueOf(view.getTag().toString());
        Exchanger exchanger = getExchanger(from, to);
        switch(exchanger){
            case Changer: to = Currency.ethereum_ETH; break;
            case Changelly: to = Currency.eth; break;
        }
        showRateChanger();
    }

    protected AlertDialog showRateDialog(){
        View view = LayoutInflater.from(this).inflate(R.layout.top_up_rate_layout,null);
        ImageView fromIcon = (ImageView)view.findViewById(R.id.imageViewFrom);
        fromIcon.setImageDrawable(getResources().getDrawable(getCurrencyIconChanger(from)));
        ImageView toIcon = (ImageView)view.findViewById(R.id.imageViewTo);
        toIcon.setImageDrawable(getResources().getDrawable(getCurrencyIconChanger(to)));
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setView(view);
        return ab.show();
    }

    protected boolean validateAddress(TextView addressTo) {
        if (addressTo.getText() == null || addressTo.getText().toString().trim().length() != 40) {
            addressTo.setError(getString(R.string.choose_address));
            addressTo.requestFocus();
            return false;
        }
        return true;
    }

    public void onClickConfirm(View view) {
        if(exchangeDialog!=null && exchangeDialog.isShowing()){
            exchangeDialog.dismiss();
        }
        if(from==null){
            showAlertDialog("","Currency from is null!");
            return;
        }
        Exchanger exchanger = getExchanger(from);
        if(exchanger.equals(Exchanger.Changer)){
            String html = exchange.getHtml();
            Document form = Jsoup.parse(html);
            Elements elements = form.getElementsByTag("input");
            Iterator<Element> input = elements.iterator();
            Element i = input.next();
            QueryString postData = new QueryString(i.attr("name"),i.attr("value"));
            while(input.hasNext()){
                i = input.next();
                String name = i.attr("name");
                String value = i.attr("value");
                postData.add(name,value);
            }
            if(from.equals(Currency.advcash_USD)) {
                postData.add("ac_success_url", Const.AdvCash.success_url);
                postData.add("ac_fail_url", Const.AdvCash.fail_url);
                postData.add("ac_success_url_method", Const.AdvCash.success_url_method);
                final String success_url = Const.AdvCash.success;
                final String transactionIdKey = Const.AdvCash.transactionIdKey;
                mWebView.postUrl(Const.AdvCash.sci_url, EncodingUtils.getBytes(postData.toString(), "BASE64"));
                configureWebView(success_url, transactionIdKey);
            } else
            if(from.equals(Currency.pm_USD)) {
                postData.add("PAYMENT_URL", Const.PM.success_url);
                postData.add("NOPAYMENT_URL", Const.PM.fail_url);
                postData.add("PAYMENT_URL_METHOD", Const.PM.success_url_method);
                final String success_url = Const.PM.success;
                final String transactionIdKey = Const.PM.transactionIdKey;
                mWebView.postUrl(Const.PM.sci_url, EncodingUtils.getBytes(postData.toString(), "BASE64"));
                configureWebView(success_url, transactionIdKey);
            } else {
            //if(from.equals(Currency.okpay_USD)) {
            //    postData.add("ok_return_success", Const.OKPAY.success_url);
            //    postData.add("ok_return_fail", Const.OKPAY.fail_url);
            //    final String success_url = Const.OKPAY.success;
            //    final String transactionIdKey = Const.OKPAY.transactionIdKey;
            //    mWebView.postUrl(Const.OKPAY.sci_url, EncodingUtils.getBytes(postData.toString(), "BASE64"));
            //    configureWebView(success_url, transactionIdKey);
            //} else {
            //if(from.equals(Currency.btce_USD)) {
            //    if(btceDialog!=null && btceDialog.isShowing()){
            //        btceDialog.dismiss();
            //    }
            //    btceDialog = showBtceDialog();
            //    return;
            //} else {
                if(sendToDialog!=null && sendToDialog.isShowing()){
                    sendToDialog.dismiss();
                }
                this.sendToDialog = showSendToDialog(this.exchange);
                return;
            }
            findViewById(R.id.changer_menu).setVisibility(View.GONE);
            findViewById(R.id.exchangeLayout).setVisibility(View.VISIBLE);
            mWebView.setVerticalScrollBarEnabled(true);
        } else {
            if(sendToDialog!=null && sendToDialog.isShowing()){
                sendToDialog.dismiss();
            }
            this.sendToDialog = showSendToDialog(this.addressResponse);
        }

    }

    private void configureWebView(final String success_url, final String transactionIdKey){
        mWebView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if(url.contains(success_url)){
                    try {
                        List<NameValuePair> params = URLEncodedUtils.parse(new URI(url), "UTF-8");
                        for (NameValuePair param : params) {
                            if(param.getName().equalsIgnoreCase(transactionIdKey)){
                                final String transactionId = param.getValue();
                                //Create payment object here
                                Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            completePayment(exchange.getExchangeId(), transactionId);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            //TODO Handle connection error
                                            showMessage(e.getMessage());
                                        }
                                    }
                                });
                                thread.start();
                                return true;
                            }
                        }
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private AlertDialog showSendToDialog(final Exchange exchange){
        View view = LayoutInflater.from(this).inflate(R.layout.send_to_layout,null);
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setView(view);
        AlertDialog sendToDialog = ab.show();
        TextView value = (TextView)sendToDialog.getWindow().findViewById(R.id.value);
        value.setText(Double.toString(exchange.getSendAmount()));
        TextView from = (TextView)sendToDialog.getWindow().findViewById(R.id.from);
        from.setText(exchange.getPair().getSend());
        TextView addressTo = (TextView)sendToDialog.getWindow().findViewById(R.id.addressTo);
        addressTo.setText(exchange.getPayee());
        final ImageView imageView = (ImageView) sendToDialog.getWindow().findViewById(R.id.imageViewFrom);
        ViewTreeObserver vto = imageView.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                imageView.getViewTreeObserver().removeOnPreDrawListener(this);
                int h = imageView.getMeasuredHeight();
                int w = imageView.getMeasuredWidth();
                Bitmap bitmap = null;
                try {
                    bitmap = encodeAsBitmap(exchange.getPayee(), ChangerTopUpActivity.this);
                } catch (WriterException e) {
                    e.printStackTrace();
                }
                imageView.setImageBitmap(getResizedBitmap(bitmap, (w>h)?h:w, (w>h)?h:w));
                return true;
            }
        });
        return sendToDialog;
    }

    private AlertDialog showSendToDialog(final GenerateAddressResponse addressResponse){
        View view = LayoutInflater.from(this).inflate(R.layout.send_to_layout,null);
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setView(view);
        AlertDialog sendToDialog = ab.show();
        TextView value = (TextView)sendToDialog.getWindow().findViewById(R.id.value);
        value.setText(Double.toString(addressResponse.getSendAmount()));
        TextView from = (TextView)sendToDialog.getWindow().findViewById(R.id.from);
        from.setText(this.from.name().toUpperCase());
        TextView addressTo = (TextView)sendToDialog.getWindow().findViewById(R.id.addressTo);
        addressTo.setText(addressResponse.getResult().getAddress());
        final ImageView imageView = (ImageView) sendToDialog.getWindow().findViewById(R.id.imageViewFrom);
        ViewTreeObserver vto = imageView.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                imageView.getViewTreeObserver().removeOnPreDrawListener(this);
                int h = imageView.getMeasuredHeight();
                int w = imageView.getMeasuredWidth();
                Bitmap bitmap = null;
                try {
                    bitmap = encodeAsBitmap(addressResponse.getResult().getAddress(), ChangerTopUpActivity.this);
                } catch (WriterException e) {
                    e.printStackTrace();
                }
                imageView.setImageBitmap(getResizedBitmap(bitmap, (w>h)?h:w, (w>h)?h:w));
                return true;
            }
        });
        return sendToDialog;
    }

    public void onClickCopyAddress(View view) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Address", ((TextView)view).getText().toString());
        clipboard.setPrimaryClip(clip);
        showMessage(getString(R.string.address_copied));
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        // Make sure to call the super method so that the states of our views are saved
        super.onSaveInstanceState(savedInstanceState);
        if(sendToDialog!=null && sendToDialog.isShowing()) {
            savedInstanceState.putSerializable(STATE_SEND_TO_CHANGER, exchange);
            savedInstanceState.putSerializable(STATE_SEND_TO_CHANGELLY, addressResponse);
        }
    }
}
