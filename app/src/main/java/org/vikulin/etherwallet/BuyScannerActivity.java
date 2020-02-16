package org.vikulin.etherwallet;

import android.content.Intent;
import androidx.fragment.app.DialogFragment;

import com.google.zxing.Result;

import static org.vikulin.etherwallet.CodeScannerActivity.QR_CODE_DATA;

public class BuyScannerActivity extends BaseScannerActivity {

    @Override
    public void handleResult(Result rawResult) {
        super.setRawResult(rawResult);
        Intent result = new Intent(BuyScannerActivity.this, BuyActivity.class);
        result.putExtra(QR_CODE_DATA, rawResult.getText());
        setResult(RESULT_OK, result);
        startActivity(result);
        finish();
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        // Resume the camera
        //mScannerView.resumeCameraPreview(this);
        Intent result = new Intent(this, BuyActivity.class);
        result.putExtra(QR_CODE_DATA, getRawResult().getText());
        setResult(RESULT_OK, result);
        startActivity(result);
        finish();
    }

}
