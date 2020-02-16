package org.vikulin.etherwallet;

import android.content.Intent;
import androidx.fragment.app.DialogFragment;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;


/**
 * Created by vadym on 20.11.16.
 */

public class CodeScannerActivity extends BaseScannerActivity {

    public static final String QR_CODE = BarcodeFormat.QR_CODE.toString();
    public static final String QR_CODE_DATA = "QR_CODE_DATA";
    public static final String BARCODE_TYPE = "barcode_type";
    public static final int SCAN_ADDRESS = 2050;
    public static final int SCAN_BYTE_CODE = 2150;
    public static final int SCAN_PRIVATE_KEY = 2250;

    @Override
    public void handleResult(Result rawResult) {
        returnQRCodeData(rawResult);
    }

    private void returnQRCodeData(Result rawResult) {
        Intent result = new Intent();
        result.putExtra(QR_CODE_DATA, rawResult.getText());
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {

    }
}