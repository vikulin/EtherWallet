package org.vikulin.etherwallet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.google.zxing.WriterException;

import org.json.JSONException;
import org.json.JSONObject;
import org.vikulin.etherwallet.adapter.SellItemListAdapter;
import org.vikulin.etherwallet.adapter.pojo.SellItem;
import org.web3j.crypto.CipherException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.vikulin.etherwallet.ShowCodeActivity.encodeAsBitmap;
import static org.vikulin.etherwallet.ShowCodeActivity.getResizedBitmap;

/**
 * Created by vadym on 09.12.16.
 */

public abstract class BuySellActivity extends AddressListActivity {

    private static final String STATE_ITEMS = "items";
    private TextView addressEditText;
    private SellItemListAdapter adapter;
    //private SharedPreferences preferences;
    protected abstract String getAddressProperty();
    protected abstract boolean validateFields();
    protected abstract void process() throws JSONException, CipherException, IOException;
    protected abstract int getContentView();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentView());
        //preferences = PreferenceManager.getDefaultSharedPreferences(this.getBaseContext());
        Set<String> savedKeys = preferences.getStringSet("keys", new HashSet<String>());
        String[] addresses = new String[savedKeys.size()];
        for(int i=0; i<savedKeys.size();i++){
            try {
                addresses[i]=new JSONObject(String.valueOf(savedKeys.toArray()[i])).getString("address");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        addressEditText = (TextView)findViewById(R.id.address);
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
        ImageButton next = (ImageButton) findViewById(R.id.next);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Show bill
                try {
                    process();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (CipherException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        ListView itemList = (ListView) findViewById(R.id.itemList);
        itemList.setTextFilterEnabled(true);
        Object savedItems = null;
        ArrayList<SellItem> listSellItem;
        if(savedInstanceState!=null) {
            savedItems = savedInstanceState.getSerializable(STATE_ITEMS);
        }
        if(savedItems!=null && ((ArrayList)savedItems).size()>0){
            listSellItem = (ArrayList)savedItems;
        } else {
            listSellItem = new ArrayList<>();
        }
        adapter = new SellItemListAdapter<>(this, R.layout.list_sell_item, listSellItem);
        itemList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            }
        });
        itemList.setAdapter(adapter);
        if(savedItems!=null && ((ArrayList)savedItems).size()>0){
            adapter.notifyDataSetChangedSilently();
        }
        if(savedKeys==null || savedKeys.size()==0){
            importKey();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        // Make sure to call the super method so that the states of our views are saved
        super.onSaveInstanceState(savedInstanceState);
        // Save our own state now
        if(adapter.getData()!=null) {
            savedInstanceState.putSerializable(STATE_ITEMS, (ArrayList) adapter.getData());
        }
    }

    public SellItemListAdapter getAdapter(){
        return adapter;
    }

    public TextView getWalletListView(){
        return addressEditText;
    }

    public SharedPreferences getSharedPreferences(){
        return preferences;
    }

    /**
     * implemented in super class
    public void onClickChooseAddress(View view) {
        if(super.getAdapter().getCount()==0){
            importKey();
        }
    }
    */

    public void onClickUpDown(View view) {
        if(getAdapter().getCount()>0){
            View footerSend = findViewById(R.id.footerSend);
            if(footerSend.getVisibility()==View.INVISIBLE) {
                ((ImageButton)view).setImageResource(R.drawable.down);
                footerSend.setVisibility(View.VISIBLE);
            } else {
                ((ImageButton)view).setImageResource(R.drawable.up);
                footerSend.setVisibility(View.INVISIBLE);
            }
        }
    }

    public void sendInvoiceByEmail(View view) {
        String[] TO = {""};
        String[] CC = {""};
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("text/html");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
        emailIntent.putExtra(Intent.EXTRA_CC, CC);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "EtherWallet - " + getString(R.string.shopping_list));
        emailIntent.putExtra(Intent.EXTRA_TEXT, getEmailPlainText());
        try {
            emailIntent.putExtra(Intent.EXTRA_STREAM, getQRCodeAttachment());
            emailIntent.setType("image/jpg");
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (WriterException e) {
            e.printStackTrace();
        }
        //emailIntent.putExtra(Intent.EXTRA_HTML_TEXT, getEmailHTMLText());
        try {
            startActivity(Intent.createChooser(emailIntent,""));
        } catch (android.content.ActivityNotFoundException ex) {
            showAlertDialog("","There is no email client installed.");
        }
    }

    public String getSellData() throws JSONException {
        JSONObject sellData = getAdapter().getSellItems();
        String paymentAddress = getWalletListView().getText().toString();
        sellData.put("a", paymentAddress);
        //generate transaction id
        String transactionId = UUID.randomUUID().toString().replace("-","");
        sellData.put("id", transactionId);
        return sellData.toString();
    }

    private Uri getQRCodeAttachment() throws JSONException, WriterException {
        Bitmap bitmap = getResizedBitmap(encodeAsBitmap(getSellData(), this), 300, 300);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "QR-code", "QR-code");
        Uri qrCodeURI = Uri.parse(path);
        return qrCodeURI;
    }

    private String getEmailPlainText() {
        StringBuilder sb = new StringBuilder();
        List<SellItem> sellItems = getAdapter().getData();
        sb.append(getString(R.string.name)).append(" ").append(getString(R.string.price)).append("\n\n");
        for(int j=0;j<sellItems.size();j++){
            sb.append(sellItems.get(j).getName());
            sb.append(" ");
            sb.append(SellItemListAdapter.REAL_FORMATTER.format(sellItems.get(j).getPrice()));
            sb.append("\n");
        }
        sb.append(getString(R.string.total)).append(" ").append(SellItemListAdapter.REAL_FORMATTER.format(getAdapter().getTotal()));
        sb.append("\n");
        sb.append("\n");
        sb.append(getString(R.string.scan_sell_item_help));
        return sb.toString();
    }

    private String getEmailHTMLText() {
        Map<String, String> map = new LinkedHashMap();
        map.put("NAME", "PRICE");
        List<SellItem> sellItems = getAdapter().getData();
        for(int j=0;j<sellItems.size();j++){
            map.put(sellItems.get(j).getName(), SellItemListAdapter.REAL_FORMATTER.format(sellItems.get(j).getPrice()));
        }

        return addStyle(getBodyHtml(map));
    }

    private String getBodyHtml(Map<String, String> map){
        StringBuilder htmlBuilder = new StringBuilder();
        htmlBuilder.append("<table>");
        htmlBuilder.append("<tr class=\"head\">\n" +
                "<th style=\"padding: 5px 30px 5px 10px;border-spacing: 0px;font-size: 90%;margin: 0px;text-align: center;background-color: #90b4d6;border-top: 1px solid #90b4d6;border-bottom: 2px solid #547ca0;border-right: 1px solid #749abe;color: #fff;text-shadow: -1px -1px 1px #666666;letter-spacing: 0.15em;-webkit-border-top-left-radius: 5px;-moz-border-radius-topleft: 5px;border-top-left-radius: 5px;\"></th>\n" +
                "<th style=\"padding: 5px 30px 5px 10px;border-spacing: 0px;font-size: 90%;margin: 0px;text-align: center;background-color: #90b4d6;border-top: 1px solid #90b4d6;border-bottom: 2px solid #547ca0;border-right: 1px solid #749abe;color: #fff;text-shadow: -1px -1px 1px #666666;letter-spacing: 0.15em;-webkit-border-top-right-radius: 5px;-moz-border-radius-topright: 5px;border-top-right-radius: 5px;\">Данные</th>\n" +
                "</tr>");
        String thStyle = "style=\"padding: 5px 30px 5px 10px;border-spacing: 0px;font-size: 90%;margin: 0px;text-align: left;background-color: #e0e9f0;border-top: 1px solid #f1f8fe;border-bottom: 1px solid #cbd2d8;border-right: 1px solid #cbd2d8;\"";
        String tdStyle = "style=\"padding: 5px 30px 5px 10px;border-spacing: 0px;font-size: 90%;margin: 0px;text-align: left;background-color: #e0e9f0;border-top: 1px solid #f1f8fe;border-bottom: 1px solid #cbd2d8;border-right: 1px solid #cbd2d8;text-shadow: 1px 1px 1px #ffffff;\"";
        for (Map.Entry<String, String> entry : map.entrySet()) {
            htmlBuilder.append(String.format("<tr><th %s>%s</th><td %s>%s</td></tr>",
                    thStyle, entry.getKey(), tdStyle, entry.getValue()));
        }
        htmlBuilder.append("</table>");
        return htmlBuilder.toString();
    }

    private String addStyle(String body) {
        return "<body>"+body+"</body>";
    }


}
