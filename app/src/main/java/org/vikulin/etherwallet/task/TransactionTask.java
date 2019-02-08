package org.vikulin.etherwallet.task;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletFile;
import org.web3j.crypto.WalletNative;
import org.web3j.protocol.ObjectMapperFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.web3j.utils.Numeric.prependHexPrefix;

/**
 * Created by vadym on 18.01.17.
 */

public abstract class TransactionTask extends AsyncTask<Void, Void, Exception> {

    private final Context context;
    private final String mPassword;
    private Credentials credentials;
    private String addressFrom;
    private JSONObject o;
    private Web3j web3j;

    public TransactionTask(Context context, String password, String keyContent) throws JSONException {
        this.context = context;
        this.mPassword = password;
        this.o = new JSONObject(String.valueOf(keyContent));
        this.o.remove("key_name");
        this.o.remove("nonce");
        this.addressFrom = prependHexPrefix(o.getString("address"));
    }

    public Credentials loadCredentials(String password, String content) throws IOException, CipherException, GeneralSecurityException {
        ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
        WalletFile walletFile = objectMapper.readValue(content, WalletFile.class);
        return Credentials.create(WalletNative.decrypt(password, walletFile));
    }

    @Override
    protected Exception doInBackground(Void... params) {
        try {
            credentials = loadCredentials(this.mPassword, o.toString());
            web3j = Web3jFactory.build(new HttpService("https://mainnet.infura.io/erbkhNQe0QE11SJcEi1B"));
            //web3j = Web3jFactory.build(new HttpService("http://hyperborian.org:8545"));
            return generateTransaction();
        } catch (IOException e) {
            e.printStackTrace();
            return e;
        } catch (CipherException e) {
            e.printStackTrace();
            return e;
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return e;
        } catch (RuntimeException e){
            e.printStackTrace();
            return e;
        } catch (OutOfMemoryError e){
            e.printStackTrace();
            return new Exception("Not enough memory [RAM] to decrypt key",e);
        }
    }

    public BigInteger getNonce(String address){
        BigInteger nonce = null;
        try {
            EthGetTransactionCount ethGetTransactionCount = getWeb3j().ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).sendAsync().get();
            nonce = ethGetTransactionCount.getTransactionCount();
        } catch (RuntimeException e){
            e.printStackTrace();
            showAlertDialog("","RuntimeException:"+e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
            showAlertDialog("","InterruptedException:"+e.getMessage());
        } catch (ExecutionException e) {
            e.printStackTrace();
            showAlertDialog("","ExecutionException:"+e.getMessage());
        }
        return nonce;
    }

    protected BigInteger updateNonce(String keyContent) {
        long nonceLong = 0;
        BigInteger nonce = getNonce(addressFrom);
        try {
            nonceLong = new JSONObject(String.valueOf(keyContent)).getLong("nonce");
        } catch (JSONException e) {
            e.printStackTrace();
            showAlertDialog("","JSONException:"+e.getMessage());
        }
        if(nonce==null){
            nonceLong++;
            nonce = BigInteger.valueOf(nonceLong);
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            JSONObject keyFileObject = new JSONObject(keyContent);
            keyFileObject.put("nonce",nonce);
            Set<String> keys = new HashSet();
            Set<String> removedKeys = new HashSet<>();
            keys.add(keyFileObject.toString());
            Set<String> savedKeys = new HashSet<>(preferences.getStringSet("keys", new HashSet<String>()));
            if(savedKeys==null){
                preferences.edit().putStringSet("keys",keys).commit();
                return nonce;
            } else {
                for(String key:savedKeys){
                    JSONObject savedKey = new JSONObject(key);
                    if(savedKey.getString("address")!=null){
                        if(savedKey.getString("address").equalsIgnoreCase(keyFileObject.getString("address"))){
                            removedKeys.add(key);
                            break;
                        }
                    }
                }
                savedKeys.removeAll(removedKeys);
                savedKeys.addAll(keys);
                preferences.edit().putStringSet("keys",savedKeys).commit();
                return nonce;
            }
        } catch (JSONException e) {
            showAlertDialog("", e.getMessage());
        }
        return nonce;
    }

    protected abstract void onPostExecute(final Exception exception);

    protected abstract Exception generateTransaction();

    public Web3j getWeb3j(){
        return web3j;
    }

    public Credentials getCredentials(){
        return credentials;
    }

    protected void showAlertDialog(String title, String message){
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
