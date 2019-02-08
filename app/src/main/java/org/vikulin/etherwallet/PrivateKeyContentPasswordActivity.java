package org.vikulin.etherwallet;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
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
import org.vikulin.etherwallet.task.CredentialsTask;

/**
 * A login screen that offers login via email/password.
 */
public class PrivateKeyContentPasswordActivity extends FullScreenActivity {


    public static final int PRIVATE_KEY = 9000;
    public static final String PRIVATE_KEY_CONTENT = "private_key_content";
    public static final String KEY_CONTENT = "key_content";
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UnlockKeyTask mAuthTask = null;

    // UI references.
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private String keyContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);
        // Set up the login form.
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            keyContent = extras.getString(KEY_CONTENT);
        } else {
            showAlertDialog("","Key content is null");
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
            try {
                mAuthTask = new UnlockKeyTask(this.getBaseContext(), password, keyContent);
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
    private final class UnlockKeyTask extends CredentialsTask {

        UnlockKeyTask(Context context, String password, String keyContent) throws JSONException {
            super(context, password, keyContent);
        }

        @Override
        protected void onPostExecute(final Exception exception) {
            mAuthTask = null;
            showProgress(false);
            if (exception==null) {
                //success
                Intent result = new Intent();
                result.putExtra(PRIVATE_KEY_CONTENT, getCredentials().getEcKeyPair().getPrivateKey().toString(16));
                setResult(RESULT_OK, result);
                finish();
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

