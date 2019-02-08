package org.vikulin.etherwallet;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.onesignal.OneSignal;
import org.json.JSONException;
import org.json.JSONObject;
import org.vikulin.ethername.Domain;
import org.vikulin.etherwallet.adapter.pojo.EthplorerResponse;
import org.vikulin.etherwallet.backup.SharedPreferencesBackupAgent;
import org.vikulin.etherwallet.cache.DiskLruCacheActivityBuilder;
import org.vikulin.etherwallet.cache.DiskLruCacheChatBuilder;
import org.vikulin.etherwallet.icon.Blockies;
import org.vikulin.etherwallet.listener.HandleExceptionListener;
import org.vikulin.etherwallet.task.CredentialsTask;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;
import org.xbill.DNS.TextParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;
import it.sephiroth.android.library.tooltip.Tooltip;
import static org.vikulin.etherwallet.ConfigurationActivity.language_codes;
import static org.vikulin.etherwallet.ConfigurationActivity.languages;
import static org.vikulin.etherwallet.DrawerActivity.LANGUAGE;

/**
 * Created by vadym on 11.12.16.
 */

public abstract class FullScreenActivity extends AppCompatActivity {

    protected volatile AlertDialog keyPasswordDialog;
    protected AlertDialog progressDialog;
    protected static SharedPreferences preferences;
    protected static Gson gson  = new GsonBuilder().create();
    public static DecimalFormat REAL_FORMATTER;

    public EthplorerResponse getTokenBalanceList(String address) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet getRequest = new HttpGet("https://api.ethplorer.io/getAddressInfo/"+address+"?apiKey=wycf7743RXH64");
        getRequest.addHeader("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        getRequest.addHeader("Cache-Control", "max-age=0");
        getRequest.addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Mobile Safari/537.36");
        getRequest.addHeader("Host", "api.ethplorer.io");
        HttpResponse response = httpClient.execute(getRequest);
        int status = response.getStatusLine().getStatusCode();
        final String content = read(new BufferedReader(new InputStreamReader((response.getEntity().getContent()))));
        httpClient.close();
        //TODO fix me the 429 error code workaround
        if (status != 200) {
            if(status!=429) {
                showAlertDialogOnUiThread("", "ethplorer.io service is unavailable. Returned status:" + status);
            }
            return null;
        }
        Gson gson = new GsonBuilder().create();
        EthplorerResponse ethplorerResponse = null;
        try {
            ethplorerResponse = gson.fromJson(content, EthplorerResponse.class);
        } catch (JsonSyntaxException ex){
            showAlertDialogOnUiThread("","JsonSyntaxException:"+ex.getMessage()+"\n"+content, new HandleExceptionListener(this, "JsonSyntaxException:"+ex.getMessage()+"\n"+content));
            return null;
        }
        return ethplorerResponse;
    }

    public void writeFile(File keyFile, String content) throws IOException {
        // get the path to sdcard
        File sdcard = Environment.getExternalStorageDirectory();
        FileOutputStream os = new FileOutputStream(keyFile);
        os.write(content.getBytes());
        os.close();
    }

    protected AlertDialog showKeyPasswordDialog(){
        View view = LayoutInflater.from(this).inflate(R.layout.open_key_password_layout,null);
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setView(view);
        AlertDialog passwordDialog = ab.show();
        passwordDialog.setCancelable(false);
        passwordDialog.setCanceledOnTouchOutside(false);
        return passwordDialog;
    }

    void showOpenKeyProgress(){
        Window rootView = keyPasswordDialog.getWindow();
        ProgressBar loginProgress = (ProgressBar) rootView.findViewById(R.id.login_progress);
        loginProgress.setPressed(true);
        loginProgress.setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.passwordLayout).setVisibility(View.GONE);
    }

    void hideOpenKeyProgress(){
        Window rootView = keyPasswordDialog.getWindow();
        ProgressBar loginProgress = (ProgressBar) rootView.findViewById(R.id.login_progress);
        loginProgress.setPressed(false);
        loginProgress.setVisibility(View.GONE);
        rootView.findViewById(R.id.passwordLayout).setVisibility(View.VISIBLE);
    }

    protected AlertDialog showProgress(){
        View rootView = LayoutInflater.from(this).inflate(R.layout.progress_layout,null);
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setView(rootView);
        AlertDialog progressDialog = ab.show();
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        ProgressBar progress = (ProgressBar) rootView.findViewById(R.id.progress);
        progress.setPressed(true);
        progress.setVisibility(View.VISIBLE);

        return progressDialog;
    }

    protected void hideProgress(){
        if(progressDialog!=null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    public void onClickPassword(View view) {
        showOpenKeyProgress();
        Editable passwordText = ((EditText)keyPasswordDialog.getWindow().findViewById(R.id.password)).getText();
        if(passwordText!=null && passwordText.toString().length()>0){
            Set<String> savedKeys = preferences.getStringSet("keys", new HashSet<String>());
            String key = null;
            String primaryAddress;
            primaryAddress = Keys.getAddress(primaryPublicKey);
            for(int i=0; i<savedKeys.size();i++){
                try {
                    key = String.valueOf(savedKeys.toArray()[i]);
                    String address = new JSONObject(key).getString("address");

                    if(primaryAddress.equalsIgnoreCase(address)){
                        /**
                         unlock key
                         */
                        try {
                            CredentialsTask mAuthTask = new CredentialsTask(this.getBaseContext(), passwordText.toString(), key) {

                                @Override
                                protected void onPostExecute(Exception exception) {
                                    hideOpenKeyProgress();
                                    if (exception==null) {
                                        //success
                                        credentials = getCredentials();
                                        keyPasswordDialog.dismiss();
                                        //getMessengerClient();
                                    } else {
                                        EditText mPasswordView = ((EditText)keyPasswordDialog.getWindow().findViewById(R.id.password));
                                        mPasswordView.setError(exception.getMessage());
                                        mPasswordView.requestFocus();
                                    }
                                }

                                @Override
                                protected void onCancelled() {
                                    hideOpenKeyProgress();
                                }
                            };
                            mAuthTask.execute((Void) null);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            showAlertDialog("",e.getMessage());
                        }
                        break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void onClickCancelPassword(View view) {
        finish();
    }

    protected static Credentials credentials;

    private final static AtomicInteger c = new AtomicInteger(0);

    private String primaryPublicKey;

    public static int getId() {
        return c.incrementAndGet();
    }

    protected MainApplication mainApplication;

    protected void onResume() {
        super.onResume();
        mainApplication.setCurrentActivity(this);
    }
    protected void onPause() {
        clearReferences();
        super.onPause();
    }
    protected void onDestroy() {
        clearReferences();
        hideProgress();
        super.onDestroy();
    }

    private void clearReferences(){
        Activity currActivity = mainApplication.getCurrentActivity();
        if (this.equals(currActivity)) {
            mainApplication.setCurrentActivity(null);
        }
    }

    private static void setUpAlarm(final Context context, final Intent intent, final int timeInterval) {
        final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        final PendingIntent pi = PendingIntent.getBroadcast(context, timeInterval, intent, 0);
        am.cancel(pi);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(System.currentTimeMillis() + timeInterval, pi);
            am.setAlarmClock(alarmClockInfo, pi);
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeInterval, pi);
        else
            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + timeInterval, pi);
    }

    private void resolveDomain(final String address, final OnAddressResolved listener){
        Thread thread = new Thread(new Runnable() {
            String v = null;
            @Override
            public void run() {
                try {
                    v = Domain.resolve(address).getResolved();
                    if(v!=null) {
                        listener.onResolved(v);
                    }
                } catch (TextParseException e) {
                    e.printStackTrace();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    private interface OnAddressResolved{
        void onResolved(String resolvedAddress);
    }


    public String read(BufferedReader reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        String aux = "";
        while ((aux = reader.readLine()) != null) {
            builder.append(aux);
        }
        String text = builder.toString();
        reader.close();
        return text;
    }

    private DiskLruCacheActivityBuilder cacheBarCodeBuilder;

    private static DiskLruCacheChatBuilder cacheChatBuilder;

    public void setIcon(String address, ImageView icon, int size){
        icon.setImageDrawable(new BitmapDrawable(getResources(), Blockies.createIcon(8, Numeric.prependHexPrefix(address), size)));
    }

    public void showTooltip(View anchor, String text, int layout){
        showTooltip(anchor, text, Tooltip.Gravity.BOTTOM, layout);
    }

    public void showTooltip(View anchor, String text, Tooltip.Gravity gravity, int layout){
        Tooltip.make(this,
                new Tooltip.Builder(101)
                        .anchor(anchor, gravity)
                        .closePolicy(new Tooltip.ClosePolicy()
                                .insidePolicy(true, false)
                                .outsidePolicy(true, false), 15000)
                        .activateDelay(800)
                        .showDelay(1000)
                        .text(text)
                        .maxWidth(500)
                        .withArrow(true)
                        .withOverlay(true)
                        .withStyleId(R.style.ToolTipLayoutCustomStyle)
                        .withCustomView(layout, false)
                        //.typeface(mYourCustomFont)
                        //.floatingAnimation(Tooltip.AnimationBuilder.DEFAULT)
                        .build()
        ).show();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        this.mainApplication = (MainApplication)this.getApplicationContext();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        this.updateConfiguration();
        this.cacheBarCodeBuilder = new DiskLruCacheActivityBuilder(this);
        cacheChatBuilder = new DiskLruCacheChatBuilder(this);
        //this.primaryPublicKey = preferences.getString(PRIMARY_PUBLIC_KEY, null);
        if(primaryPublicKey!=null && credentials==null){
            keyPasswordDialog = showKeyPasswordDialog();
        }

        DecimalFormatSymbols ds = new DecimalFormatSymbols(this.getResources().getConfiguration().locale);
        ds.setDecimalSeparator('.');
        ds.setGroupingSeparator(',');
        REAL_FORMATTER = new DecimalFormat("#,###,###,##0.00################", ds);
    }

    protected void onCreateWithTaskBar(@Nullable Bundle savedInstanceState, Activity messengerActivity) {
        super.onCreate(savedInstanceState);
        this.mainApplication = (MainApplication)this.getApplicationContext();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        this.updateConfiguration();
        this.cacheBarCodeBuilder = new DiskLruCacheActivityBuilder(this);
        cacheChatBuilder = new DiskLruCacheChatBuilder(this);
        //this.primaryPublicKey = preferences.getString(PRIMARY_PUBLIC_KEY, null);
        if(primaryPublicKey!=null && credentials==null){
            keyPasswordDialog = showKeyPasswordDialog();
        }

        DecimalFormatSymbols ds = new DecimalFormatSymbols(this.getResources().getConfiguration().locale);
        ds.setDecimalSeparator('.');
        ds.setGroupingSeparator(',');
        REAL_FORMATTER = new DecimalFormat("#,###,###,##0.00################", ds);
    }

    protected DiskLruCacheActivityBuilder getCacheBarCodeBuilder(){
        return cacheBarCodeBuilder;
    }

    public static DiskLruCacheChatBuilder getCacheChatBuilder(){
        return cacheChatBuilder;
    }

    protected void updateConfiguration() {
        Configuration config = getBaseContext().getResources().getConfiguration();
        //SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
        final String language = preferences.getString(LANGUAGE, null);
        if (language != null) {
            updateLanguage(language);
        }
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.updateConfiguration();
    }

    protected void updateLanguage(String language){
        int i = Arrays.asList(languages).indexOf(language);
        Configuration config = getBaseContext().getResources().getConfiguration();
        Locale locale = new Locale(language_codes[i]);
        Locale.setDefault(locale);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }
    }

    protected void updateOneSignalTags() throws JSONException {
        //SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
        Set<String> savedKeys = preferences.getStringSet("keys", null);
        if(savedKeys==null){
            return;
        }
        JSONObject tags = new JSONObject();
        List<String> addresses = new ArrayList();
        for(String key:savedKeys){
            JSONObject wallet = new JSONObject(key);
            String address = wallet.getString("address");
            addresses.add(address);
        }
        if(addresses.size()>0){
            String idList = addresses.toString();
            String csv = idList.substring(1, idList.length() - 1).replace(", ", ",");
            tags.put("addresses",csv);
            OneSignal.sendTags(tags);
        }
    }

    public void deleteAndUpdateOneSignalTags() throws JSONException {
        OneSignal.deleteTag("addresses");
        updateOneSignalTags();
    }

    public void updateKeyName(String address, String keyName) {
        //SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            Set<String> keys = new HashSet();
            Set<String> removedKeys = new HashSet<>();
            Set<String> savedKeys = new HashSet<>(preferences.getStringSet("keys", new HashSet<String>()));
            for(String key:savedKeys){
                JSONObject savedKey = new JSONObject(key);
                if(savedKey.getString("address")!=null){
                    if(savedKey.getString("address").equalsIgnoreCase(Numeric.cleanHexPrefix(address))){
                        savedKey.put("key_name",keyName);
                        removedKeys.add(key);
                        keys.add(savedKey.toString());
                        break;
                    }
                }
            }
            savedKeys.removeAll(removedKeys);
            savedKeys.addAll(keys);
            preferences.edit().putStringSet("keys",savedKeys).commit();
        } catch (JSONException e) {
            showAlertDialog("", e.getMessage());
        }
    }

    public String  importKey(String keyFileContent) throws JSONException, IOException {
        //SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
        JSONObject keyFileObject = new JSONObject(keyFileContent);
        keyFileObject.put("key_name","");
        BigInteger nonce = BigInteger.ZERO;
        keyFileObject.put("nonce",nonce);
        Set<String> keys = new HashSet();
        Set<String> removedKeys = new HashSet<>();
        keys.add(keyFileObject.toString());
        String newAddress = keyFileObject.getString("address");
        Set<String> savedKeys = new HashSet<>(preferences.getStringSet("keys", new HashSet<String>()));
        if(savedKeys==null){
            preferences.edit().putStringSet("keys",keys).commit();
            SharedPreferencesBackupAgent.requestBackup(getApplicationContext());
            return newAddress;
        } else {
            for(String key:savedKeys){
                JSONObject savedKey = new JSONObject(key);
                if(savedKey.getString("address")!=null){
                    if(savedKey.getString("address").equalsIgnoreCase(newAddress)){
                        removedKeys.add(key);
                        break;
                    }
                }
            }
            savedKeys.removeAll(removedKeys);
            savedKeys.addAll(keys);
            preferences.edit().putStringSet("keys",savedKeys).commit();
            SharedPreferencesBackupAgent.requestBackup(getApplicationContext());
            return newAddress;
        }
    }

    protected void showMessage(String message){
        if(!this.isFinishing())
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    protected void showMessageOnUiThread(final String message){
        this.runOnUiThread(new Runnable() {
            public void run() {
                if(!FullScreenActivity.this.isFinishing())
                    Toast.makeText(FullScreenActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    protected void showInfoDialogOnUiThread(final String title, final String message){
        this.runOnUiThread(new Runnable() {
            public void run() {
                showInfoDialog(title, message);
            }
        });
    }

    public void showAlertDialogOnUiThread(final String title, final String message){
        this.runOnUiThread(new Runnable() {
            public void run() {
                showAlertDialog(title, message);
            }
        });
    }

    public void showAlertDialogOnUiThread(final String title, final String message, final Bitmap icon){
        this.runOnUiThread(new Runnable() {
            public void run() {
                showAlertDialog(title, message, icon);
            }
        });
    }

    protected void showAlertDialogOnUiThread(final String title, final String message, final DialogInterface.OnClickListener listener){
        this.runOnUiThread(new Runnable() {
            public void run() {
                showAlertDialog(title, message, listener);
            }
        });
    }

    protected void showInfoDialogOnUiThread(final String title, final String message, final DialogInterface.OnClickListener listener){
        this.runOnUiThread(new Runnable() {
            public void run() {
                showInfoDialog(title, message, listener);
            }
        });
    }

    public void showInfoDialog(String title, String message){
        if(!this.isFinishing())
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    public void showInfoDialog(String title, String message, Bitmap icon, DialogInterface.OnClickListener listener){
        ImageView image = new ImageView(this);
        image.setImageDrawable(new BitmapDrawable(getResources(), icon));
        if(!this.isFinishing())
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(image)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, listener)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    public void showAlertDialog(String title, String message){
        if(!this.isFinishing())
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public void showAlertDialog(String title, String message, Bitmap icon){
        if(!this.isFinishing()) {
            Drawable drawableIcon = new BitmapDrawable(getResources(), icon);
            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(message)
                    .setIcon(drawableIcon)
                    .show();
        }
    }

    public void showInfoDialog(String title, String message, DialogInterface.OnClickListener listener){
        if(!this.isFinishing())
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, listener)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    public void showAlertDialog(String title, String message, DialogInterface.OnClickListener listener){
        if(!this.isFinishing())
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, listener)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public void showConfirmDialog(String title, String message, DialogInterface.OnClickListener pressedYes, DialogInterface.OnClickListener pressedNo){
        if(!this.isFinishing())
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, pressedYes)
                .setNegativeButton(android.R.string.no, pressedNo)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    public String readFile(File keyFile) throws IOException {
        int length = (int) keyFile.length();
        byte[] bytes = new byte[length];
        InputStream inputStream = null;
        String content = null;
        try {
            inputStream = new FileInputStream(keyFile);
            inputStream.read(bytes);
            content = new String(bytes);
        } catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        } finally {
            inputStream.close();
        }
        return content;
    }


}

