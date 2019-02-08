package org.vikulin.etherwallet;

/**
 * Created by vadym on 10.12.16.
 */

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.vikulin.etherwallet.listener.HandleExceptionListener;
import org.vikulin.etherwallet.task.TransactionTask;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Convert;

import java.math.BigInteger;

import static org.web3j.utils.Numeric.prependHexPrefix;
import static org.web3j.utils.Numeric.toHexString;

/**
 * A login screen that offers login via email/password.
 */
public class SignTransactionActivity extends FullScreenActivity {

    public static final int SIGN_TRANSACTION = 2090;

    public static final String ADDRESS_TO = "address_to";
    public static final String KEY_CONTENT = "key_content";
    public static final String VALUE = "value";
    public static final String TRANSACTION_ID = "transaction_id";
    public static final String ADDRESS_FROM = "address_from";
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UnlockKeyTask mAuthTask = null;

    // UI references.
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private String keyContent;
    private String addressTo;
    private String addressFrom;
    private double value;
    private String transactionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);
        // Set up the login form.

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            keyContent = extras.getString(KEY_CONTENT);
            addressTo = extras.getString(ADDRESS_TO);
            addressFrom = extras.getString(ADDRESS_FROM);
            value = extras.getDouble(VALUE);
            transactionId = extras.getString(TRANSACTION_ID);
            if(keyContent == null){
                showAlertDialog("","Key not found", new HandleExceptionListener(this, "Key not found"));
                return;
            }
            if(addressTo == null){
                showAlertDialog("","Address to not found", new HandleExceptionListener(this, "Address to not found"));
                return;
            }
            if(addressFrom == null){
                showAlertDialog("","Address from found", new HandleExceptionListener(this, "Address from found"));
                return;
            }
            if(value <=0 ){
                showAlertDialog("","value <=0", new HandleExceptionListener(this, "value <=0"));
                return;
            }
            if(transactionId == null){
                showAlertDialog("","Transaction not found", new HandleExceptionListener(this, "Transaction not found"));
                return;
            }
        } else {
            finish();
        }
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
        openKeyButton.setOnClickListener(new View.OnClickListener() {
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
            try {
                mAuthTask = new UnlockKeyTask(password, keyContent);
                mAuthTask.execute((Void) null);
            } catch (JSONException e) {
                e.printStackTrace();
                showAlertDialog("",e.getMessage());
            }
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
    private final class UnlockKeyTask extends TransactionTask {

        UnlockKeyTask(String password, String keyContent) throws JSONException {
            super(SignTransactionActivity.this, password, keyContent);
        }

        @Override
        protected void onPostExecute(final Exception exception) {
            mAuthTask = null;
            showProgress(false);
            if (exception==null) {
                //success
                BigInteger gasPrice = BigInteger.valueOf(41000000000l);
                BigInteger gasLimit = BigInteger.valueOf(50000l);
                // TODO correct walletList
                String data=transactionId;
                BigInteger value = Convert.toWei(Double.toString(SignTransactionActivity.this.value),Convert.Unit.ETHER).toBigInteger();
                BigInteger nonce = updateNonce(keyContent);
                //showMessage("nonce:"+nonce.toString()+" "+addressFrom);
                String transactionHash = signTransaction(getCredentials(), nonce, gasPrice, gasLimit, addressTo, value, data);
                //Run QR-code activity
                Intent intent = new Intent(SignTransactionActivity.this, TransactionCodeActivity.class);
                intent.putExtra(TransactionCodeActivity.TRANSACTION_HASH, transactionHash);
                intent.putExtra(TransactionCodeActivity.ADDRESS_FROM, prependHexPrefix(addressFrom));
                intent.putExtra(TransactionCodeActivity.ADDRESS_TO, prependHexPrefix(addressTo));
                intent.putExtra(TransactionCodeActivity.VALUE, SignTransactionActivity.this.value);
                intent.putExtra(TransactionCodeActivity.TRANSACTION_ID, SignTransactionActivity.this.transactionId);
                startActivity(intent);
                //setResult(Activity.RESULT_OK);
                //finish();
            } else {
                mPasswordView.setError(exception.getMessage());
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected Exception generateTransaction() {
            return null;
        }

        private String signTransaction(Credentials credentials, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, String toAddress, BigInteger value, String data) {
            RawTransaction rawTransaction  = RawTransaction.createTransaction(nonce, gasPrice, gasLimit, toAddress, value, data);
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            String hexValue = toHexString(signedMessage);
            return hexValue;
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}