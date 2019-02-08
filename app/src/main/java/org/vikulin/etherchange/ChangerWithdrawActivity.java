package org.vikulin.etherchange;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.vikulin.etherwallet.CodeScannerActivity;
import org.vikulin.etherwallet.PaymentPlanActivity;
import org.vikulin.etherwallet.R;

import java.util.regex.Pattern;

/**
 * Created by vadym on 11.04.17.
 */

public class ChangerWithdrawActivity extends ChangerActivity {

    private static final int ZXING_VIEW_RESULT_PERMISSION = 44;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        from = Currency.eth;
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void modifyAddress() {

    }

    public void onClick(View view) {
        to = Currency.valueOf(view.getTag().toString());
        Exchanger exchanger = getExchanger(from, to);
        switch(exchanger){
            case Changer: from = Currency.ethereum_ETH; break;
            case Changelly: from = Currency.eth; break;
        }
        showRateChanger();
    }

    protected AlertDialog showRateDialog(){
        View view = LayoutInflater.from(this).inflate(R.layout.withdraw_rate_layout,null);
        ImageView fromIcon = (ImageView)view.findViewById(R.id.imageViewFrom);
        fromIcon.setImageDrawable(getResources().getDrawable(getCurrencyIconChanger(from)));
        ImageView toIcon = (ImageView)view.findViewById(R.id.imageViewTo);
        toIcon.setImageDrawable(getResources().getDrawable(getCurrencyIconChanger(to)));
        EditText address = (EditText)view.findViewById(R.id.address);
        address.setHint(getString(R.string.addressLabel)+" "+ getCurrencyNameChanger(to));
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setView(view);
        return ab.show();
    }

    private Class<?> mClss;

    public void launchActivityForResult(Class<?> clss) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            mClss = clss;
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.CAMERA }, ZXING_VIEW_RESULT_PERMISSION);
        } else {
            Intent intent = new Intent(this, clss);
            intent.putExtra(CodeScannerActivity.BARCODE_TYPE, CodeScannerActivity.QR_CODE);
            startActivityForResult(intent, CodeScannerActivity.SCAN_ADDRESS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == CodeScannerActivity.SCAN_ADDRESS && resultCode == RESULT_OK) {
            Bundle extras = intent.getExtras();
            String qrCodeData = (String) extras.get(CodeScannerActivity.QR_CODE_DATA);
            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
            EditText addressTo = (EditText) rateDialog.getWindow().findViewById(R.id.address);
            addressTo.setText(qrCodeData);
            return;
        }
    }

    public void onClickScanAddress(View view) {
        launchActivityForResult(CodeScannerActivity.class);
    }

    //implement different merchant's addresses validation
    protected boolean validateAddress(TextView addressTo) {
        CharSequence value = addressTo.getText();
        if ( value == null || value.toString().trim().length() < 3) {
            addressTo.setError(getString(R.string.choose_address));
            addressTo.requestFocus();
            return false;
        }
        switch (to){
            case pm_USD:
                if(!value.toString().startsWith("U")){
                    addressTo.setError(getString(R.string.choose_address)+": Uxxxxxx");
                    addressTo.requestFocus();
                    return false;
                }   return true;
            //case okpay_USD:
            //    if(!value.toString().startsWith("OK") && !isValidEmail(value)){
            //        addressTo.setError(getString(R.string.choose_address)+": OKxxxxxx, example@gmail.com");
            //        addressTo.requestFocus();
            //        return false;
            //    }   return true;
            case advcash_USD:
                if(!isValidEmail(value)){
                    addressTo.setError(getString(R.string.choose_address)+": example@gmail.com");
                    addressTo.requestFocus();
                    return false;
                }   return true;
            //case gnt:
            //    if(!isValidEmail(value)){
            //        addressTo.setError(getString(R.string.choose_address)+": example@gmail.com");
            //        addressTo.requestFocus();
            //        return false;
            //    }   return true;
            case bitcoin_BTC:
                if(!isValidBitcoinAddress(value)){
                    addressTo.setError(getString(R.string.choose_address)+": ^[13][a-km-zA-HJ-NP-Z1-9]{25,34}$");
                    addressTo.requestFocus();
                    return false;
                }   return true;
            case btc:
                if(!isValidBitcoinAddress(value)){
                    addressTo.setError(getString(R.string.choose_address)+": ^[13][a-km-zA-HJ-NP-Z1-9]{25,34}$");
                    addressTo.requestFocus();
                    return false;
                }   return true;
            case bch:
                if(!isValidBitcoinAddress(value)){
                    addressTo.setError(getString(R.string.choose_address)+": ^[13][a-km-zA-HJ-NP-Z1-9]{25,34}$");
                    addressTo.requestFocus();
                    return false;
                }
            case etc:
                if(!isValidEtcAddress(value)){
                    addressTo.setError(getString(R.string.choose_address)+": 0x...");
                    addressTo.requestFocus();
                    return false;
                }   return true;
        }
        return true;
    }

    public final static boolean isValidEmail(CharSequence target) {
        if (TextUtils.isEmpty(target)) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }

    public final static boolean isValidBitcoinAddress(CharSequence target) {
        if (TextUtils.isEmpty(target)) {
            return false;
        } else {
            Pattern p = Pattern.compile(BITCOIN_REGEX);
            return p.matcher(target.toString()).matches();
        }
    }

    public final static boolean isValidEtcAddress(CharSequence target) {
        if (TextUtils.isEmpty(target)) {
            return false;
        } else {
            return target.toString().startsWith("0x") && target.toString().length()==42;
        }
    }

    private final static String BITCOIN_REGEX="^[13][a-km-zA-HJ-NP-Z1-9]{25,34}$";

    public void onClickConfirm(View view) {
        TextView from = (TextView) exchangeDialog.getWindow().findViewById(R.id.from);
        Intent intent = new Intent(this, PaymentPlanActivity.class);
        if(to==null){
            showAlertDialog("","Currency to is null!");
            return;
        }
        Exchanger exchanger = getExchanger(this.to);
        if(exchanger.equals(Exchanger.Changer)){
            if(exchange!=null) {
                intent.putExtra(PaymentPlanActivity.ADDRESS_TO, exchange.getPayee());
            } else {
                showAlertDialog("","Response is null. Try to exchange again.");
                return;
            }
        } else {
            if(addressResponse!=null) {
                intent.putExtra(PaymentPlanActivity.ADDRESS_TO, addressResponse.getResult().getAddress());
            } else {
                showAlertDialog("","Address response is null. Try to exchange again.");
                return;
            }
        }
        intent.putExtra(PaymentPlanActivity.PAYMENT_VALUE, from.getText().toString());
        startActivityForResult(intent, PaymentPlanActivity.CHANGER_PAYMENT);
    }
}
