package org.vikulin.etherwallet;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;
import com.jakewharton.disklrucache.DiskLruCache;

import org.json.JSONException;
import org.json.JSONObject;
import org.vikulin.etherchange.ChangerTopUpActivity;
import org.vikulin.etherchange.ChangerWithdrawActivity;
import org.vikulin.etherchange.Currency;
import org.vikulin.etherchange.changer.Rate;
import org.vikulin.etherwallet.adapter.CountryAdapter;
import org.vikulin.etherwallet.adapter.S3ObjectSummaryAdapter;
import org.vikulin.etherwallet.adapter.S3ObjectVersionAdapter;
import org.vikulin.etherwallet.adapter.pojo.SellItem;
import org.vikulin.etherwallet.cache.DiskLruCacheActivityBuilder;
import org.vikulin.etherwallet.listener.HandleExceptionListener;
import org.vikulin.etherwallet.task.AmazonS3Task;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.WalletUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;

public class DrawerActivity extends MainActivity {

    private static final String BAR_CODE_BUCKET = "bar-code.hyperborian.org";
    public static final int SEND_WALLET_KEY = 3000;
    public static String LANGUAGE = "language";
    public static String OPEN_DRAWER = "open_drawer";
    public static final int KEY_FILE_PICKER = 2051;
    private static final int SELL_ACTIVITY = 2055;
    public static final int SEND_ETHER_ACTIVITY = 5000;
    private DecimalFormatSymbols ds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ds = new DecimalFormatSymbols(this.getResources().getConfiguration().locale);
        ds.setDecimalSeparator('.');
        LinearLayout topUp = (LinearLayout)findViewById(R.id.topUp);
        if(topUp!=null) {
            topUp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    topUp(view);
                }
            });
        }
        LinearLayout withdraw = (LinearLayout)findViewById(R.id.withdraw);
        if(withdraw!=null) {
            withdraw.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    withdraw(view);
                }
            });
        }
        final TextView pm = (TextView)findViewById(R.id.pm);
        final TextView bcc = (TextView)findViewById(R.id.bch);
        final TextView advcash = (TextView)findViewById(R.id.advcash);
        final TextView btc = (TextView)findViewById(R.id.btc);
        final TextView dash = (TextView)findViewById(R.id.dash);
        final TextView gnt = (TextView)findViewById(R.id.gnt);
        final TextView litecoin = (TextView)findViewById(R.id.litecoin);
        final TextView augur = (TextView)findViewById(R.id.augur);
        final TextView zcash = (TextView)findViewById(R.id.zcash);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close){

            Timer timer = new Timer();

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if(timer!=null){
                    timer.cancel();
                    timer.purge();
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {

                super.onDrawerOpened(drawerView);
                //if (slideOffset == 0) {
                //    timer.cancel();
                //    timer.purge();
                //} else if (slideOffset != 0) {
                    if(timer!=null){
                        timer.cancel();
                        timer.purge();
                    }
                    // started opening
                    TimerTask timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                final Rate pmRate = getRate(Currency.pm_USD, Currency.ethereum_ETH);
                                final Rate bccRate = getRate(Currency.bitcoincash_BCH, Currency.ethereum_ETH);
                                final Rate advcashRate = getRate(Currency.advcash_USD, Currency.ethereum_ETH);
                                final Rate btcRate = getRate(Currency.bitcoin_BTC, Currency.ethereum_ETH);
                                final Rate dashRate = getRate(Currency.dash_DASH, Currency.ethereum_ETH);
                                final Rate golemRate = getRate(Currency.golem_GNT, Currency.ethereum_ETH);
                                final Rate litecoinRate = getRate(Currency.litecoin_LTC, Currency.ethereum_ETH);
                                final Rate augurRate = getRate(Currency.augur_REP, Currency.ethereum_ETH);
                                final Rate zcashRate = getRate(Currency.zcash_ZEC, Currency.ethereum_ETH);
                                DrawerActivity.this.runOnUiThread(new Runnable() {
                                    public void run() {
                                        refreshRate(pmRate, pm);
                                        refreshRate(bccRate, bcc);
                                        refreshRate(advcashRate, advcash);
                                        refreshRate(btcRate, btc);
                                        refreshRate(dashRate, dash);
                                        refreshRate(golemRate, gnt);
                                        refreshRate(litecoinRate, litecoin);
                                        refreshRate(augurRate, augur);
                                        refreshRate(zcashRate, zcash);
                                    }
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                                //showMessageOnUiThread(e.getMessage());
                            }
                        }
                    };
                    timer = new Timer();
                    timer.schedule(timerTask, 0, 30000);
                    //getActionBar()
                    //        .setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                }
            //    super.onDrawerSlide(drawerView, slideOffset);
            //}

        };
        toggle.syncState();
        toggle.onDrawerOpened(drawer);
        drawer.addDrawerListener(toggle);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String openDrawer = extras.getString(OPEN_DRAWER);
            if(openDrawer!=null && openDrawer.equals(OPEN_DRAWER)){
                drawer.openDrawer((int) Gravity.LEFT);
            }
        }
        try {
            updateOneSignalTags();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void refreshRate(Rate rate, TextView from) {
        if(rate!=null){
            DecimalFormat REAL_FORMATTER = new DecimalFormat("#0.0#", ds);
            String toValue = REAL_FORMATTER.format(1.0/rate.getRate());
            from.setText(toValue);
        } else {
            from.setText(null);
        }
    }

    private Rate getRate(Currency from, Currency to) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet getRequest = new HttpGet("https://www.changer.com/api/v2/rates/"+from.name()+"/"+to.name());
        getRequest.addHeader("accept", "application/json");
        HttpResponse response = httpClient.execute(getRequest);
        final int status = response.getStatusLine().getStatusCode();
        final BufferedReader br = new BufferedReader(
                new InputStreamReader((response.getEntity().getContent())));
        if (response.getStatusLine().getStatusCode() != 200) {
            this.runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        showAlertDialog("", "Changer error:" + status + " \n" + br.readLine(), new HandleExceptionListener(DrawerActivity.this, "Changer error:" + status + " \n" + br.readLine()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }});
            br.close();
            return null;
        }
        Gson gson = new GsonBuilder().create();
        try {
            Rate rate = gson.fromJson(br, Rate.class);
            br.close();
            return rate;
        } catch (JsonSyntaxException e){
            this.runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        showAlertDialog("", "Changer error: JSON syntax exception " + status + " \n" + br.readLine(), new HandleExceptionListener(DrawerActivity.this, "Changer error: JSON syntax exception" + status + " \n" + br.readLine()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }});
            return null;
        }
    }


    public void updateMenu() {

        LinearLayout mDrawerList = (LinearLayout) findViewById(R.id.nav_view);
        ((TextView) mDrawerList.findViewById(R.id.topUpText)).setText(R.string.top_up);
        ((TextView) mDrawerList.findViewById(R.id.withdrawText)).setText(R.string.withdraw);

        View mMainList = findViewById(R.id.main_menu);
        ((TextView) mMainList.findViewById(R.id.main_generateWallet)).setText(R.string.wallets);
        ((TextView) mMainList.findViewById(R.id.main_sendEther)).setText(R.string.sendEther);
        ((TextView) mMainList.findViewById(R.id.main_messages)).setText(R.string.messages);
        ((TextView) mMainList.findViewById(R.id.main_receivePayment)).setText(R.string.receivePayment);
        ((TextView) mMainList.findViewById(R.id.main_deployContract)).setText(R.string.deployContract);
        ((TextView) mMainList.findViewById(R.id.main_viewTransactionList)).setText(R.string.transactions);
        ((TextView) mMainList.findViewById(R.id.main_sendEtherToEmail)).setText(R.string.backupPkToEmail);
        ((TextView) mMainList.findViewById(R.id.main_importKeyText)).setText(R.string.importWalletKey);
        ((TextView) mMainList.findViewById(R.id.main_importKeyFileText)).setText(R.string.key_file);
        ((TextView) mMainList.findViewById(R.id.main_importQRCodeText)).setText(R.string.private_key_short);
        ((TextView) mMainList.findViewById(R.id.main_buy)).setText(R.string.buy);
        ((TextView) mMainList.findViewById(R.id.main_sell)).setText(R.string.sell);
        supportInvalidateOptionsMenu();

    }

    @Override
    public void onBackPressed() {
        if(getWebView().getUrl()!=null && !getWebView().getUrl().contains("file://")){
            getWebView().goBack();
            return;
        }
        if(getWebView().getVisibility()==View.VISIBLE) {
            findViewById(R.id.main_menu).setVisibility(View.VISIBLE);
            getWebView().setVisibility(View.GONE);
            return;
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.drawer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_language: {
                CustomDialog cd = new CustomDialog(this);
                cd.show();
                return true;
            }
            case R.id.bar_code_list: {
                Intent intent = new Intent(this, BarCodeListActivity.class);
                this.startActivity(intent);
                return true;
            }
            case R.id.publish: {
                showBackupBarCode();
                return true;
            }
            case R.id.restore: {
                showRestoreBarCode();
                return true;
            }
            case R.id.help: {
                Intent intent = new Intent(this, HelpActivity.class);
                this.startActivity(intent);
                return true;
            }

        }
        return super.onOptionsItemSelected(item);
    }

    private View showRestore(){
        View view = LayoutInflater.from(this).inflate(R.layout.restore_barcode_layout,null);
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setView(view);
        ab.show();
        return view;
    }

    public void showRestoreBarCode() {
        View popup = showRestore();
        TextView countryTextView = (TextView)popup.findViewById(R.id.country);
        getPopupWindow(R.layout.spinner_country_item, getResources().getStringArray(R.array.countries_array), countryTextView, getString(R.string.choose_country));
    }

    private View showBackup(){
        View view = LayoutInflater.from(this).inflate(R.layout.backup_barcode_layout,null);
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setView(view);
        ab.show();
        return view;
    }

    public void showBackupBarCode() {
        View popup = showBackup();
        TextView countryTextView = (TextView)popup.findViewById(R.id.country);
        getPopupWindow(R.layout.spinner_country_item, getResources().getStringArray(R.array.countries_array), countryTextView, getString(R.string.choose_country));
    }

    private void listS32Backup(final String name, final String country, final String key, final ListView listView) {
        AmazonS3Task listTask = new AmazonS3Task(this) {
            @Override
            public void run(AmazonS3 s3) {
                ObjectListing putResponse = s3.listObjects(name, country);
                List<S3ObjectSummary> objectSummaries = putResponse.getObjectSummaries();
                List<S3ObjectSummary> cleanedUpSummaries  = new ArrayList<>();
                S3ObjectSummary os;
                Iterator<S3ObjectSummary> i = objectSummaries.iterator();
                while(i.hasNext()){
                    os = i.next();
                    if(!os.getKey().endsWith("/") && os.getSize()>0 && os.getKey().contains(key)){
                        os.setKey(os.getKey().replace(country+"/",""));
                        cleanedUpSummaries.add(os);
                    }
                    i.remove();
                }
                objectSummaries.clear();
                final ListAdapter adapter = new S3ObjectSummaryAdapter(DrawerActivity.this, R.layout.list_s3_object, cleanedUpSummaries);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listView.setAdapter(adapter);
                        if(adapter.getCount()>0){
                            listView.setVisibility(View.VISIBLE);
                        } else {
                            DialogInterface.OnClickListener yes = new DialogInterface.OnClickListener(){
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    uploadToS3(key, country);
                                }
                            };
                            DialogInterface.OnClickListener no = new DialogInterface.OnClickListener(){
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                }
                            };
                            showConfirmDialog("","No records found. Would you like to create a new one?", yes, no);
                        }
                    }
                });
            }
        };
        listTask.execute((Void) null);
    }

    private void listS32Restore(final String name, final String country, final String key, final ListView listView) {
        AmazonS3Task listTask = new AmazonS3Task(this) {
            @Override
            public void run(AmazonS3 s3) {
                ObjectListing putResponse = s3.listObjects(name, country);
                List<S3ObjectSummary> objectSummaries = putResponse.getObjectSummaries();
                List<S3ObjectSummary> cleanedUpSummaries  = new ArrayList<>();
                S3ObjectSummary os;
                Iterator<S3ObjectSummary> i = objectSummaries.iterator();
                while(i.hasNext()){
                    os = i.next();
                    if(!os.getKey().endsWith("/") && os.getSize()>0 && os.getKey().contains(key)){
                        os.setKey(os.getKey().replace(country+"/",""));
                        cleanedUpSummaries.add(os);
                    }
                    i.remove();
                }
                objectSummaries.clear();
                final ListAdapter adapter = new S3ObjectSummaryAdapter(DrawerActivity.this, R.layout.list_s3_object, cleanedUpSummaries);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listView.setAdapter(adapter);
                        if(adapter.getCount()>0){
                            listView.setVisibility(View.VISIBLE);
                        } else {
                            listView.setVisibility(View.GONE);
                        }
                    }
                });
            }
        };
        listTask.execute((Void) null);
    }

    private void uploadToS3(final String name, final String country) {
        DiskLruCacheActivityBuilder builder = getCacheBarCodeBuilder();
        DiskLruCache cache = builder.getCache();
        if (cache.getLruEntries().size() == 0){
            return;
        }
        Gson gson = new GsonBuilder().create();
        LinkedHashMap<String, ?> entries = cache.getLruEntries();
        Map<String, SellItem> items = new HashMap<String, SellItem>();
        for (Map.Entry<String, ?> entry : entries.entrySet()) {
            Long key = Long.parseLong(entry.getKey());
            items.put(entry.getKey(), builder.getSellItem(key));
        }
        final String st = gson.toJson(items);
        AmazonS3Task mBalanceTask = new AmazonS3Task(this) {
            @Override
            public void run(AmazonS3 s3) {
                InputStream stream = new ByteArrayInputStream(st.getBytes());
                ObjectMetadata objectMetadata = new ObjectMetadata();
                objectMetadata.setContentLength(st.length());
                //(Replace "MY-BUCKET" with your S3 bucket name, and "MY-OBJECT-KEY" with whatever you would like to name the file in S3)
                PutObjectRequest putRequest = new PutObjectRequest(BAR_CODE_BUCKET, country+"/"+name, stream, objectMetadata);
                PutObjectResult putResponse = s3.putObject(putRequest);
                if(putResponse!=null){
                    showInfoDialogOnUiThread("", name+" backed up successfully!");
                }
            }
        };
        mBalanceTask.execute((Void) null);
    }

    public void onClickExchange(View view) {
        Intent intent = new Intent(this, ChangerTopUpActivity.class);
        this.startActivity(intent);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }

    public void onClickTransactionList(View view) {
        Intent intent = new Intent(this, TransactionListActivity.class);
        this.startActivity(intent);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }

    public void onClickWalletList(View view) {
        Intent intent = new Intent(this, AccountListActivity.class);
        this.startActivity(intent);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }

    public void onClickBackupPkToEmail(View view) {
        Set<String> savedKeys = preferences.getStringSet("keys", new HashSet<String>());
        if(savedKeys!=null && savedKeys.size()>0) {
            try {
                sendWalletKeyByEmail(savedKeys);
            } catch (JSONException e) {
                e.printStackTrace();
                showAlertDialog("", e.getMessage());
            } catch (IOException e) {
                e.printStackTrace();
                showAlertDialog("", e.getMessage());
            }
        }
    }

    public void sendWalletKeyByEmail(Set<String> decodedKeyFile) throws JSONException, IOException {
        String[] keyFileName = new String[decodedKeyFile.size()];
        File[] keyFile = new File[decodedKeyFile.size()];
        int i=0;
        String addressString = "";
        for(String fileContent:decodedKeyFile) {
            JSONObject jObject = new JSONObject(fileContent);
            String addressTo = jObject.getString("address");
            keyFileName[i] = "ETH-" + addressTo + ".key";
            addressString+=" "+addressTo;
            keyFile[i] = new File(getExternalCacheDir(), keyFileName[i]);
            writeFile(keyFile[i], fileContent);
            i++;
        }
        String text = "ETH address: "+ addressString+"\n"+
                "1. Save the addresses. You can keep it to yourself or share it with others. That way, others can transfer ether to you.\n" +
                "2. Save versions of the private key. Do not share it with anyone else. Your private key is necessary when you want to access your Ether to send it!\n";
        String[] TO = {""};
        String[] CC = {""};
        Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("text/html");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
        emailIntent.putExtra(Intent.EXTRA_CC, CC);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Backup EtherWallet private keys "+(new Date()).toString());
        emailIntent.putExtra(Intent.EXTRA_TEXT, text);
        ArrayList<Uri> uris = new ArrayList<Uri>();
        for(File f:keyFile) {
            uris.add(FileProvider.getUriForFile(DrawerActivity.this,
                    BuildConfig.APPLICATION_ID + ".provider",f));
        }
        emailIntent.putExtra(Intent.EXTRA_STREAM, uris);
        emailIntent.setType("application/json");
        Intent result = Intent.createChooser(emailIntent,"");
        try {
            startActivityForResult(result, DrawerActivity.SEND_WALLET_KEY);
        } catch (android.content.ActivityNotFoundException ex) {
            showAlertDialog("","There is no email client installed.");
        }
    }

    private void generateWalletFile(File destination){
        String fileName = null;

        Intent chooseAddressAndEthValue = new Intent(DrawerActivity.this, ChooseAddressAndEthValue.class);
        try {
            chooseAddressAndEthValue.putExtra(ChooseAddressAndEthValue.KEY_CONTENT, readFile(new File(destination, fileName)));
            startActivityForResult(chooseAddressAndEthValue, ChooseAddressAndEthValue.CHOOSE_ADDRESS_AND_VALUE);
        } catch (IOException e) {
            e.printStackTrace();
            showAlertDialog("", e.getMessage());
        }
    }

    private void generateWalletFile(String passCode, File destination, boolean useFullScrypt){
        String fileName = null;
        try {
            fileName = WalletUtils.generateNewWalletFile(passCode, destination, useFullScrypt);
        } catch (CipherException e) {
            e.printStackTrace();
            showAlertDialog("", e.getMessage());
            return;
        } catch (IOException e) {
            e.printStackTrace();
            showAlertDialog("", e.getMessage());
            return;
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            showAlertDialog("", e.getMessage());
            return;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            if(!useFullScrypt) {
                showAlertDialog("", "Not enough RAM memory!");
                return;
            }
            generateWalletFile(passCode,destination , false);
        }
        Intent chooseAddressAndEthValue = new Intent(DrawerActivity.this, ChooseAddressAndEthValue.class);
        try {
            chooseAddressAndEthValue.putExtra(ChooseAddressAndEthValue.KEY_CONTENT, readFile(new File(destination, fileName)));
            startActivityForResult(chooseAddressAndEthValue, ChooseAddressAndEthValue.CHOOSE_ADDRESS_AND_VALUE);
        } catch (IOException e) {
            e.printStackTrace();
            showAlertDialog("", e.getMessage());
        }
    }

    public void onClickReceivePayment(View view) {
        Intent receivePayment = new Intent(DrawerActivity.this, PaymentReceiveActivity.class);
        startActivity(receivePayment);
    }

    public void onClickMainReceivePayment(View view) {
        Intent receivePayment = new Intent(DrawerActivity.this, PaymentReceiveActivity.class);
        startActivity(receivePayment);
    }

    public void onClickMessages(View view) {
        //Intent messages = new Intent(DrawerActivity.this, MessengerActivity.class);
        //startActivity(messages);
    }

    public void onClickSendEther(View view) {
        Intent intent = new Intent(this, SendEtherActivity.class);
        this.startActivityForResult(intent, SEND_ETHER_ACTIVITY);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }

    public void onClickContract(View view) {
        Intent intent = new Intent(this, ContractActivity.class);
        this.startActivity(intent);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }

    public void onClickKeyImport(View view) {
        Intent intent = new Intent(this, FSObjectPicker.class);
        intent.putExtra(FSObjectPicker.ONLY_DIRS, false);
        intent.putExtra(FSObjectPicker.ASK_READ, true);
        intent.putExtra(FSObjectPicker.START_DIR, "/sdcard");
        this.startActivityForResult(intent, KEY_FILE_PICKER);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        hideImportButtons();
    }

    public void onClickQRCodeImport(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.CAMERA }, ZXING_WEB_VIEW_RESULT_FOR_SCAN_PRIVATE_KEY);
        } else {
            launchActivityForResult(CodeScannerActivity.class, ZXING_WEB_VIEW_RESULT_FOR_SCAN_PRIVATE_KEY, CodeScannerActivity.SCAN_PRIVATE_KEY);
        }
        hideImportButtons();
    }

    public void onClickKeyMainImport(View view) {
        Intent intent = new Intent(this, FSObjectPicker.class);
        intent.putExtra(FSObjectPicker.ONLY_DIRS, false);
        intent.putExtra(FSObjectPicker.ASK_READ, true);
        intent.putExtra(FSObjectPicker.START_DIR, "/sdcard");
        this.startActivityForResult(intent, KEY_FILE_PICKER);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        hideImportMainButtons();
    }

    public void onClickQRCodeMainImport(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.CAMERA }, ZXING_WEB_VIEW_RESULT_FOR_SCAN_PRIVATE_KEY);
        } else {
            launchActivityForResult(CodeScannerActivity.class, ZXING_WEB_VIEW_RESULT_FOR_SCAN_PRIVATE_KEY, CodeScannerActivity.SCAN_PRIVATE_KEY);
        }
        hideImportMainButtons();
    }

    public void onClickImport(View view) {
        //LinearLayout advanced= (LinearLayout) findViewById(R.id.importKey);
        //advanced.setVisibility(View.GONE);
        //LinearLayout mListView=(LinearLayout) findViewById(R.id.importKeyContent);
        //mListView.setVisibility(View.VISIBLE);
    }

    public void onClickMainImport(View view) {
        LinearLayout advanced= (LinearLayout) findViewById(R.id.main_importKey);
        advanced.setVisibility(View.GONE);
        LinearLayout mListView=(LinearLayout) findViewById(R.id.main_importKeyContent);
        mListView.setVisibility(View.VISIBLE);
    }

    public void onClickBuy(View view) {
        launchFullActivity(view);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }

    public void onClickSell(View view) {
        Intent intent = new Intent(this, SellActivity.class);
        this.startActivityForResult(intent, SELL_ACTIVITY);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }

    private void hideImportButtons(){
        //LinearLayout mListView=(LinearLayout) findViewById(R.id.importKeyContent);
        //mListView.setVisibility(View.GONE);
        //LinearLayout advanced= (LinearLayout) findViewById(R.id.importKey);
        //advanced.setVisibility(View.VISIBLE);
    }

    private void hideImportMainButtons(){
        LinearLayout mListView=(LinearLayout) findViewById(R.id.main_importKeyContent);
        mListView.setVisibility(View.GONE);
        LinearLayout advanced= (LinearLayout) findViewById(R.id.main_importKey);
        advanced.setVisibility(View.VISIBLE);
    }

    public void onClickCountryList(View v) {
        int height = -1 * v.getHeight();
        popupCountry.showAsDropDown(v, -2, height);
    }

    public void topUp(View view) {
        Intent intent = new Intent(this, ChangerTopUpActivity.class);
        startActivity(intent);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }

    public void withdraw(View view) {
        Intent intent = new Intent(this, ChangerWithdrawActivity.class);
        startActivity(intent);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }

    public void searchBarCode2Backup(View view) {
        View rootView = view.getRootView();
        final ListView lv = (ListView) rootView.findViewById(R.id.objectList);
        final TextView country = (TextView)rootView.findViewById(R.id.country);
        EditText key = (EditText)rootView.findViewById(R.id.barCodeFile);
        if(!validateFields(country, key, 10)){
            return;
        }
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> adapterView, View view, int i, long l) {
                final S3ObjectSummary os = (S3ObjectSummary) adapterView.getAdapter().getItem(i);
                DialogInterface.OnClickListener yes = new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        uploadToS3(os.getKey(), country.getText().toString());
                    }
                };
                DialogInterface.OnClickListener no = new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                };
                showConfirmDialog("Backup", "Add new version to "+os.getKey()+" ?", yes, no);
            }
        });

        listS32Backup(BAR_CODE_BUCKET, country.getText().toString(), key.getText().toString(), lv);
    }

    private boolean validateFields(TextView country, EditText key, int minLeght) {
        if (country.getText()==null || country.getText().toString().trim().length()<3) {
            country.setError(getString(R.string.choose_country));
            country.requestFocus();
            return false;
        }
        if (country.getText().toString().length()>300) {
            country.setError(getString(R.string.too_long_value));
            country.requestFocus();
            return false;
        }
        if (key.getText()==null || key.getText().toString().trim().length()<minLeght) {
            key.setError(getString(R.string.enter_value));
            key.requestFocus();
            return false;
        }
        if (key.getText().toString().trim().length()<minLeght) {
            key.setError(getString(R.string.too_short_value));
            key.requestFocus();
            return false;
        }
        if (key.getText().toString().length()>64) {
            key.setError(getString(R.string.too_long_value));
            key.requestFocus();
            return false;
        }
        String keyString = key.getText().toString();
        String pattern = "`~!@#$;:%^&?*()+=\"'|\\/[]{},.";
        for(byte s:pattern.getBytes()){
            if(keyString.indexOf((int)s)>-1){
                key.setError(getString(R.string.enter_value));
                key.requestFocus();
                return false;
            }
        }
        return true;
    }

    public void searchBarCode2Restore(View view) {
        View rootView = view.getRootView();
        final ListView lv = (ListView) rootView.findViewById(R.id.objectList);
        final TextView country = (TextView)rootView.findViewById(R.id.country);
        EditText key = (EditText)rootView.findViewById(R.id.barCodeFile);
        if(!validateFields(country, key, 4)){
            return;
        }
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(final AdapterView<?> adapterView, View view, int i, long l) {
                S3ObjectSummary os = (S3ObjectSummary) adapterView.getAdapter().getItem(i);
                final ListView version = (ListView)view.findViewById(R.id.versions);
                version.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(final AdapterView<?> adapterView, View view, final int i, long l) {

                        AmazonS3Task getVersionTask = new AmazonS3Task(DrawerActivity.this) {
                            @Override
                            public void run(AmazonS3 s3) {
                                S3VersionSummary vs = (S3VersionSummary)adapterView.getAdapter().getItem(i);
                                final String versionKey = vs.getKey();
                                final String versionId = vs.getVersionId();
                                S3Object object = s3.getObject(new GetObjectRequest(BAR_CODE_BUCKET, versionKey, versionId));
                                InputStream objectData = object.getObjectContent();
                                try {
                                    BufferedInputStream bis = new BufferedInputStream(objectData);
                                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                                    int result = bis.read();
                                    while(result != -1) {
                                        buf.write((byte) result);
                                        result = bis.read();
                                    }
                                    String barCodes = buf.toString("UTF-8");
                                    objectData.close();
                                    Gson gson = new GsonBuilder().create();
                                    try {
                                        final Map<String, LinkedTreeMap> si = gson.fromJson(barCodes, new HashMap<String,LinkedTreeMap>().getClass());
                                        for (Map.Entry<String, LinkedTreeMap> entry : si.entrySet()) {
                                            SellItem s = new SellItem();
                                            s.setBarCode(((Double)entry.getValue().get("barCode")).longValue());
                                            s.setName((String)entry.getValue().get("name"));
                                            s.setPrice((Double)entry.getValue().get("price"));
                                            getCacheBarCodeBuilder().putSellItemSilently(Long.parseLong(entry.getKey()), s);
                                        }
                                        //showInfoDialogOnUiThread("","Imported "+si.size()+" bar-code(s). version key = "+versionKey+" \n"+barCodes);
                                        showInfoDialogOnUiThread("","Imported "+si.size()+" bar-code(s)");

                                    } catch (JsonSyntaxException e){
                                        showAlertDialogOnUiThread("","JSON syntax error:"+e.getMessage()+"\n no entries imported.");
                                        return;
                                    } catch (ClassCastException e){
                                        showAlertDialogOnUiThread("",e.getMessage());
                                        return;
                                    }
                                    //showAlertDialogOnUiThread("","size="+result);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        };
                        getVersionTask.execute((Void) null);
                    }
                });
                if(version.getVisibility()==View.GONE) {
                    version.setVisibility(View.VISIBLE);
                    final ListVersionsRequest request = new ListVersionsRequest()
                            .withBucketName(BAR_CODE_BUCKET)
                            //.withMaxResults(20)
                            .withPrefix(country.getText().toString()+"/"+os.getKey());
                    AmazonS3Task listTask = new AmazonS3Task(DrawerActivity.this) {
                        @Override
                        public void run(AmazonS3 s3) {
                            VersionListing versionListing;
                            List<S3VersionSummary> objectSummaryList = new ArrayList<>();
                            do {
                                versionListing = s3.listVersions(request);
                                for (S3VersionSummary objectSummary: versionListing.getVersionSummaries()) {
                                    if(objectSummary.getSize()>0) {
                                        objectSummaryList.add(objectSummary);
                                    }
                                }
                                request.setKeyMarker(versionListing.getNextKeyMarker());
                                request.setVersionIdMarker(versionListing.getNextVersionIdMarker());
                            } while (versionListing.isTruncated());
                            final ListAdapter adapterVersion = new S3ObjectVersionAdapter<>(DrawerActivity.this, R.layout.list_s3_object_version, objectSummaryList);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    version.setAdapter(adapterVersion);
                                }
                            });
                        }
                    };
                    listTask.execute((Void) null);
                } else {
                    version.setVisibility(View.GONE);
                }
            }
        });
        listS32Restore(BAR_CODE_BUCKET, country.getText().toString(), key.getText().toString(), lv);
    }

    private CountryAdapter adapter;

    private PopupWindow popupCountry;

    public PopupWindow getPopupWindow(int textViewResourceId, String[] objects, TextView editText, String hint) {
        // initialize a pop up window type
        PopupWindow popupWindow = new PopupWindow(this);
        // the drop down list is a list view
        ListView listView = new ListView(this);
        listView.setDividerHeight(0);
        View header = getLayoutInflater().inflate(R.layout.country_header, null);
        TextView headerTitle = (TextView)header.findViewById(R.id.headerTitle);
        headerTitle.setText(hint);
        listView.addHeaderView(header);
        // set our adapter and pass our pop up window contents
        adapter = new CountryAdapter(this, textViewResourceId, objects, popupWindow , editText);
        listView.setAdapter(adapter);
        // set the item click listener
        listView.setOnItemClickListener(adapter);
        // some other visual settings
        popupWindow.setFocusable(true);
        //popupWindow.setWidth(400);
        Display display = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        popupWindow.setWidth(display.getWidth()-30);
        popupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        // set the list view as pop up window content
        popupWindow.setContentView(listView);
        this.popupCountry = popupWindow;
        return popupWindow;
    }
}