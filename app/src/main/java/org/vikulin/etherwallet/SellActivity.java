package org.vikulin.etherwallet;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.vincentbrison.openlibraries.android.dualcache.Builder;

import org.json.JSONException;
import org.vikulin.etherwallet.adapter.pojo.SellItem;

import java.util.Arrays;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class SellActivity extends BuySellActivity implements ZXingScannerView.ResultHandler {

    private EditText barCode;
    private EditText price;
    private EditText itemName;
    private ZXingScannerView mScannerView;

    public Builder cacheBarCodeArray;
    final static int RAM_MAX_SIZE = 20000;
    final static int DISK_MAX_SIZE = 2000000;//2GB max size
    public final static int TEST_APP_VERSION = 30;
    final static String TYPE_ARRAY_CACHE_NAME = "type_array_cache";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        barCode = (EditText) findViewById(R.id.enterBarCode);
        itemName = (EditText) findViewById(R.id.itemName);
        price = (EditText) findViewById(R.id.enterPrice);
        mScannerView = (ZXingScannerView) findViewById(R.id.scanBarCode);
        mScannerView.setFormats(Arrays.asList(BarcodeFormat.EAN_8, BarcodeFormat.EAN_13));
        mScannerView.setResultHandler(this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK) {
            getAdapter().clear();
            getAdapter().notifyDataSetChanged();
        }
    }

    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }
    @Override
    public void onResume() {
        super.onResume();
        mScannerView.startCamera();
        mScannerView.setResultHandler(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }


    @Override
    protected String getAddressProperty() {
        return DEFAULT_SELL_ADDRESS;
    }

    public void onAddClick(View view) {
        SellItem sellItem = new SellItem();
        if(!validateFields()){
            return;
        }
        sellItem.setName(itemName.getText().toString());
        sellItem.setPrice(Double.valueOf(price.getText().toString()));
        getAdapter().add(0, sellItem);
        getAdapter().notifyDataSetChanged();
        itemName.requestFocus();
        itemName.setText(null);
        barCode.setText(null);
        price.setText(null);
    }

    protected boolean validateFields() {
        if(barCode.getText()!=null && barCode.getText().toString().trim().length()>5){
            Long barCodeLong;
            try {
                barCodeLong = Long.valueOf(barCode.getText().toString());
            } catch (NumberFormatException e){
                barCode.setError("Incorrect code format!");
                return false;
            }
            SellItem sellItem = getCacheBarCodeBuilder().getSellItem(barCodeLong);
            if(sellItem==null){
                showAlertDialog("",getString(R.string.product_not_found));
                return false;
            } else {
                itemName.setText(sellItem.getName());
                price.setText(sellItem.getPrice().toString());
            }
        }
        if ((itemName.getText()==null || itemName.getText().toString().trim().length()<4) && (barCode.getText()==null || barCode.getText().toString().trim().length()<5)) {
            itemName.setError(getString(R.string.enter_item_name));
            itemName.requestFocus();
            return false;
        }

        if (price.getText() == null || price.getText().toString().trim().length() == 0) {
            price.setError(getString(R.string.enter_price));
            price.requestFocus();
            return false;
        }
        try {
            Double.parseDouble(price.getText().toString());
        } catch (NumberFormatException e){
            price.setError(getString(R.string.enter_price));
            price.requestFocus();
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
    protected void process() {
        if(getAdapter().getCount()==0 && !validateFields()){
            return;
        }
        Intent intent = new Intent(this, BillCodeActivity.class);
        try {
            String paymentAddress = getWalletListView().getText().toString();
            intent.putExtra(BillCodeActivity.SELL_DATA, getSellData());
            getSharedPreferences().edit().putString(getAddressProperty(), paymentAddress.toLowerCase()).commit();
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        mScannerView.stopCamera();
        startActivityForResult(intent, BillCodeActivity.PAYMENT_SUCCESS);
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_sell;
    }

    @Override
    public void handleResult(Result result) {
        Long barCode;
        try {
            barCode = Long.valueOf(result.getText());
        } catch (NumberFormatException e){
            showAlertDialog("","Incorrect code format!");
            return;
        }
        final SellItem sellItemExist = getCacheBarCodeBuilder().getSellItem(barCode);

        if(sellItemExist!=null) {
            this.barCode.setText(String.valueOf(sellItemExist.getBarCode()));
            this.itemName.setText(String.valueOf(sellItemExist.getName()));
            this.price.setText(String.valueOf(sellItemExist.getPrice()));
            onAddClick(null);
        } else {
            showAlertDialog("",getString(R.string.product_not_found));
        }
        mScannerView.resumeCameraPreview(this);
    }

}
