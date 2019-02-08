package org.vikulin.etherwallet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.vikulin.etherwallet.backup.SharedPreferencesBackupAgent;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletNativeUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.http.HttpService;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

import static org.web3j.utils.Numeric.prependHexPrefix;

/**
 * A login screen that offers login via email/password.
 */
public class KeyFilePasswordActivity extends FullScreenActivity {

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UnlockKeyTask mAuthTask = null;

    // UI references.
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private Uri keyPath;
    private Web3j web3j;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);
        // Set up the login form.
        keyPath = getIntent().getData();
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button openKeyButton = (Button) findViewById(R.id.open_key_button);
        openKeyButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }
        // Reset errors.
        mPasswordView.setError(null);
        // Store values at the time of the login attempt.
        String password = mPasswordView.getText().toString();
        boolean cancel = false;
        View focusView = null;
        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }
        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UnlockKeyTask(password, keyPath);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    private final class UnlockKeyTask extends AsyncTask<Void, Void, Exception> {

        private final String mPassword;
        private final Uri keyPath;
        private Credentials credentials;
        private File keyFile;

        UnlockKeyTask(String password, Uri keyPath) {
            mPassword = password;
            this.keyPath = keyPath;
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                keyFile = new File(this.keyPath.getPath());
                credentials = WalletNativeUtils.loadCredentials(this.mPassword, keyFile);
                web3j = Web3jFactory.build(new HttpService("https://mainnet.infura.io/erbkhNQe0QE11SJcEi1B"));
                importKey();
            } catch (IOException e) {
                e.printStackTrace();
                return e;
            } catch (CipherException e) {
                e.printStackTrace();
                return e;
            } catch (OutOfMemoryError e){
                e.printStackTrace();
                return new Exception("Not enough memory [RAM] to decrypt file",e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Exception exception) {
            mAuthTask = null;
            showProgress(false);
            if (exception==null) {
                //success
                setResult(Activity.RESULT_OK);
                finish();
            } else {
                mPasswordView.setError(exception.getMessage());
                mPasswordView.requestFocus();
            }
        }

        private void importKey() {
            try {
                String keyContent = readFile(keyFile);
                JSONObject keyFileObject = new JSONObject(keyContent);
                keyFileObject.put("key_name","");
                BigInteger nonce = BigInteger.ZERO;
                //Web3j init
                if(web3j!=null) {
                    nonce = web3j.ethGetTransactionCount(prependHexPrefix(keyFileObject.getString("address")), DefaultBlockParameterName.LATEST).send().getTransactionCount();
                }
                keyFileObject.put("nonce",nonce);
                Set<String> keys = new HashSet();
                Set<String> removedKeys = new HashSet<>();
                keys.add(keyFileObject.toString());
                Set<String> savedKeys = new HashSet<>(preferences.getStringSet("keys", new HashSet<String>()));
                if(savedKeys==null){
                    preferences.edit().putStringSet("keys",keys).commit();
                    SharedPreferencesBackupAgent.requestBackup(getApplicationContext());
                    return;
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
                    SharedPreferencesBackupAgent.requestBackup(getApplicationContext());
                    return;
                }
            } catch (JSONException e) {
                mPasswordView.setError(e.getMessage());
            } catch (IOException e) {
                mPasswordView.setError(e.getMessage());
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

