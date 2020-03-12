package org.vikulin.etherwallet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
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
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.Transfer;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by vadym on 25.12.16.
 */



/**
 * A login screen that offers login via email/password.
 */
public class SendEthOnlineActivity extends FullScreenActivity {

    public static final String ADDRESS_FROM = "address_from";
    public static final String ADDRESS_TO = "address_to";
    public static final String KEY_CONTENT_FROM = "key_content_to";
    public static final String KEY_CONTENT_TO = "key_content_to";
    public static final String PAYMENT_DATA = "payment_data";
    public static final String GAS_LIMIT = "gas_limit";
    public static final String GAS_PRICE = "gas_price";
    public static final String VALUE = "value";
    public static final String TOKEN_CONTRACT = "token_contract";

    public static final int SEND_ETHER = 4000;
    public static final String TOKEN_VALUE = "token_value";
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UnlockKeyTask mAuthTask = null;

    // UI references.
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private String keyContentFrom;
    private String keyContentTo;
    private String addressFrom;
    private String addressTo;
    private double value;
    private String paymentData;
    private String gasLimit;
    private String gasPrice;
    private String tokenContract;
    private String tokenValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);
        // Set up the login form.
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            keyContentFrom = extras.getString(KEY_CONTENT_FROM);
            keyContentTo = extras.getString(KEY_CONTENT_TO);
            addressFrom = extras.getString(ADDRESS_FROM);
            addressTo = extras.getString(ADDRESS_TO);
            value = extras.getDouble(VALUE);
            tokenValue = extras.getString(TOKEN_VALUE);
            paymentData = extras.getString(PAYMENT_DATA);
            gasLimit = extras.getString(GAS_LIMIT);
            gasPrice = extras.getString(GAS_PRICE);
            tokenContract = extras.getString(TOKEN_CONTRACT);
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
                mAuthTask = new UnlockKeyTask(this, password, keyContentFrom);
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

    public static void main( String[] args ) {
    	/*Lookup l = new Lookup("vikulin.eth", Type.TXT);
		SimpleResolver sr = new SimpleResolver("ns1.hyperborian.org");
		l.setResolver(sr);
		Record [] records = l.run();
		if(records!=null){
			for (int i = 0; i < records.length; i++) {
				TXTRecord address = (TXTRecord) records[i];
				System.out.println(address.rdataToString());
			}
		} else {
			System.err.println("No TXT records found");
		}*/
        String cleanValue = "0x06afe662d987ec872c129bd6134673ec4108847c";
        System.out.println(new BigInteger(cleanValue, 16));
        System.out.println(new Address(cleanValue));
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    private final class UnlockKeyTask extends TransactionTask {

        private final BigDecimal value;


        UnlockKeyTask(Context context, String password, String keyContent) throws JSONException {
            super(context, password, keyContent);
            this.value = Convert.toWei(Double.toString(SendEthOnlineActivity.this.value),Convert.Unit.ETHER);
        }

        @Override
        protected void onPostExecute(final Exception exception) {
            mAuthTask = null;
            showProgress(false);
            if (exception==null) {
                showInfoDialogOnUiThread("SUCCESS", getString(R.string.payment_successful), (dialogInterface, i) -> {
                    updateNonce(keyContentFrom);
                    setResult(Activity.RESULT_OK);
                    finish();
                });
            } else {
                StringWriter sw = new StringWriter();
                exception.printStackTrace(new PrintWriter(sw));
                String exceptionAsString = "";
                exceptionAsString = sw.toString();

                SendEthOnlineActivity.this.showAlertDialogOnUiThread("Error:", exception.getMessage()+"\n"+
                                exceptionAsString,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //TODO fake result
                        //setResult(Activity.RESULT_OK);
                        setResult(Activity.RESULT_CANCELED);
                        finish();
                    }
                });
            }
        }

        @Override
        protected Exception generateTransaction() {
            try {
                BigInteger gasLimit = BigInteger.valueOf(Long.parseLong(SendEthOnlineActivity.this.gasLimit));
                BigInteger gasPriceValue = org.web3j.abi.ManagedTransaction.GAS_PRICE;
                if(gasPrice!=null && gasPrice.length()>0){
                    gasPriceValue = BigInteger.valueOf(Integer.parseInt(gasPrice)*1000_000_000l);
                }
                TransactionReceipt transactionReceipt=null;
                String hash=null;
                if(tokenContract!=null && tokenContract.length()>0){
                    BigInteger valueBigInteger = new BigInteger(tokenValue, 10);
                    transactionReceipt = sendTransferTokensTransaction(getCredentials(), gasPriceValue, gasLimit, addressTo, tokenContract, valueBigInteger);
                    hash = transactionReceipt.getTransactionHash();
                } else {
                    if(paymentData==null) {
                        //success
                        transactionReceipt = Transfer.sendFunds(getWeb3j(), getCredentials(), gasPriceValue, gasLimit, addressTo, value, Convert.Unit.WEI, "");
                        hash = transactionReceipt.getTransactionHash();
                    } else {
                        //BigInteger gasPrice = BigInteger.valueOf(41000000000l);
                        transactionReceipt = Transfer.sendFunds(getWeb3j(), getCredentials(), gasPriceValue, gasLimit, addressTo, value, Convert.Unit.WEI, paymentData);
                        //TransactionReceipt transactionReceipt = Transfer.sendFunds(getWeb3j(), getCredentials(), addressTo, value, Convert.Unit.WEI);
                        hash = transactionReceipt.getTransactionHash();
                    }
                }
            } catch (InterruptedException e) {
                return e;
            } catch (ExecutionException e) {
                return e;
            } catch (RuntimeException e) {
                return e;
            } catch (Exception e) {
                return e;
            }
            return null;
        }


        private EthSendTransaction execute(Credentials credentials, BigInteger gasPriceValue, BigInteger gasLimit, Function function, String contractAddress) throws Exception {
            String encodedFunction = FunctionEncoder.encode(function);
            BigInteger nonce = null;
            EthGetTransactionCount ethGetTransactionCount = getWeb3j().ethGetTransactionCount(Numeric.prependHexPrefix(addressFrom), DefaultBlockParameterName.LATEST).sendAsync().get();
            nonce = ethGetTransactionCount.getTransactionCount();
            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    nonce,
                    gasPriceValue,
                    gasLimit,
                    contractAddress,
                    encodedFunction);
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            String hexValue = Numeric.toHexString(signedMessage);
            EthSendTransaction transactionResponse = getWeb3j().ethSendRawTransaction(hexValue)
                    .sendAsync().get();
            return transactionResponse;
        }


        private Function transfer(String to, BigInteger value) {
            List<Type> types = Arrays.asList(new Address(to), (Type)new Uint256(value));
            TypeReference<?> tr = new TypeReference<Bool>() {};
            List typeReferences = Collections.singletonList(tr);
            return new Function("transfer", types, (List<TypeReference<?>>) typeReferences);
        }

        private TransactionReceipt sendTransferTokensTransaction(Credentials credentials, BigInteger gasPriceValue, BigInteger gasLimit, String to, String contractAddress, BigInteger qty) throws Exception {
            Function function = transfer(to, qty);
            EthSendTransaction transactionResponse = execute(credentials, gasPriceValue, gasLimit, function, contractAddress);
            if (transactionResponse.getTransactionHash()==null) {
                throw new ExecutionException(new Exception(transactionResponse.getError().getMessage()));
            } else {
                return getTransactionReceipt(transactionResponse.getTransactionHash(), SLEEP_DURATION, ATTEMPTS);
            }
        }

        private TransactionReceipt getTransactionReceipt(String transactionHash, int sleepDuration, int attempts) throws Exception {

            EthGetTransactionReceipt receipt = sendTransactionReceiptRequest(transactionHash);
            for (int i = 0; i < attempts; i++) {
                if (receipt.getError()!=null) {
                    throw new ExecutionException(new Exception(receipt.getError().getMessage()+"\nHash:"+transactionHash));
                } else {
                    if(receipt.getTransactionReceipt()!=null) {
                        return receipt.getTransactionReceipt();
                    }
                }
                Thread.sleep(sleepDuration);
                receipt = sendTransactionReceiptRequest(transactionHash);
            }
            throw new ExecutionException(new Exception("App did not receive a transaction hash within 75 sec. Check transaction completion later."));
        }

        private EthGetTransactionReceipt sendTransactionReceiptRequest(String transactionHash) throws Exception {
            return getWeb3j().ethGetTransactionReceipt(transactionHash).sendAsync().get();
        }


        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }

        private static final int SLEEP_DURATION = 5000;
        private static final int ATTEMPTS = 15;
    }

}