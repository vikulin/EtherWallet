package org.vikulin.etherwallet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
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

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONException;
import org.vikulin.etherwallet.icon.Blockies;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.crypto.WalletNative;
import org.web3j.protocol.ObjectMapperFactory;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A login screen that offers login via email/password.
 */
public class CreateEncryptedKeyPasswordActivity extends FullScreenActivity {

    public static final int PRIVATE_KEY = 12000;
    public static final String PRIVATE_KEY_CONTENT = "private_key_content";
    public static final String KEY_CONTENT = "key_content";
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private EncryptKeyTask mAuthTask = null;

    // UI references.
    private EditText mPasswordView;
    private EditText mConfirmPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private String privateKeyContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_password);
        // Set up the login form.
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            privateKeyContent = extras.getString(PRIVATE_KEY_CONTENT);
        } else {
            showAlertDialog("","Private key content is null");
            finish();
        }

        mPasswordView = (EditText) findViewById(R.id.password);
        mConfirmPasswordView = (EditText) findViewById(R.id.confirmPassword);
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
        mConfirmPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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
        String confirmPassword = mConfirmPasswordView.getText().toString();
        boolean cancel = false;
        View focusView = null;
        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }
        if (!password.equals(confirmPassword)) {
            mConfirmPasswordView.setError(getString(R.string.passwords_do_not_match));
            focusView = mConfirmPasswordView;
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
            mAuthTask = new EncryptKeyTask(this.getBaseContext(), password, privateKeyContent);
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
    private final class EncryptKeyTask extends AsyncTask<Void, Void, Exception> {

        private final Context context;
        private String password;
        private String privateKeyContent;
        private String address;


        EncryptKeyTask(Context context, String password, String privateKeyContent){
            this.context = context;
            this.password = password;
            this.privateKeyContent = privateKeyContent;
        }

        private void encryptPrivateKey(String password, String privateKeyContent) throws CipherException, GeneralSecurityException, IOException, JSONException {
            ECKeyPair ecKeyPair = ECKeyPair.create(Numeric.hexStringToByteArray(privateKeyContent));
            WalletFile walletFile = WalletNative.createStandard(password, ecKeyPair);
            this.address = Numeric.prependHexPrefix(walletFile.getAddress());
            String fileName = getWalletFileName(walletFile);
            File destination = new File(getExternalCacheDir(), fileName);
            ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
            objectMapper.writeValue(destination, walletFile);
            importKey(readFile(destination));
        }

        private String getWalletFileName(WalletFile walletFile) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("'UTC--'yyyy-MM-dd'T'HH-mm-ss.SSS'--'");
            return dateFormat.format(new Date()) + walletFile.getAddress() + ".json";
        }

        @Override
        protected Exception doInBackground(Void... voids) {
            try {
                encryptPrivateKey(this.password, this.privateKeyContent);
            } catch (CipherException e) {
                e.printStackTrace();
                return e;
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
                return e;
            } catch (JsonGenerationException e) {
                e.printStackTrace();
                return e;
            } catch (JsonMappingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                return e;
            } catch (JSONException e) {
                e.printStackTrace();
                return e;
            }   catch (OutOfMemoryError e){
                e.printStackTrace();
                return new Exception("Too low free RAM memory!");
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Exception exception) {
            mAuthTask = null;
            showProgress(false);
            if (exception==null) {
                //success
                showInfoDialog("", getString(R.string.addressLabel) + "\n\n" + address, Blockies.createIcon(8, address, 12), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        setResult(RESULT_OK);
                        finish();
                    }
                });

            } else {
                mPasswordView.setError(exception.getMessage());
                mPasswordView.requestFocus();
            }
        }


        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

