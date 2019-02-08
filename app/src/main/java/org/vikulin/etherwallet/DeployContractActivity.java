package org.vikulin.etherwallet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
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
import org.vikulin.etherwallet.task.TransactionTask;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import static org.web3j.utils.Numeric.toHexString;

/**
 * Created by vadym on 25.12.16.
 */

/**
 * A login screen that offers login via email/password.
 */
public class DeployContractActivity extends FullScreenActivity {

    public static final String ADDRESS_FROM = "address_from";
    public static final String KEY_CONTENT_FROM = "key_content_to";
    public static final String BYTE_CODE = "byte_code";
    public static final int DEPLOY_CONTRACT = 8000;
    public static final String GAS_LIMIT = "gas_limit";
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UnlockKeyTask mAuthTask = null;

    // UI references.
    private PaymentCheckTask paymentCheck;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private String keyContentFrom;
    private String addressFrom;
    private String byteCode;
    private String gasLimit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);
        // Set up the login form.
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            keyContentFrom = extras.getString(KEY_CONTENT_FROM);
            addressFrom = extras.getString(ADDRESS_FROM);
            byteCode = extras.getString(BYTE_CODE);
            gasLimit = extras.getString(GAS_LIMIT);
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
                mAuthTask = new UnlockKeyTask(password, keyContentFrom);
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

    private final class PaymentCheckTask extends TimerTask {

        private final String transactionHash;
        private final Web3j web3j;
        private BigInteger startBlock;
        private int index = 0;

        public PaymentCheckTask(Web3j web3j, BigInteger startBlock, String transactionHash){
            this.transactionHash = transactionHash;
            this.web3j = web3j;
            this.startBlock = startBlock;
        }

        public void run() {
            try {
                BigInteger currentBlock = web3j.ethBlockNumber().sendAsync().get().getBlockNumber();
                if (currentBlock.compareTo(startBlock) > 0) {
                    //EthTransaction o = web3j.ethGetTransactionByHash(transactionHash).sendAsync().get();
                    //EthBlock.TransactionObject o = findTransactionByHash(web3j, startBlock, currentBlock, transactionHash);
                    EthGetTransactionReceipt o = web3j.ethGetTransactionReceipt(transactionHash).sendAsync().get();
                    if (o != null && o.getTransactionReceipt() != null) {
                        // get contract address
                        String contractAddress = o.getTransactionReceipt().getContractAddress();
                        if (contractAddress!=null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showProgress(false);
                                }
                            });
                            showInfoDialogOnUiThread("SUCCESS", getString(R.string.success_contract_deploy) + "\n\n" + getString(R.string.contract_address)+ "\n" + contractAddress + "\n", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    setResult(Activity.RESULT_OK);
                                    PaymentCheckTask.this.cancel();
                                    DeployContractActivity.this.finish();
                                }
                            });
                        }
                    }
                    index++;
                    startBlock = currentBlock;
                    if (index > 11) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showProgress(false);
                            }
                        });
                        showInfoDialogOnUiThread("", getString(R.string.operation_timeout), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                setResult(Activity.RESULT_CANCELED);
                                PaymentCheckTask.this.cancel();
                                DeployContractActivity.this.finish();
                            }
                        });
                    }
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
                showAlertDialogOnUiThread("Error!","ETH transaction error:"+e.getMessage());
            } catch (InterruptedException e) {
                e.printStackTrace();
                showAlertDialogOnUiThread("Error!", "ETH transaction error:"+e.getMessage());
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showProgress(false);
                }
            });
            PaymentCheckTask.this.cancel();
            DeployContractActivity.this.finish();
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    private final class UnlockKeyTask extends TransactionTask {

        private BigInteger startBlock;

        UnlockKeyTask(String password, String keyContent) throws JSONException {
            super(DeployContractActivity.this, password, keyContent);
        }

        @Override
        protected void onPostExecute(final Exception exception) {
            mAuthTask = null;
            BigInteger GAS_PRICE = BigInteger.valueOf(41000000000l);
            BigInteger GAS_LIMIT = BigInteger.valueOf(Long.valueOf(gasLimit));
            if (exception==null) {
                String transaction = null;
                try {
                    // using a raw transaction
                    BigInteger nonce = updateNonce(keyContentFrom);
                    transaction = Numeric.prependHexPrefix(signTransaction(
                            getCredentials(),
                            nonce,
                            GAS_PRICE,
                            GAS_LIMIT,
                            BigInteger.ZERO,
                            byteCode));
                } catch (RuntimeException e) {
                    showInfoDialogOnUiThread("", e.getMessage(), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //TODO fake result
                            //setResult(Activity.RESULT_OK);
                            setResult(Activity.RESULT_CANCELED);
                            finish();
                        }
                    });
                    return;
                }
                try {
                    startBlock = getWeb3j().ethBlockNumber().sendAsync().get().getBlockNumber();
                } catch (InterruptedException e) {
                    showAlertDialogOnUiThread("",e.getMessage());
                    return;
                } catch (ExecutionException e) {
                    showAlertDialogOnUiThread("",e.getMessage());
                    return;
                }
                Request<?, EthSendTransaction> ether = getWeb3j().ethSendRawTransaction(transaction);
                EthSendTransaction ethSendTransaction = null;
                try {
                    ethSendTransaction = ether.sendAsync().get();
                    String transactionHash = ethSendTransaction.getTransactionHash();
                    Timer timer = new Timer();
                    paymentCheck = new PaymentCheckTask(getWeb3j(), startBlock, transactionHash);
                    timer.scheduleAtFixedRate(paymentCheck, 0, 1000*BLOCK_TIME);
                } catch (InterruptedException e) {
                    showAlertDialogOnUiThread("",e.getMessage());
                    return;
                } catch (ExecutionException e) {
                    showAlertDialogOnUiThread("",e.getMessage());
                    return;
                }
            } else {
                showAlertDialogOnUiThread("",exception.getMessage());
            }
        }

        @Override
        protected Exception generateTransaction() {
            return null;
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }

        private String signTransaction(Credentials credentials, BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, BigInteger value, String data) {
            RawTransaction rawTransaction  = RawTransaction.createContractTransaction(nonce, gasPrice, gasLimit, value, data);
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            String hexValue = toHexString(signedMessage);
            return hexValue;
        }
    }

    private static final int BLOCK_TIME=10;
}