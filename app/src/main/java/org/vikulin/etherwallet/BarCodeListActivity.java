package org.vikulin.etherwallet;

import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;

import com.google.zxing.Result;

import org.vikulin.etherwallet.adapter.pojo.SellItem;


/**
 * Created by vadym on 16.12.16.
 */

public class BarCodeListActivity extends BaseScannerActivity {

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {

    }

    @Override
    public void handleResult(Result result) {
        Long barCode;
        try {
            barCode = Long.valueOf(result.getText());
        } catch (NumberFormatException e){
            resumeCameraPreview();
            return;
        }
        final SellItem sellItemExist = getCacheBarCodeBuilder().getSellItem(barCode);
        stopCamera();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.sell_item));
        // Set up the input
        View header = getLayoutInflater().inflate(R.layout.bar_code_dialog, null);
        builder.setView(header);
        final EditText barCodeText = (EditText) header.findViewById(R.id.barCode);
        final EditText sellItemNameText = (EditText) header.findViewById(R.id.sellItemName);
        final EditText itemPriceText = (EditText) header.findViewById(R.id.itemPrice);
        if(sellItemExist!=null) {
            barCodeText.setText(String.valueOf(sellItemExist.getBarCode()));
            sellItemNameText.setText(String.valueOf(sellItemExist.getName()));
            itemPriceText.setText(String.valueOf(sellItemExist.getPrice()));
        } else {
            barCodeText.setText(String.valueOf(barCode));
        }
        ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
        // Set up the buttons
        builder.setPositiveButton(R.string.add_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SellItem sellItem = new SellItem();
                Long barCode = Long.valueOf(barCodeText.getText().toString());
                sellItem.setBarCode(barCode);
                sellItem.setName(sellItemNameText.getText().toString());
                try {
                    sellItem.setPrice(Double.valueOf(itemPriceText.getText().toString()));
                } catch (NumberFormatException e){
                    itemPriceText.setError(getString(R.string.enter_price));
                }
                getCacheBarCodeBuilder().putSellItem(barCode, sellItem);
                startCamera();
            }
        });
        builder.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startCamera();
                dialog.cancel();
            }
        });

        builder.show();
    }
}
