package org.vikulin.etherwallet;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.acra.ACRA;
import org.json.JSONException;
import org.json.JSONObject;
import org.vikulin.etherwallet.adapter.JSONKeyItemTokenAdaptor;
import org.vikulin.etherwallet.adapter.pojo.EtherscanBalance;
import org.vikulin.etherwallet.adapter.pojo.EtherscanBalanceListResponse;
import org.vikulin.etherwallet.adapter.pojo.EthplorerResponse;
import org.vikulin.etherwallet.adapter.pojo.Token;
import org.vikulin.etherwallet.adapter.pojo.TokenInfo;
import org.vikulin.etherwallet.backup.SharedPreferencesBackupAgent;
import org.vikulin.etherwallet.comparator.JSONKeyObject;
import org.vikulin.etherwallet.listener.HandleExceptionListener;
import org.vikulin.etherwallet.task.CheckBalanceTask;
import org.vikulin.etherwallet.task.CheckDomain;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.WalletNativeUtils;
import org.web3j.utils.Convert;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;
import it.sephiroth.android.library.tooltip.Tooltip;

import static org.vikulin.etherwallet.DrawerActivity.LANGUAGE;
import static org.vikulin.etherwallet.DrawerActivity.OPEN_DRAWER;
import static org.vikulin.etherwallet.DrawerActivity.SEND_ETHER_ACTIVITY;
import static org.vikulin.etherwallet.TransactionListActivity.ETHERSCAN_API_KEY;
import static org.web3j.utils.Numeric.cleanHexPrefix;
import static org.web3j.utils.Numeric.prependHexPrefix;

public class AccountListActivity extends AddressListActivity {


    private static final String STATE_GENERATING = "state_generating";

    private View createAccount;
    private String selectedAddress;
    private String selectedDomain;
    private ExpandableListView lv;
    private static final String DOMAIN_CONTRACT_ADDRESS = "0x3fb496e391ec71d369522d4dc2bf880d507d54fd";
    public static final String DOT_ETH=".eth";
    private TextView addressEditText;
    private boolean generating = false;
    private BigDecimal totalBalance;
    private TextSwitcher totalBalanceText;


    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        // Make sure to call the super method so that the states of our views are saved
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(STATE_GENERATING, generating);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_list);
        if(savedInstanceState!=null) {
            generating = savedInstanceState.getBoolean(STATE_GENERATING);
            if(generating){
                progressDialog = showProgress();
            }
        }
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        totalBalanceText = (TextSwitcher) findViewById(R.id.balanceTotal);
        totalBalanceText.setFactory(new  TextSwitcher.ViewFactory (){

            @Override
            public View makeView() {
                LayoutInflater inflater = AccountListActivity.this.getLayoutInflater();
                View view = inflater.inflate(R.layout.balance_textview, null);
                return view;
            }
        });
        //accountDetails = findViewById(R.id.account_details);
        createAccount = findViewById(R.id.new_account);
        lv = (ExpandableListView) findViewById(R.id.account_list);
        Set<String> savedKeys = preferences.getStringSet("keys", null);
        if(savedKeys==null){
            savedKeys = new HashSet<>();
        }
        final List<JSONObject> jsonObjectKeys = new ArrayList<>(savedKeys.size());
        for(String key:savedKeys){
            try {
                jsonObjectKeys.add(new JSONObject(key));
            } catch (JSONException e) {
                showAlertDialog("",e.getMessage());
            }
        }
        Collections.sort(jsonObjectKeys, JSONKeyObject.JSONObjectNameComparator);
        JSONKeyItemTokenAdaptor adapter=null;
        final FloatingActionButton addWallet = (FloatingActionButton) findViewById(R.id.add_wallet);
        try {
            adapter = new JSONKeyItemTokenAdaptor(this, R.layout.list_wallet_key, jsonObjectKeys, new ArrayList(Collections.nCopies(jsonObjectKeys.size(), new ArrayList())));
            lv.setAdapter(adapter);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        if(adapter.getGroupCount()==0){
            showTooltip(addWallet, getString(R.string.create), Tooltip.Gravity.TOP, R.layout.tooltip_layout);
        } else {
            List<String> addressList = adapter.getAddressList();
            setTotalBalance(addressList, totalBalanceText);
            try {
                showTokenList(jsonObjectKeys);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        lv.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener(){

            @Override
            public boolean onGroupClick(ExpandableListView expandableListView, View view, int i, long l) {
                view.setPressed(true);
                createAccount.setVisibility(View.GONE);
                JSONKeyItemTokenAdaptor adapter = (JSONKeyItemTokenAdaptor)lv.getExpandableListAdapter();
                String address= prependHexPrefix(adapter.getGroup(i).toString());
                AccountListActivity.this.selectedAddress = address;
                try {
                    AccountListActivity.this.selectedDomain = adapter.getWallet(i).getString("key_name");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                /**TODO refresh balance in token list
                mBalanceTask = new CheckBalanceTask(AccountListActivity.this, address, balanceText, "ETH");
                mBalanceTask.execute((Void) null);
                */
                FrameLayout openAccountDetails = (FrameLayout) findViewById(R.id.open_account_details);

                addWallet.setVisibility(View.GONE);
                openAccountDetails.setVisibility(View.VISIBLE);
                return false;
            }
        });

        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int i, final long id) {

                // if child is long-clicked
                if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
                    showConfirmDialog(getString(R.string.confirm), getString(R.string.delete_wallet), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int j) {
                            Set<String> savedKeys = preferences.getStringSet("keys", new HashSet<String>());
                            try {
                                String selectedAddress = lv.getExpandableListAdapter().getGroup(i).toString();
                                for (String key : savedKeys) {
                                    String address = prependHexPrefix(new JSONObject(key).getString("address"));
                                    if (selectedAddress.equalsIgnoreCase(address)) {
                                        savedKeys.remove(key);
                                        break;
                                    }
                                }
                            } catch (JSONException e) {
                                ACRA.getErrorReporter().handleSilentException(e);
                                return;
                            } catch (IndexOutOfBoundsException e) {
                                ACRA.getErrorReporter().handleSilentException(new Exception("Detected an attempt to get an item from size adapter=" + lv.getAdapter().getCount() + " index=" + i));
                                return;
                            }
                            JSONKeyItemTokenAdaptor adapter = (JSONKeyItemTokenAdaptor) lv.getExpandableListAdapter();
                            adapter.removeGroup(i);
                            preferences.edit().remove("keys").apply();
                            preferences.edit().putStringSet("keys", savedKeys).commit();
                            adapter.notifyDataSetChanged();
                            Thread oneSignalThread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        deleteAndUpdateOneSignalTags();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            oneSignalThread.start();
                        }
                    }, null);
                    return true;
                }

                return false;
            }
        });
        //workaround for 429 HTTP error code
        //for ( int i = 0; i < adapter.getGroupCount(); i++ ) {
        //    lv.expandGroup(i);
        //}
    }

    private EtherscanBalanceListResponse getBalances(List<String> addresses) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        String url = "https://api.etherscan.io/api?module=account&action=balancemulti&address=";
        Iterator<String> iterator = addresses.iterator();
        while (iterator.hasNext()) {
            url += iterator.next() + ",";
        }
        url = url.substring(0, url.length()-1) + "&tag=latest&apikey="+ETHERSCAN_API_KEY; // remove last , AND add token
        HttpGet getRequest = new HttpGet(url);
        getRequest.addHeader("accept", "application/json");
        HttpResponse response = httpClient.execute(getRequest);
        int status = response.getStatusLine().getStatusCode();
        if (status != 200) {
            showAlertDialogOnUiThread("","Etherscan service is unavailable. Returned status:"+status, new HandleExceptionListener(this, "Etherscan service is unavailable. Returned status:"+status));
            return null;
        }
        BufferedReader br = new BufferedReader(
                new InputStreamReader((response.getEntity().getContent())));
        String content = read(br);
        Gson gson = new GsonBuilder().create();
        EtherscanBalanceListResponse etherscanBalanceListResponse = null;
        try {
            etherscanBalanceListResponse = gson.fromJson(content, EtherscanBalanceListResponse.class);
        } catch (JsonSyntaxException e){
            showAlertDialogOnUiThread("",e.getMessage()+"\n"+content+"\n"+url);
        } finally {
            br.close();
            httpClient.close();
        }
        return etherscanBalanceListResponse;
    }

    private void setTotalBalance(final List<String> addresses, final TextSwitcher totalBalanceText){
        if(addresses!=null && addresses.size()>0) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        EtherscanBalanceListResponse etherscanBalanceListResponse = getBalances(addresses);
                        if (etherscanBalanceListResponse == null) {
                            return;
                        }
                        List<EtherscanBalance> result = etherscanBalanceListResponse.getResult();
                        if (result == null) {
                            result = new ArrayList();
                        }
                        BigInteger totalBalance = BigInteger.ZERO;
                        for (EtherscanBalance balance : result) {
                            try {
                                totalBalance = totalBalance.add(new BigInteger(balance.getBalance()));
                            } catch (NumberFormatException e) {
                                //fallback to infura.io
                                /* TODO get back to infura
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        CheckTotalBalanceTask mTotalBalanceTask = new CheckTotalBalanceTask(AccountListActivity.this, addresses, balanceText, "ETH");
                                        mTotalBalanceTask.execute((Void) null);
                                    }
                                });
                                */
                                return;
                            }
                        }
                        AccountListActivity.this.totalBalance = Convert.fromWei(totalBalance.toString(10), Convert.Unit.ETHER);
                        DecimalFormat formatter = (DecimalFormat) REAL_FORMATTER.clone();
                        formatter.setMaximumFractionDigits(2);
                        final String balanceText = formatter.format(AccountListActivity.this.totalBalance);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                totalBalanceText.setText(balanceText + " ETH");
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                        showAlertDialogOnUiThread("Error", e.getMessage());
                    }
                }
            });
            thread.start();
        }
    }

    public View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

    @Override
    public void onBackPressed() {
        if(createAccount.getVisibility()==View.VISIBLE){
            createAccount.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    public void addWallet(View view) {
        createAccount.setVisibility(View.VISIBLE);
    }

    public void home(View view) {
        Intent intent = new Intent(this, DrawerActivity.class);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String language = extras.getString(LANGUAGE);
            intent.putExtra(LANGUAGE, language);
        }
        startActivity(intent);
    }

    public void pay(View view) {
        Intent intent = new Intent(this, SendEtherActivity.class);
        this.startActivityForResult(intent, SEND_ETHER_ACTIVITY);
    }

    public void exchange(View view) {
        Intent intent = new Intent(this, DrawerActivity.class);
        intent.putExtra(OPEN_DRAWER, OPEN_DRAWER);
        startActivity(intent);
    }

    public void onClickCreateAccount(View view) {
        progressDialog = showProgress();
        final EditText password = (EditText) findViewById(R.id.passwordText);
        final EditText confirmPassword = (EditText) findViewById(R.id.confirmPasswordText);
        if(!validateFields(password, confirmPassword)){
            hideProgress();
            return;
        }
        final File destination = getExternalCacheDir();
        final String pass = password.getText().toString();
        try {
            AsyncTask newWallet = new AsyncTask<Object, Void, String>(){

                @Override
                protected String doInBackground(Object... voids) {
                    generating = true;
                    String fileName = null;
                    try {
                        fileName = WalletNativeUtils.generateNewWalletFile(pass, destination, true);
                    } catch (CipherException e) {
                        e.printStackTrace();
                        showAlertDialogOnUiThread("", e.getMessage());
                        return null;
                    } catch (IOException e) {
                        e.printStackTrace();
                        showAlertDialogOnUiThread("", e.getMessage());
                        return null;
                    } catch (GeneralSecurityException e) {
                        e.printStackTrace();
                        showAlertDialogOnUiThread("", e.getMessage());
                        return null;
                    }
                    String keyFileContent = null;
                    try {
                        keyFileContent = readFile(new File(destination, fileName));
                    } catch (IOException e) {
                        e.printStackTrace();
                        showAlertDialogOnUiThread("", e.getMessage());
                        return null;
                    }
                    return keyFileContent;
                }

                @Override
                protected void onPostExecute(String keyFileContent) {
                    if(keyFileContent==null){
                        hideProgress();
                        return;
                    }
                    try {
                        String address = importKey(keyFileContent);
                        notifyKeyListAdapter(address);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        showAlertDialog("", e.getMessage());
                    } catch (IOException e) {
                        e.printStackTrace();
                        showAlertDialog("", e.getMessage());
                    }
                    createAccount.setVisibility(View.GONE);
                    password.setText("");
                    confirmPassword.setText("");
                    SharedPreferencesBackupAgent.requestBackup(getApplicationContext());
                    hideProgress();
                    try {
                        updateOneSignalTags();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        showAlertDialog("Error", e.getMessage());
                    }
                }
            };
            newWallet.execute();
        } catch (OutOfMemoryError e){
            showAlertDialog("","Too low free RAM memory!");
            return;
        }

    }

    public void openAccountDetails(View view) {
        Intent intent = new Intent(this, AccountDetailsActivity.class);
        intent.putExtra(AccountDetailsActivity.ADDRESS, this.selectedAddress);
        intent.putExtra(AccountDetailsActivity.DOMAIN, this.selectedDomain);
        startActivity(intent);
    }

    public void notifyKeyListAdapter() throws JSONException {
        Set<String> savedKeys = preferences.getStringSet("keys", new HashSet<String>());
        List<JSONObject> jsonObjectKeys = new ArrayList<>();
        for(String key:savedKeys){
            try {
                jsonObjectKeys.add(new JSONObject(key));
            } catch (JSONException e) {
                showAlertDialog("",e.getMessage());
            }
        }
        Collections.sort(jsonObjectKeys, JSONKeyObject.JSONObjectNameComparator);
        JSONKeyItemTokenAdaptor adapter = (JSONKeyItemTokenAdaptor)lv.getExpandableListAdapter();
        adapter.setWalletList(jsonObjectKeys);
        adapter.resetTokenList(jsonObjectKeys.size());
        showTokenList(jsonObjectKeys);
        adapter.notifyDataSetChanged();
        setTotalBalance(adapter.getAddressList(), totalBalanceText);
    }

    private void showTokenList(List<JSONObject> jsonObjectKeys) throws JSONException {
        int walletIndex = 0;
        for(JSONObject d: jsonObjectKeys){
            final String address = prependHexPrefix(d.getString("address"));
            final int finalWalletIndex = walletIndex;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        EthplorerResponse ethplorerResponse = AccountListActivity.this.getTokenBalanceList(address);
                        if (ethplorerResponse == null) {
                            return;
                        }
                        final List<Token> tokens = ethplorerResponse.getTokens();

                        if(tokens!=null) {
                            Token ether = new Token();
                            TokenInfo ti = new TokenInfo();
                            ti.setSymbol("ETH");
                            ti.setName("Ethereum");
                            ti.setDecimals(0l);
                            ether.setTokenInfo(ti);
                            ether.setBalance(ethplorerResponse.getEth().getBalance());
                            tokens.add(0, ether);
                            AccountListActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    JSONKeyItemTokenAdaptor adapter = (JSONKeyItemTokenAdaptor) lv.getExpandableListAdapter();
                                    adapter.setTokenList(finalWalletIndex, tokens);
                                    adapter.notifyDataSetChanged();
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
            walletIndex++;
        }
    }

    public void notifyKeyListAdapter(String address) throws JSONException {
        Set<String> savedKeys = preferences.getStringSet("keys", new HashSet<String>());
        List<JSONObject> jsonObjectKeys = new ArrayList<>();
        for(String key:savedKeys){
            try {
                jsonObjectKeys.add(new JSONObject(key));
            } catch (JSONException e) {
                showAlertDialog("Error",e.getMessage());
            }
        }
        Collections.sort(jsonObjectKeys, JSONKeyObject.JSONObjectNameComparator);
        int index = 0;

        for (JSONObject o : jsonObjectKeys) {
            if (o.getString("address").equalsIgnoreCase(address)) {
                JSONKeyItemTokenAdaptor adapter = (JSONKeyItemTokenAdaptor) lv.getExpandableListAdapter();
                adapter.setWalletList(jsonObjectKeys);
                adapter.addEmptyTokenList(index);
                showTokenList(index, address);
                adapter.notifyDataSetChanged();
                setTotalBalance(adapter.getAddressList(), totalBalanceText);
                return;
            }
            index++;
        }

    }

    private void showTokenList(final int finalWalletIndex, final String address) throws JSONException {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    EthplorerResponse ethplorerResponse = AccountListActivity.this.getTokenBalanceList(prependHexPrefix(address));
                    if (ethplorerResponse == null) {
                        return;
                    }
                    final List<Token> tokens = ethplorerResponse.getTokens();
                    if(tokens!=null) {
                        Token ether = new Token();
                        TokenInfo ti = new TokenInfo();
                        ti.setSymbol("ETH");
                        ti.setName("Ethereum");
                        ti.setDecimals(0l);
                        ether.setTokenInfo(ti);
                        ether.setBalance(ethplorerResponse.getEth().getBalance());
                        tokens.add(0, ether);
                        AccountListActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                JSONKeyItemTokenAdaptor adapter = (JSONKeyItemTokenAdaptor) lv.getExpandableListAdapter();
                                adapter.setTokenList(finalWalletIndex, tokens);
                                adapter.notifyDataSetChanged();
                                //showInfoDialog("token2","tl:"+tokens.size()+" i1:"+adapter.getChildrenCount(1)+" i0:"+adapter.getChildrenCount(0));
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

    protected boolean validateFields(EditText password, EditText confirmPassword) {
        if (password.getText().toString().trim().length()<9) {
            password.setError(getString(R.string.too_short_password));
            password.requestFocus();
            return false;
        }
        if (confirmPassword.getText().toString().trim().length()<9) {
            confirmPassword.setError(getString(R.string.too_short_password));
            confirmPassword.requestFocus();
            return false;
        }
        if (!password.getText().toString().equals(confirmPassword.getText().toString())) {
            password.setError(getString(R.string.passwords_do_not_match));
            password.requestFocus();
            return false;
        }
        return true;
    }

    private View showPlan(){
        View view = LayoutInflater.from(this).inflate(R.layout.payment_plan_layout,null);
        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setView(view);
        ab.show();
        return view;
    }

    public void showPaymentPlan(View view) {
        View popup = showPlan();
        Set<String> savedKeys = preferences.getStringSet("keys", new HashSet<String>());
        String[] addresses = new String[savedKeys.size()];
        for(int i=0; i<savedKeys.size();i++){
            try {
                addresses[i]=new JSONObject(String.valueOf(savedKeys.toArray()[i])).getString("address");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        addressEditText = (TextView)popup.findViewById(R.id.address);
        String address = preferences.getString(getAddressProperty(), null);
        if(address!=null) {
            int i = Arrays.asList(addresses).indexOf(address);
            if(i>=0){
                addressEditText.setText(address);
                setIcon(address);
            }
        } else {
            if(addresses.length==1){
                addressEditText.setText(addresses[0]);
                setIcon(addresses[0]);
            }
        }
        getPopupWindow(R.layout.spinner_address_item, addresses, addressEditText, getString(R.string.choose_address));
        ((View)view.getParent()).setVisibility(View.GONE);
    }

    protected String getAddressProperty() {
        return DEFAULT_BUY_ADDRESS;
    }

    final String getPrice(String domain){
        return getPaymentPlan(domain).toString();
    }

    final PaymentPlan getPaymentPlan(String domain){
        if(domain.length()==1){
            return new PaymentPlan("19.99","ETH",getString(R.string.year));
        }
        if(domain.length()==2){
            return new PaymentPlan("9.99","ETH",getString(R.string.year));
        }
        if(domain.length()==3){
            return new PaymentPlan("2.99","ETH",getString(R.string.year));
        }
        if(domain.length()==4){
            return new PaymentPlan("0.99","ETH",getString(R.string.year));
        }
        if(domain.length()==5){
            return new PaymentPlan("0.49","ETH",getString(R.string.year));
        }
        return new PaymentPlan("0.09","ETH",getString(R.string.year));
    }

    @Override
    public TextView getWalletListView(){
        return addressEditText;
    }

    private String[] currencySymbols = new String[]{
            "ETH", "BTC", "USD", "EUR", "GBP", "RUB"
    };

    private int currencyIndex=0;

    public void showLeft(View view) {
        if(currencyIndex>0) {
            currencyIndex--;

            Animation slideInLeftAnimation = AnimationUtils.loadAnimation(this,
                    R.anim.slide_in_left);
            Animation slideOutRightAnimation = AnimationUtils.loadAnimation(this,
                    R.anim.slide_out_right);
            totalBalanceText.setInAnimation(slideInLeftAnimation);
            totalBalanceText.setOutAnimation(slideOutRightAnimation);

            setRate(currencySymbols[currencyIndex], totalBalanceText);
        }
    }

    private void setRate(final String currency, final TextSwitcher totalBalanceText){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DecimalFormat formatter = (DecimalFormat) REAL_FORMATTER.clone();
                    formatter.setMaximumFractionDigits(2);
                    Double rate = getRate(currency);
                    if(rate==null || totalBalance==null){
                        return;
                    }
                    final String balanceText = formatter.format(totalBalance.doubleValue()*rate);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            totalBalanceText.setText(balanceText+" "+currency);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    showAlertDialogOnUiThread("",e.getMessage());
                }
            }
        });
        thread.start();
    }

    public void showRight(View view) {
        if(currencyIndex<=4) {
            currencyIndex++;
            Animation slideInLeftAnimation = AnimationUtils.loadAnimation(this,
                    android.R.anim.slide_in_left);
            Animation slideOutRightAnimation = AnimationUtils.loadAnimation(this,
                    android.R.anim.slide_out_right);
            totalBalanceText.setInAnimation(slideInLeftAnimation);
            totalBalanceText.setOutAnimation(slideOutRightAnimation);

            setRate(currencySymbols[currencyIndex], totalBalanceText);
        }
    }

    private Double getRate(String symbol) throws IOException {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        String url = "https://min-api.cryptocompare.com/data/price?fsym=ETH&tsyms="+symbol;
        HttpGet getRequest = new HttpGet(url);
        HttpResponse response = httpClient.execute(getRequest);
        int status = response.getStatusLine().getStatusCode();
        if (status != 200) {
            showAlertDialogOnUiThread("","min-api.cryptocompare.com service is unavailable. Returned status:"+status, new HandleExceptionListener(this, "download.finance.yahoo.com service is unavailable. Returned status:"+status));
            return null;
        }
        BufferedReader br = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));
        String content = read(br);
        Double rate = null;
        try {
            rate = parsePriceConversionRate(content);
        } catch (Exception e){
            showAlertDialogOnUiThread("Error",e.getMessage()+"\n"+content+"\n"+url);
        } finally {
            br.close();
            httpClient.close();
        }
        return rate;
    }

    public static Double parsePriceConversionRate(String response){
        Type type = new TypeToken<Map<String, Double>>(){}.getType();
        Map<String, Double> myMap = gson.fromJson(response, type);
        return myMap.entrySet().iterator().next().getValue();
    }

    static class PaymentPlan{

        public PaymentPlan(String value, String currency, String period){
            this.value = value;
            this.currency = currency;
            this.period = period;
        }
        String value;
        String currency;
        String period;

        public String getValue(){
            return value;
        }

        @Override
        public String toString() {
            return value+" "+currency+"/"+period;
        }
    }

    final void setPaymentControls(View rootView, String domain, Boolean isAvailable) throws InterruptedException {
        TextView domainStatus = (TextView) rootView.findViewById(R.id.domainStatus);
        domainStatus.setVisibility(View.VISIBLE);
        ImageView link = (ImageView) rootView.findViewById(R.id.link);
        link.setVisibility(View.GONE);
        TextView address = (TextView) rootView.findViewById(R.id.address);
        address.setVisibility(View.GONE);
        View delimiter1 = rootView.findViewById(R.id.delimiter1);
        delimiter1.setVisibility(View.GONE);
        View delimiter2 = rootView.findViewById(R.id.delimiter2);
        delimiter2.setVisibility(View.GONE);
        if(isAvailable!=null && isAvailable){
            TextView plan = (TextView) rootView.findViewById(R.id.plan);
            plan.setText(getPrice(domain));
            domainStatus.setTextColor(Color.GREEN);
            domainStatus.setText(R.string.domain_available);
            Thread.sleep(1000l);
            domainStatus.setVisibility(View.GONE);
            Thread.sleep(300l);
            LinearLayout ll = (LinearLayout) rootView.findViewById(R.id.planLayout);
            ll.setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.search).setVisibility(View.GONE);
            rootView.findViewById(R.id.buy).setVisibility(View.VISIBLE);
            return;
        }
        if(isAvailable!=null && !isAvailable){
            domainStatus.setText("");
            Thread.sleep(300l);
            domainStatus.setTextColor(Color.RED);
            domainStatus.setText(getString(R.string.domain_unavailable));
            rootView.findViewById(R.id.search).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.buy).setVisibility(View.GONE);
            return;
        }
        if(isAvailable==null){
            domainStatus.setText("");
            Thread.sleep(300l);
            domainStatus.setTextColor(Color.RED);
            domainStatus.setText(getString(R.string.network_error));
            rootView.findViewById(R.id.search).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.buy).setVisibility(View.GONE);
        }
    }

    private void resetPaymentControls(View rootView){
        LinearLayout ll = (LinearLayout) rootView.findViewById(R.id.planLayout);
        ll.setVisibility(View.GONE);
        TextView domainStatus = (TextView) rootView.findViewById(R.id.domainStatus);
        domainStatus.setVisibility(View.GONE);
        rootView.findViewById(R.id.search).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.buy).setVisibility(View.GONE);
        ImageView link = (ImageView) rootView.findViewById(R.id.link);
        link.setVisibility(View.VISIBLE);
        TextView address = (TextView) rootView.findViewById(R.id.address);
        address.setVisibility(View.VISIBLE);
        View delimiter1 = rootView.findViewById(R.id.delimiter1);
        delimiter1.setVisibility(View.VISIBLE);
        View delimiter2 = rootView.findViewById(R.id.delimiter2);
        delimiter2.setVisibility(View.VISIBLE);
    }

    public void checkDomain(View view) {

        final View rootView = view.getRootView();
        final EditText domain = (EditText) rootView.findViewById(R.id.domain);
        final TextView address = getWalletListView();

        if(domain.getText()==null || domain.getText().length()==0){
            domain.setError(getString(R.string.choose_address));
            return;
        }
        if(address.getText()==null || address.getText().length()==0){
            address.setError(getString(R.string.choose_address));
            return;
        }
        Thread thread = new Thread(new CheckDomain(this, domain.getText().toString()) {

            @Override
            public void refreshUI() throws InterruptedException {
                setPaymentControls(rootView, getValue(), isAvailable());
            }
        });
        thread.start();
        domain.addTextChangedListener(new TextWatcher(){

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                resetPaymentControls(rootView);
            }
        });
    }

    public void buyDomain(View view) {
        final View rootView = view.getRootView();
        final EditText domain = (EditText) rootView.findViewById(R.id.domain);
        String value = domain.getText().toString()+DOT_ETH;
        Intent intent = new Intent(this, PaymentPlanActivity.class);
        intent.putExtra(PaymentPlanActivity.ADDRESS_TO, DOMAIN_CONTRACT_ADDRESS);
        intent.putExtra(PaymentPlanActivity.PAYMENT_VALUE, getPaymentPlan(domain.getText().toString()).getValue());
        intent.putExtra(PaymentPlanActivity.DOMAIN, value);
        intent.putExtra(PaymentPlanActivity.LINKED_ADDRESS, getWalletListView().getText().toString());
        startActivityForResult(intent, PaymentPlanActivity.DOMAIN_PAYMENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(requestCode==PaymentPlanActivity.DOMAIN_PAYMENT && resultCode==RESULT_OK){
            Bundle extras = intent.getExtras();
            String address = cleanHexPrefix(extras.getString(PaymentPlanActivity.LINKED_ADDRESS));
            String keyName = extras.getString(PaymentPlanActivity.DOMAIN);
            updateKeyName(address, keyName);
            finish();
            startActivity(getIntent());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            notifyKeyListAdapter();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
