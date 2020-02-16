package org.vikulin.etherwallet.task;

/**
 * Created by vadym on 12.06.17.
 */

import android.content.Context;
import android.os.AsyncTask;
import androidx.appcompat.app.AlertDialog;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletFile;
import org.web3j.crypto.WalletNative;
import org.web3j.protocol.ObjectMapperFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;


/**
 * Created by vadym on 18.01.17.
 */

public abstract class CredentialsTask extends AsyncTask<Void, Void, Exception> {

    private final Context context;
    private final String mPassword;
    private Credentials credentials;
    private JSONObject o;

    public CredentialsTask(Context context, String password, String keyContent) throws JSONException {
        this.context = context;
        this.mPassword = password;
        this.o = new JSONObject(String.valueOf(keyContent));
        this.o.remove("key_name");
        this.o.remove("nonce");
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
            return null;
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

    protected abstract void onPostExecute(final Exception exception);

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
