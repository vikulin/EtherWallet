package org.vikulin.etherwallet;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.vikulin.etherwallet.view.UploadWebView;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import im.delight.android.webview.AdvancedWebView;

import static org.vikulin.etherwallet.DrawerActivity.LANGUAGE;

public abstract class MainActivity extends FullScreenActivity implements AdvancedWebView.Listener {

    public static final int REQUEST_PASSWORD = 2053;
    public static final int NEW_KEY_PASSWORD = 2054;
    private static final int PRIVATE_KEY_SIZE = 32;
    private UploadWebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // landscape
            setContentView(R.layout.activity_drawer_h);
        } else {
            // portrait
            setContentView(R.layout.activity_drawer_w);
        }
        mWebView = (UploadWebView) findViewById(R.id.webview);
        mWebView.setListener(this, this);
        /*mWebView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                if(url.contains("scan://")){
                    try {
                        destination = url.substring(7);
                    } catch (IndexOutOfBoundsException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        return true;
                    }
                    try {
                       launchActivityForResult(CodeScannerActivity.class, ZXING_WEB_VIEW_RESULT_FOR_SCAN_ADDRESS, CodeScannerActivity.SCAN_ADDRESS);
                    } catch (Exception e){
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        return true;
                    }
                    return true;
                }
                return false;
            }
        });*/
        changeLanguage();
    }

    protected WebView getWebView(){
        return mWebView;
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        mWebView.onResume();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onPause() {
        mWebView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mWebView.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(requestCode == DrawerActivity.KEY_FILE_PICKER && resultCode == RESULT_OK) {
            Uri path = intent.getData();
            Intent passwordActivity = new Intent(MainActivity.this, KeyFilePasswordActivity.class);
            passwordActivity.setData(path);
            startActivityForResult(passwordActivity, REQUEST_PASSWORD);
            return;
        }
        mWebView.onActivityResult(requestCode, resultCode, intent);
        if(requestCode == CodeScannerActivity.SCAN_ADDRESS && resultCode == RESULT_OK) {
            Bundle extras = intent.getExtras();
            String qrCodeData = (String) extras.get(CodeScannerActivity.QR_CODE_DATA);
            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
            //mWebView.loadUrl("javascript:setTextField('"+destination+"','"+qrCodeData+"');");
            return;
        }
        if(requestCode == CodeScannerActivity.SCAN_PRIVATE_KEY && resultCode == RESULT_OK) {
            Bundle extras = intent.getExtras();
            String qrCodeData = (String) extras.get(CodeScannerActivity.QR_CODE_DATA);
            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
            try {
                byte[] pkArray = Numeric.hexStringToByteArray(qrCodeData);
                if(pkArray.length!= PRIVATE_KEY_SIZE){
                    showAlertDialog("", "Invalid input walletList: please check you are scanning private key QR-code with 64 bytes walletList length");
                    return;
                }
                ECKeyPair keyPair = ECKeyPair.create(pkArray);
                Keys.serialize(keyPair);
            } catch (RuntimeException e){
                showAlertDialog("", e.getMessage());
                return;
            }
            Intent passwordActivity = new Intent(MainActivity.this, CreateEncryptedKeyPasswordActivity.class);
            passwordActivity.putExtra(CreateEncryptedKeyPasswordActivity.PRIVATE_KEY_CONTENT, qrCodeData);
            startActivityForResult(passwordActivity, NEW_KEY_PASSWORD);
            return;
        }
        if(requestCode == NEW_KEY_PASSWORD && resultCode == RESULT_OK) {
            //nothing
        }
        if(requestCode == FSObjectPicker.PICK_FSOBJECT && resultCode == RESULT_OK) {
            Bundle extras = intent.getExtras();
            String path = (String) extras.get(FSObjectPicker.CHOSEN_FSOBJECT);
            try {
                JSONObject jObject = new JSONObject(new String(decodedKeyFile));
                String keyFileName = "ETH-"+jObject.getString("address")+".key";
                writeFile(path, keyFileName , decodedKeyFile);
                Toast.makeText(this, "Wallet key saved:"+keyFileName, Toast.LENGTH_LONG).show();
                return;
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(requestCode == ChooseAddressAndEthValue.CHOOSE_ADDRESS_AND_VALUE){
            //Set main menu
            findViewById(R.id.main_menu).setVisibility(View.VISIBLE);
            findViewById(R.id.webview).setVisibility(View.GONE);
        }
    }

    private void writeFile(String path, String fileName, byte[] content) throws IOException {
        // get the path to sdcard
        File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(path, fileName);
        FileOutputStream os = new FileOutputStream(file);
        os.write(content);
        os.close();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onPageStarted(String url, Bitmap favicon) {
        progressDialog = showProgress();
    }

    @Override
    public void onPageFinished(String url) {
        hideProgress();
        changeLanguage();
    }

    protected void changeLanguage(){
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String language = extras.getString(LANGUAGE);
            changeLanguage(language);
        }
    }

    public void changeLanguage(String language){
        getWebView().loadUrl("javascript:changeLanguage('"+language+"');");
    }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl){
        hideProgress();
        mWebView.goBack();
    }

    private byte[] decodedKeyFile;

    @Override
    public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) {
        int slash = url.lastIndexOf('/');
        byte[] decodedKeyFile = Base64.decode(url.substring(slash + 1), Base64.DEFAULT);
        if(url.contains("mailto")) {

        } else {
            this.decodedKeyFile = decodedKeyFile;
            Intent intent = new Intent(this, FSObjectPicker.class);
            // optionally set options here
            intent.putExtra(FSObjectPicker.ASK_WRITE, true);
            intent.putExtra(FSObjectPicker.START_DIR, "/");
            startActivityForResult(intent, FSObjectPicker.PICK_FSOBJECT);
        }
    }

    @Override
    public void onExternalPageRequest(String url) {

    }

    private static final int ZXING_CAMERA_PERMISSION = 1;
    public static final int ZXING_WEB_VIEW_RESULT_FOR_SCAN_ADDRESS = 2;
    public static final int ZXING_WEB_VIEW_RESULT_FOR_SCAN_PRIVATE_KEY = 3;
    private Class<?> mClss;

    public void launchActivity(Class<?> clss) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            mClss = clss;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, ZXING_CAMERA_PERMISSION);
        } else {
            Intent intent = new Intent(this, clss);
            startActivity(intent);
        }
    }

    public void launchActivityForResult(Class<?> clss, int permission_code, int result_code) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            mClss = clss;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, permission_code);
        } else {
            Intent intent = new Intent(this, clss);
            intent.putExtra(CodeScannerActivity.BARCODE_TYPE, CodeScannerActivity.QR_CODE);
            startActivityForResult(intent, result_code);
        }
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
            case ZXING_WEB_VIEW_RESULT_FOR_SCAN_ADDRESS:
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
            case ZXING_WEB_VIEW_RESULT_FOR_SCAN_PRIVATE_KEY:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(this, CodeScannerActivity.class);
                    intent.putExtra(CodeScannerActivity.BARCODE_TYPE, CodeScannerActivity.QR_CODE);
                    startActivityForResult(intent, CodeScannerActivity.SCAN_PRIVATE_KEY);
                } else {
                    Toast.makeText(this, "Please grant camera permission to use the QR Scanner", Toast.LENGTH_SHORT).show();
                }
        }
    }


    public void launchFullActivity(View v) {
        launchActivity(BuyScannerActivity.class);
    }

}