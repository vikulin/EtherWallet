package org.vikulin.etherwallet.adapter;

/**
 * Created by vadym on 30.07.17.
 */

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.vikulin.etherwallet.AccountDetailsActivity;
import org.vikulin.etherwallet.AddressListActivity;
import org.vikulin.etherwallet.FullScreenActivity;
import org.vikulin.etherwallet.R;
import org.vikulin.etherwallet.adapter.pojo.Token;
import org.vikulin.etherwallet.cache.TokenIconCache;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClientBuilder;

/**
 * Created by vadym on 01.12.16.
 */

public class TokenItemAdaptor<L extends List<?>> extends ArrayAdapter<Token> {

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
    }};

    private String getTokenIconName(String tokenSymbol){
        if(tokenSymbol==null){
            return null;
        }
        return tokenIcons.get(tokenSymbol.toLowerCase());
    }

    private Context context;
    private int layoutResourceId;
    protected List<Token> data = null;

    public TokenItemAdaptor(Context context, int layoutResourceId, List<Token> data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.context = context;
        Collections.sort(data);
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        TokenHolder holder = null;
        final Token token = data.get(position);
        if(row == null) {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            holder = new TokenHolder();
            holder.imgIcon = (ImageView)row.findViewById(R.id.idIcon);
            holder.name = (TextView) row.findViewById(R.id.name);
            holder.balance = (TextView)row.findViewById(R.id.balance);
            holder.tokenSymbol = (TextView)row.findViewById(R.id.tokenSymbol);
            row.setTag(holder);
        } else {
            holder = (TokenHolder)row.getTag();
        }
        holder.name.setText(token.getTokenInfo().getName());
        double balance = token.getBalance()/Math.pow(10.0d,token.getTokenInfo().getDecimals());
        holder.balance.setText(((FullScreenActivity)context).REAL_FORMATTER.format(balance));
        holder.tokenSymbol.setText(token.getTokenInfo().getSymbol());

        final TokenHolder finalHolder = holder;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap tokenIcon = null;
                try {
                    tokenIcon = getTokenIcon(context,token.getTokenInfo().getSymbol());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(tokenIcon!=null) {
                    final Bitmap finalTokenIcon = tokenIcon;
                    ((AccountDetailsActivity)context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            finalHolder.imgIcon.setImageBitmap(finalTokenIcon);
                        }
                    });
                }
            }
        });
        thread.start();

        return row;
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

    static class TokenHolder {
        ImageView imgIcon;
        TextView name;
        TextView tokenSymbol;
        TextView balance;
    }
}