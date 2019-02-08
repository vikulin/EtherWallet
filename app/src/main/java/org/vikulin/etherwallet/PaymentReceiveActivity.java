package org.vikulin.etherwallet;

/**
 * Created by vadym on 07.02.17.
 */

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.WriterException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.vikulin.etherwallet.ShowCodeActivity.encodeAsBitmap;
import static org.vikulin.etherwallet.ShowCodeActivity.getResizedBitmap;

/**
 * Created by vadym on 10.12.16.
 */

public class PaymentReceiveActivity extends AddressListActivity {

    private static final String VALUE = "value";
    private static final String ADDRESS = "address";
    private EditText value;
    private TextView address;

    protected String getAddressProperty() {
        return DEFAULT_BUY_ADDRESS;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // landscape
            setContentView(R.layout.activity_receive_payment_h);
        } else {
            // portrait
            setContentView(R.layout.activity_receive_payment_w);
        }
        value = (EditText) findViewById(R.id.value);
        address = (TextView) findViewById(R.id.address);
        //SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
        Set<String> savedKeys = preferences.getStringSet("keys", new HashSet<String>());
        String[] addresses = new String[savedKeys.size()];
        for(int i=0; i<savedKeys.size();i++){
            try {
                addresses[i]=new JSONObject(String.valueOf(savedKeys.toArray()[i])).getString("address");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        String addressValue = preferences.getString(getAddressProperty(), null);
        if(addressValue!=null) {
            int i = Arrays.asList(addresses).indexOf(addressValue);
            if(i>=0){
                address.setText(addressValue);
                setIcon(addressValue);
            }
        } else {
            if(addresses.length==1){
                address.setText(addresses[0]);
                setIcon(addresses[0]);
            }
        }
        getPopupWindow(R.layout.spinner_address_item, addresses, address, getString(R.string.choose_address));
        Object savedValue = null;
        Object savedAddress = null;
        if(savedInstanceState!=null) {
            savedValue = savedInstanceState.getDouble(VALUE);
            savedAddress = savedInstanceState.getString(ADDRESS);
            if(savedValue!=null && savedAddress!=null){
                value.setText(savedValue.toString());
                address.setText(savedAddress.toString());
                onClickReceive(null);
            }
        }
        /*
        value.addTextChangedListener(new TextWatcher(){

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                onClickReceive(null);
            }
        });
        */
    }


    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        // Make sure to call the super method so that the states of our views are saved
        super.onSaveInstanceState(savedInstanceState);
        // Save our own state now
        if(validateFields()) {
            savedInstanceState.putDouble(VALUE, Double.valueOf(value.getText().toString()));
            savedInstanceState.putString(ADDRESS, address.getText().toString());
        }
    }

    @Override
    public TextView getWalletListView() {
        return address;
    }

    public void onClickReceive(View view) {
        if(!validateFields()){
            return;
        }
        final ImageView imageView = (ImageView) findViewById(R.id.qrCodeImage);
        //findViewById(R.id.receive).setVisibility(View.GONE);
        imageView.setVisibility(View.VISIBLE);
        ViewTreeObserver vto = imageView.getViewTreeObserver();
        try{
            final double amount = Double.valueOf(value.getText().toString());
            vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

                public boolean onPreDraw() {
                    Display display = getWindowManager().getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    int w = size.x;
                    int h = size.y;
                    try {
                        String address = getWalletListView().getText().toString();
                        String data = "ethereum:"+address+"?amount="+Double.toString(amount);
                        Bitmap bitmap = encodeAsBitmap(data, PaymentReceiveActivity.this);
                        imageView.setImageBitmap(getResizedBitmap(bitmap, (w>h)?h:w, (w>h)?h:w));
                    } catch (WriterException e) {
                        e.printStackTrace();
                        showAlertDialog("",e.getMessage());
                        return false;
                    } catch (NumberFormatException e){
                        e.printStackTrace();
                        value.requestFocus();
                        value.setError("invalid symbol");
                        String cleared = value.getText().toString().replaceAll("[^\\d.]", "");
                        value.setText(cleared);
                        return false;
                    }
                    return true;
                }
            });
        } catch (NumberFormatException e){
            e.printStackTrace();
            value.requestFocus();
            value.setError("invalid symbol");
            String cleared = value.getText().toString().replaceAll("[^\\d.]", "");
            value.setText(cleared);
        }
    }

    protected boolean validateFields() {
        if (getWalletListView().getText()==null || getWalletListView().getText().toString().trim().length()!=40) {
            getWalletListView().setError(getString(R.string.choose_address));
            getWalletListView().requestFocus();
            return false;
        }
        if (value.getText()==null || value.getText().toString().trim().length()==0) {
            value.setError(getString(R.string.enter_value));
            value.requestFocus();
            return false;
        }
        if (Double.valueOf(value.getText().toString())==0) {
            value.setError(getString(R.string.enter_value));
            value.requestFocus();
            return false;
        }
        return true;
    }
}
