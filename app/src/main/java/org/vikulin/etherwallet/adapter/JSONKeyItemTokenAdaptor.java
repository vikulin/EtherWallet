package org.vikulin.etherwallet.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.vikulin.etherwallet.FullScreenActivity;
import org.vikulin.etherwallet.R;
import org.vikulin.etherwallet.adapter.pojo.Token;
import org.vikulin.etherwallet.cache.TokenIconCache;
import org.vikulin.etherwallet.icon.Blockies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;

import static org.web3j.utils.Numeric.prependHexPrefix;

/**
 * Created by vadym on 01.12.16.
 */

public class JSONKeyItemTokenAdaptor extends BaseExpandableListAdapter {

    Map<String, String> tokenIcons = new HashMap<String, String>() {{
        put("eos","eos_28");
        put("trx","tronlab_28");
        put("qtm","qtum_28");
        put("omg","omise");
        put("ppt","populous_28");
        put("bnb","binance_28");
        put("snt","status");
        put("mkr","mkr-etherscan-35");
        put("rep","augur");
        put("zrx","0xtoken_28");
        put("dgd","digix-logo");
        put("knc","kyber28");
        put("bat","bat");
        put("qash","qash_28");
        put("gnt","golem");
        put("ethos","ethos_28");
        put("fun","funfair");
        put("salt","salt_28");
        put("bnt","bancor");
        put("tenx","tenx_28");
        put("qsp","quantstamp28");
        put("req","request_28");
        put("icn","ICONOMI");
        put("gno","gnosis");
        put("cvc","civic_28");
        put("rdn","raiden28");
        put("storj","storj2");
        put("ant","aragon_28");
        put("mana","decentraland_28");
        put("enj","enjincoin_28");
        put("rlc","iexec_28");
        put("san","san_28");
        put("mco","Monaco");
        put("rcn","ripio_28");
        put("ast","airswap_28");
        put("sngls","sngls");
        put("mtl","metalpay_28");
        put("amb","ambrosus_28");
        put("viu","viuly_28");
        put("edg","edgeless");
        put("mln","melonport_28");
        put("wings","wingsdao");
        put("dnt","district0x");
        put("cdt","coindash_28");
        put("taas","TAAS");
        put("adt","adtoken");
        put("1st","firstblood");
        put("lun","lunyr_28");
        put("tkn","tokencard");
        put("hmq","humaniq");
        put("cfi","cofoundit");
        put("trst","wetrust");
        put("nmr","numeraire");
        put("guppy","Matchpool");
        put("bcap","bcap");
        put("swt","SwarmCity");
        put("hgt","hellogold_28");
        put("plu","Pluton");
        put("xaur","xaurum2_28");
        put("dice","etheroll");
        put("time","Chronobank");
        put("vsl","vslice");
        put("ind","indorse_etherscan");
        put("fyn","FundYourselfNow");
        put("ngc","naga");
    }};

    private String getTokenIconName(String tokenSymbol){
        if(tokenSymbol==null){
            return null;
        }
        return tokenIcons.get(tokenSymbol.toLowerCase());
    }

    private Context context;
    private int layoutResourceId;
    protected List<JSONObject> walletList = null;
    private List<String> addressList;
    private List<List<Token>> tokenList;

    public JSONKeyItemTokenAdaptor(final Context context, int layoutResourceId, List<JSONObject> walletList, List<List<Token>> tokenList) throws JSONException {
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        this.walletList = walletList;
        this.tokenList = tokenList;
        this.addressList = new ArrayList<>();
        for(JSONObject d: walletList){
            final String address = prependHexPrefix(d.getString("address"));
            addressList.add(address);
        }
    }

    public List<String> getAddressList(){
        return addressList;
    }

    public void setTokenList(int walletIndex, List<Token> tokenList){
        Collections.sort(tokenList);
        this.tokenList.set(walletIndex, tokenList);
    }

    public void setWalletList(List<JSONObject> walletList) throws JSONException {
        this.walletList = walletList;
        this.addressList.clear();
        for(JSONObject d: walletList){
            final String address = prependHexPrefix(d.getString("address"));
            this.addressList.add(address);
        }
    }

    public void removeGroup(int i) {
        this.addressList.remove(i);
        this.walletList.remove(i);
        this.tokenList.remove(i);
    }

    public void addEmptyTokenList(int index){
        this.tokenList.add(index, new ArrayList());
    }

    public void resetTokenList(int size){
        this.tokenList.clear();
        this.tokenList = new ArrayList(Collections.nCopies(size, new ArrayList()));
    }

    public JSONObject getWallet(int index){
        return this.walletList.get(index);
    }

    @Override
    public int getGroupCount() {
        return this.addressList.size();
    }

    @Override
    public int getChildrenCount(int walletIndex) {
        return this.tokenList.get(walletIndex).size();
    }

    @Override
    public Object getGroup(int i) {
        return this.addressList.get(i);
    }

    @Override
    public Object getChild(int i, int i1) {
        return this.tokenList.get(i).get(i1);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 100*childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int position, boolean b, View row, ViewGroup parent) {

        WalletHolder holder = null;
        JSONObject wallet = this.walletList.get(position);
        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            holder = new WalletHolder();
            holder.imgIcon = (ImageView)row.findViewById(R.id.idIcon);
            holder.address = (TextView)row.findViewById(R.id.address);
            row.setTag(holder);
        } else {
            holder = (WalletHolder)row.getTag();
        }
        try {
            String address = addressList.get(position);
            boolean isNamePresent=false;
            try {
                isNamePresent = wallet.getString("key_name").length()>0;
            } catch (JSONException e) {
                isNamePresent=false;
            }
            /*
            Random rand = new Random();
            //25% probability to show a tip
            int n = rand.nextInt(5);
            if(!isNamePresent && n==0) {
                ((FullScreenActivity)context).showTooltip(holder.address, context.getString(R.string.buy_domain_tooltip), R.layout.payment_tooltip_layout);
            }*/
            if(isNamePresent) {
                holder.address.setText(wallet.getString("key_name"));
                holder.address.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            } else {
                holder.address.setText(address);
            }
            holder.imgIcon.setImageBitmap(Blockies.createIcon(8, address, 12));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return row;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {

        WalletHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_token_balance, parent, false);
            holder = new WalletHolder();
            holder.imgIcon = (ImageView)convertView.findViewById(R.id.idIcon);
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.balance = (TextView) convertView.findViewById(R.id.balance);
            holder.tokenSymbol = (TextView) convertView.findViewById(R.id.tokenSymbol);
            convertView.setTag(holder);
        } else {
            holder = (WalletHolder) convertView.getTag();
        }

        final Token token = tokenList.get(groupPosition).get(childPosition);
        holder.name.setText(token.getTokenInfo().getName());
        double balance = token.getBalance()/Math.pow(10.0d,token.getTokenInfo().getDecimals());
        holder.balance.setText(((FullScreenActivity)context).REAL_FORMATTER.format(balance));
        holder.tokenSymbol.setText(token.getTokenInfo().getSymbol());
        if(token.getTokenInfo().getSymbol().equals("ETH")){
            holder.imgIcon.setImageDrawable(this.context.getResources().getDrawable(R.drawable.ether));
            return convertView;
        }
        final WalletHolder finalHolder = holder;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Bitmap tokenIcon = getTokenIcon(context, token.getTokenInfo().getSymbol());
                    ((FullScreenActivity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            finalHolder.imgIcon.setImageBitmap(tokenIcon);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }

    static class WalletHolder
    {
        ImageView imgIcon;
        TextView address;
        TextView name;
        TextView balance;
        TextView tokenSymbol;

    }


    private Bitmap getTokenIcon(final Context context, String tokenSymbol) throws IOException {
        if(TokenIconCache.getInstance(context).contains(tokenSymbol)) {
            return TokenIconCache.getInstance(context).get(tokenSymbol);
        }
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        String tokenIconName = getTokenIconName(tokenSymbol);
        if(tokenIconName==null){
            return null;
        }
        HttpGet getRequest = new HttpGet("https://etherscan.io/token/images/"+getTokenIconName(tokenSymbol)+".png");
        getRequest.addHeader("accept", "application/json");
        HttpResponse response = httpClient.execute(getRequest);
        int status = response.getStatusLine().getStatusCode();
        if (status != 200) {
            //showAlertDialog("","ethplorer.io service is unavailable. Returned status:"+status, new HandleExceptionListener(this, "ethplorer.io service is unavailable. Returned status:"+status));
            return null;
        }

        final Bitmap bitmap = BitmapFactory.decodeStream(response.getEntity().getContent());
        TokenIconCache.getInstance(context).put(context, tokenSymbol, new BitmapDrawable(context.getResources(), bitmap).getBitmap());
        httpClient.close();
        return bitmap;
    }


}