package org.vikulin.etherwallet;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.zxing.WriterException;

import org.json.JSONException;
import org.json.JSONObject;
import org.vikulin.etherwallet.adapter.TokenItemAdaptor;
import org.vikulin.etherwallet.adapter.pojo.EthplorerResponse;
import org.vikulin.etherwallet.adapter.pojo.Token;
import org.vikulin.etherwallet.icon.Blockies;
import org.vikulin.etherwallet.listener.HandleExceptionListener;
import org.vikulin.etherwallet.task.CheckBalanceTask;
import org.vikulin.etherwallet.task.CheckDomain;
import org.xbill.DNS.TextParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;

import static org.vikulin.etherwallet.AccountListActivity.DOT_ETH;
import static org.vikulin.etherwallet.ShowCodeActivity.encodeAsBitmap;
import static org.vikulin.etherwallet.ShowCodeActivity.getResizedBitmap;
import static org.web3j.utils.Numeric.prependHexPrefix;

public class AccountDetailsActivity extends FullScreenActivity {

    public static final String ADDRESS = "address";
    public static final String DOMAIN = "domain";
    private static final String PK_UNLOCK_STATUS = "pk_unlock_status";
    private static final String PK_CONTENT = "pk_content";
    private String address;
    private String domain;
    private boolean unlocked = false;
    private String privateKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_details);
        final Bundle extras = getIntent().getExtras();
        final ImageView imageView = (ImageView) findViewById(R.id.qrCodeImage);
        final EditText account = (EditText) findViewById(R.id.account);
        final TextView copy = (TextView) findViewById(R.id.copy);
        TextView domainText = (TextView) findViewById(R.id.domainLabel);
        TextView balanceText = (TextView) findViewById(R.id.balance);
        /*
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // landscape
            imageView.setLayoutParams(new LinearLayout.LayoutParams(imageView.getMeasuredHeight(), imageView.getMeasuredHeight()));
        } else {
            // portrait
            imageView.setLayoutParams(new LinearLayout.LayoutParams(imageView.getMeasuredWidth(), imageView.getMeasuredWidth()));
        }
        */
        if(extras!=null) {
            address = extras.getString(ADDRESS);
            //address="0x8d12a197cb00d4747a1fe03395095ce2a5cc6819";
            domain = extras.getString(DOMAIN);
            account.setText(address);
            if(domain!=null){
                domainText.setText(domain);
            }
            CheckBalanceTask mBalanceTask = new CheckBalanceTask(this, address, balanceText,"");
            mBalanceTask.execute((Void) null);
        }
        ViewTreeObserver vto = imageView.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                imageView.getViewTreeObserver().removeOnPreDrawListener(this);
                int h = imageView.getMeasuredHeight();
                int w = imageView.getMeasuredWidth();
                if(address!=null){
                    try {
                        Bitmap bitmap = encodeAsBitmap(address, AccountDetailsActivity.this);
                        imageView.setImageBitmap(getResizedBitmap(bitmap, (w>h)?h:w, (w>h)?h:w));
                    } catch (WriterException e) {
                        showAlertDialog("",e.getMessage());
                    } catch (IllegalArgumentException e){
                        showAlertDialog("",e.getMessage(), new HandleExceptionListener(AccountDetailsActivity.this, e.getMessage()));
                    }
                }
                return true;
            }
        });
        if(savedInstanceState!=null) {
            unlocked = savedInstanceState.getBoolean(PK_UNLOCK_STATUS);
        }
        if(unlocked){
            this.privateKey = savedInstanceState.getString(PK_CONTENT);
            setImageViewContent(this.privateKey);
            TextView pkContent = (TextView) findViewById(R.id.pkContent);
            pkContent.setText(privateKey);
        }
        final ImageView idIcon = (ImageView) findViewById(R.id.idIcon);
        ViewTreeObserver vtoId = idIcon.getViewTreeObserver();
        vtoId.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                idIcon.getViewTreeObserver().removeOnPreDrawListener(this);
                idIcon.setImageBitmap(Blockies.createIcon(8, address, 12));
                return true;
            }
        });
        idIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enterDomain(account);
            }
        });
        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickCopyAddress(copy);
            }
        });
        final ListView balanceTokenList = (ListView) findViewById(R.id.balanceToken);
        TextView emptyText = (TextView)findViewById(android.R.id.empty);
        balanceTokenList.setEmptyView(emptyText);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    EthplorerResponse ethplorerResponse = getTokenBalanceList(address);
                    if(ethplorerResponse==null){
                        return;
                    }
                    List<Token> tokens = ethplorerResponse.getTokens();
                    if(tokens!=null && tokens.size()>0) {
                        final ListAdapter adapter = new TokenItemAdaptor<List<Token>>(AccountDetailsActivity.this, R.layout.list_token_balance, tokens);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                balanceTokenList.setAdapter(adapter);
                                setListViewHeightBasedOnChildren(balanceTokenList);
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        });
        thread.start();

    }

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        // Make sure to call the super method so that the states of our views are saved
        super.onSaveInstanceState(savedInstanceState);
        // Save our own state now
        if(unlocked) {
            savedInstanceState.putBoolean(PK_UNLOCK_STATUS, unlocked);
            savedInstanceState.putString(PK_CONTENT, privateKey);
        }
    }

    public void onClickPKUnlock(View view) {
        if(unlocked){
            ImageView lockIcon = (ImageView) findViewById(R.id.lockIcon);
            lockIcon.setImageResource(R.drawable.lock);
            final ImageView privateKeyQRCode = (ImageView) findViewById(R.id.pKRQCodeImage);
            privateKeyQRCode.setImageResource(R.drawable.lock);
            privateKeyQRCode.setVisibility(View.GONE);
            TextView pkContent = (TextView) findViewById(R.id.pkContent);
            pkContent.setText(null);
            pkContent.setVisibility(View.GONE);
            TextView lockButton = (TextView) findViewById(R.id.showPkQRCode);
            lockButton.setText(getString(R.string.show));
            findViewById(R.id.pkWarning).setVisibility(View.GONE);
            unlocked=false;
            return;
        }
        //SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
        Set<String> savedKeys = preferences.getStringSet("keys", new HashSet<String>());
        String key = null;
        for(int i=0; i<savedKeys.size();i++){
            try {
                key = String.valueOf(savedKeys.toArray()[i]);
                String address = prependHexPrefix(new JSONObject(key).getString("address"));
                if(this.address.equalsIgnoreCase(address)){
                    Intent chooseAddressAndEthValue = new Intent(this, PrivateKeyContentPasswordActivity.class);
                    chooseAddressAndEthValue.putExtra(PrivateKeyContentPasswordActivity.KEY_CONTENT, key);
                    startActivityForResult(chooseAddressAndEthValue, PrivateKeyContentPasswordActivity.PRIVATE_KEY);
                    break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == PrivateKeyContentPasswordActivity.PRIVATE_KEY && resultCode == RESULT_OK) {
            unlocked = true;
            Bundle extras = intent.getExtras();
            this.privateKey = (String) extras.get(PrivateKeyContentPasswordActivity.PRIVATE_KEY_CONTENT);
            setImageViewContent(this.privateKey);
            return;
        }
    }

    private void setImageViewContent(final String privateKey){
        final ImageView privateKeyQRCode = (ImageView) findViewById(R.id.pKRQCodeImage);
        ImageView lockIcon = (ImageView) findViewById(R.id.lockIcon);
        lockIcon.setImageResource(R.drawable.unlock);
        privateKeyQRCode.setVisibility(View.VISIBLE);
        ViewTreeObserver vto = privateKeyQRCode.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                privateKeyQRCode.getViewTreeObserver().removeOnPreDrawListener(this);
                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int w = size.x;
                int h = size.y;

                if(privateKey!=null){
                    try {
                        Bitmap bitmap = encodeAsBitmap(privateKey, AccountDetailsActivity.this);
                        privateKeyQRCode.setImageBitmap(getResizedBitmap(bitmap, (w>h)?h:w, (w>h)?h:w));
                    } catch (WriterException e) {
                        e.printStackTrace();
                    }
                    ScrollView sv = (ScrollView)findViewById(R.id.scrollView);
                    sv.scrollTo(0, sv.getBottom());
                    TextView lockButton = (TextView) findViewById(R.id.showPkQRCode);
                    lockButton.setText(getString(R.string.hide));
                    findViewById(R.id.pkWarning).setVisibility(View.VISIBLE);
                }
                return true;
            }
        });
        TextView pkContent = (TextView) findViewById(R.id.pkContent);
        pkContent.setText(privateKey);
        pkContent.setVisibility(View.VISIBLE);
    }

    public void enterDomain(View view) {
        final EditText name = ((EditText)view);
        name.setEnabled(true);
        name.setSelected(true);
        name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                Thread thread = new Thread(new CheckDomain(AccountDetailsActivity.this, editable.toString().replaceAll(".eth","")) {

                    @Override
                    public void refreshUI() throws InterruptedException, TextParseException, UnknownHostException {
                        if(name.getText()!=null && !name.getText().toString().startsWith("0x")){
                            Boolean isAvailable = isAvailable();
                            if(isAvailable!=null && !isAvailable) {
                                String resolved = getResolved();
                                if (resolved != null && resolved.equalsIgnoreCase(address)) {
                                    name.setEnabled(false);
                                    name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                                    updateKeyName(address, name.getText().toString()+DOT_ETH);
                                }
                            }
                        }
                    }
                });
                thread.start();
            }
        });
    }


    public void onClickCopyAddress(TextView view) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Ether address", address);
        clipboard.setPrimaryClip(clip);
        showMessage(getString(R.string.address_copied));
    }
}
